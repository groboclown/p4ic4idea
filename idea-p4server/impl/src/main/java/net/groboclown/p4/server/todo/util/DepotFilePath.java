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

package net.groboclown.p4.server.todo.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

@Deprecated
public class DepotFilePath implements FilePath {
    private final ClientServerRef clientServerRef;
    private final String depotPath;
    private final String name;

    public DepotFilePath(@NotNull final ClientServerRef clientServerRef, @NotNull  final String depotPath) {
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

    @Deprecated
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

    @Deprecated
    @Override
    public void refresh() {

    }

    @Deprecated
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
        return null;
    }

    @Override
    public boolean isNonLocal() {
        return true;
    }
}
