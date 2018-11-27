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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.RemoveJobFromChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.file.ShelveFilesAction;
import net.groboclown.p4.server.api.util.EqualUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

/**
 * Examines newly added actions and checks how that impacts existing actions in the pending list.
 *
 * TODO this needs to be part of the PendingAction object lifecycle.
 */
class PendingActionCurator {
    private static final Logger LOG = Logger.getInstance(PendingActionCurator.class);

    interface PendingActionFactory {
        @NotNull ActionStore.PendingAction create(@NotNull P4CommandRunner.ClientAction<?> action);
        @NotNull ActionStore.PendingAction create(@NotNull P4CommandRunner.ServerAction<?> action);
    }

    static class CurateResult {
        final boolean removeExisting;
        final boolean removeAdded;
        private final ActionStore.PendingAction replacedAddedAction;
        private final ActionStore.PendingAction replacedExistingAction;

        CurateResult(boolean keepAdded, boolean keepExisting,
                @Nullable ActionStore.PendingAction replacedAddedAction,
                @Nullable ActionStore.PendingAction replacedExistingAction) {
            this.removeAdded = !keepAdded;
            this.removeExisting = !keepExisting;
            this.replacedAddedAction = replacedAddedAction;
            this.replacedExistingAction = replacedExistingAction;
            if (removeExisting && replacedExistingAction != null) {
                throw new IllegalStateException("Cannot both replace existing and remove existing");
            }
        }

        @NotNull
        ActionStore.PendingAction replacedAdded(@NotNull ActionStore.PendingAction previousAddedAction) {
            if (replacedAddedAction != null) {
                LOG.info("Replaced added action " + previousAddedAction + " with " + replacedAddedAction);
                return replacedAddedAction;
            }
            return previousAddedAction;
        }

        @NotNull
        ActionStore.PendingAction replacedExisting(@NotNull ActionStore.PendingAction previousExistingAction) {
            if (replacedExistingAction != null) {
                LOG.info("Replaced existing action " + previousExistingAction + " with " + replacedExistingAction);
                return replacedExistingAction;
            }
            return previousExistingAction;
        }

        // For unit tests...
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof CurateResult) {
                CurateResult that = (CurateResult) o;
                return that.removeExisting == this.removeExisting
                        && that.removeAdded == this.removeAdded
                        && EqualUtil.isEqual(that.replacedExistingAction, this.replacedExistingAction)
                        && EqualUtil.isEqual(that.replacedAddedAction, this.replacedAddedAction);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (removeAdded ? 97 : 0) +
                    (removeExisting ? 23 : 0) +
                    (replacedAddedAction == null ? 0 : replacedAddedAction.hashCode()) +
                    (replacedExistingAction == null ? 0 : replacedExistingAction.hashCode());
        }

