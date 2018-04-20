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

package net.groboclown.p4.server.cache.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.cache.UpdateGroup;
import net.groboclown.p4.server.cache.state.PendingUpdateState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Package-level marker for the cache front ends.  This just enforces
 * that all the front ends conform to the same API.
 */
abstract class CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(CacheFrontEnd.class);


    // TODO make this configurable
    static final long MIN_REFRESH_INTERVAL_MS = 1000L;


    final ServerQuery<CacheFrontEnd> createRefreshQuery(final boolean forceRefresh) {
        return new ServerQuery<CacheFrontEnd>() {
            @Nullable
            @Override
            public CacheFrontEnd query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                    @NotNull final ServerConnection connection, @NotNull SynchronizedActionRunner runner,
                    @NotNull final AlertManager alerts)
                    throws InterruptedException {
                ServerConnection.assertInServerConnection();
                // TODO pass in the syncRunner?
                loadServerCache(exec, cacheManager, alerts, forceRefresh);
                return CacheFrontEnd.this;
            }
        };
    }


    private void loadServerCache(@NotNull P4Exec2 exec, @NotNull ClientCacheManager cacheManager,
            @NotNull AlertManager alerts, boolean forceRefresh) {
        if (forceRefresh || needsRefresh()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Refreshing the cache for " +
                        getClass().getSimpleName() + "; last refresh was " +
                        getLastRefreshDate());
            }
            innerLoadServerCache(exec, alerts);
            final List<PendingUpdateState> updates = new ArrayList<PendingUpdateState>();
            for (PendingUpdateState updateState : cacheManager.getCachedPendingUpdates()) {
                if (getSupportedUpdateGroups().contains(updateState.getUpdateGroup())) {
                    updates.add(updateState);
                }
            }
            rectifyCache(exec.getProject(), updates, alerts);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("No need to refresh the cache for " + getClass().getSimpleName() + "; last refresh was " + getLastRefreshDate());
        }
    }

    /**
     * Reload the underlying server cache.  Only called when the client is online.
     *
     * @param exec connected server API
     * @param alerts user message handler
     */
    protected abstract void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts);

    /**
     * Fix the local cache to be in-line with
     * Called after the inner cache has been loaded from the server,
     * so the server cache is up-to-date.
     *
     * @param project project invoking
     * @param pendingUpdateStates all pending updates that have a supported
     *                            group for this class.
     * @param alerts alerts
     */
    protected abstract void rectifyCache(@NotNull Project project,
            @NotNull Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull AlertManager alerts);

    @NotNull
    protected abstract Collection<UpdateGroup> getSupportedUpdateGroups();


    boolean needsRefresh() {
        return (getLastRefreshDate().getTime() + MIN_REFRESH_INTERVAL_MS < System.currentTimeMillis());
    }


    @NotNull
    protected abstract Date getLastRefreshDate();

    protected abstract void checkLocalIntegrity(@NotNull List<PendingUpdateState> pendingUpdates);
}
