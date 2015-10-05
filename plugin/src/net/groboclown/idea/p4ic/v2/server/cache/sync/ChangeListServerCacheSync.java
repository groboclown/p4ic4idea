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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.server.P4Job;
import net.groboclown.idea.p4ic.v2.server.cache.AbstractServerUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.ServerUpdateActionFactory;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ChangeListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChangeListServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(FileActionsServerCacheSync.class);

    private final Cache cache;
    private final Set<P4ChangeListState> localClientChanges;
    private final Set<P4ChangeListState> cachedServerChanges;
    private final AtomicInteger previousLocalChangelistId = new AtomicInteger();
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

        // get the starting local changelist ID number.
        int lowestId = P4ChangeListId.P4_LOCAL;
        for (P4ChangeListState local: localClientChanges) {
            if (lowestId > local.getChangelistId()) {
                lowestId = local.getChangelistId();
            }
        }
        previousLocalChangelistId.set(lowestId);
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


    @Nullable
    public PendingUpdateState deleteChangelist(final int changeListId) {
        if (changeListId == P4ChangeListId.P4_UNKNOWN || changeListId == P4ChangeListId.P4_DEFAULT) {
            // can't delete these ones
            return null;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);
        return new PendingUpdateState(UpdateAction.DELETE_CHANGELIST,
                Collections.singleton(Integer.toString(changeListId)),
                params);
    }

    /**
     * Allocate a new ID without actually creating it.
     *
     * @return the next reserved changelist ID.
     */
    public int reserveLocalChangelistId() {
        // FIXME actually create the object; this requires thinking about the locks correctly.
        // Maybe move the local changelists into a synchronized array?

        return previousLocalChangelistId.addAndGet(-1);
    }


    @Nullable
    public PendingUpdateState moveFileToChangelist(@NotNull Collection<FilePath> files,
            @NotNull LocalChangeList source, final int changeListId) {
        if (changeListId == P4ChangeListId.P4_UNKNOWN) {
            throw new IllegalStateException("must create a changelist with a reserved local ID");
        }
        if (files.isEmpty()) {
            return null;
        }


        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);
        String description = source.getName();
        if (source.getComment() != null && source.getComment().length() > 0) {
            description += "\n\n" + source.getComment();
        }
        params.put(UpdateParameterNames.DESCRIPTION.getKeyName(), description);
        int index = 0;
        for (FilePath file: files) {
            params.put(UpdateParameterNames.FIELD.getKeyName() + index, file.getIOFile().getAbsolutePath());
            index++;
        }

        return new PendingUpdateState(UpdateAction.REOPEN_FILES_INTO_CHANGELIST,
                Collections.singleton(Integer.toString(changeListId)),
                params);
    }

    @Nullable
    public PendingUpdateState renameChangelist(final int changeListId, final String description) {
        if (changeListId == P4ChangeListId.P4_UNKNOWN || changeListId == P4ChangeListId.P4_DEFAULT) {
            return null;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);
        params.put(UpdateParameterNames.DESCRIPTION.getKeyName(), description);

        return new PendingUpdateState(UpdateAction.CHANGE_CHANGELIST_DESCRIPTION,
                Collections.singleton(Integer.toString(changeListId)),
                params);
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
        Set<P4ChangeListState> refreshed = new HashSet<P4ChangeListState>(pendingChanges.size());
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

    // =======================================================================
    // ACTIONS

    // Action classes must be static so that they can be correctly referenced
    // from the UpdateAction class.  They also must be fully executable
    // from the passed-in arguments and the pending state value.

    // The actions are also placed in here, rather than as stand-alone classes,
    // so that they have increased access to the cache objects.


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Add/Edit Action

    public static class DeleteFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new DeleteAction(states);
        }
    }

    static class DeleteAction extends AbstractServerUpdateAction {
        DeleteAction(final Collection<PendingUpdateState> states) {
            super(states);
        }

        @Nullable
        @Override
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createChangeListRefreshQuery();
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            ExecutionStatus ret = ExecutionStatus.NO_OP;
            final Collection<P4ChangeListValue> openedChanges = clientCacheManager.getCachedOpenedChanges();
            for (PendingUpdateState state: getPendingUpdateStates()) {
                final Integer changeListId = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                if (changeListId == null || changeListId == P4ChangeListId.P4_DEFAULT ||
                        changeListId == P4ChangeListId.P4_UNKNOWN) {
                    continue;
                }

                // See if the change state is known.
                P4ChangeListValue cachedValue = null;
                for (P4ChangeListValue value: openedChanges) {
                    if (value.getChangeListId() == changeListId) {
                        cachedValue = value;
                        break;
                    }
                }

                if ((cachedValue == null && changeListId > P4ChangeListId.P4_LOCAL) ||
                        (cachedValue != null && cachedValue.isDeleted() && cachedValue.isOnServer())) {
                    // The value either is not known but could exist on the server,
                    // or it's on the server and is locally marked for delete.
                    try {
                        exec.deletePendingChangelist(changeListId);
                        if (ret == ExecutionStatus.NO_OP) {
                            ret = ExecutionStatus.RELOAD_CACHE;
                        }
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.changelist.delete.failure", changeListId), e);
                        ret = ExecutionStatus.FAIL;
                    }
                } else {
                    // else the cached version had its "deleted" setting changed back, or is only locally stored
                    // and marked for delete, or it is not known and numbered like a locally stored change.
                    // In any case, ignore the change.
                    alerts.addNotice(P4Bundle.message("changelist.delete.ignored", changeListId), null);
                }
            }

            return ret;
        }
    }
}
