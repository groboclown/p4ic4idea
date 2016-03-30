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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
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
import net.groboclown.idea.p4ic.v2.server.util.ChangelistDescriptionGenerator;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class ChangeListServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(ChangeListServerCacheSync.class);

    private final Cache cache;

    // TODO unlike the files, a single changelist can have multiple actions
    // associated with it.  Change from a set to a list, and the returned
    // opened changelists will need to build up the objects based on the
    // changes.
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

        // Ensure the default changelist is included.  This will be overwritten
        // if there are files associated with it.
        ret.put(P4ChangeListId.P4_DEFAULT, new P4ChangeListValue(
                cache.getClientServerId(), true));

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
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);
        final PendingUpdateState ret = new PendingUpdateState(UpdateAction.DELETE_CHANGELIST,
                Collections.singleton(Integer.toString(changelistId)),
                params);

        final P4ChangeListState state = new P4ChangeListState(ret, changelistId);
        state.setDeleted(true);
        localClientChanges.add(state);
        committed.remove(changelistId);

        return ret;
    }


    @Nullable
    public PendingUpdateState moveFileToChangelist(final Project project, @NotNull Collection<FilePath> files,
            @NotNull LocalChangeList source, @Nullable P4ChangeListId changelistId) {
        if (files.isEmpty()) {
            return null;
        }

        // If the changelist is null, then we need to reserve one, as it will be the
        // replacement that we use when the real changelist is eventually created.

        if (changelistId == null) {
            // allocate a local changelist id
            synchronized (localCacheSync) {
                previousLocalChangelistId -= 1;
                changelistId = new P4ChangeListIdImpl(cache.getClientServerId(), previousLocalChangelistId);
            }
            // make sure we create this association in the changelist mapping.
            P4ChangeListMapping.getInstance(project).bindChangelists(source, changelistId);
        }

        String description = ChangelistDescriptionGenerator.getDescription(source);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId.getChangeListId());
        params.put(UpdateParameterNames.DESCRIPTION.getKeyName(), description);
        int index = 0;
        for (FilePath file : files) {
            params.put(UpdateParameterNames.FIELD.getKeyName() + index, file.getIOFile().getAbsolutePath());
            index++;
        }

        final PendingUpdateState ret = new PendingUpdateState(UpdateAction.REOPEN_FILES_INTO_CHANGELIST,
                Collections.singleton(Integer.toString(changelistId.getChangeListId())),
                params);

        // add the changelist into the altered changelist list.
        // The description is updated, so the changelist needs to reflect this.
        final P4ChangeListState local = getOrAddChangeState(ret, changelistId.getChangeListId());
        if (! description.equals(local.getComment())) {
            // FIXME this could be a duplicate add!!!
            // This can be fixed by making the local cache building be incremental when
            // returned above.
            synchronized (localCacheSync) {
                localClientChanges.add(local);
            }
        }
        local.setComment(description);

        return ret;

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

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);
        params.put(UpdateParameterNames.DESCRIPTION.getKeyName(), description);

        final PendingUpdateState ret = new PendingUpdateState(UpdateAction.CHANGE_CHANGELIST_DESCRIPTION,
                Collections.singleton(Integer.toString(changelistId)),
                params);

        final P4ChangeListState state = new P4ChangeListState(ret, changelistId);
        state.setComment(description);
        localClientChanges.add(state);
        committed.remove(changelistId);

        return ret;
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
            // This generally is always entered.  Note that, because the
            // default changelist ALWAYS exists for a client, we can safely
            // create this "back door", that, and it's going in the server
            // cache, so it needs to pending update.
            ChangelistSummary summary = new ChangelistSummary(
                    P4ChangeListId.P4_DEFAULT, exec.getClientName(),
                    exec.getUsername(), ChangelistStatus.NEW, new Date(),
                    "", false);
            refreshed.add(new P4ChangeListState(summary));
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

    @Override
    protected void rectifyCache(@NotNull final Project project,
            @NotNull final Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull final AlertManager alerts) {
        // TODO check the state
    }

    private static final Collection<UpdateGroup> SUPPORTED_GROUPS =
            Collections.unmodifiableCollection(Arrays.asList(
                    UpdateGroup.CHANGELIST,
                    UpdateGroup.CHANGELIST_DELETE,
                    UpdateGroup.CHANGELIST_FILES
            ));
    @NotNull
    @Override
    protected Collection<UpdateGroup> getSupportedUpdateGroups() {
        return SUPPORTED_GROUPS;
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshDate;
    }

    @Override
    protected void checkLocalIntegrity(@NotNull List<PendingUpdateState> pendingUpdates) {
        // if there are local changes that aren't in the pending changes,
        // that's an issue with the local cache.

        Set<Integer> pendingChanges = new HashSet<Integer>();
        for (PendingUpdateState update : pendingUpdates) {
            Integer cl = UpdateParameterNames.CHANGELIST.getParameterValue(update);
            if (cl != null) {
                pendingChanges.add(cl);
            }
        }

        synchronized (localCacheSync) {
            Iterator<P4ChangeListState> iter = localClientChanges.iterator();
            while (iter.hasNext()) {
                final P4ChangeListState next = iter.next();
                if (! pendingChanges.contains(next.getChangelistId())) {
                    LOG.warn("Incorrect mapping: pending change did not remove " + next);
                    iter.remove();
                }
            }
        }
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
     *
     * @param update update to which any added change will be added.
     * @param changelistId changelist id
     * @return local changelist state
     */
    @NotNull
    private P4ChangeListState getOrAddChangeState(@NotNull PendingUpdateState update, int changelistId) {
        synchronized (localCacheSync) {
            for (P4ChangeListState local : localClientChanges) {
                if (local.getChangelistId() == changelistId) {
                    return local;
                }
            }
            for (P4ChangeListState remote : cachedServerChanges) {
                if (remote.getChangelistId() == changelistId) {
                    P4ChangeListState local = new P4ChangeListState(update, remote);
                    localClientChanges.add(local);
                    return local;
                }
            }
            LOG.info("Could not find a changelist numbered " + changelistId + "; creating one");
            P4ChangeListState local = new P4ChangeListState(update, changelistId);
            localClientChanges.add(local);
            return local;
        }
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

        @Nullable
        protected Integer mapToState(final PendingUpdateState update) {
            return UpdateParameterNames.CHANGELIST.getParameterValue(update);
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
                final Integer changelistId = mapToState(state);
                if (changelistId == null || changelistId == P4ChangeListId.P4_DEFAULT ||
                        changelistId == P4ChangeListId.P4_UNKNOWN) {
                    markFailed(state);
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
                        markSuccess(state);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.changelist.delete.failure", changelistId),
                                P4Bundle.message("error.changelist.delete.failure", changelistId),
                                e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
                        markFailed(state);
                        ret = ExecutionStatus.FAIL;
                    }
                } else {
                    // else the cached version had its "deleted" setting changed back, or is only locally stored
                    // and marked for delete, or it is not known and numbered like a locally stored change.
                    // In any case, ignore the change.
                    alerts.addNotice(
                            exec.getProject(),
                            P4Bundle.message("changelist.delete.ignored", changelistId), null);
                    markSuccess(state);
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

            for (PendingUpdateState update : getPendingUpdateStates()) {
                final Integer changelistId = mapToState(update);
                final String description = UpdateParameterNames.DESCRIPTION.getParameterValue(update);
                final List<FilePath> files = new ArrayList<FilePath>();
                for (Map.Entry<String, Object> entry: update.getParameters().entrySet()) {
                    if (UpdateParameterNames.FIELD.matches(entry.getKey())) {
                        files.add(FilePathUtil.getFilePath(
                                (String) UpdateParameterNames.FIELD.getValue(entry.getValue())));
                    }
                }
                if (changelistId == null || files.isEmpty() || description == null) {
                    alerts.addNotice(
                            exec.getProject(),
                            P4Bundle.message("pendingupdatestate.invalid", update), null);
                    markFailed(update);
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
                        markSuccess(update);
                    } catch (VcsException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("error.createchangelist.title"),
                                P4Bundle.message("error.createchangelist", description),
                                e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
                        // cannot actually perform the action
                        markFailed(update);
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
                            markFailed(update);
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
                    markFailed(update);
                    continue;
                }
                if (status.isEmpty()) {
                    // no file motion to perform
                    markSuccess(update);
                    continue;
                }

                List<IFileSpec> reopen = new ArrayList<IFileSpec>(status.size());
                List<IFileSpec> edit = new ArrayList<IFileSpec>(status.size());
                List<IFileSpec> add = new ArrayList<IFileSpec>(status.size());

                for (IExtendedFileSpec spec: status) {

                    // the spec file will need to be re-escaped and stripped of annotations (#103)
                    final IFileSpec forServer;
                    try {
                        forServer = FileSpecUtil.escapeAndStripSpec(spec);
                    } catch (P4FileException e) {
                        alerts.addWarning(
                                exec.getProject(),
                                P4Bundle.message("filestatus.error.title"),
                                P4Bundle.message("filestatus.error", files),
                                e, files);
                        markFailed(update);
                        continue;
                    }

                    if (spec.getOpStatus() != FileSpecOpStatus.VALID) {
                        LOG.debug("File status: " + spec.getOpStatus() + ": " + spec.getStatusMessage());
                    } else if (spec.getOpenAction() != null || spec.getAction() != null) {
                        if (spec.getOpenChangelistId() != changelistId) {
                            // already opened; reopen it
                            reopen.add(forServer);
                        } else {
                            LOG.info("Ignoring reopen request for " + spec.getDepotPathString() + "; already in the right changelist");
                        }
                    } else if (spec.getHeadRev() <= 0) {
                        // add
                        add.add(forServer);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Marking as edit: " + spec +
                                    "; action: " + spec.getOpenAction() + "/" + spec.getAction() + "/" + spec
                                    .getHeadAction() + "/" + spec.getOtherAction() +
                                    "; change: " + spec.getOpenChangelistId());
                        }
                        edit.add(forServer);
                    }
                }

                if (! reopen.isEmpty()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Reopening files into " + realChangeListId + ": " + reopen);
                    }
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Editing files into " + realChangeListId + ": " + edit);
                    }
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding files into " + realChangeListId + ": " + add);
                    }
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
            for (PendingUpdateState update : getPendingUpdateStates()) {
                Integer cl = mapToState(update);
                if (cl != null) {
                    List<PendingUpdateState> values = clToUpdate.get(cl);
                    if (values == null) {
                        values = new ArrayList<PendingUpdateState>();
                        clToUpdate.put(cl, values);
                    }
                    values.add(update);
                } else {
                    alerts.addNotice(exec.getProject(),
                            P4Bundle.message("error.bad-update.ignored", update),
                            null);
                    markFailed(update);
                }
            }

            for (Entry<Integer, List<PendingUpdateState>> entry : clToUpdate.entrySet()) {
                final Integer cl = entry.getKey();
                if (cl <= 0) {
                    // not correct
                    // TODO should this be a error dialog?
                    LOG.error("Encountered a changelist update for an invalid changelist number " +
                        cl);
                    markFailed(entry.getValue());
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
                        markSuccess(entry.getValue());
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.description.title", cl),
                                P4Bundle.message("exception.update-changelist.description", cl),
                                e, new FilePath[0]);
                        markFailed(entry.getValue());
                        ret = ExecutionStatus.FAIL;
                    }
                }
                if (! newJobs.isEmpty()) {
                    try {
                        exec.addJobsToChangelist(cl, newJobs, fixState);
                        updated = true;
                        markSuccess(entry.getValue());
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.add-jobs.title", cl),
                                P4Bundle.message("exception.update-changelist.add-jobs", cl, newJobs),
                                e, new FilePath[0]);
                        markFailed(entry.getValue());
                        ret = ExecutionStatus.FAIL;
                    }
                }
                if (! removedJobs.isEmpty()) {
                    try {
                        exec.removeJobsFromChangelist(cl, removedJobs);
                        updated = true;
                        markSuccess(entry.getValue());
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("exception.update-changelist.remove-jobs.title", cl),
                                P4Bundle.message("exception.update-changelist.remove-jobs", cl, removedJobs),
                                e, new FilePath[0]);
                        markFailed(entry.getValue());
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
