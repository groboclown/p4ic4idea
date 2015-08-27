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

package net.groboclown.idea.p4ic.v2.server.cache;

import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class AbstractServerUpdateAction implements ServerUpdateAction {
    public enum ExecutionStatus {
        PASS,
        RETRY,
        FAIL
    }


    private final Collection<PendingUpdateState> pendingUpdateStates;

    protected AbstractServerUpdateAction(@NotNull final Collection<PendingUpdateState> pendingUpdateStates) {
        this.pendingUpdateStates = pendingUpdateStates;
    }

    @NotNull
    @Override
    public Collection<PendingUpdateState> getPendingUpdateStates() {
        return pendingUpdateStates;
    }

    @Override
    public void perform(@NotNull final P4Exec2 exec, @NotNull final P4Server server,
            @NotNull ClientCacheManager clientCacheManager,
            @NotNull ServerConnection connection, @NotNull final AlertManager alerts) {
        final ExecutionStatus result = executeAction(exec, server, clientCacheManager, alerts);
        if (result == ExecutionStatus.PASS) {
            updateCache(server, clientCacheManager, alerts);
        } else if (result == ExecutionStatus.RETRY) {
            requeue(server, connection, alerts);
        }
        // failure: don't retry
    }

    /**
     * Run the action, and return PASS if it executed without an issue.
     *
     * @param exec server executor
     * @param server client/server description
     * @param alerts UI interactions
     * @return PASS if the execution worked correctly and the cache
     *   must be updated, FAIL if the execution did not work, the
     *   cache should not be updated, and the execution should
     *   not be rerun, and RETRY if the execution did not work,
     *   but it should be attempted again.  RETRY should only
     *   be performed for actions that are known to run
     *   in the queue.
     */
    @NotNull
    protected abstract ExecutionStatus executeAction(@NotNull final P4Exec2 exec, @NotNull final P4Server server,
            @NotNull ClientCacheManager clientCacheManager, @NotNull final AlertManager alerts);

    /**
     * Update the internal cache as necessary.  This includes both the server
     * and local cache.
     *
     * @param server client/server description
     * @param alerts UI interactions
     */
    protected abstract void updateCache(@NotNull final P4Server server, @NotNull ClientCacheManager clientCacheManager,
            @NotNull final AlertManager alerts);

    /**
     * Requeue this action if the execute did not happen correctly.
     *
     * @param server client/server description
     * @param connection server request handler
     * @param alerts UI interactions
     */
    protected void requeue(@NotNull final P4Server server, @NotNull final ServerConnection connection,
            @NotNull final AlertManager alerts)
    {
        connection.requeueAction(server, this);
    }
}
