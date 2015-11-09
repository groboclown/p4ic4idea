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
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public abstract class AbstractServerUpdateAction implements ServerUpdateAction {
    private static final Logger LOG = Logger.getInstance(AbstractServerUpdateAction.class);

    public enum ExecutionStatus {
        RELOAD_CACHE,
        NO_OP,
        RETRY,
        FAIL
    }


    private final Collection<PendingUpdateState> pendingUpdateStates;
    private final List<PendingUpdateState> failed = new ArrayList<PendingUpdateState>();
    private final List<PendingUpdateState> success = new ArrayList<PendingUpdateState>();


    protected AbstractServerUpdateAction(@NotNull Collection<PendingUpdateState> pendingUpdateStates) {
        this.pendingUpdateStates = new ArrayList<PendingUpdateState>(pendingUpdateStates);
    }

    @NotNull
    @Override
    public Collection<PendingUpdateState> getPendingUpdateStates() {
        return Collections.unmodifiableCollection(pendingUpdateStates);
    }

    @Override
    public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
            @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
            throws InterruptedException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing action " + getClass().getSimpleName() + " on " + pendingUpdateStates);
        }
        final ExecutionStatus result = executeAction(exec, clientCacheManager, alerts);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Result: " + result);
        }
        switch (result) {
            case NO_OP:
                // don't update
                break;
            case RELOAD_CACHE:
                LOG.debug("Updating the cache");
                ServerQuery query = updateCache(clientCacheManager, alerts);
                if (query != null) {
                    connection.query(exec.getProject(), query);
                }
                break;
            case FAIL:
                // don't retry, don't update
                break;
            case RETRY:
                LOG.debug("Retrying the action");
                pendingUpdateStates.removeAll(success);
                pendingUpdateStates.removeAll(failed);
                success.clear();
                failed.clear();
                requeue(exec.getProject(), connection, alerts);
                break;
            default:
                throw new IllegalStateException("invalid result " + result);
        }
    }


    @Override
    public final void abort(@NotNull final ClientCacheManager clientCacheManager) {
        for (PendingUpdateState update : pendingUpdateStates) {
            markFailed(update);
        }
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
            @NotNull AlertManager alerts) {
        connection.requeueAction(project, this);
    }

    protected void markSuccess(@NotNull PendingUpdateState update) {
        success.add(update);
    }

    protected void markSuccess(@NotNull Collection<PendingUpdateState> updates) {
        for (PendingUpdateState update : updates) {
            markSuccess(update);
        }
    }

    protected void markFailed(@NotNull PendingUpdateState update) {
        failed.add(update);
    }

    protected void markFailed(final Collection<PendingUpdateState> values) {
        for (PendingUpdateState update : values) {
            markFailed(update);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + pendingUpdateStates;
    }
}
