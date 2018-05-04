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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class P4VFSListener extends VcsVFSListener {
    private static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

    protected P4VFSListener(@NotNull Project project,
            @NotNull P4Vcs vcs) {
        super(project, vcs);
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
    protected void performAdding(Collection<VirtualFile> collection, Map<VirtualFile, VirtualFile> map) {
        throw new IllegalStateException("not implemented");
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
    protected void performDeletion(List<FilePath> list) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected void performMoveRename(List<MovedFileInfo> list) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected boolean isDirectoryVersioningSupported() {
        return false;
    }
}
