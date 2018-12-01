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

package net.groboclown.idea.p4ic.mock;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

public class MockFilePath implements FilePath {
    private final File f;
    private final VirtualFile vf;

    public MockFilePath(final File f) {
        this.f = f;
        this.vf = new MockVirtualFile(f);
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
        return vf;
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFileParent() {
        return vf.getParent();
    }

    @NotNull
    @Override
    public File getIOFile() {
        return f;
    }

    @NotNull
    @Override
    public String getName() {
        return vf.getName();
    }

    @NotNull
    @Override
    public String getPresentableUrl() {
        return f.toURI().toASCIIString();
    }

    @Nullable
    @Override
    public Document getDocument() {
        return null;
    }

    @NotNull
    @Override
    public Charset getCharset() {
        return null;
    }

    @NotNull
    @Override
    public Charset getCharset(@Nullable final Project project) {
        return Charset.defaultCharset();
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return JavaFileType.INSTANCE;
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
        return vf.getPath();
    }

    @Override
    public boolean isDirectory() {
        return vf.isDirectory();
    }

    @Override
    public boolean isUnder(@NotNull final FilePath filePath, final boolean b) {
        // not implemented here
        return false;
    }

    @Nullable
    @Override
    public FilePath getParentPath() {
        return new MockFilePath(f.getParentFile());
    }

    @Override
    public boolean isNonLocal() {
        return false;
    }

    @Override
    public int hashCode() {
        return vf.getPath().hashCode() + vf.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj == null || !(obj instanceof FilePath)) && vf.equals(((FilePath) obj).getVirtualFile());
    }
}
