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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.async.BlockingAnswer;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.impl.cache.CacheQueryHandlerImpl;
import net.groboclown.p4.server.impl.cache.CacheStoreUpdateListener;
import net.groboclown.p4.server.impl.cache.IdeChangelistMapImpl;
import net.groboclown.p4.server.impl.cache.IdeFileMapImpl;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@State(
        name = "P4ProjectCache"
)
public class CacheComponent implements ProjectComponent, PersistentStateComponent<ProjectCacheStore.State> {
    private static final Logger LOG = Logger.getInstance(CacheComponent.class);

    private static final String COMPONENT_NAME = "Perforce Project Cached Data";
    private final Project project;
    private final ProjectCacheStore projectCache = new ProjectCacheStore();
    private IdeChangelistMap changelistMap;
    private CacheQueryHandler queryHandler;
    private CacheStoreUpdateListener updateListener;


    public static CacheComponent getInstance(Project project) {
        // a non-registered component can happen when the config is loaded outside a project.
        CacheComponent ret = null;
        if (project != null) {
            ret = project.getComponent(CacheComponent.class);
        }
        if (ret == null) {
            ret = new CacheComponent(project);
        }
        return ret;
    }

    @SuppressWarnings("WeakerAccess")
    public CacheComponent(Project project) {
        this.project = project;
    }

    public CacheQueryHandler getQueryHandler() {
        return queryHandler;
    }

    /**
     * Force a cache refresh on the opened server objects (changelists and files).
     *
     * @return the pending answer.
     */
    private Answer<Pair<IdeChangelistMap, IdeFileMap>> refreshServerOpenedCache(Collection<ClientConfig> clients) {
        Answer<?> ret = Answer.resolve(null);

        for (ClientConfig client : clients) {
            ret = ret.mapAsync((x) ->
            Answer.background((sink) -> P4ServerComponent.getInstance(project).getCommandRunner().syncQuery(
                    client,
                    new SyncListOpenedFilesChangesQuery(
                            UserProjectPreferences.getMaxChangelistRetrieveCount(project),
                            UserProjectPreferences.getMaxFileRetrieveCount(project))).getPromise()
            .whenCompleted((changesResult) -> {
                updateListener.setOpenedChanges(
                        changesResult.getClientConfig().getClientServerRef(),
                        changesResult.getPendingChangelists(), changesResult.getOpenedFiles());
                sink.resolve(null);
            })
            .whenServerError(sink::reject)));
        }

        return ret.map((x) -> getServerOpenedCache());
    }

    public Pair<IdeChangelistMap, IdeFileMap> getServerOpenedCache() {
        // TODO should the mapping be internal, rather than created on the fly?
        return new Pair<>(
                changelistMap,
                new IdeFileMapImpl(project, queryHandler));
    }

    public Pair<IdeChangelistMap, IdeFileMap> blockingRefreshServerOpenedCache(Collection<ClientConfig> clients,
            int timeout, TimeUnit timeoutUnit) {
        try {
            return BlockingAnswer.defaultBlockingGet(refreshServerOpenedCache(clients), timeout, timeoutUnit,
                    this::getServerOpenedCache);
        } catch (P4CommandRunner.ServerResultException e) {
            LOG.info(e);
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

    @Override
    public void initComponent() {
        queryHandler = new CacheQueryHandlerImpl(projectCache);
        changelistMap = new IdeChangelistMapImpl(project, queryHandler, projectCache.getChangelistCacheStore());
        updateListener = new CacheStoreUpdateListener(project, projectCache, changelistMap);
    }

    @Override
    public void disposeComponent() {
        if (queryHandler != null) {
            queryHandler = null;
        }
        if (updateListener != null) {
            updateListener.dispose();
            updateListener = null;
        }
    }

    @Nullable
    @Override
    public ProjectCacheStore.State getState() {
        try {
            return projectCache.getState();
        } catch (InterruptedException e) {
            LOG.warn("Timed out while trying to access the cache.  Could not serialize cache state.", e);
            return null;
        }
    }

    @Override
    public void loadState(ProjectCacheStore.State state) {
        try {
            this.projectCache.setState(state);
        } catch (InterruptedException e) {
            LOG.warn("Timed out while writing to the cache.  Could not deserialize the cache state.", e);
        }
    }

    @Override
    public void noStateLoaded() {
        // do nothing
    }
}
