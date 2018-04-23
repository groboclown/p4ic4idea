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

package net.groboclown.idea.mock;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MockVirtualFile extends VirtualFile {
    private static final VirtualFile[] EMPTY = new VirtualFile[0];

    private final MockVirtualFileSystem fileSystem;
    private final String name;
    private final boolean isDir;
    private final MockVirtualFile parent;
    private final List<MockVirtualFile> children = new ArrayList<>();
    private ByteArrayOutputStream contents;
    private boolean writable = true;
    private Charset charset = Charset.defaultCharset();

    // Creation of virtual files should only be called by the MockVirtualFileSystem.
    // It will perform all the right calls.
    MockVirtualFile(@NotNull MockVirtualFileSystem vfs, @NotNull String name, @Nullable MockVirtualFile parent) {
        this.fileSystem = vfs;
        this.name = name;
        this.parent = parent;
        this.isDir = true;
        this.contents = null;
    }

    MockVirtualFile(@NotNull MockVirtualFileSystem vfs, @NotNull String name, @Nullable MockVirtualFile parent,
            @NotNull String contents, @NotNull Charset charset) {
        this.fileSystem = vfs;
        this.name = name;
        this.parent = parent;
        this.contents = new ByteArrayOutputStream();
        this.isDir = false;
        setContents(contents, charset);
    }

    // called by the MockVirtualFileSystem to add a created file as a child.
    void markChild(@NotNull MockVirtualFile mvf) {
        this.children.add(mvf);
    }

    // Convenience function.  Calls into the MVFS to actually create the instance.
    @NotNull
    public MockVirtualFile addChildFile(Object requestor, @NotNull String name, @NotNull String contents,
            @Nullable Charset charset) {
        try {
            MockVirtualFile child = (MockVirtualFile) fileSystem.createChildFile(requestor, this, name);
            child.setContents(contents, charset);
            return child;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Convenience function.  Calls into the MVFS to actually create the instance.
    @NotNull
    public MockVirtualFile addChildDir(Object requestor, @NotNull String name) {
        try {
            return (MockVirtualFile) fileSystem.createChildDirectory(this, this, name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public VFFilePath asFilePath() {
        return new VFFilePath(this);
    }

    @Nullable
    public MockVirtualFile childNamed(@NotNull String name) {
        for (MockVirtualFile child : children) {
            if (name.equals(child.name)) {
                return child;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    // Returns the directory (parent) + this name.
    @NotNull
    @Override
    public String getPath() {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.getPath());
            if (! sb.toString().equals("/")) {
                sb.append("/");
            }
            //sb.append(parent.getName());
        }
        sb.append(name);
        return sb.toString();
    }

    @Override
    public boolean isWritable() {
        return writable;
    }

    @Override
    public boolean isDirectory() {
        return isDir;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return parent;
    }

    @Override
    public VirtualFile[] getChildren() {
        if (! isDir) {
            return EMPTY;
        }
        return children.toArray(EMPTY);
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp)
            throws IOException {
        if (isDir || contents == null) {
            throw new IOException("directory");
        }
        return contents;
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray()
            throws IOException {
        if (isDir || contents == null) {
            throw new IOException("directory");
        }
        return contents.toByteArray();
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        if (contents != null) {
            return contents.size();
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
        return new ByteArrayInputStream(contentsToByteArray());
    }

    @NotNull
    @Override
    public Charset getCharset() {
        return charset;
    }

    public void setContents(@NotNull String newContents, @Nullable Charset charset) {
        if (charset != null) {
            this.charset = charset;
        }
        contents = new ByteArrayOutputStream();
        try {
            contents.write(newContents.getBytes(this.charset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public MockVirtualFile getChild(@NotNull String dirName) {
        if (isDir) {
            for (MockVirtualFile child : children) {
                if (dirName.equals(child.getName())) {
                    return child;
                }
            }
        }
        return null;
    }
}
