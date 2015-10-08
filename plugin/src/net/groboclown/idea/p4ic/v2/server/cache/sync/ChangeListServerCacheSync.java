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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListIdImpl;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.cache.*;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ChangeListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChangeListServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(ChangeListServerCacheSync.class);

    private final Cache cache;
    private final Set<P4ChangeListState> localClientChanges;
    private final Set<P4ChangeListState> cachedServerChanges;
    private final Set<Integer> committed = new HashSet<Integer>();
    private int previousLocalChangelistId = 0;
    private final Object localCacheSync = new Object();
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
        previousLocalChangelistId = lowestId;
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

        synchronized (localCacheSync) {
            for (P4ChangeListState change : localClientChanges) {
                if (change.isDeleted()) {
                    ret.remove(change.getChangelistId());
                } else {
                    ret.put(change.getChangelistId(), new P4ChangeListValue(cache.getClientServerId(), change));
                }
            }
        }

        return ret.values();
    }


    @Nullable
    public PendingUpdateState deleteChangelist(final int changeListId) {
        if (changeListId == P4ChangeListId.P4_UNKNOWN || changeListId == P4ChangeListId.P4_DEFAULT) {
            // can't delete these ones
            return null;
        }

        // FIXME put in local cache, and remove from committed

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
        synchronized (localCacheSync) {
            previousLocalChangelistId -= 1;

            P4ChangeListState tempState = new P4ChangeListState(previousLocalChangelistId);
            localClientChanges.add(tempState);

            return previousLocalChangelistId;
        }
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

        String description = setDescription(changeListId, source);

        // FIXME remove from committed

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);
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

        // FIXME put in local cache, and remove from committed

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
                        final P4JobState job = exec.getJobForId(jobId);
                        if (job != null) {
                            state.addJob(job);
                        }
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

        // clean up our local cache
        final Iterator<P4ChangeListState> iter = localClientChanges.iterator();
        while (iter.hasNext()) {
            final P4ChangeListState next = iter.next();
            if (committed.remove(next.getChangelistId())) {
                iter.remove();
            }
        }
        committed.clear();
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshDate;
    }


    void markLocalStateCommitted(final int changeListId) {
        synchronized (localCacheSync) {
            committed.add(changeListId);
        }
    }


    /**
     * Gets a locally modified version of the changelist state.  If the
     * state is only server cached, then a local copy is made.  If the
     * state is not in either one, it is created.
     *
     * @param changeListId changelist id
     * @return local changelist state
     */
    @NotNull
    private P4ChangeListState getOrAddChangeState(int changeListId) {
        synchronized (localCacheSync) {
            for (P4ChangeListState local : localClientChanges) {
                if (local.getChangelistId() == changeListId) {
                    return local;
                }
            }
            for (P4ChangeListState remote : cachedServerChanges) {
                if (remote.getChangelistId() == changeListId) {
                    P4ChangeListState local = new P4ChangeListState(remote);
                    localClientChanges.add(local);
                    return local;
                }
            }
            LOG.info("Could not find a changelist numbered " + changeListId + "; creating one");
            P4ChangeListState local = new P4ChangeListState(changeListId);
            localClientChanges.add(local);
            return local;
        }
    }


    @NotNull
    private String setDescription(final int changeListId, @NotNull LocalChangeList source) {
        // FIXME there are other places where the description is joined together.
        // Put them all here, or at least in one place.
        String description = source.getName();
        if (source.getComment() != null && source.getComment().length() > 0) {
            description += "\n\n" + source.getComment();
        }
        final P4ChangeListState local = getOrAddChangeState(changeListId);
        local.setComment(description);
        return description;
    }

    @NotNull
    public Collection<P4ChangeListJob> getJobsInChangelists(@NotNull final Collection<P4ChangeListId> changes) {
        final Collection<P4ChangeListValue> allChanges = getOpenedChangeLists();
        Set<P4ChangeListJob> ret = new HashSet<P4ChangeListJob>();
        for (P4ChangeListValue change : allChanges) {
            for (P4ChangeListId changeId: changes) {
                if (change.getChangeListId() == changeId.getChangeListId() &&
                        change.getClientServerId().equals(changeId.getClientServerId())) {
                    for (P4JobState job: change.getJobStates()) {
                        ret.add(new P4ChangeListJob(change, job));
                    }
                }
            }
        }
        return ret;
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
    // Delete Action

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


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Delete Action

    public static class MoveFileFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new MoveFileAction(states);
        }
    }


    static class MoveFileAction extends AbstractServerUpdateAction {
        MoveFileAction(final Collection<PendingUpdateState> states) {
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
            final P4ChangeListMapping changeListMapping = P4ChangeListMapping.getInstance(exec.getProject());
            final ClientServerId clientServerId = ClientServerId.create(exec.getServerConfig(), exec.getClientName());
            Map<Integer, P4ChangeListValue> changeListMap = new HashMap<Integer, P4ChangeListValue>();
            for (P4ChangeListValue value: clientCacheManager.getCachedOpenedChanges()) {
                changeListMap.put(value.getChangeListId(), value);
            }

            // FIXME update the localClientChanges list.


            for (PendingUpdateState state : getPendingUpdateStates()) {
                final Integer changeListId = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                final String description = UpdateParameterNames.DESCRIPTION.getParameterValue(state);
                final List<FilePath> files = new ArrayList<FilePath>();
                for (Map.Entry<String, Object> entry: state.getParameters().entrySet()) {
                    if (UpdateParameterNames.FIELD.matches(entry.getKey())) {
                        files.add(FilePathUtil.getFilePath(
                                (String) UpdateParameterNames.FIELD.getValue(entry.getValue())));
                    }
                }
                if (changeListId == null || files.isEmpty() || description == null) {
                    alerts.addNotice(P4Bundle.message("pendingupdatestate.invalid", state), null);
                    continue;
                }

                int realChangeListId = changeListId;
                if (realChangeListId <= P4ChangeListId.P4_LOCAL) {
                    // create a new changelist and force the update across the files
                    try {
                        final IChangelist changelist = exec.createChangeList(description);
                        realChangeListId = changelist.getId();
                        P4ChangeListId oldCl = new P4ChangeListIdImpl(clientServerId, changeListId);
                        P4ChangeListId newCl = new P4ChangeListIdImpl(clientServerId, realChangeListId);
                        changeListMapping.replace(oldCl, newCl);
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.createchangelist", description), e);
                        // cannot actually perform the action
                        ret = ExecutionStatus.FAIL;
                        continue;
                    }

                    ret = ExecutionStatus.RELOAD_CACHE;
                } else {
                    final P4ChangeListValue changeList = changeListMap.get(realChangeListId);
                    // getComment can be null...
                    if (changeList != null && !Comparing.equal(description, changeList.getComment())) {
                        try {
                            exec.updateChangelistDescription(realChangeListId, description);
                            ret = ExecutionStatus.RELOAD_CACHE;
                        } catch (VcsException e) {
                            alerts.addWarning(P4Bundle.message("error.updatechangelist", realChangeListId), e);
                            // cannot actually perform the action
                            ret = ExecutionStatus.FAIL;
                            continue;
                        }
                    }
                }

                final List<IExtendedFileSpec> status;
                try {
                    status = exec.getFileStatus(FileSpecUtil.getFromFilePaths(files));
                } catch (VcsException e) {
                    alerts.addWarning(P4Bundle.message("filestatus.error", files), e);
                    continue;
                }
                if (status.isEmpty()) {
                    continue;
                }

                List<IFileSpec> reopen = new ArrayList<IFileSpec>(status.size());
                List<IFileSpec> edit = new ArrayList<IFileSpec>(status.size());
                List<IFileSpec> add = new ArrayList<IFileSpec>(status.size());

                for (IExtendedFileSpec spec: status) {
                    if (spec.getOpenAction() != null || spec.getAction() != null) {
                        if (spec.getOpenChangelistId() != changeListId) {
                            // already opened; reopen it
                            reopen.add(spec);
                        } else {
                            LOG.info("Ignoring reopen request for " + spec.getDepotPathString() + "; already in the right changelist");
                        }
                    } else if (spec.getHeadRev() <= 0) {
                        // add
                        add.add(spec);
                    } else {
                        LOG.info("Marking as edit: " + spec +
                            "; action: " + spec.getOpenAction() + "/" + spec.getAction() + "/" + spec.getHeadAction() + "/" + spec.getOtherAction() +
                            "; change: " + spec.getOpenChangelistId());
                        edit.add(spec);
                    }
                }

                if (! reopen.isEmpty()) {
                    LOG.info("Reopening files into " + realChangeListId + ": " + reopen);
                    try {
                        alerts.addWarnings(
                                P4Bundle.message("error.reopen", reopen),
                                exec.reopenFiles(reopen, realChangeListId, null),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.reopen", reopen), e);
                        // keep going
                    }
                }

                if (! edit.isEmpty()) {
                    LOG.info("Editing files into " + realChangeListId + ": " + edit);
                    try {
                        alerts.addWarnings(
                                P4Bundle.message("error.edit", edit),
                                exec.editFiles(edit, realChangeListId),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.edit", edit), e);
                        // keep going
                    }
                }

                if (!add.isEmpty()) {
                    LOG.info("Adding files into " + realChangeListId + ": " + add);
                    try {
                        alerts.addWarnings(
                                P4Bundle.message("error.add", add),
                                exec.addFiles(add, realChangeListId),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.add", add), e);
                        // keep going
                    }
                }

                clientCacheManager.markLocalChangelistStateCommitted(changeListId);
                clientCacheManager.markLocalChangelistStateCommitted(realChangeListId);
            }

            // FIXME ensure the file actions are refreshed with the new changelist.

            return ret;
        }
    }

}
