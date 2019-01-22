/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.p4plugin.components;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.async.BlockingAnswer;
import net.groboclown.p4.server.api.cache.CachePendingActionHandler;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.impl.cache.CachePendingActionHandlerImpl;
import net.groboclown.p4.server.impl.cache.CacheQueryHandlerImpl;
import net.groboclown.p4.server.impl.cache.CacheStoreUpdateListener;
import net.groboclown.p4.server.impl.cache.IdeChangelistMapImpl;
import net.groboclown.p4.server.impl.cache.IdeFileMapImpl;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(
        name = "p4-ProjectCache",
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )
        }
)
public class CacheComponent implements
        ProjectComponent, PersistentStateComponent<ProjectCacheStore.State>,
        Disposable {
    private static final Logger LOG = Logger.getInstance(CacheComponent.class);

    private static final String COMPONENT_NAME = "Perforce Project Cached Data";

    private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    private static final AtomicInteger CREATION_COUNT = new AtomicInteger(0);
    private static final String SERIALIZE_CACHE_TIMEOUT_MESSAGE =
            "Timed out while trying to access the cache.  Could not serialize cache state.";
    private static final String DESERIALIZE_CACHE_TIMEOUT_MESSAGE =
            "Timed out while writing to the cache.  Could not deserialize the cache state.";

    @Nullable
    private final Project project;
    private final int instanceId;


    private final ProjectCacheStore projectCache = new ProjectCacheStore();
    private IdeChangelistMap changelistMap;
    private IdeFileMap fileMap;
    private CacheQueryHandler queryHandler;
    private CachePendingActionHandler pendingHandler;
    private CacheStoreUpdateListener updateListener;
    private boolean disposed = false;


    @NotNull
    public static CacheComponent getInstance(@Nullable Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        CacheComponent ret;
        if (project != null) {
            ret = project.getComponent(CacheComponent.class);
            if (ret == null) {
                LOG.error("No CacheComponent registered project.  Was the load out of order?");
                throw new IllegalStateException();
            }
        } else {
            // Potentially hazardous situation.  It means multiple cache stores
            // floating around.  It also means that, if it's a short-term store, that
            // the data is not going to be thrown away.
            ret = new CacheComponent(null);
        }
        return ret;
    }

    public CacheComponent(@Nullable Project project) {
        // See #193 - this is a good contender for tracking memory usage.
        this.instanceId = CREATION_COUNT.incrementAndGet();
        this.project = project;
        final int postCreationCount = ACTIVE_COUNT.incrementAndGet();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created " + this + " for " + project + "; " + postCreationCount
                    + " total now active in memory");
        }
    }

    // See #193
    @Override
    protected void finalize() throws Throwable {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finalized " + this);
        }
        super.finalize();
    }

    @NotNull
    public CacheQueryHandler getCacheQuery() {
        // This can happen when creating a project from version control when the project isn't configured yet.
        initComponent();
        return queryHandler;
    }

    public CachePendingActionHandler getCachePending() {
        initComponent();
        return pendingHandler;
    }

    /**
     * Force a cache refresh on the opened server objects (changelists and files).
     *
     * @return the pending answer.
     */
    private Answer<Pair<IdeChangelistMap, IdeFileMap>> refreshServerOpenedCache(Collection<ClientConfigRoot> clients) {
        initComponent();
        if (project == null) {
            // Null project cannot have anything to refresh.
            throw new IllegalStateException("project not set for call");
        }
        Answer<?> ret = Answer.resolve(null);

        for (ClientConfigRoot clientRoot : clients) {
            final File root = clientRoot.getClientRootDir() != null
                    ? VcsUtil.getFilePath(clientRoot.getClientRootDir()).getIOFile()
                    : null;

            ret = ret.mapAsync((x) ->
            Answer.background((sink) -> P4ServerComponent.syncQuery(project,
                    clientRoot.getClientConfig(),
                    new SyncListOpenedFilesChangesQuery(
                            root,
                            UserProjectPreferences.getMaxChangelistRetrieveCount(project),
                            UserProjectPreferences.getMaxFileRetrieveCount(project))).getPromise()
            .whenCompleted((changesResult) -> {
                // TODO is this a duplicate call for the updateListener's CacheListener?
                updateListener.setOpenedChanges(
                        changesResult.getClientConfig().getClientServerRef(),
                        changesResult.getPendingChangelists(), changesResult.getOpenedFiles());
                sink.resolve(null);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + " opened cache refreshed; " + clientRoot.getClientRootDir()
                            + " contains " + changelistMap.getEstimateCount() + " pending changes, "
                            + fileMap.getEstimateSize() + " opened files.");
                }
            })
            .whenServerError(sink::reject)));
        }

        return ret.map((x) -> getServerOpenedCache());
    }

    /**
     *
     * @return the opened cache pair.  The values can be null if the cache has not yet been initialized.
     */
    @NotNull
    public synchronized Pair<IdeChangelistMap, IdeFileMap> getServerOpenedCache() {
        return new Pair<>(changelistMap, fileMap);
    }

    /**
     *
     * @return the opened cache pair.  The values can be null if the cache has not yet been initialized.
     */
    public Pair<IdeChangelistMap, IdeFileMap> blockingRefreshServerOpenedCache(Collection<ClientConfigRoot> clients,
            int timeout, TimeUnit timeoutUnit) {
        try {
            return BlockingAnswer.defaultBlockingGet(refreshServerOpenedCache(clients), timeout, timeoutUnit,
                    this::getServerOpenedCache);
        } catch (P4CommandRunner.ServerResultException e) {
            // User error handling is done through events.
            LOG.debug(e);
            return getServerOpenedCache();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    // Synchronized to help prevent double event listener registration.
    @Override
    public synchronized void initComponent() {
        if (isDisposed()) {
            throw new IllegalStateException("already disposed");
        }
        if (queryHandler == null) {
            queryHandler = new CacheQueryHandlerImpl(project, projectCache);
        }
        if (pendingHandler == null) {
            pendingHandler = new CachePendingActionHandlerImpl(projectCache);
        }
        if (changelistMap == null && project != null) {
            changelistMap = new IdeChangelistMapImpl(project, projectCache.getChangelistCacheStore());
        }
        if (fileMap == null && project != null) {
            fileMap = new IdeFileMapImpl(project, queryHandler);
        }
        if (updateListener == null && project != null) {
            updateListener = new CacheStoreUpdateListener(project, projectCache, this);
        }
    }

    @Override
    public synchronized void disposeComponent() {
        dispose();
    }

    @Nullable
    @Override
    public ProjectCacheStore.State getState() {
        try {
            return projectCache.getState();
        } catch (InterruptedException e) {
            if (project != null) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException(SERIALIZE_CACHE_TIMEOUT_MESSAGE, e)));
            } else {
                LOG.warn(SERIALIZE_CACHE_TIMEOUT_MESSAGE, e);
            }
            return null;
        }
    }

    @Override
    public void loadState(ProjectCacheStore.State state) {
        try {
            this.projectCache.setState(state);
        } catch (InterruptedException e) {
            if (project != null) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException(DESERIALIZE_CACHE_TIMEOUT_MESSAGE, e)));
            } else {
                LOG.warn(DESERIALIZE_CACHE_TIMEOUT_MESSAGE, e);
            }
        }
    }

    @Override
    public void noStateLoaded() {
        // do nothing
    }

    @Override
    public void dispose() {
        if (!disposed) {
            this.disposed = true;
            Disposer.dispose(this);
            changelistMap = null;
            fileMap = null;
            queryHandler = null;
            pendingHandler = null;
            updateListener = null;

            int postReleaseCount = ACTIVE_COUNT.decrementAndGet();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Disposed " + this + " for " + project + "; " + postReleaseCount
                        + " total still in memory");
            }
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public String toString() {
        return "CacheComponent#" + instanceId;
    }
}
