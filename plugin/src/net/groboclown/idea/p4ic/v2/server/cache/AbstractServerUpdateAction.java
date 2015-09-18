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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class AbstractServerUpdateAction implements ServerUpdateAction {
    private static final Logger LOG = Logger.getInstance(AbstractServerUpdateAction.class);

    public enum ExecutionStatus {
        RELOAD_CACHE,
        NO_OP,
        RETRY,
        FAIL
    }


    private final Collection<PendingUpdateState> pendingUpdateStates;

    protected AbstractServerUpdateAction(@NotNull Collection<PendingUpdateState> pendingUpdateStates) {
        this.pendingUpdateStates = pendingUpdateStates;
    }

    @NotNull
    @Override
    public Collection<PendingUpdateState> getPendingUpdateStates() {
        return pendingUpdateStates;
    }

    @Override
    public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
            @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
            throws InterruptedException {
        // FIXME debug
        LOG.info("Performing action " + getClass().getSimpleName() + " on " + pendingUpdateStates);
        final ExecutionStatus result = executeAction(exec, clientCacheManager, alerts);
        LOG.info("Result: " + result);
        if (result == ExecutionStatus.RELOAD_CACHE) {
            LOG.info("Updating the cache");
            ServerQuery query = updateCache(clientCacheManager, alerts);
            if (query != null) {
                connection.query(exec.getProject(), query);
            }
        } else if (result == ExecutionStatus.RETRY) {
            LOG.info("Retrying the action");
            requeue(exec.getProject(), connection, alerts);
        }
        // failure: don't retry, don't update
        // no-op: don't update
    }

    /**
     * Run the action, and return RELOAD_CACHE if it executed without an issue.
     *
     * @param exec server executor
     * @param alerts UI interactions
     * @return RELOAD_CACHE if the execution worked correctly and the cache
     *   must be updated, FAIL if the execution did not work, the
     *   cache should not be updated, and the execution should
     *   not be rerun, and RETRY if the execution did not work,
     *   but it should be attempted again.  RETRY should only
     *   be performed for actions that are known to run
     *   in the queue.
     */
    @NotNull
    protected abstract ExecutionStatus executeAction(@NotNull P4Exec2 exec,
            @NotNull ClientCacheManager clientCacheManager, @NotNull AlertManager alerts);

    /**
     * CreateUpdate the internal cache as necessary.  This includes both the server
     * and local cache.
     *
     * @param alerts UI interactions
     * @return the query to run
     */
    @Nullable
    protected abstract ServerQuery updateCache(@NotNull ClientCacheManager clientCacheManager,
            @NotNull AlertManager alerts);

    /**
     * Requeue this action if the execute did not happen correctly.
     *
     * @param connection server request handler
     * @param alerts UI interactions
     */
    protected void requeue(@NotNull Project project, @NotNull final ServerConnection connection,
            @NotNull AlertManager alerts)
    {
        connection.requeueAction(project, this);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + pendingUpdateStates;
    }
}
