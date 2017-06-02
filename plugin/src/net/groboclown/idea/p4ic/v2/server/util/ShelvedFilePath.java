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
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.util.PathUtil;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Allows for shelved files to appear in the changelist UI.
 */
public class ShelvedFilePath implements FilePath {
    private final P4ShelvedFile shelvedFile;
    @NotNull
    private final FilePath local;

    public ShelvedFilePath(@NotNull P4ShelvedFile shelvedFile) {
        this.shelvedFile = shelvedFile;
        this.local = FilePathUtil.getFilePath(shelvedFile.getLocalPath());
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
        return local.getVirtualFile();
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFileParent() {
        return local.getVirtualFileParent();
    }

    @NotNull
    @Override
    public File getIOFile() {
        return new File(shelvedFile.getDepotPath());
    }

    @NotNull
    @Override
    public String getName() {
        // return PathUtil.getFileName(shelvedFile.getDepotPath());
        return shelvedFile.getDepotPath();
    }

    @NotNull
    @Override
    public String getPresentableUrl() {
        return shelvedFile.getDepotPath();
    }

    @Nullable
    @Override
    public Document getDocument() {
        return null;
    }

    @NotNull
    @Override
    public Charset getCharset() {
        return getCharset(null);
    }

    @NotNull
    @Override
    public Charset getCharset(@Nullable Project project) {
        EncodingManager em = project == null ? EncodingManager.getInstance() : EncodingProjectManager.getInstance(project);
        return em.getDefaultCharset();
    }

    @NotNull
    @Override
    public FileType getFileType() {
        VirtualFile file = getVirtualFile();
        FileTypeManager manager = FileTypeManager.getInstance();
        return file != null ? manager.getFileTypeByFile(file) : manager.getFileTypeByFileName(getName());
    }

    @Override
    public void refresh() {
        // do nothing
    }

    @Override
    public void hardRefresh() {
        // do nothing
    }

    @NotNull
    @Override
    public String getPath() {
        // return shelvedFile.getDepotPath();
        return local.getPath();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isUnder(@NotNull FilePath parent, boolean strict) {
        return local.isUnder(parent, strict);
    }

    @Nullable
    @Override
    public FilePath getParentPath() {
        return local;
    }

    @Override
    public boolean isNonLocal() {
        return true;
    }
}
