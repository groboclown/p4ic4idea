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
import net.groboclown.idea.p4ic.v2.server.cache.*;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            if (state.getLastUpdated().getTime() > lastRefreshed.getTime()) {
                lastRefreshed = state.getLastUpdated();
            }
        }
    }


    /**
     * Return all the file actions.  If the executor is null, then this will work in offline mode.
     * Must be called by the {@link net.groboclown.idea.p4ic.v2.server.connection.ServerConnection}
     * to correctly handle the locking.
     *
     * @param exec server connection, or {@code null} if not connected.
     * @return file action version actions
     */
    Collection<P4FileAction> getFileActions(@Nullable P4Exec2 exec, @NotNull AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        if (exec != null) {
            // FIXME needs to be on a timer for refresh rate.
            loadServerCache(exec, alerts);
        }
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
        lastRefreshed = new Date();

        // Load our cache.  Note that we only load specs that we consider to be in a
        // "valid" file action state.

        MessageResult<List<IFileSpec>> results = null;
        try {
            // This is okay to run with the "-s" argument.
            results = exec.loadOpenedFiles(getClientRootSpecs(exec.getProject(), alerts), true);
        } catch (VcsException e) {
            alerts.addWarning(P4Bundle.message("error.load-opened", cache.getClientName()), e);
        }
        if (results != null && !results.isError()) {
            // Only clear the cache once we know that we have valid results.

            final List<IFileSpec> validSpecs = new ArrayList<IFileSpec>(results.getResult());
            final List<IFileSpec> invalidSpecs = sortInvalidActions(validSpecs);
            addInvalidActionAlerts(alerts, invalidSpecs);

            cachedServerUpdatedFiles.clear();
            cachedServerUpdatedFiles.addAll(cache.fromOpenedToAction(validSpecs, alerts));

            // All the locally pending changes will remain unchanged; it's up to the
            // ServerUpdateAction to correctly handle the differences.
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
     * @param file file to edit
     * @param changeListId P4 changelist to add the file into.
     * @return the update state
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull FilePath file, int changeListId) {
        // Only ignore the file if we know it's an "add" operation.  Thus, we can't
        // determine that until we're connected.

        // Check if it is already in an action state.
        P4FileUpdateState action = getCachedUpdateState(file);
        if (action != null &&
                UpdateAction.EDIT_FILE.equals(action.getFileUpdateAction().getUpdateAction())) {
            LOG.info("Already opened for edit " + file);
            return null;
        }
        // If the action already exists but isn't the right update action,
        // we still need to create a new one to put in our local cache.
        P4FileUpdateState newAction = new P4FileUpdateState(
                cache.getClientMappingFor(file), changeListId, FileUpdateAction.EDIT_FILE);
        localClientUpdatedFiles.add(newAction);

        // FIXME debug
        LOG.info("Switching from " + action + " to " + newAction);

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), file.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                newAction.getFileUpdateAction().getUpdateAction(),
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
            // TODO localize these strings
            sb
                .append(sep).append("[")
                .append("depot:").append(spec.getDepotPathString())
                .append(", client:").append(spec.getClientPathString())
                .append(", action: ").append(spec.getAction());
            sep = "]; ";
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
     * @param project
     * @param alerts
     */
    private List<IFileSpec> getClientRootSpecs(@NotNull Project project, @NotNull AlertManager alerts) throws VcsException {
        final List<VirtualFile> roots = cache.getClientRoots(project, alerts);
        return FileSpecUtil.makeRootFileSpecs(roots.toArray(new VirtualFile[roots.size()]));
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

            // Discover the current state of the files, so that we can perform the
            // appropriate actions.
            List<PendingUpdateState> updateList = new ArrayList<PendingUpdateState>(getPendingUpdateStates());
            List<FilePath> filenames = getFilePaths(updateList);

            LOG.info("Editing files: " + filenames);

            final List<IFileSpec> srcSpecs;
            try {
                srcSpecs = FileSpecUtil.getFromFilePaths(filenames);
            } catch (P4Exception e) {
                alerts.addWarning(P4Bundle.message("error.file-spec.create", filenames), e);
                return ExecutionStatus.FAIL;
            }
            LOG.info("File specs: " + srcSpecs);
            final List<IExtendedFileSpec> fullSpecs;
            try {
                fullSpecs = exec.getFileStatus(srcSpecs);
            } catch (VcsException e) {
                alerts.addWarning(P4Bundle.message("error.file-status.fetch", srcSpecs), e);
                return ExecutionStatus.FAIL;
            }
            LOG.info("Full specs: " + fullSpecs);

            Set<FilePath> reverts = new HashSet<FilePath>();
            Map<Integer, Set<FilePath>> adds = new HashMap<Integer, Set<FilePath>>();
            Map<Integer, Set<FilePath>> edits = new HashMap<Integer, Set<FilePath>>();
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
                String filename = UpdateParameterNames.FILE.getValue(
                        update.getParameters().get(UpdateParameterNames.FILE.getKeyName()));
                FilePath filePath = FilePathUtil.getFilePath(filename);
                if (isNotInClientView(spec)) {
                    alerts.addNotice(P4Bundle.message("error.client.not-in-view", spec.getClientPathString()), null);
                    // don't handle
                    continue;
                }
                Map<Integer, Set<FilePath>> container = null;
                if (isNotKnownToServer(spec)) {
                    LOG.info("Adding, because not known to server");
                    container = adds;
                } else {
                    FileAction action = spec.getOpenAction();
                    if (action == null) {
                        // In perforce, not already opened.
                        container = edits;
                    } else {
                        switch (action) {
                            case ADD:
                            case ADDED:
                            case EDIT:
                            case EDIT_FROM:
                            case MOVE_ADD:
                                // Already in the correct state - open for add or edit.
                                LOG.info("Already open for add or edit: " + filename);
                                continue;
                            case INTEGRATE:
                            case BRANCH:
                                // Integrated, but not open for edit.  Open for edit.

                            case MOVE_DELETE:
                                // On the delete side of a move.  Mark as open for edit.

                                LOG.info("Marking integrated file as open for edit: " + filename);
                                container = edits;
                                break;
                            case DELETE:
                            case DELETED:
                                // Already open for delete; need to revert the file and edit.
                                // Because it's open for delete, that means the server knows
                                // about it.

                                LOG.info("Reverting delete and opening for edit: " + filename);
                                reverts.add(filePath);
                                container = edits;
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

            // Perform the reverts first
            boolean hasUpdate = false;
            if (! reverts.isEmpty()) {
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
            if (! adds.isEmpty()) {
                for (Entry<Integer, Set<FilePath>> entry : adds.entrySet()) {
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
            }
            if (!edits.isEmpty()) {
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
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        private boolean isNotKnownToServer(@NotNull final IExtendedFileSpec spec) {
            return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                    spec.getStatusMessage().hasMessageFragment(" - no such file(s).");
        }

        private boolean isNotInClientView(@NotNull final IExtendedFileSpec spec) {
            return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                    spec.getStatusMessage().hasMessageFragment(" is not under client's root ");
        }

        private boolean isStatusMessage(@NotNull final IExtendedFileSpec spec) {
            return spec.getOpStatus() == FileSpecOpStatus.INFO ||
                    spec.getOpStatus() == FileSpecOpStatus.CLIENT_ERROR ||
                    spec.getOpStatus() == FileSpecOpStatus.UNKNOWN;
        }

        @NotNull
        private List<FilePath> getFilePaths(@NotNull final List<PendingUpdateState> updateList) {
            List<FilePath> filenames = new ArrayList<FilePath>(updateList.size());
            for (PendingUpdateState update: updateList) {
                String val = UpdateParameterNames.FILE.getParameterValue(update);
                if (val != null) {
                    filenames.add(FilePathUtil.getFilePath(val));
                }
            }
            return filenames;
        }

        @Override
        @Nullable
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }
}