        @Override
        public String toString() {
            return "CurateReseult(removeAdded: " + removeAdded
                    + ", removeExisting: " + removeExisting
                    + ", replaceAdded: " + replacedAddedAction
                    + ", replaceExisting: " + replacedExistingAction
                    + ")";
        }
    }

    // Should be private, but unit tests...
    static final CurateResult KEEP_EXISTING_REMOVE_ADDED =
            new CurateResult(false, true, null, null);
    static final CurateResult KEEP_ADDED_REMOVE_EXISTING =
            new CurateResult(true, false, null, null);
    static final CurateResult KEEP_BOTH =
            new CurateResult(true, true, null, null);
    static final CurateResult REMOVE_BOTH =
            new CurateResult(false, false, null, null);

    private final PendingActionFactory actionFactory;

    PendingActionCurator(@NotNull PendingActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }


    /**
     * Modify the list of actions by adding in the new action, along with curation of the list due to the changes
     * caused by adding the new action.  The existing list is appended to with each new action (so oldest first).
     *
     * @param added new action
     * @param actions existing list of actions.
     */
    void curateActionList(@NotNull ActionStore.PendingAction added, @NotNull List<ActionStore.PendingAction> actions) {
        // Curate the pending list of actions.
        // Curation MUST be done in reverse order of the existing pending actions.
        final ListIterator<ActionStore.PendingAction> iter = actions.listIterator(actions.size());
        while (iter.hasPrevious()) {
            final ActionStore.PendingAction existingAction = iter.previous();
            PendingActionCurator.CurateResult result = curate(added, existingAction);
            added = result.replacedAdded(added);
            if (result.removeExisting) {
                iter.remove();
            } else {
                iter.set(result.replacedExisting(existingAction));
            }
            if (result.removeAdded) {
                // Halt the add operation
                return;
            }
        }

        actions.add(added);
    }


    /**
     *
     * @param added a new requested action to add to the pending list.
     * @param existing existing action within the pending list.
     * @return true if the added action forces the existing action to be reverted
     */
    @NotNull
    CurateResult curate(@NotNull ActionStore.PendingAction added, @NotNull ActionStore.PendingAction existing) {
        // Simple duplicate check
        if (added.equals(existing)) {
            return KEEP_EXISTING_REMOVE_ADDED;
        }

        if (added.clientAction != null) {
            if (existing.clientAction != null) {
                // Duplicate check
                if (added.sourceId.equals(existing.sourceId) &&
                        added.clientAction.getActionId().equals(existing.clientAction.getActionId())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Skipping duplicate action " + added.clientAction);
                    }
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return curateClientActions(added.clientAction, existing.clientAction);
            }
            if (existing.serverAction != null) {
                return curateClientServerActions(added.clientAction, existing.serverAction);
            }
        }
        if (added.serverAction != null) {
            if (existing.clientAction != null) {
                return curateServerClientActions(added.serverAction, existing.clientAction);
            }
            if (existing.serverAction != null) {
                // Duplicate check
                if (added.sourceId.equals(existing.sourceId) &&
                        added.serverAction.getActionId().equals(existing.serverAction.getActionId())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Skipping duplicate action " + added.serverAction);
                    }
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return curateServerActions(added.serverAction, existing.serverAction);
            }
        }
        return KEEP_BOTH;
    }

    @NotNull
    private CurateResult curateServerActions(
            @NotNull P4CommandRunner.ServerAction<?> added,
            @NotNull P4CommandRunner.ServerAction<?> existing) {
        switch (added.getCmd()) {
            case CREATE_JOB:
                if (existing.getCmd() == P4CommandRunner.ServerActionCmd.CREATE_JOB) {
                    if (((CreateJobAction) added).getJob().getJobId().equals(
                            ((CreateJobAction) existing).getJob().getJobId())) {
                        // Duplicate add of the same job ID
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Remove duplicate action `create job " +
                                    ((CreateJobAction) added).getJob().getJobId() + "`");
                        }
                        return KEEP_EXISTING_REMOVE_ADDED;
                    }
                }
                return KEEP_BOTH;

            case LOGIN:
                // Shouldn't ever be in the pending list...
                LOG.warn("Purging LOGIN actions from list; should never have been added");
                return KEEP_EXISTING_REMOVE_ADDED;

            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateClientActions(
            @NotNull P4CommandRunner.ClientAction<?> added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        switch (added.getCmd()) {
            case MOVE_FILE:
                return curateMoveRequest((MoveFileAction) added, existing);
            case ADD_EDIT_FILE:
                return curateAddEditRequest((AddEditAction) added, existing);
            case DELETE_FILE:
                return curateDeleteRequest((DeleteFileAction) added, existing);
            case REVERT_FILE:
                // FIXME implement.  Requires local history tracking.
                LOG.warn("FIXME implement revert file action consolidation");
                return KEEP_BOTH;
            case MOVE_FILES_TO_CHANGELIST:
                return curateMoveFilesToChangelistRequest((MoveFilesToChangelistAction) added, existing);
            case EDIT_CHANGELIST_DESCRIPTION:
                return curateEditChangelistDescriptionRequest((EditChangelistAction) added, existing);
            case ADD_JOB_TO_CHANGELIST:
                return curateAddJobToChangelistRequest((AddJobToChangelistAction) added, existing);
            case REMOVE_JOB_FROM_CHANGELIST:
                return curateRemoveJobFromChangelistRequest((RemoveJobFromChangelistAction) added, existing);
            case CREATE_CHANGELIST:
                return curateCreateChangelistRequest((CreateChangelistAction) added, existing);
            case DELETE_CHANGELIST:
                return curateDeleteChangelistRequest((DeleteChangelistAction) added, existing);

            // Doesn't have an effect on the list of actions.  Shouldn't be in the pending list anyway.
            // Shouldn't ever be in the pending list...
            case FETCH_FILES:
            case SUBMIT_CHANGELIST:
                LOG.warn("Purging " + added.getCmd() + " actions from list; should never have been added");
                return KEEP_EXISTING_REMOVE_ADDED;

            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateAddEditRequest(AddEditAction added, P4CommandRunner.ClientAction<?> existing) {
        if (haveNoCommonFiles(added, existing)) {
            return KEEP_BOTH;
        }

        // FIXME implement
        LOG.warn("FIXME implement add/edit action consolidation");
        return KEEP_BOTH;
    }

    @NotNull
    private CurateResult curateDeleteRequest(DeleteFileAction added, P4CommandRunner.ClientAction<?> existing) {
        if (haveNoCommonFiles(added, existing)) {
            return KEEP_BOTH;
        }

        // FIXME implement
        LOG.warn("FIXME implement delete action consolidation");
        return KEEP_BOTH;
    }

    @NotNull
    private CurateResult curateMoveRequest(MoveFileAction added, P4CommandRunner.ClientAction<?> existingSrc) {
        if (haveNoCommonFiles(added, existingSrc)) {
            return KEEP_BOTH;
        }

        switch (existingSrc.getCmd()) {
            case MOVE_FILE: {
                // Two move operations affecting at least one shared file.

                MoveFileAction existing = (MoveFileAction) existingSrc;
                if (existing.getSourceFile().equals(added.getSourceFile())) {
                    if (existing.getTargetFile().equals(added.getTargetFile())) {
                        // Duplicate action
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Removing duplicate action `move " + existing.getSourceFile() + " to " +
                                    existing.getTargetFile() + "`");
                        }
                        return KEEP_EXISTING_REMOVE_ADDED;
                    }

                    // This shouldn't happen.
                    LOG.warn("Confusing set of changes.  Requested move the same file (" + added.getSourceFile() +
                            ") to both " + existing.getTargetFile() + " and " + added.getTargetFile());
                    return KEEP_BOTH;
                }
                if (existing.getTargetFile().equals(added.getTargetFile())) {
                    // Putting different sources to the same target.
                    LOG.info("Move request for " + added.getSourceFile() + " into " + added.getTargetFile() +
                            " conflicts with existing move request from " + existing.getSourceFile() +
                            ".  Changing existing move request to a delete.");
                    return new CurateResult(true, true,
                            // added action stays the same
                            null,
                            // existing action changes to a delete
                            actionFactory.create(new DeleteFileAction(existing.getSourceFile(), existing.getChangelistId())));
                }
                if (existing.getTargetFile().equals(added.getSourceFile())) {
                    // Requested a move from A to B, then from B to A.
                    LOG.info("Consolidating move from " + existing.getSourceFile() + " to " +
                            existing.getTargetFile() + " to " + added.getTargetFile());
                    // Change the new request to a single move from the original source to the new target,
                    // and remove the original request.
                    return new CurateResult(true, false,
                            actionFactory.create(new MoveFileAction(existing.getSourceFile(), added.getTargetFile(),
                                    added.getChangelistId())),
                            null);
                }
                if (existing.getSourceFile().equals(added.getTargetFile())) {
                    if (existing.getTargetFile().equals(added.getSourceFile())) {
                        // Requested to move A to B, then B to A.
                        // These cancel each other out.
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Removed actions because they cancel each other out: `move " +
                                    existing.getSourceFile() + " to " + existing.getTargetFile() + "` and `move " +
                                    added.getSourceFile() + " to " + added.getTargetFile() + "`");
                        }
                        return REMOVE_BOTH;
                    }

                    // Requested to move A to B, then C to A.
                    // The move command, when executed in order, should perform the correct behavior?
                    // Really, the existing operation should be turned into an integrate, and the
                    // added one kept the same.
                    // TODO double check if this works as expected.
                    return KEEP_BOTH;
                }

                // Shouldn't happen, but for completeness, keep this line.
                return KEEP_BOTH;
            }

            case ADD_EDIT_FILE: {
                AddEditAction existing = (AddEditAction) existingSrc;

                // We know that either the move source was marked for add or edit, or the
                // move target was marked for add or edit.  In both cases, the move action will
                // replace the original request.

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Remove existing action `add/edit " + existing.getFile() +
                            "` because of a later move for the same file.");
                }

                return KEEP_ADDED_REMOVE_EXISTING;
            }
            case DELETE_FILE: {
                DeleteFileAction existing = (DeleteFileAction) existingSrc;

                // We know that either the move source was originally marked for delete, or the
                // move target was originally marked for delete (because of the !hasCommonFiles at the start).
                // In both cases, the move will either just replace the original, or will perform an
                // alternate behavior that negates the original behavior.  So just remove the original action.

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Remove existing action `delete " + existing.getFile() +
                            "` because of a later move for the same file.");
                }

                return KEEP_ADDED_REMOVE_EXISTING;
            }
            case REVERT_FILE: {
                RevertFileAction existing = (RevertFileAction) existingSrc;

                // We know that either the move source was originally marked to be reverted, or the
                // move target was originally marked to be reverted.  In both cases, the move command
                // will perform the correct behavior to the open-for-edit/whatever of the source file.

                return KEEP_BOTH;
            }

            // Move shouldn't affect these actions.
            case MOVE_FILES_TO_CHANGELIST:
            case EDIT_CHANGELIST_DESCRIPTION:
            case ADD_JOB_TO_CHANGELIST:
            case REMOVE_JOB_FROM_CHANGELIST:
            case CREATE_CHANGELIST:
            case DELETE_CHANGELIST:
            case FETCH_FILES:
            case SUBMIT_CHANGELIST:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateMoveFilesToChangelistRequest(MoveFilesToChangelistAction added, P4CommandRunner.ClientAction<?> existing) {
        if (haveNoCommonFiles(added, existing)) {
            return KEEP_BOTH;
        }

        // This has the potential to modify the existing and added actions.
        switch (existing.getCmd()) {
            case MOVE_FILE:
                // FIXME implement
                LOG.warn("FIXME implement move moved files to changelist consolidation");
                return KEEP_BOTH;
            case ADD_EDIT_FILE:
                // FIXME implement
                LOG.warn("FIXME implement move add/edit files to changelist consolidation");
                return KEEP_BOTH;
            case DELETE_FILE:
                // FIXME implement
                LOG.warn("FIXME implement move delete file to changelist consolidation");
                return KEEP_BOTH;
            case MOVE_FILES_TO_CHANGELIST:
                // FIXME implement
                LOG.warn("FIXME implement move move-files-to-changelist to changelist consolidation");
                return KEEP_BOTH;

            // Actions not affected by move-file-to-changelists.
            case REVERT_FILE:
            case EDIT_CHANGELIST_DESCRIPTION:
            case ADD_JOB_TO_CHANGELIST:
            case REMOVE_JOB_FROM_CHANGELIST:
            case CREATE_CHANGELIST:
            case DELETE_CHANGELIST:
            case FETCH_FILES:
            case SUBMIT_CHANGELIST:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateClientServerActions(
            @NotNull P4CommandRunner.ClientAction<?> added,
            @NotNull P4CommandRunner.ServerAction<?> existing) {
        // Server actions are only LOGIN and CREATE_JOB.  If we allowed
        // for deleting a job, then we'd have to handle ADD_JOB_TO_CHANGELIST.
        // But, because we don't, there is no situation where a server action
        // is influenced by an existing action, or vice versa.
        return KEEP_BOTH;
    }

    @NotNull
    private CurateResult curateServerClientActions(
            @NotNull P4CommandRunner.ServerAction<?> added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        // Server actions are only LOGIN and CREATE_JOB.  If we allowed
        // for deleting a job, then we'd have to handle ADD_JOB_TO_CHANGELIST.
        // But, because we don't, there is no situation where a server action
        // is influenced by an existing action, or vice versa.
        return KEEP_BOTH;
    }

    @NotNull
    private CurateResult curateEditChangelistDescriptionRequest(
            @NotNull EditChangelistAction added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        if (! added.getChangelistId().equals(getAssociatedChangelistId(existing))) {
            // They do not share the same changelist ID.  Keep both actions.
            return KEEP_BOTH;
        }

        // Changelists match (ID and client/server)
        switch (existing.getCmd()) {
            case CREATE_CHANGELIST:
                // TODO if the local changelist IDs match, then consolidate down to a single create.
                // As this code stands now, this particular block is unreachable, because create changelist has no
                // changelist ID.
                return KEEP_BOTH;
            case DELETE_CHANGELIST: {
                // Changelists match...
                DeleteChangelistAction ex = (DeleteChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error(
                        "Attempted to change the description of a deleted changelist: " + ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }
            case SUBMIT_CHANGELIST: {
                // Changelists match...
                SubmitChangelistAction ex = (SubmitChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error("Attempted to change the description of a submitted changelist: " +
                        ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }
            case EDIT_CHANGELIST_DESCRIPTION:
                // Replace the original change description with the new one.
                return KEEP_ADDED_REMOVE_EXISTING;
            case ADD_JOB_TO_CHANGELIST:
                // Does not affect the existing action.
                return KEEP_BOTH;

            // These do not affect changelist descriptions.
            case MOVE_FILE:
            case ADD_EDIT_FILE:
            case DELETE_FILE:
            case REVERT_FILE:
            case MOVE_FILES_TO_CHANGELIST:
            case REMOVE_JOB_FROM_CHANGELIST:
            case FETCH_FILES:
            case SHELVE_FILES:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateAddJobToChangelistRequest(
            @NotNull AddJobToChangelistAction added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        if (! added.getChangelistId().equals(getAssociatedChangelistId(existing))) {
            // They do not share the same changelist ID.  Keep both actions.
            return KEEP_BOTH;
        }

        // Changelists must match (ID and client/server).
        switch (existing.getCmd()) {
            case ADD_JOB_TO_CHANGELIST: {
                AddJobToChangelistAction ex = (AddJobToChangelistAction) existing;
                if (ex.getJob().equals(added.getJob())) {
                    LOG.info("Removing duplicate call to add the same job " + added.getJob() +
                            " to changelist " + added.getChangelistId());
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return KEEP_BOTH;
            }
            case REMOVE_JOB_FROM_CHANGELIST: {
                RemoveJobFromChangelistAction ex = (RemoveJobFromChangelistAction) existing;
                if (ex.getJob().equals(added.getJob())) {
                    // Removed the job then added it.
                    LOG.info("Removing remove then re-add for same job " + added.getJob() +
                            " for changelist " + added.getChangelistId());
                    return REMOVE_BOTH;
                }
                return KEEP_BOTH;
            }
            case SUBMIT_CHANGELIST: {
                // Know that the changelists match...
                SubmitChangelistAction ex = (SubmitChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error("Attempted to change the description of a submitted changelist: " + ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }
            case DELETE_CHANGELIST: {
                // Know that the changelists match...
                DeleteChangelistAction ex = (DeleteChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error(
                        "Attempted to change the description of a deleted changelist: " + ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }

            // These actions are not affected by jobs on a changelist
            case MOVE_FILE:
            case ADD_EDIT_FILE:
            case DELETE_FILE:
            case REVERT_FILE:
            case MOVE_FILES_TO_CHANGELIST:
            case EDIT_CHANGELIST_DESCRIPTION:
            case CREATE_CHANGELIST:
            case FETCH_FILES:
            case SHELVE_FILES:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateRemoveJobFromChangelistRequest(
            @NotNull RemoveJobFromChangelistAction added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        if (! added.getChangelistId().equals(getAssociatedChangelistId(existing))) {
            // They do not share the same changelist ID.  Keep both actions.
            return KEEP_BOTH;
        }

        // The actions share the same changelist reference (changelist Id and client/server).
        switch (existing.getCmd()) {
            case ADD_JOB_TO_CHANGELIST: {
                AddJobToChangelistAction ex = (AddJobToChangelistAction) existing;
                if (ex.getJob().equals(added.getJob())) {
                    // Attempted to remove a job that was added earlier.
                    LOG.info("Removed then added job " + ex.getJob() + " from changelist " + ex.getChangelistId());
                    return REMOVE_BOTH;
                }
                return KEEP_BOTH;
            }
            case REMOVE_JOB_FROM_CHANGELIST: {
                RemoveJobFromChangelistAction ex = (RemoveJobFromChangelistAction) existing;
                if (ex.getJob().equals(added.getJob())) {
                    // Attempted to remove the same job twice.
                    LOG.info("Removed the job " + ex.getJob() + " twice from changelist " + ex.getChangelistId());
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return KEEP_BOTH;
            }
            case SUBMIT_CHANGELIST: {
                // Know the changelists match...
                SubmitChangelistAction ex = (SubmitChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error("Attempted to change the description of a submitted changelist: " + ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }
            case DELETE_CHANGELIST: {
                // Know the changelists match...
                DeleteChangelistAction ex = (DeleteChangelistAction) existing;

                // Remove the newly added value, because it doesn't make sense in this context?
                // Report this as an error, because it should never happen.  If it does, then this is a situation
                // that needs to be considered.  And with my luck, it does need special handling.
                LOG.error(
                        "Attempted to change the description of a deleted changelist: " + ex.getChangelistId());
                return KEEP_EXISTING_REMOVE_ADDED;
            }

            // These actions are not affected by jobs on changelist
            case MOVE_FILE:
            case ADD_EDIT_FILE:
            case DELETE_FILE:
            case REVERT_FILE:
            case MOVE_FILES_TO_CHANGELIST:
            case EDIT_CHANGELIST_DESCRIPTION:
            case CREATE_CHANGELIST:
            case FETCH_FILES:
            case SHELVE_FILES:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateCreateChangelistRequest(
            @NotNull CreateChangelistAction added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        // Create changelist does not have an associated changelist ID, so we can't match it well.

        switch (existing.getCmd()) {
            case CREATE_CHANGELIST: {
                CreateChangelistAction ex = (CreateChangelistAction) existing;
                if (ex.getClientServerRef().equals(added.getClientServerRef())
                        && ex.getLocalChangelistId().equals(added.getLocalChangelistId())) {
                    LOG.info("Attempted to create a second changelist for an existing one.");
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return KEEP_BOTH;
            }

            // Not affected by create changelist action
            case ADD_JOB_TO_CHANGELIST:
            case REMOVE_JOB_FROM_CHANGELIST:
            case EDIT_CHANGELIST_DESCRIPTION:
            case DELETE_CHANGELIST:
            case SUBMIT_CHANGELIST:
            case MOVE_FILE:
            case ADD_EDIT_FILE:
            case DELETE_FILE:
            case REVERT_FILE:
            case MOVE_FILES_TO_CHANGELIST:
            case FETCH_FILES:
            case SHELVE_FILES:
            default:
                return KEEP_BOTH;
        }
    }

    @NotNull
    private CurateResult curateDeleteChangelistRequest(
            @NotNull DeleteChangelistAction added,
            @NotNull P4CommandRunner.ClientAction<?> existing) {
        if (! added.getChangelistId().equals(getAssociatedChangelistId(existing))) {
            // They do not share the same changelist ID.  Keep both actions.
            return KEEP_BOTH;
        }

        switch (existing.getCmd()) {
            case EDIT_CHANGELIST_DESCRIPTION: {
                EditChangelistAction ex = (EditChangelistAction) existing;

                // This is definitely not needed.  The changelist is deleted after we change its description.
                LOG.info("Removing action `edit description of " + ex.getChangelistId() +
                        "` because the changelist was deleted later.");
                return KEEP_ADDED_REMOVE_EXISTING;
            }
            case ADD_JOB_TO_CHANGELIST: {
                AddJobToChangelistAction ex = (AddJobToChangelistAction) existing;

                // Not needed, because the changelist is deleted after the job is added.
                LOG.info("Removing action `add job " + ex.getJob() + " to changelist " + ex.getChangelistId() +
                        "` because the changelist was deleted later.");
                return KEEP_ADDED_REMOVE_EXISTING;
            }
            case REMOVE_JOB_FROM_CHANGELIST: {
                RemoveJobFromChangelistAction ex = (RemoveJobFromChangelistAction) existing;

                // Not needed, because the changelist is deleted after the job is added.
                LOG.info("Removing action `remove job " + ex.getJob() + " from changelist " + ex.getChangelistId() +
                        "` because the changelist was deleted later.");
                return KEEP_ADDED_REMOVE_EXISTING;
            }
            // FIXME IMPLEMENT
            case DELETE_CHANGELIST: {
                DeleteChangelistAction ex = (DeleteChangelistAction) existing;

                // The changelist was already marked for delete.
                LOG.info("Removing second action `remove changelist " + ex.getChangelistId() + "`");
                return KEEP_EXISTING_REMOVE_ADDED;
            }


            // TODO these actions should be checked for their effects.
            // However, this has very complex implications when mucking around with these actions.
            case MOVE_FILE:
            case ADD_EDIT_FILE:
            case DELETE_FILE:
            case MOVE_FILES_TO_CHANGELIST:

            // Creating a changelist does not give it a changelist ID, so we can't corroborate the changes.
            // Additionally, because the create changelist action doesn't have an ID, this switch statement
            // can't be reached.
            case CREATE_CHANGELIST:

            // Not affected by delete changelist action
            case REVERT_FILE:
            case FETCH_FILES:
            case SUBMIT_CHANGELIST:
            case SHELVE_FILES:
            default:
                return KEEP_BOTH;
        }
    }

    private boolean haveNoCommonFiles(P4CommandRunner.ClientAction<?> a, P4CommandRunner.ClientAction<?>  b) {
        HashSet<FilePath> commonFiles = new HashSet<>(a.getAffectedFiles());
        int aSize = commonFiles.size();
        commonFiles.removeAll(b.getAffectedFiles());
        return aSize == commonFiles.size();
    }

    @Nullable
    private P4ChangelistId getAssociatedChangelistId(@NotNull P4CommandRunner.ClientAction<?> action) {
        switch (action.getCmd()) {
            case MOVE_FILE:
                return ((MoveFileAction) action).getChangelistId();
            case ADD_EDIT_FILE:
                return ((AddEditAction) action).getChangelistId();
            case DELETE_FILE:
                return ((DeleteFileAction) action).getChangelistId();
            case REVERT_FILE:
                return null;
            case MOVE_FILES_TO_CHANGELIST:
                return ((MoveFilesToChangelistAction) action).getChangelistId();
            case EDIT_CHANGELIST_DESCRIPTION:
                return ((EditChangelistAction) action).getChangelistId();
            case ADD_JOB_TO_CHANGELIST:
                return ((AddJobToChangelistAction) action).getChangelistId();
            case REMOVE_JOB_FROM_CHANGELIST:
                return ((RemoveJobFromChangelistAction) action).getChangelistId();
            case CREATE_CHANGELIST:
                // No changelist created for this request!
                return null;
            case DELETE_CHANGELIST:
                return ((DeleteChangelistAction) action).getChangelistId();
            case FETCH_FILES:
                return null;
            case SUBMIT_CHANGELIST:
                return ((SubmitChangelistAction) action).getChangelistId();
            case SHELVE_FILES:
                return ((ShelveFilesAction) action).getChangelistId();
            default:
                return null;
        }
    }
}
