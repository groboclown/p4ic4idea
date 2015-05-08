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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

public class EditRunner extends ServerTask<List<P4StatusMessage>> {
    private static final Logger LOG = Logger.getInstance(EditRunner.class);

    private final Project project;
    private final Collection<VirtualFile> editedFiles;
    private final int destination;
    private final FileInfoCache fileInfoCache;


    public EditRunner(
            @NotNull Project project, @NotNull Collection<VirtualFile> editedFiles,
            int destination, @NotNull FileInfoCache fileInfoCache) {
        this.project = project;
        this.editedFiles = editedFiles;
        this.destination = destination;
        this.fileInfoCache = fileInfoCache;
    }

    @NotNull
    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        int changelistId = -1;
        if (destination > 0) {
            changelistId = destination;
        }

        List<P4FileInfo> specs = exec.loadFileInfo(project, FileSpecUtil.getFromVirtualFiles(editedFiles), fileInfoCache);

        log("edit request for " + specs);
        List<IFileSpec> reverted = new ArrayList<IFileSpec>();
        List<IFileSpec> edited = new ArrayList<IFileSpec>();
        List<IFileSpec> added = new ArrayList<IFileSpec>();

        for (P4FileInfo spec: specs) {
            if (spec.isInDepot()) {
                if (spec.isDeletedInDepot()) {
                    log("Edit; open for add (deleted in depot) " + spec);
                    added.add(spec.toClientSpec());
                } else if (! spec.isOpenInClient()) {
                    log("Edit: open for edit " + spec);
                    edited.add(spec.toClientSpec());
                } else if (spec.isOpenForDelete()) {
                    // revert then edit; no need to check if added, because
                    // to be deleted means that it exists in the depot.
                    log("Edit: revert for delete then edit " + spec);
                    reverted.add(spec.toDepotSpec());
                    edited.add(spec.toDepotSpec());
                } else {
                    log("Edit: already open for edit " + spec);
                }
            } else if (spec.isInClientView()) {
                log("Edit: open for add " + spec.toClientSpec());
                added.add(spec.toClientSpec());
            } else {
                log("Edit: not in client " + spec);
            }
        }

        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();

        // Revert comes first
        if (! reverted.isEmpty()) {
            ret.addAll(exec.revertFiles(project, reverted));
            LOG.debug("Edit After revert: " + ret);
        }

        if (! edited.isEmpty()) {
            ret.addAll(exec.editFiles(project, edited, changelistId));
            LOG.debug("Edit after edited: " + ret);
        }

        if (! added.isEmpty()) {
            ret.addAll(exec.addFiles(project, added, changelistId));
            LOG.debug("Edit after added: " + ret);
        }
        return ret;
    }
}
