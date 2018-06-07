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
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsFileUtil;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Copies are handled as add commands, so we don't need to worry about
        // performing integrations - this is the common use case desired by the
        // user - they want to use another file as a template for work.
        // This could eventually be supported as a feature flag if the user really
        // wants it.  If they are implemented, then the "add" files should first be
        // pruned of the "integrate" files.

        List<VirtualFile> dirty = new ArrayList<>(addedFiles.size() + copyFromMap.size());
        dirty.addAll(addedFiles);
        dirty.addAll(copyFromMap.keySet());
        VcsFileUtil.markFilesDirty(myProject, dirty);

        // Bug #102: The keys in the "copyFromMap" will also be in the "addedFiles"
        // list.  If copy rather than integrate is supported, this loop will need to be
        // changed.
        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (VirtualFile file : addedFiles) {
            ClientConfigRoot root = getClientFor(file);
            if (root != null) {
                FilePath fp = VcsUtil.getFilePath(file);
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for add/edit: " + fp + " (@" + id + ")");
                }
                P4ServerComponent.getInstance(myProject).getCommandRunner()
                        .perform(root.getClientConfig(), new AddEditAction(fp, getFileType(fp), id, null));
            } else {
                LOG.info("Skipped adding " + file + "; not under known P4 client");
            }
        }
    }

    @Override
    protected void performDeletion(List<FilePath> filesToDelete) {
        VcsFileUtil.markFilesDirty(myProject, filesToDelete);

        Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
        for (FilePath filePath : filesToDelete) {
            ClientConfigRoot root = getClientFor(filePath);
            if (root != null) {
                P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opening for delete: " + filePath + " (@" + id + ")");
                }
                P4ServerComponent.getInstance(myProject).getCommandRunner()
                        .perform(root.getClientConfig(), new DeleteFileAction(filePath, id));
            } else {
                LOG.info("Skipped deleting " + filePath + "; not under known P4 client");
            }
        }
    }

    @Override
    protected void performMoveRename(List<MovedFileInfo> movedFiles) {
        throw new IllegalStateException("not implemented");
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
        LocalChangeList defaultIdeChangeList =
                ChangeListManager.getInstance(myProject).getDefaultChangeList();
        Map<ClientServerRef, P4ChangelistId> ret = new HashMap<>();
        try {
            CacheComponent.getInstance(myProject).getServerOpenedCache().first
                    .getP4ChangesFor(defaultIdeChangeList)
                    .forEach((id) -> ret.put(id.getClientServerRef(), id));
        } catch (InterruptedException e) {
            LOG.warn(e);
        }
        return ret;
    }

    private P4ChangelistId getActiveChangelistFor(ClientConfigRoot root, Map<ClientServerRef, P4ChangelistId> ids) {
        ClientServerRef ref = root.getClientConfig().getClientServerRef();
        P4ChangelistId ret = ids.get(ref);
        if (ret == null) {
            ret = P4ChangelistIdImpl.createDefaultChangelistId(ref);
            ids.put(ref, ret);
        }
        return ret;
    }

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
