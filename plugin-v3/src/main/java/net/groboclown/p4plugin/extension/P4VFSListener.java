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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsFileUtil;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.util.ChangelistUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the file change requests: add, edit, delete, move.
 */
public class P4VFSListener extends VcsVFSListener {
    private static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

    P4VFSListener(@NotNull Project project,
            @NotNull P4Vcs vcs) {
        super(project, vcs);
    }

    @Override
    protected void performAdding(
            @NotNull final Collection<VirtualFile> addedFiles,
            @NotNull final Map<VirtualFile, VirtualFile> copyFromMap) {

        // TODO all add requests must go through the IgnoreFileSet.

        // Copies are handled as add commands, so we don't need to worry about
        // performing integrations - this is the common use case desired by the
        // user - they want to use another file as a template for work.
        // This could eventually be supported as a feature flag if the user really
        // wants it.  If they are implemented, then the "add" files should first be
        // pruned of the "integrate" files.

        // Bug #102: The keys in the "copyFromMap" will also be in the "addedFiles"
        // list.  If copy rather than integrate is supported, this loop will need to be
        // changed.
        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (VirtualFile file : addedFiles) {
            if (file.isDirectory()) {
                LOG.warn("Attempted to add a directory " + file);
                continue;
            }
            ClientConfigRoot root = getClientFor(file);
            if (root != null) {
                FilePath fp = VcsUtil.getFilePath(file);
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for add/edit: " + fp + " (@" + id + ")");
                }
                P4ServerComponent
                .perform(myProject, root.getClientConfig(),
                        new AddEditAction(fp, getFileType(fp), id, (String) null))
                .whenAnyState(() -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Completed call to add " + addedFiles + "; copy " + copyFromMap);
                    }
                });
            } else {
                LOG.info("Skipped adding " + file + "; not under known P4 client");
            }
        }
    }

    @Override
    protected void performDeletion(List<FilePath> filesToDelete) {
        VcsFileUtil.markFilesDirty(myProject, filesToDelete);
        final List<VirtualFile> affectedFiles = filesToDelete.stream().map(FilePath::getVirtualFile)
                .collect(Collectors.toList());

                Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (FilePath filePath : filesToDelete) {
            ClientConfigRoot root = getClientFor(filePath);
            if (root != null) {
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for delete: " + filePath + " (@" + id + ")");
                }
                P4ServerComponent
                .perform(myProject, root.getClientConfig(),
                        new DeleteFileAction(filePath, id))
                .whenAnyState(() -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Completed call to delete file " + filesToDelete);
                    }
                });
            } else {
                LOG.info("Skipped deleting " + filePath + "; not under known P4 client");
            }
        }
    }

    @Override
    protected void performMoveRename(List<MovedFileInfo> movedFiles) {
        Set<FilePath> allFiles = new HashSet<>(movedFiles.size());
        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();

        // All the move operations need to complete before we can request an update to the changelist view
        List<P4CommandRunner.ActionAnswer<?>> pendingAnswers = new ArrayList<>();

        for (MovedFileInfo movedFile : movedFiles) {
            LOG.info("Moving file `" + movedFile.myOldPath + "` to `" + movedFile.myNewPath + "`");
            final FilePath src = VcsUtil.getFilePath(movedFile.myOldPath);
            if (src.isDirectory()) {
                LOG.warn("Attempted to move directory " + src + "; refusing move.");
                continue;
            }

            // For a single move request, all requests must be run serially.
            P4CommandRunner.ActionAnswer<?> pending = new DoneActionAnswer<>(null);

            final FilePath tgt;
            if (VcsUtil.getFilePath(movedFile.myNewPath).isDirectory()) {
                LOG.info("Moving file into directory " + movedFile.myNewPath);
                tgt = VcsUtil.getFilePath(new File(VcsUtil.getFilePath(movedFile.myNewPath).getIOFile(), src.getName()));
            } else {
                tgt = VcsUtil.getFilePath(movedFile.myNewPath);
            }
            allFiles.add(src);
            allFiles.add(tgt);
            ClientConfigRoot srcRoot = getClientFor(src);
            ClientConfigRoot tgtRoot = getClientFor(tgt);
            if (srcRoot != null && tgtRoot != null &&
                    srcRoot.getClientConfig().getClientServerRef().equals(tgtRoot.getClientConfig().getClientServerRef())) {
                // A real P4 move operation.
                P4ChangelistId id = getActiveChangelistFor(srcRoot, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for move: " + src + " -> " + tgt + " (@" + id + ")");
                }
                pending = pending.mapActionAsync((x) ->
                        P4ServerComponent.perform(myProject, srcRoot.getClientConfig(),
                                new MoveFileAction(src, tgt, id)));
            } else {
                // Not a move operation, because they aren't in the same perforce client.

                if (srcRoot != null) {
                    // Source is in P4, so delete it.
                    P4ChangelistId id = getActiveChangelistFor(srcRoot, activeChangelistIds);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Opening for move/delete: " + src + " (@" + id + ")");
                    }
                    pending = pending.mapActionAsync((x) ->
                            P4ServerComponent.perform(myProject, srcRoot.getClientConfig(),
                                    new DeleteFileAction(src, id)));
                }

                if (tgtRoot != null) {
                    if (srcRoot != null &&
                            srcRoot.getServerConfig().getServerName().equals(tgtRoot.getServerConfig().getServerName())) {
                        // Source and target are on the same server.  We can perform an integrate here.
                        // TODO perform an integrate
                    }
                    // Just a regular Perforce add
                    P4ChangelistId id = getActiveChangelistFor(tgtRoot, activeChangelistIds);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Opening for move/add-edit: " + tgt + " (@" + id + ")");
                    }
                    pending = pending.mapActionAsync((x) ->
                            P4ServerComponent.perform(myProject, tgtRoot.getClientConfig(),
                                    new AddEditAction(tgt, null, id, (String) null)));
                }
            }

            pending.whenAnyState(() -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Completed move request for " + src + " -> " + tgt);
                }
            });

            pendingAnswers.add(pending);
        }

        /*
        // This method is called from the EDT, so DO NOT WAIT IN THIS THREAD.
        // Wait for the pending requests to complete before marking files as dirty.
        for (P4CommandRunner.ActionAnswer<?> pendingAnswer : pendingAnswers) {
            try {
                pendingAnswer.waitForCompletion(UserProjectPreferences.getLockWaitTimeoutMillis(myProject),
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.warn("Interruption waiting on move commands to complete.", e);
            }
        }

        // Make sure any potential null isn't in the set.
        allFiles.remove(null);
        VcsFileUtil.markFilesDirty(myProject, new ArrayList<>(allFiles));
        */
    }

    @Override
    protected String getAddTitle() {
        return P4Bundle.message("vfs.add.files");
    }

    @Override
    protected String getSingleFileAddTitle() {
        return P4Bundle.message("vfs.add.file");
    }

    @Override
    protected String getSingleFileAddPromptTemplate() {
        return P4Bundle.getString("vfs.add.single.prompt");
    }

    @Override
    protected String getDeleteTitle() {
        return P4Bundle.message("vfs.delete.files");
    }

    @Override
    protected String getSingleFileDeleteTitle() {
        return P4Bundle.message("vfs.delete.file");
    }

    @Override
    protected String getSingleFileDeletePromptTemplate() {
        return P4Bundle.getString("vfs.delete.single.prompt");
    }

    @Override
    protected boolean isDirectoryVersioningSupported() {
        return false;
    }


    private Map<ClientServerRef, P4ChangelistId> getActiveChangelistIds() {
        return ChangelistUtil.getActiveChangelistIds(myProject);
    }

    @NotNull
    private P4ChangelistId getActiveChangelistFor(ClientConfigRoot root, Map<ClientServerRef, P4ChangelistId> ids) {
        return ChangelistUtil.getActiveChangelistFor(root, ids);
    }

    // TODO this is shared with P4CheckinEnvironment
    private P4FileType getFileType(FilePath fp) {
        FileType ft = fp.getFileType();
        if (ft.isBinary()) {
            return P4FileType.convert("binary");
        }
        return P4FileType.convert("text");
    }

    private ClientConfigRoot getClientFor(FilePath file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(myProject);
        return reg == null ? null : reg.getClientFor(file);
    }

    private ClientConfigRoot getClientFor(VirtualFile file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(myProject);
        return reg == null ? null : reg.getClientFor(file);
    }
}
