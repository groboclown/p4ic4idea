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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import net.groboclown.idea.p4ic.v2.server.cache.state.JobStatusListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryState;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryStateList;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Keeps track of all the job status states.  It does not have any ability to have
 * the client change those; that's the pervue of the admin, and must be done
 * meticulously, not just willy-nilly in the IDE.
 */
public class UserSummaryListStateServerCacheSync
        extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(UserSummaryListStateServerCacheSync.class);
    private static final long DAY_INTERVAL_MS = 1000L * 60L * 60L * 24L;

    private final UserSummaryStateList cachedServerState;


    UserSummaryListStateServerCacheSync(@NotNull final UserSummaryStateList cachedServerState) {
        this.cachedServerState = cachedServerState;
    }

    @NotNull
    public Collection<UserSummaryState> getUserSummaryStateList() {
        return Collections.unmodifiableCollection(cachedServerState.copy().values());
    }


    @Override
    protected void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        try {
            cachedServerState.setUsers(exec.getUsers(false, 1000000));
        } catch (VcsException e) {
            alerts.addWarning(exec.getProject(),
                    P4Bundle.getString("exception.user-fetch"),
                    P4Bundle.getString("exception.user-fetch"),
                    e, new FilePath[0]);
        }
    }

    @Override
    protected void rectifyCache(@NotNull final Project project,
            @NotNull final Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull final AlertManager alerts) {
        // Nothing to do
        // Users aren't modified by the plugin.
    }

    @NotNull
    @Override
    protected Collection<UpdateGroup> getSupportedUpdateGroups() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return cachedServerState.getLastUpdated();
    }

    @Override
    protected void checkLocalIntegrity(@NotNull final List<PendingUpdateState> pendingUpdates) {
        // nothing to do, because there are no local changed versions
    }

    // This updaes so infrequently that there's no need to update more than once a day.
    @Override
    boolean needsRefresh() {
        return (getLastRefreshDate().getTime() + DAY_INTERVAL_MS < System.currentTimeMillis());
    }
}
