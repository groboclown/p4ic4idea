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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.altmock.MockFilePath;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnActionEvent;

import java.io.File;
import java.nio.file.Path;


public class MockVcsContextFactory implements VcsContextFactory {
    @NotNull
    @Override
    public VcsContext createCachedContextOn(@NotNull AnActionEvent anActionEvent) {
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public VcsContext createContextOn(@NotNull AnActionEvent anActionEvent) {
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public FilePath createFilePathOn(@NotNull VirtualFile virtualFile) {
        return new VFFilePath(virtualFile);
    }

    @NotNull
    @Override
    public FilePath createFilePathOn(@NotNull File file) {
        return new IOFilePath(file);
    }

    // Removed in v>=211
    @NotNull
    // @Override
    public FilePath createFilePathOnDeleted(@NotNull File file, boolean isDirectory) {
        return new IOFilePath(file, isDirectory);
    }

    @NotNull
    @Override
    public FilePath createFilePathOn(@NotNull File file, boolean isDirectory) {
        return new IOFilePath(file, isDirectory);
    }

    @Override
    public @NotNull FilePath createFilePath(@NotNull Path path, boolean isDirectory) {
        return new MockFilePath(path.toFile());
    }

    @NotNull
    @Override
    public FilePath createFilePathOnNonLocal(@NotNull String path, boolean isDirectory) {
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public FilePath createFilePathOn(@NotNull VirtualFile parent, @NotNull String name) {
        return createFilePath(parent, name, false);
    }

    @NotNull
    @Override
    public FilePath createFilePath(@NotNull VirtualFile parent, @NotNull String fileName, boolean isDirectory) {
        return createFilePath(parent.getPath() + "/" + fileName, isDirectory);
    }

    @NotNull
    @Override
    public LocalChangeList createLocalChangeList(@NotNull Project project, @NotNull String name) {
        return new MockLocalChangeList()
                .withName(name);
    }

    @NotNull
    @Override
    public FilePath createFilePath(@NotNull String path, boolean isDirectory) {
        return new IOFilePath(new File(path));
    }
}
