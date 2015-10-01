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
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.server.P4Job;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ChangeListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChangeListServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(FileActionsServerCacheSync.class);

    private final Cache cache;
    private final Set<P4ChangeListState> localClientChanges;
    private final Set<P4ChangeListState> cachedServerChanges;
    private Date lastRefreshDate;

    public ChangeListServerCacheSync(final Cache cache,
            final Set<P4ChangeListState> localClientChanges,
            final Set<P4ChangeListState> cachedServerChanges) {
        this.cache = cache;
        this.localClientChanges = localClientChanges;
        this.cachedServerChanges = cachedServerChanges;

        lastRefreshDate = CachedState.NEVER_LOADED;
        for (P4ChangeListState server: cachedServerChanges) {
            if (lastRefreshDate.before(server.getLastUpdated())) {
                lastRefreshDate = server.getLastUpdated();
            }
        }
    }


    @NotNull
    public Collection<P4ChangeListValue> getOpenedChangeLists() {
        Map<Integer, P4ChangeListValue> ret = new HashMap<Integer, P4ChangeListValue>();

        // Load up the cached server changes, then overlay with the
        // local changes.

        // Ensure the default changelist is included.  This will be
        ret.put(P4ChangeListId.P4_DEFAULT, new P4ChangeListValue(cache.getClientServerId(), P4ChangeListId.P4_DEFAULT));

        for (P4ChangeListState change: cachedServerChanges) {
            ret.put(change.getChangelistId(), new P4ChangeListValue(cache.getClientServerId(), change));
        }

        for (P4ChangeListState change: localClientChanges) {
            ret.put(change.getChangelistId(), new P4ChangeListValue(cache.getClientServerId(), change));
        }

        return ret.values();
    }


    @Override
    protected void innerLoadServerCache(@NotNull final P4Exec2 exec, @NotNull final AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        // Load our server cache.

        final List<IChangelistSummary> pendingChanges;
        try {
            pendingChanges = exec.getPendingClientChangelists();
        } catch (VcsException e) {
            alerts.addWarning(
                    P4Bundle.message("error.getPendingClientChangelists", exec.getClientName()), e);
            return;
        }
        lastRefreshDate = new Date();
        List<P4ChangeListState> refreshed = new ArrayList<P4ChangeListState>(pendingChanges.size());
        boolean foundDefault = false;
        for (IChangelistSummary pendingChange : pendingChanges) {
            // FIXME include job fix state

            P4ChangeListState state = new P4ChangeListState(pendingChange);
            refreshed.add(state);
            if (state.getChangelistId() == P4ChangeListId.P4_DEFAULT) {
                foundDefault = true;
            }
            try {
                final Collection<String> jobs = exec.getJobIdsForChangelist(state.getChangelistId());
                if (jobs != null) {
                    for (String jobId : jobs) {
                        final P4Job job = exec.getJobForId(jobId);
                        state.addJob(new P4JobState(job));
                    }
                }
            } catch (VcsException e) {
                alerts.addNotice(P4Bundle.message("error.getJobIdsForChangelist",
                        pendingChange.getId(), exec.getClientName()), e);
            }
        }
        if (! foundDefault) {
            // This generally is always entered.
            refreshed.add(new P4ChangeListState(P4ChangeListId.P4_DEFAULT));
        }
        cachedServerChanges.clear();
        cachedServerChanges.addAll(refreshed);
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshDate;
    }
}
