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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * Package-level marker for the cache front ends.  This just enforces
 * that all the front ends conform to the same API.
 */
abstract class CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(CacheFrontEnd.class);


    // FIXME make this configurable
    private static final long MIN_REFRESH_INTERVAL_MS = 1000L;


    protected final ServerQuery<CacheFrontEnd> createRefreshQuery() {
        return new ServerQuery<CacheFrontEnd>() {
            @Nullable
            @Override
            public CacheFrontEnd query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                ServerConnection.assertInServerConnection();
                loadServerCache(exec, alerts);
                return CacheFrontEnd.this;
            }
        };
    }


    protected final void loadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        if (needsRefresh()) {
            LOG.info("Refreshing the cache for " + getClass().getSimpleName() + "; last refresh was " + getLastRefreshDate());
            innerLoadServerCache(exec, alerts);
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


    boolean needsRefresh() {
        return (getLastRefreshDate().getTime() + MIN_REFRESH_INTERVAL_MS < System.currentTimeMillis());
    }


    @NotNull
    protected abstract Date getLastRefreshDate();
}
