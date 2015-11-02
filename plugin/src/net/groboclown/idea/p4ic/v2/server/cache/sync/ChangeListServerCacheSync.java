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
import java.util.Map.Entry;

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
    public PendingUpdateState deleteChangelist(final int changelistId) {
        if (changelistId == P4ChangeListId.P4_UNKNOWN || changelistId == P4ChangeListId.P4_DEFAULT) {
            // can't delete these ones
            return null;
        }

        final P4ChangeListState state = new P4ChangeListState(changelistId);
        state.setDeleted(true);
        localClientChanges.add(state);
        committed.remove(changelistId);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);
        return new PendingUpdateState(UpdateAction.DELETE_CHANGELIST,
                Collections.singleton(Integer.toString(changelistId)),
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
    public PendingUpdateState renameChangelist(final int changelistId, final String description) {
        if (changelistId == P4ChangeListId.P4_UNKNOWN || changelistId == P4ChangeListId.P4_DEFAULT) {
            return null;
        }

        // Note that in the world of possibilities, this could potentially
        // be called when the changelist is already registered as deleted.
        // However, that shouldn't be likely.  In this situation, the
        // changelist will no longer be cached as deleted, but the pending
        // update could still exist.

        final P4ChangeListState state = new P4ChangeListState(changelistId);
        state.setComment(description);
        localClientChanges.add(state);
        committed.remove(changelistId);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);
        params.put(UpdateParameterNames.DESCRIPTION.getKeyName(), description);

        return new PendingUpdateState(UpdateAction.CHANGE_CHANGELIST_DESCRIPTION,
                Collections.singleton(Integer.toString(changelistId)),
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
            alerts.addWarning(exec.getProject(),
                    P4Bundle.message("error.getPendingClientChangelists", exec.getClientName()),
                    P4Bundle.message("error.getPendingClientChangelists", exec.getClientName()),
                    e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
            return;
        }
        lastRefreshDate = new Date();
        Set<P4ChangeListState> refreshed = new HashSet<P4ChangeListState>(pendingChanges.size());
        boolean foundDefault = false;
        for (IChangelistSummary pendingChange : pendingChanges) {
            // TODO include job fix state
            // It's attached to the IServer / IFix classes.

            P4ChangeListState state = new P4ChangeListState(pendingChange);
            refreshed.add(state);
            if (state.getChangelistId() == P4ChangeListId.P4_DEFAULT) {
                foundDefault = true;
            }
            try {
                final Collection<String> jobs = exec.getJobIdsForChangelist(state.getChangelistId());
                if (jobs != null) {
                    cache.refreshJobState(exec, alerts, jobs);
                    for (String jobId : jobs) {
                        final P4JobState job = exec.getJobForId(jobId);
                        if (job != null) {
                            state.addJob(job);
                        }
                    }
                }
            } catch (VcsException e) {
                alerts.addNotice(
                        exec.getProject(),
                        P4Bundle.message("error.getJobIdsForChangelist",
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
        // TODO there are other places where the description is joined together.
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


    static abstract class AbstractChangelistAction extends AbstractServerUpdateAction {


        protected AbstractChangelistAction(@NotNull final Collection<PendingUpdateState> pendingUpdateStates) {
            super(pendingUpdateStates);
        }

        @Override
        public void abort(@NotNull final ClientCacheManager clientCacheManager) {
            for (PendingUpdateState state : getPendingUpdateStates()) {
                final Integer changelistId = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                if (changelistId != null) {
                    clientCacheManager.markLocalChangelistStateCommitted(changelistId);
                }
            }
        }
    }


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

    static class DeleteAction extends AbstractChangelistAction {
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
                final Integer changelistId = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                if (changelistId == null || changelistId == P4ChangeListId.P4_DEFAULT ||
                        changelistId == P4ChangeListId.P4_UNKNOWN) {
                    continue;
                }

                // See if the change state is known.
                P4ChangeListValue cachedValue = null;
                for (P4ChangeListValue value: openedChanges) {
                    if (value.getChangeListId() == changelistId) {
                        cachedValue = value;
                        break;
                    }
                }

                if ((cachedValue == null && changelistId > P4ChangeListId.P4_LOCAL) ||
                        (cachedValue != null && cachedValue.isDeleted() && cachedValue.isOnServer())) {
                    // The value either is not known but could exist on the server,
                    // or it's on the server and is locally marked for delete.
                    try {
                        exec.deletePendingChangelist(changelistId);
                        if (ret == ExecutionStatus.NO_OP) {
                            ret = ExecutionStatus.RELOAD_CACHE;
                        }
                        clientCacheManager.markLocalChangelistStateCommitted(changelistId);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.changelist.delete.failure", changelistId),
                                P4Bundle.message("error.changelist.delete.failure", changelistId),
                                e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
                        ret = ExecutionStatus.FAIL;
                    }
                } else {
                    // else the cached version had its "deleted" setting changed back, or is only locally stored
                    // and marked for delete, or it is not known and numbered like a locally stored change.
                    // In any case, ignore the change.
                    alerts.addNotice(
                            exec.getProject(),
                            P4Bundle.message("changelist.delete.ignored", changelistId), null);
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


    static class MoveFileAction extends AbstractChangelistAction {
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
                final Integer changelistId = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                final String description = UpdateParameterNames.DESCRIPTION.getParameterValue(state);
                final List<FilePath> files = new ArrayList<FilePath>();
                for (Map.Entry<String, Object> entry: state.getParameters().entrySet()) {
                    if (UpdateParameterNames.FIELD.matches(entry.getKey())) {
                        files.add(FilePathUtil.getFilePath(
                                (String) UpdateParameterNames.FIELD.getValue(entry.getValue())));
                    }
                }
                if (changelistId == null || files.isEmpty() || description == null) {
                    alerts.addNotice(
                            exec.getProject(),
                            P4Bundle.message("pendingupdatestate.invalid", state), null);
                    continue;
                }

                int realChangeListId = changelistId;
                if (realChangeListId <= P4ChangeListId.P4_LOCAL) {
                    // create a new changelist and force the update across the files
                    try {
                        final IChangelist changelist = exec.createChangeList(description);
                        realChangeListId = changelist.getId();
                        P4ChangeListId oldCl = new P4ChangeListIdImpl(clientServerId, changelistId);
                        P4ChangeListId newCl = new P4ChangeListIdImpl(clientServerId, realChangeListId);
                        changeListMapping.replace(oldCl, newCl);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.createchangelist.title"),
                                P4Bundle.message("error.createchangelist", description),
                                e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
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
                            alerts.addWarning(
                                    exec.getProject(),
                                    P4Bundle.message("error.updatechangelist.title"),
                                    P4Bundle.message("error.updatechangelist", realChangeListId),
                                    e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
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
                    alerts.addWarning(
                            exec.getProject(),
                            P4Bundle.message("filestatus.error.title"),
                            P4Bundle.message("filestatus.error", files),
                            e, files);
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
                        if (spec.getOpenChangelistId() != changelistId) {
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
                                exec.getProject(),
                                P4Bundle.message("error.reopen", reopen),
                                exec.reopenFiles(reopen, realChangeListId, null),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.reopen.title"),
                                P4Bundle.message("error.reopen", reopen),
                                e, reopen);
                        // keep going
                    }
                }

                if (! edit.isEmpty()) {
                    LOG.info("Editing files into " + realChangeListId + ": " + edit);
                    try {
                        alerts.addWarnings(
                                exec.getProject(),
                                P4Bundle.message("error.edit", edit),
                                exec.editFiles(edit, realChangeListId),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.edit.title"),
                                P4Bundle.message("error.edit", edit),
                                e, edit);
                        // keep going
                    }
                }

                if (!add.isEmpty()) {
                    LOG.info("Adding files into " + realChangeListId + ": " + add);
                    try {
                        alerts.addWarnings(
                                exec.getProject(),
                                P4Bundle.message("error.add", add),
                                exec.addFiles(add, realChangeListId),
                                false);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.add.title"),
                                P4Bundle.message("error.add", add),
                                e, add);
                        // keep going
                    }
                }

                clientCacheManager.markLocalChangelistStateCommitted(changelistId);
                clientCacheManager.markLocalChangelistStateCommitted(realChangeListId);
            }

            // FIXME ensure the file actions are refreshed with the new changelist.

            return ret;
        }
    }


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Delete Action

    public static class UpdateChangelistFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new UpdateChangelistAction(states);
        }
    }


    static class UpdateChangelistAction extends AbstractChangelistAction {
        UpdateChangelistAction(final Collection<PendingUpdateState> states) {
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
            // organize the changes by ID, so that they can be done at a single
            // pass.

            ExecutionStatus ret = ExecutionStatus.NO_OP;
            boolean updated = false;

            Map<Integer, List<PendingUpdateState>> clToUpdate = new HashMap<Integer, List<PendingUpdateState>>();
            for (PendingUpdateState state : getPendingUpdateStates()) {
                Integer cl = UpdateParameterNames.CHANGELIST.getParameterValue(state);
                if (cl != null) {
                    List<PendingUpdateState> values = clToUpdate.get(cl);
                    if (values == null) {
                        values = new ArrayList<PendingUpdateState>();
                        clToUpdate.put(cl, values);
                    }
                    values.add(state);
                } else {
                    alerts.addNotice(exec.getProject(),
                            P4Bundle.message("error.bad-update.ignored", state),
                            null);
                }
            }

            for (Entry<Integer, List<PendingUpdateState>> entry : clToUpdate.entrySet()) {
                final Integer cl = entry.getKey();
                if (cl <= 0) {
                    // not correct
                    // TODO should this be a error dialog?
                    LOG.error("Encountered a changelist update for an invalid changelist number " +
                        cl);
                    continue;
                }
                String newComment = null;
                String fixState = null;
                List<String> newJobs = new ArrayList<String>();
                List<String> removedJobs = new ArrayList<String>();
                for (PendingUpdateState state : entry.getValue()) {
                    switch (state.getUpdateAction()) {
                        case CHANGE_CHANGELIST_DESCRIPTION:
                            newComment = UpdateParameterNames.DESCRIPTION.getParameterValue(state);
                            break;
                        case ADD_JOB_TO_CHANGELIST:
                            newJobs.add((String) UpdateParameterNames.JOB.getParameterValue(state));
                            break;
                        case REMOVE_JOB_FROM_CHANGELIST:
                            removedJobs.add((String) UpdateParameterNames.JOB.getParameterValue(state));
                            break;
                        case SET_CHANGELIST_FIX_STATE:
                            fixState = UpdateParameterNames.FIX_STATE.getParameterValue(state);
                            break;
                        default:
                            LOG.error("Incorrect update group (changelist) for action " +
                                    state.getUpdateAction());
                            break;
                    }
                }
                if (newComment != null) {
                    try {
                        exec.updateChangelistDescription(cl, newComment);
                        updated = true;
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.description.title", cl),
                                P4Bundle.message("exception.update-changelist.description", cl),
                                e, new FilePath[0]);
                        ret = ExecutionStatus.FAIL;
                    }
                }
                if (! newJobs.isEmpty()) {
                    try {
                        exec.addJobsToChangelist(cl, newJobs, fixState);
                        updated = true;
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.add-jobs.title", cl),
                                P4Bundle.message("exception.update-changelist.add-jobs", cl, newJobs),
                                e, new FilePath[0]);
                        ret = ExecutionStatus.FAIL;
                    }
                }
                if (! removedJobs.isEmpty()) {
                    try {
                        exec.removeJobsFromChangelist(cl, removedJobs);
                        updated = true;
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.remove-jobs.title", cl),
                                P4Bundle.message("exception.update-changelist.remove-jobs", cl, removedJobs),
                                e, new FilePath[0]);
                        ret = ExecutionStatus.FAIL;
                    }
                }
            }

            if (updated && ret == ExecutionStatus.NO_OP) {
                ret = ExecutionStatus.RELOAD_CACHE;
            }
            return ret;
        }
    }
}
