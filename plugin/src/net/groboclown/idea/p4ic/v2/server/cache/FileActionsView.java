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

package net.groboclown.idea.p4ic.v2.server.cache;

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
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Keeps track of all the file actions (files open for edit, move, etc).
 * All actions must be performed from within a
 * {@link net.groboclown.idea.p4ic.v2.server.connection.ServerConnection}
 * in order to ensure correct asynchronous behavior.
 */
public class FileActionsView extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(FileActionsView.class);

    private final Project project;
    private final Cache cache;
    private final Set<P4FileUpdateState> localClientUpdatedFiles;
    private final Set<P4FileUpdateState> cachedServerUpdatedFiles;


    public FileActionsView(@NotNull Project project, @NotNull final Cache cache,
            @NotNull final Set<P4FileUpdateState> localClientUpdatedFiles,
            @NotNull final Set<P4FileUpdateState> cachedServerUpdatedFiles) {
        this.project = project;
        this.cache = cache;
        this.localClientUpdatedFiles = localClientUpdatedFiles;
        this.cachedServerUpdatedFiles = cachedServerUpdatedFiles;
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
    void loadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        // Load our cache.  Note that we only load specs that we consider to be in a
        // "valid" file action state.

        MessageResult<List<IFileSpec>> results = null;
        try {
            // This is okay to run with the "-s" argument.
            results = exec.loadOpenedFiles(project,
                    getClientRootSpecs(), true);
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


    /**
     *
     *
     * @param file
     * @param changeListId
     * @return
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull FilePath file, int changeListId) {
        // First check if it is already in an action state.
        if (cache.isFileIgnored(file)) {
            return null;
        }
        P4FileUpdateState action = getCachedUpdateState(file);
        if (action != null &&
                UpdateAction.EDIT_FILE.equals(action.getFileUpdateAction().getUpdateAction())) {
            LOG.info("Already opened for edit " + file);
            return null;
        }
        // Even if the action already exists, we need to create a new one to put in our local cache.
        P4FileUpdateState newAction = new P4FileUpdateState(
                cache.getClientMappingFor(file), changeListId, FileUpdateAction.EDIT_FILE);
        localClientUpdatedFiles.add(newAction);
        LOG.debug("Switching from " +action + " to " + newAction);

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
        for (IFileSpec spec: invalidSpecs) {
            sb
                .append(spec.getDepotPathString() != null
                    ? spec.getDepotPathString()
                    : spec.getClientPathString())
                .append(": ")
                .append(spec.getAction());
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
     */
    private List<IFileSpec> getClientRootSpecs() throws VcsException {
        final List<VirtualFile> roots = cache.getClient().getRoots();
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
        public ServerUpdateAction create(@NotNull final Collection<PendingUpdateState> states) {
            return new AddEditAction(states);
        }
    }


    static class AddEditAction extends AbstractServerUpdateAction {

        AddEditAction(@NotNull final Collection<PendingUpdateState> pendingUpdateState) {
            super(pendingUpdateState);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec, @NotNull final P4Server server,
                @NotNull ClientCacheManager clientCacheManager, @NotNull final AlertManager alerts) {
            // Discover the current state of the files, so that we can perform the
            // appropriate actions.
            List<PendingUpdateState> updateList = new ArrayList<PendingUpdateState>(getPendingUpdateStates());
            List<FilePath> filenames = new ArrayList<FilePath>(updateList.size());
            for (PendingUpdateState update: updateList) {
                String val = UpdateParameterNames.FILE.getParameterValue(update);
                if (val != null) {
                    filenames.add(FilePathUtil.getFilePath(val));
                }
            }
            final List<IFileSpec> srcSpecs;
            try {
                srcSpecs = FileSpecUtil.getFromFilePaths(filenames);
            } catch (P4Exception e) {
                alerts.addWarning(P4Bundle.message("error.file-spec.create", filenames), e);
                return ExecutionStatus.FAIL;
            }
            final List<IExtendedFileSpec> fullSpecs;
            try {
                fullSpecs = exec.getFileStatus(server.getProject(), srcSpecs);
            } catch (VcsException e) {
                alerts.addWarning(P4Bundle.message("error.file-status.fetch", srcSpecs), e);
                return ExecutionStatus.FAIL;
            }

            Map<Integer, Set<FilePath>> adds = new HashMap<Integer, Set<FilePath>>();
            Map<Integer, Set<FilePath>> edits = new HashMap<Integer, Set<FilePath>>();
            Iterator<PendingUpdateState> updatesIter = updateList.iterator();
            for (IExtendedFileSpec spec : fullSpecs) {
                if (spec.getOpStatus() == FileSpecOpStatus.INFO ||
                        spec.getOpStatus() == FileSpecOpStatus.CLIENT_ERROR ||
                        spec.getOpStatus() == FileSpecOpStatus.UNKNOWN) {
                    // no corresponding file, so don't increment the update
                    continue;
                }
                final PendingUpdateState update = updatesIter.next();
                Integer changelist = UpdateParameterNames.CHANGELIST.getParameterValue(update);
                if (changelist == null) {
                    changelist = -1;
                }
                if (spec.getOpStatus() == FileSpecOpStatus.ERROR) {
                    // File not on server?
                    // FIXME
                    throw new IllegalStateException("not implemented");
                }
                FileAction action = spec.getOpenAction();
                Set<FilePath> updateSet;
                if (action == null) {
                    // In perforce, not already opened.
                    //action = spec.getAction()
                    updateSet = edits.get(changelist);
                    if (updateSet == null) {
                        updateSet = new HashSet<FilePath>();
                        edits.put(changelist, updateSet);
                    }
                    // FIXME
                    throw new IllegalStateException("not implemented");
                } else {
                    switch (action) {
                        case ADD:
                        case ADDED:
                        case EDIT:
                        case EDIT_FROM:
                        case MOVE_ADD:
                            // Already in the correct state - open for add or edit.
                            // FIXME
                            throw new IllegalStateException("not implemented");
                            //break;
                        case INTEGRATE:
                        case BRANCH:
                            // Integrated, but not open for edit.
                            // FIXME
                            throw new IllegalStateException("not implemented");
                            //break;
                        case DELETE:
                        case DELETED:
                            // FIXME
                            throw new IllegalStateException("not implemented");
                            //break;
                        case MOVE_DELETE:
                            // FIXME
                            throw new IllegalStateException("not implemented");
                            //break;
                        default:
                            LOG.error("Unexpected file action " + action + " from fstat");
                            // do nothing
                    }
                }
            }


            for (PendingUpdateState update : getPendingUpdateStates()) {
                String filename = UpdateParameterNames.FILE.getValue(
                        update.getParameters().get(UpdateParameterNames.FILE.getKeyName()));
                Integer changelist = UpdateParameterNames.CHANGELIST.getValue(
                        update.getParameters().get(UpdateParameterNames.CHANGELIST.getKeyName()));

                // FIXME
                throw new IllegalStateException("not implemented");

                /*
                // The only time we need to switch an action to something other than
                // edit is when the existing action is "delete".
                final UpdateAction oldAction = update.getUpdateAction();
                if (oldAction == UpdateAction.DELETE_FILE ||
                        oldAction == FileUpdateAction.MOVE_DELETE_FILE) {
                    action.setFileUpdateAction(FileUpdateAction.ADD_FILE);
                } else {
                    action.setFileUpdateAction(FileUpdateAction.EDIT_FILE);
                }
                */
            }


            return null;
        }

        @Override
        protected void updateCache(@NotNull final P4Server server, @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            // FIXME
            throw new IllegalStateException("not implemented");
        }
    }
}
