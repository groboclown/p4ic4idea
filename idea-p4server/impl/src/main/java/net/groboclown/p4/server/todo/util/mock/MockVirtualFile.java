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

package net.groboclown.p4.server.todo.util.mock;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MockVirtualFile extends VirtualFile {
    private static final VirtualFile[] EMPTY = new VirtualFile[0];
    private final File f;

    public MockVirtualFile(@NotNull File f) {
        this.f = f;
    }

    @NotNull
    @Override
    public String getName() {
        return f.getName();
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public String getPath() {
        return f.getPath();
    }

    @Override
    public boolean isWritable() {
        return f.canWrite();
    }

    @Override
    public boolean isDirectory() {
        return f.isDirectory();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return new MockVirtualFile(f.getParentFile());
    }

    @Override
    public VirtualFile[] getChildren() {
        if (! f.isDirectory()) {
            return EMPTY;
        }
        File[] files = f.listFiles();
        if (files == null) {
            return EMPTY;
        }
        List<VirtualFile> ret = new ArrayList<>();
        for (File f : files) {
            ret.add(new MockVirtualFile(f));
        }
        return ret.toArray(EMPTY);
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp)
            throws IOException {
        return new FileOutputStream(f);
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray()
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream inp = getInputStream()) {
            byte[] buff = new byte[4096];
            int len;
            while ((len = inp.read(buff, 0, 4096)) > 0) {
                out.write(buff, 0, len);
            }
        }
        return out.toByteArray();
    }

    @Override
    public long getTimeStamp() {
        if (f.exists()) {
            return f.lastModified();
        }
        return 0;
    }

    @Override
    public long getLength() {
        if (f.isFile()) {
            return f.length();
        }
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
        if (postRunnable != null) {
            postRunnable.run();
        }
    }

    @Override
    public InputStream getInputStream()
            throws IOException {
        return new FileInputStream(f);
    }
}
