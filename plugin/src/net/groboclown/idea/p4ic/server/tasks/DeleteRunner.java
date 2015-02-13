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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

public class DeleteRunner extends ServerTask<List<P4StatusMessage>> {
    private final Project project;
    private final Collection<FilePath> deletedFiles;
    private final int destination;

    public DeleteRunner(
            @NotNull Project project, @NotNull Collection<FilePath> deletedFiles,
            int destination) {
        this.project = project;
        this.deletedFiles = deletedFiles;
        this.destination = destination;
    }

    @NotNull
    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        int changelistId = -1;
        if (destination > 0) {
            changelistId = destination;
        }

        List<P4FileInfo> specs = exec.loadFileInfo(project, FileSpecUtil.getFromFilePaths(deletedFiles));

        List<IFileSpec> deleted = new ArrayList<IFileSpec>();
        List<IFileSpec> reverted = new ArrayList<IFileSpec>();
        for (P4FileInfo spec: specs) {
            if (spec.isOpenForDelete()) {
                log("delete: ignoring already open for delete " + spec);
            } else if (spec.isOpenInClient()) {
                reverted.add(spec.toClientSpec());
                log("delete: revert " + spec);
                if (spec.isInDepot()) {
                    deleted.add(spec.toDepotSpec());
                    log("delete: delete " + spec);
                }
            } else if (spec.isInDepot()) {
                deleted.add(spec.toDepotSpec());
                log("delete: delete " + spec);
            } else {
                log("delete: ignoring " + spec);
            }
        }
        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();

        // Revert first.
        if (!reverted.isEmpty()) {
            // revert from whatever changelist it's currently in.
            ret.addAll(exec.revertFiles(project, reverted));
        }

        if (! deleted.isEmpty()) {
            // Sending "false" tells the client to keep the file on the
            // local system, while setting to "true" is supposed to leave
            // them alone.  Either way, this seems to put the file back
            // with a file size of 0
            ret.addAll(exec.deleteFiles(project, deleted, changelistId, false));

            // So explicitly delete the files when we're done.
            for (P4FileInfo file: specs) {
                if (file.getPath().getVirtualFile() != null) {
                    log("+ forcing file delete for " + file.getPath().getVirtualFile());
                    try {
                        file.getPath().getVirtualFile().delete(this);
                    } catch (IOException e) {
                        throw new P4Exception(e);
                    }
                }
            }
        }
        return ret;
    }
}
