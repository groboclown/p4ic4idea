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
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockVirtualFileSystem
        extends VirtualFileSystem {
    private static final String ROOT_NAME = "";
    public static final MockVirtualFileSystem DEFAULT_INSTANCE = new MockVirtualFileSystem(false);

    // Note: not thread safe.
    private final Map<String, MockVirtualFile> registeredFiles = new HashMap<>();
    private final List<VirtualFileListener> listeners = new ArrayList<>();

    private final boolean readOnly;
    private Charset defaultCharset = StandardCharsets.UTF_8;


    @NotNull
    public static MockVirtualFile createRoot() {
        return createTree().get(ROOT_NAME);
    }

    @NotNull
    public static Map<String, MockVirtualFile> createTree(String... args) {
        return createTree(false, args);
    }

    @NotNull
    public static Map<String, MockVirtualFile> createTree(boolean readOnly, String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Argument format: filepath, file contents, ...");
        }
        MockVirtualFileSystem vfs = new MockVirtualFileSystem(readOnly);
        Map<String, MockVirtualFile> ret = new HashMap<>();
        MockVirtualFile root = new MockVirtualFile(vfs, ROOT_NAME, null);
        ret.put(ROOT_NAME, root);
        vfs.registeredFiles.put(root.getPath(), root);

        for (int ai = 0; ai < args.length; ai += 2) {
            String filename = args[ai];
            String fileContents = args[ai + 1];

            MockVirtualFile parent = root;
            String[] parts = filename.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                String name = parts[i];
                if (name.length() <= 0) {
                    continue;
                }
                MockVirtualFile next = parent.childNamed(name);
                if (next == null) {
                    next = parent.addChildDir(null, name);
                    ret.put(next.getPath(), next);
                }
                parent = next;
            }
            MockVirtualFile child = parent.addChildFile(null, parts[parts.length - 1], fileContents, vfs.defaultCharset);
            ret.put(filename, child);
        }

        return ret;
    }



    private MockVirtualFileSystem(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return "mock";
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@NotNull String path) {
        return registeredFiles.get(path);
    }

    @Override
    public void refresh(boolean asynchronous) {
        // do nothing
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        refresh(false);
        return findFileByPath(path);
    }

    @Override
    public void addVirtualFileListener(@NotNull VirtualFileListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {
        listeners.remove(listener);
    }

    @Override
    protected void deleteFile(Object requestor, @NotNull VirtualFile vFile)
            throws IOException {
        if (vFile instanceof MockVirtualFile) {
            MockVirtualFile mvf = (MockVirtualFile) vFile;
            if (registeredFiles.containsKey(mvf.getPath())) {
                VirtualFileEvent event = new VirtualFileEvent(requestor, vFile, mvf.getParent(), 0, 1);
                for (VirtualFileListener listener : listeners) {
                    listener.beforeFileDeletion(event);
                }
                registeredFiles.remove(mvf.getPath());
                for (VirtualFileListener listener : listeners) {
                    listener.fileDeleted(event);
                }
                return;
            }
        }
        throw new NoSuchFileException(vFile.getCanonicalPath());
    }

    @Override
    protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent)
            throws IOException {
        throw new IOException("not implemented");
    }

    @Override
    protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName)
            throws IOException {
        throw new IOException("not implemented");
    }

    @NotNull
    @Override
    protected VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName)
            throws IOException {
        if (vDir instanceof MockVirtualFile) {
            MockVirtualFile mvd = (MockVirtualFile) vDir;
            if (registeredFiles.containsKey(mvd.getPath())) {
                if (mvd.getChild(fileName) != null) {
                    throw new FileAlreadyExistsException(mvd.getPath() + "/" + fileName);
                }
                MockVirtualFile child = new MockVirtualFile(this, fileName, mvd, "", getDefaultCharset());
                mvd.markChild(child);
                VirtualFileEvent event = new VirtualFileEvent(requestor, child, vDir, 0, 1);
                for (VirtualFileListener listener : listeners) {
                    listener.fileCreated(event);
                }
                return child;
            }
        } else if (vDir instanceof IOVirtualFile) {
            return new IOVirtualFile(new File(((IOVirtualFile) vDir).getIOFile(), fileName), false);
        }
        throw new NoSuchFileException(vDir.getCanonicalPath());
    }

    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    public void setDefaultCharset(@NotNull Charset charset) {
        defaultCharset = charset;
    }

    @NotNull
    @Override
    protected VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName)
            throws IOException {
        if (vDir instanceof MockVirtualFile) {
            MockVirtualFile mvd = (MockVirtualFile) vDir;
            if (registeredFiles.containsKey(mvd.getPath())) {
                if (mvd.getChild(dirName) != null) {
                    throw new FileAlreadyExistsException(mvd.getPath() + "/" + dirName);
                }
                MockVirtualFile child = new MockVirtualFile(this, dirName, mvd);
                mvd.markChild(child);
                registeredFiles.put(child.getPath(), child);
                VirtualFileEvent event = new VirtualFileEvent(requestor, child, vDir, 0, 1);
                for (VirtualFileListener listener : listeners) {
                    listener.fileCreated(event);
                }
                return child;
            }
        }
        throw new NoSuchFileException(vDir.getCanonicalPath());
    }

    @NotNull
    @Override
    protected VirtualFile copyFile(Object requestor, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent,
            @NotNull String copyName)
            throws IOException {
        throw new IOException("not implemented");
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    public void onFileContentsChanged(Object requestor, MockVirtualFile file, String newContents) {
        VirtualFileEvent event = new VirtualFileEvent(requestor, file, file.getParent(), 0, 1);
        for (VirtualFileListener listener : listeners) {
            listener.beforeContentsChange(event);
        }
        file.setContents(newContents, null);
        for (VirtualFileListener listener : listeners) {
            listener.contentsChanged(event);
        }
    }
}
