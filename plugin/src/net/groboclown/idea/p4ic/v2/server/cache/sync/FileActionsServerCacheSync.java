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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.cache.AbstractServerUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.AbstractServerUpdateAction.ExecutionStatus;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.ServerUpdateActionFactory;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Keeps track of all the file actions (files open for edit, move, etc).
 * All actions must be performed from within a
 * {@link net.groboclown.idea.p4ic.v2.server.connection.ServerConnection}
 * in order to ensure correct asynchronous behavior.
 */
public class FileActionsServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(FileActionsServerCacheSync.class);

    private final Cache cache;
    private final Set<P4FileUpdateState> localClientUpdatedFiles;
    private final Set<P4FileUpdateState> cachedServerUpdatedFiles;
    private Date lastRefreshed;


    public FileActionsServerCacheSync(@NotNull final Cache cache,
            @NotNull final Set<P4FileUpdateState> localClientUpdatedFiles,
            @NotNull final Set<P4FileUpdateState> cachedServerUpdatedFiles) {
        this.cache = cache;
        this.localClientUpdatedFiles = localClientUpdatedFiles;
        this.cachedServerUpdatedFiles = cachedServerUpdatedFiles;

        lastRefreshed = CachedState.NEVER_LOADED;
        for (P4FileUpdateState state: cachedServerUpdatedFiles) {
            if (state.getLastUpdated().before(lastRefreshed)) {
                lastRefreshed = state.getLastUpdated();
            }
        }
    }


    public Collection<P4FileAction> getOpenFiles() {
        // Get all the cached files that we know about from the server
        final Set<P4FileUpdateState> files = new HashSet<P4FileUpdateState>(cachedServerUpdatedFiles);
        // Overwrite them with the locally updated versions of them.
        files.addAll(localClientUpdatedFiles);
        final List<P4FileAction> ret = new ArrayList<P4FileAction>(files.size());
        for (P4FileUpdateState file : files) {
            ret.add(new P4FileAction(file, file.getFileUpdateAction().getUpdateAction()));
        }
        return ret;
    }


    @Override
    protected void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        // Load our server cache.  Note that we only load specs that we consider to be in a
        // "valid" file action state.

        try {
            // This is okay to run with the "-s" argument.
            final MessageResult<List<IFileSpec>> results =
                exec.loadOpenedFiles(getClientRootSpecs(exec.getProject(), alerts), true);
            if (!alerts.addWarnings(P4Bundle.message("error.load-opened", cache.getClientName()), results, true)) {
                lastRefreshed = new Date();

                // Only clear the cache once we know that we have valid results.

                final List<IFileSpec> validSpecs = new ArrayList<IFileSpec>(results.getResult());
                final List<IFileSpec> invalidSpecs = sortInvalidActions(validSpecs);
                addInvalidActionAlerts(alerts, invalidSpecs);

                cachedServerUpdatedFiles.clear();
                cachedServerUpdatedFiles.addAll(cache.fromOpenedToAction(validSpecs, alerts));

                // All the locally pending changes will remain unchanged; it's up to the
                // ServerUpdateAction to correctly handle the differences.
            }
        } catch (VcsException e) {
            alerts.addWarning(P4Bundle.message("error.load-opened", cache.getClientName()), e);
        }
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshed;
    }


    /**
     * Create the edit file update state.  Called whether the file needs to be edited or added.
     *
     * @param file file to add or edit
     * @param changeListId P4 changelist to add the file into.
     * @return the update state
     */
    @Nullable
    public PendingUpdateState addOrEditFile(@NotNull final FilePath file, int changeListId) {
        // Edit operations will need to have the file be writable.  The Perforce
        // command may delay when the edit action actually occurs, so ensure
        // the file is set to writable first.
        makeWritable(file);

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(file, FileUpdateAction.ADD_EDIT_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), file.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(file.getIOFile().getAbsolutePath()),
                params);
    }


    /**
     * Create the edit file update state.  Called whether the file needs to be edited or added.
     *
     * @param file         file to edit
     * @param changeListId P4 changelist to add the file into.
     * @return the update state
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull final VirtualFile file, int changeListId) {
        // Edit operations will need to have the file be writable.  The Perforce
        // command may delay when the edit action actually occurs, so ensure
        // the file is set to writable first.
        FilePath fp = FilePathUtil.getFilePath(file);
        makeWritable(fp);

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(fp, FileUpdateAction.EDIT_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), fp.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(fp.getIOFile().getAbsolutePath()),
                params);
    }


    @Nullable
    public PendingUpdateState deleteFile(@NotNull FilePath file, int changeListId) {
        // Let the IDE deal with the actual removal of the file.
        // But just to be sure, make it writable first.
        if (file.getIOFile().exists()) {
            makeWritable(file);
        }

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(file, FileUpdateAction.DELETE_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), file.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(file.getIOFile().getAbsolutePath()),
                params);
    }


    @Nullable
    private P4FileUpdateState getCachedUpdateState(@NotNull final FilePath file) {
        P4FileUpdateState action = getUpdateStateFrom(file, localClientUpdatedFiles);
        if (action != null) {
            return action;
        }
        return getUpdateStateFrom(file, cachedServerUpdatedFiles);
    }

    @Nullable
    private P4FileUpdateState getUpdateStateFrom(@NotNull final FilePath file,
            @NotNull final Set<P4FileUpdateState> updatedFiles) {
        for (P4FileUpdateState updatedFile : updatedFiles) {
            if (file.equals(updatedFile.getLocalFilePath())) {
                return updatedFile;
            }
        }
        return null;
    }


    private void addInvalidActionAlerts(@NotNull final AlertManager alerts, @NotNull final List<IFileSpec> invalidSpecs) {
        if (invalidSpecs.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(P4Bundle.message("error.opened-action-status.invalid"));
        String sep = "";
        for (IFileSpec spec: invalidSpecs) {
            sb.append(sep);
            if (spec.getStatusMessage() != null) {
                P4Bundle.message("file.invalid.action.spec.message",
                        spec.getDepotPathString(),
                        spec.getClientPathString(),
                        spec.getAction(),
                        spec.getStatusMessage());
            } else {
                P4Bundle.message("file.invalid.action.spec.no-message",
                        spec.getDepotPathString(),
                        spec.getClientPathString(),
                        spec.getAction(),
                        spec.getStatusMessage());
            }
            sep = P4Bundle.message("file.invalid.action.spec.seperator");
        }
        alerts.addNotice(sb.toString(), null);
    }

    @NotNull
    private List<IFileSpec> sortInvalidActions(@NotNull List<IFileSpec> validSpecs) {
        List<IFileSpec> ret = new ArrayList<IFileSpec>(validSpecs.size());
        Iterator<IFileSpec> iter = validSpecs.iterator();
        while (iter.hasNext()) {
            final IFileSpec next = iter.next();
            if (next != null) {
                if (! isValidUpdateAction(next) || next.getClientPathString() == null || next.getDepotPathString() == null) {
                    // FIXME debug
                    LOG.info("invalid spec: " + next + ": action: " + next.getAction() + "; client path string: " +
                        next.getClientPathString() + "; depot path string: " + next.getDepotPathString());
                    ret.add(next);
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        return ret;
    }

    private boolean isValidUpdateAction(IFileSpec spec) {
        final FileAction action = spec.getAction();
        return action != null && UpdateAction.getUpdateActionForOpened(action) != null;
    }

    /**
     *
     * @return all the "..." directory specs for the roots of this client.
     * @param project project
     * @param alerts alerts manager
     */
    private List<IFileSpec> getClientRootSpecs(@NotNull Project project, @NotNull AlertManager alerts) throws VcsException {
        final List<VirtualFile> roots = cache.getClientRoots(project, alerts);
        return FileSpecUtil.makeRootFileSpecs(roots.toArray(new VirtualFile[roots.size()]));
    }

    private void makeWritable(@NotNull final FilePath file) {
        // write actions can only be in the dispatch thread.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (file.getVirtualFile() != null) {
                            file.getVirtualFile().setWritable(true);
                        }
                    } catch (IOException e) {
                        // ignore for now
                        LOG.info(e);
                    }
                }
            });
        }


        File f = file.getIOFile();
        if (f.exists() && f.isFile() && !f.canWrite()) {
            if (!f.setWritable(true)) {
                // FIXME get access to the alerts in a better way
                AlertManager.getInstance()
                        .addWarning(P4Bundle.message("error.writable.failed", file), null);

                // Keep going with the action, even though we couldn't set it to
                // writable...
            }
        }
    }


    @Nullable
    private P4FileUpdateState createFileUpdateState(@NotNull FilePath file,
            @NotNull FileUpdateAction fileUpdateAction, int changeListId) {
        // Check if it is already in an action state.
        P4FileUpdateState action = getCachedUpdateState(file);
        if (action != null && fileUpdateAction.equals(action.getFileUpdateAction())) {
            LOG.info("Already opened for " + fileUpdateAction + " " + file);

            // TODO this might be wrong - the file may need to move to a different changelist.

            return null;
        }
        // If the action already exists but isn't the right update action,
        // we still need to create a new one to put in our local cache.
        P4FileUpdateState newAction = new P4FileUpdateState(
                cache.getClientMappingFor(file), changeListId, fileUpdateAction);
        localClientUpdatedFiles.add(newAction);

        // FIXME debug
        LOG.info("Switching from " + action + " to " + newAction);

        return newAction;
    }


    private static boolean isNotKnownToServer(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                spec.getStatusMessage().hasMessageFragment(" - no such file(s).");
    }

    private static boolean isNotInClientView(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                spec.getStatusMessage().hasMessageFragment(" is not under client's root ");
    }

    private static boolean isStatusMessage(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.INFO ||
                spec.getOpStatus() == FileSpecOpStatus.CLIENT_ERROR ||
                spec.getOpStatus() == FileSpecOpStatus.UNKNOWN;
    }

    @NotNull
    private static List<FilePath> getFilePaths(@NotNull final List<PendingUpdateState> updateList) {
        List<FilePath> filenames = new ArrayList<FilePath>(updateList.size());
        final Iterator<PendingUpdateState> iter = updateList.iterator();
        while (iter.hasNext()) {
            PendingUpdateState update = iter.next();
            String val = UpdateParameterNames.FILE.getParameterValue(update);
            if (val != null) {
                filenames.add(FilePathUtil.getFilePath(val));
            } else {
                iter.remove();
            }
        }
        return filenames;
    }

    static class ActionSplit {
        ExecutionStatus status;
        final Map<Integer, Set<FilePath>> notInPerforce = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> notOpened = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> edited = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> deleted = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> integrated = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> move_deleted = new HashMap<Integer, Set<FilePath>>();

        ActionSplit(@NotNull P4Exec2 exec,
                @NotNull Collection<PendingUpdateState> pendingUpdateStates,
                @NotNull AlertManager alerts,
                boolean ignoreAddsIfEditOnly) {
            List<PendingUpdateState> updateList = new ArrayList<PendingUpdateState>(pendingUpdateStates);
            List<FilePath> filenames = getFilePaths(updateList);
            final List<IFileSpec> srcSpecs;
            try {
                srcSpecs = FileSpecUtil.getFromFilePaths(filenames);
            } catch (P4Exception e) {
                alerts.addWarning(P4Bundle.message("error.file-spec.create", filenames), e);
                status = ExecutionStatus.FAIL;
                return;
            }
            LOG.info("File specs: " + srcSpecs);
            final List<IExtendedFileSpec> fullSpecs;
            try {
                fullSpecs = exec.getFileStatus(srcSpecs);
            } catch (VcsException e) {
                alerts.addWarning(P4Bundle.message("error.file-status.fetch", srcSpecs), e);
                status = ExecutionStatus.FAIL;
                return;
            }
            LOG.info("Full specs: " + fullSpecs);

            Iterator<PendingUpdateState> updatesIter = updateList.iterator();
            for (IExtendedFileSpec spec : fullSpecs) {

                if (isStatusMessage(spec)) {
                    // no corresponding file, so don't increment the update
                    // however, it could mean that the file isn't on the server.
                    // there's the chance that it means that the file isn't
                    // in the client view.  Generally, that's an ERROR status.
                    LOG.info("fstat non-valid state: " + spec.getOpStatus() + ": " + spec.getStatusMessage());
                    continue;
                }

                // Valid response.  Advance iterators
                final PendingUpdateState update = updatesIter.next();
                Integer changelist = UpdateParameterNames.CHANGELIST.getParameterValue(update);
                if (changelist == null) {
                    changelist = -1;
                }
                String filename = UpdateParameterNames.FILE.getParameterValue(update);
                // is edit only mode is only valid for the EDIT_FILE action.
                boolean isEditOnly = update.getUpdateAction() == UpdateAction.EDIT_FILE;
                FilePath filePath = FilePathUtil.getFilePath(filename);
                LOG.info(" - Checking category for " + filePath + " (from " + spec + ")");
                if (isNotInClientView(spec)) {
                    alerts.addNotice(P4Bundle.message("error.client.not-in-view", spec.getClientPathString()), null);
                    // don't handle
                    continue;
                }
                Map<Integer, Set<FilePath>> container = null;
                if (isNotKnownToServer(spec)) {
                    if (isEditOnly && ignoreAddsIfEditOnly) {
                        LOG.info(" -+- Ignoring because Perforce doesn't know it, and the change is edit-only.");
                    } else {
                        LOG.info(" -+- not known to server");
                        container = notInPerforce;
                    }
                } else {
                    FileAction action = spec.getOpenAction();
                    if (action == null) {
                        // In perforce, not already opened.
                        container = notOpened;
                    } else {
                        switch (action) {
                            case ADD_EDIT:
                            case ADDED:
                            case EDIT:
                            case EDIT_FROM:
                            case MOVE_ADD:
                                // open for add or edit.
                                container = edited;
                                break;
                            case INTEGRATE:
                            case BRANCH:
                                // Integrated, but not open for edit.  Open for edit.
                                container = integrated;
                                break;

                            case MOVE_DELETE:
                                // On the delete side of a move.  Mark as open for edit.
                                container = move_deleted;
                                break;
                            case DELETE:
                            case DELETED:
                                // Already open for delete; need to revert the file and edit.
                                // Because it's open for delete, that means the server knows
                                // about it.

                                LOG.info("Reverting delete and opening for edit: " + filename);
                                container = deleted;
                                break;
                            default:
                                LOG.error("Unexpected file action " + action + " from fstat");
                                // do nothing
                        }
                    }
                }
                if (container != null) {
                    Set<FilePath> set = container.get(changelist);
                    if (set == null) {
                        set = new HashSet<FilePath>();
                        container.put(changelist, set);
                    }
                    set.add(filePath);
                }
            }
        }
    }

    private static Map<Integer, Set<FilePath>> joinChangelistFiles(final Map<Integer, Set<FilePath>>... mList) {
        Map<Integer, Set<FilePath>> ret = new HashMap<Integer, Set<FilePath>>();
        for (Map<Integer, Set<FilePath>> m : mList) {
            for (Entry<Integer, Set<FilePath>> entry : m.entrySet()) {
                final Set<FilePath> fpSet = ret.get(entry.getKey());
                if (fpSet == null) {
                    ret.put(entry.getKey(), entry.getValue());
                } else {
                    fpSet.addAll(entry.getValue());
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
    // Add/Edit Action

    public static class AddEditFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new AddEditAction(states);
        }
    }

    static class AddEditAction extends AbstractServerUpdateAction {
        AddEditAction(@NotNull Collection<PendingUpdateState> pendingUpdateState) {
            super(pendingUpdateState);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull ClientCacheManager clientCacheManager, @NotNull final AlertManager alerts) {
            // FIXME debug
            LOG.info("Running edit");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, true);
            if (split.status != null) {
                return split.status;
            }

            // open for add, only if allowsAdd is true:
            //     split.notInPerforce
            // open for edit:
            //     split.notOpened
            // ignore (already open for edit or add)
            //     split.edited
            // open for edit (integrated, but not open for edit)
            //     split.integrated
            // on the delete side of a move; open for edit
            //     split.move_deleted
            // already open for delete; revert and edit
            // (because it is open for delete, the server knows about it).
            //     split.deleted

            // Perform the reverts first
            boolean hasUpdate = false;
            if (! split.deleted.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<P4StatusMessage> msgs =
                            exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    alerts.addNotices(P4Bundle.message("warning.edit.file.revert", reverts), msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(P4Bundle.message("error.revert", reverts), e);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }

            ExecutionStatus returnCode = ExecutionStatus.NO_OP;
            if (! split.notInPerforce.isEmpty()) {
                for (Entry<Integer, Set<FilePath>> entry : split.notInPerforce.entrySet()) {
                    try {
                        final List<P4StatusMessage> msgs =
                                exec.addFiles(FileSpecUtil.getFromFilePaths(entry.getValue()), entry.getKey());
                        alerts.addNotices(P4Bundle.message("warning.edit.file.add", entry.getValue()), msgs, false);
                        hasUpdate = true;
                    } catch (P4DisconnectedException e) {
                        // error already handled as critical
                        return ExecutionStatus.RETRY;
                    } catch (VcsException e) {
                        alerts.addWarning(P4Bundle.message("error.add", entry.getValue()), e);
                        returnCode = ExecutionStatus.FAIL;
                    }
                }
            } else if (! split.notInPerforce.isEmpty()) {
                LOG.info("Skipping add (because command is edit-only: " + split.notInPerforce);
            }

            // open for edit:
            //     split.notOpened
            // open for edit (integrated, but not open for edit)
            //     split.integrated
            // on the delete side of a move; open for edit
            //     split.move_deleted
            // already open for delete; revert and edit
            //     split.deleted
            @SuppressWarnings("unchecked") Map<Integer, Set<FilePath>> edits = joinChangelistFiles(
                    split.notOpened, split.integrated, split.move_deleted, split.deleted);


            for (Entry<Integer, Set<FilePath>> entry : edits.entrySet()) {
                try {
                    final List<P4StatusMessage> msgs =
                            exec.editFiles(FileSpecUtil.getFromFilePaths(entry.getValue()),
                                    entry.getKey());
                    // Note: any errors here will be displayed to the
                    // user, but they do not indicate that the actual
                    // actions were errors.  Instead, they are notifications
                    // to the user that they must do something again
                    // with a correction.
                    alerts.addWarnings(P4Bundle.message("warning.edit.file.edit", entry.getValue()), msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(P4Bundle.message("error.edit", entry.getValue()), e);
                    returnCode = ExecutionStatus.FAIL;
                }
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Override
        @Nullable
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
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


    static class DeleteAction extends AbstractServerUpdateAction {

        DeleteAction(final Collection<PendingUpdateState> states) {
            super(states);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            // FIXME debug
            LOG.info("Running delete");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, false);
            if (split.status != null) {
                return split.status;
            }

            // ignore, because perforce doesn't know about it
            //     split.notInPerforce
            // open for delete:
            //     split.notOpened
            // revert and delete
            //     split.edited
            // revert and delete
            //     split.integrated
            // on the delete side of a move; ignore
            //     split.move_deleted
            // already open for delete; ignore
            //     split.deleted

            boolean hasUpdate = false;
            ExecutionStatus returnCode = ExecutionStatus.NO_OP;

            // Perform the reverts first
            @SuppressWarnings("unchecked") final Map<Integer, Set<FilePath>> revertSets =
                    joinChangelistFiles(split.edited, split.integrated);
            if (!revertSets.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<P4StatusMessage> msgs =
                            exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    alerts.addNotices(P4Bundle.message("warning.edit.file.revert", reverts), msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(P4Bundle.message("error.revert", reverts), e);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }


            @SuppressWarnings("unchecked") Map<Integer, Set<FilePath>> deletes = joinChangelistFiles(
                    split.notOpened, split.integrated, split.edited);

            for (Entry<Integer, Set<FilePath>> entry : deletes.entrySet()) {
                try {
                    final List<P4StatusMessage> msgs =
                            exec.deleteFiles(FileSpecUtil.getFromFilePaths(entry.getValue()),
                                    entry.getKey(), true);
                    // Note: any errors here will be displayed to the
                    // user, but they do not indicate that the actual
                    // actions were errors.  Instead, they are notifications
                    // to the user that they must do something again
                    // with a correction.
                    alerts.addWarnings(P4Bundle.message("warning.edit.file.edit", entry.getValue()), msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(P4Bundle.message("error.edit", entry.getValue()), e);
                    returnCode = ExecutionStatus.FAIL;
                }
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Nullable
        @Override
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }


}
