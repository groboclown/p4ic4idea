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

package net.groboclown.idea.p4ic.v2.server.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public class DepotFilePath implements FilePath {
    private final P4Vcs vcs;
    private final ClientServerRef clientServerRef;
    private final String depotPath;
    private final String name;

    public DepotFilePath(@Nullable Project project,
            @NotNull final ClientServerRef clientServerRef, @NotNull  final String depotPath) {
        this.vcs = P4Vcs.getInstance(project);
        this.clientServerRef = clientServerRef;
        this.depotPath = depotPath;

        int pos = depotPath.lastIndexOf('/');
        if (pos >= 0) {
            this.name = depotPath.substring(pos + 1);
        } else {
            this.name = "";
        }
    }


    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
        return null;
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFileParent() {
        return null;
    }

    @NotNull
    @Override
    public File getIOFile() {
        return new File("/");
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPresentableUrl() {
        return clientServerRef.getServerDisplayId() + "/" + clientServerRef.getClientName() + depotPath;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public Charset getCharset() {
        return null;
    }

    @Override
    public Charset getCharset(final Project project) {
        return null;
    }

    @Override
    public FileType getFileType() {
        return null;
    }

    @Override
    public void refresh() {

    }

    @Override
    public void hardRefresh() {

    }

    @NotNull
    @Override
    public String getPath() {
        return depotPath;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isUnder(@NotNull final FilePath parent, final boolean strict) {
        return false;
    }

    @Nullable
    @Override
    public FilePath getParentPath() {
        // For compatibility with the discovery of the owning VCS tree, we return one of the
        // VcsRoot paths from the project.
        if (vcs != null) {
            List<VirtualFile> roots = vcs.getVcsRoots();
            if (! roots.isEmpty()) {
                return FilePathUtil.getFilePath(roots.get(0));
            }
        }
        return null;
    }

    @Override
    public boolean isNonLocal() {
        return true;
    }
}
