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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class MockVirtualFile extends VirtualFile {
    private static final VirtualFileSystem VFS = new CoreLocalFileSystem();
    private final File nativeFile;

    public MockVirtualFile(final File nativeFile) {
        this.nativeFile = nativeFile;
    }

    @NotNull
    @Override
    public String getName() {
        return nativeFile.getName();
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return VFS;
    }

    @NotNull
    @Override
    public String getPath() {
        return nativeFile.getPath();
    }

    @Override
    public boolean isWritable() {
        return nativeFile.canWrite();
    }

    @Override
    public boolean isDirectory() {
        return nativeFile.isDirectory();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return new MockVirtualFile(nativeFile.getParentFile());
    }

    @Override
    public VirtualFile[] getChildren() {
        File[] children = nativeFile.listFiles();
        VirtualFile[] ret = new VirtualFile[children.length];
        for (int i = 0; i < children.length; i++) {
            ret[i] = new MockVirtualFile(children[i]);
        }
        return ret;
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(final Object o, final long l, final long l1) throws IOException {
        return new FileOutputStream(nativeFile);
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int len;
        InputStream in = getInputStream();
        try {
            while ((len = in.read(buff, 0, 4096)) > 0) {
                baos.write(buff, 0, len);
            }
        } finally {
            in.close();
        }
        return baos.toByteArray();
    }

    @Override
    public long getTimeStamp() {
        return nativeFile.lastModified();
    }

    @Override
    public long getLength() {
        return nativeFile.length();
    }

    @Override
    public void refresh(final boolean b, final boolean b1, @Nullable final Runnable runnable) {
        // ignore
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(nativeFile);
    }

}
