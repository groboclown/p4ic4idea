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
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

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
    }

    private static final CurateResult KEEP_EXISTING_REMOVE_ADDED =
            new CurateResult(false, true, null, null);
    private static final CurateResult KEEP_ADDED_REMOVE_EXISTING =
            new CurateResult(true, false, null, null);
    private static final CurateResult KEEP_BOTH =
            new CurateResult(true, true, null, null);
    private static final CurateResult REMOVE_BOTH =
            new CurateResult(false, false, null, null);

    private final PendingActionFactory actionFactory;

    PendingActionCurator(@NotNull PendingActionFactory actionFactory) {
        this.actionFactory = actionFactory;
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
                        LOG.debug("Skipping duplicate action " + added.clientAction);
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
                        return KEEP_EXISTING_REMOVE_ADDED;
                    }
                }
                return KEEP_BOTH;

            case LOGIN:
                // Shouldn't ever be in the pending list, but for completeness...
                if (existing.getCmd() == P4CommandRunner.ServerActionCmd.LOGIN) {
                    return KEEP_EXISTING_REMOVE_ADDED;
                }
                return KEEP_BOTH;

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
                // FIXME implement
                LOG.warn("FIXME implement changelist action consolidation");
                return KEEP_BOTH;
            case ADD_JOB_TO_CHANGELIST:
                // FIXME implement
                LOG.warn("FIXME implement changelist action consolidation");
                return KEEP_BOTH;
            case REMOVE_JOB_FROM_CHANGELIST:
                // FIXME implement
                LOG.warn("FIXME implement changelist action consolidation");
                return KEEP_BOTH;
            case CREATE_CHANGELIST:
                // FIXME implement
                LOG.warn("FIXME implement changelist action consolidation");
                return KEEP_BOTH;
            case DELETE_CHANGELIST:
                // FIXME implement
                LOG.warn("FIXME implement changelist action consolidation");
                return KEEP_BOTH;

            // Doesn't have an effect on the list of actions.  Shouldn't be in the pending list anyway.
            case FETCH_FILES:
            case SUBMIT_CHANGELIST:
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

                return KEEP_ADDED_REMOVE_EXISTING;
            }
            case DELETE_FILE: {
                DeleteFileAction existing = (DeleteFileAction) existingSrc;

                // We know that either the move source was originally marked for delete, or the
                // move target was originally marked for delete (because of the !hasCommonFiles at the start).
                // In both cases, the move will either just replace the original, or will perform an
                // alternate behavior that negates the original behavior.  So just remove the original action.

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
}
