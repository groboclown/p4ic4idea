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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// v183 - com.intellij.mock.MockCommandProcessor is no longer provided.
public class MockCommandProcessor
        extends CommandProcessor {
    @Override
    public void executeCommand(@NotNull Runnable runnable, @Nullable String s, @Nullable Object o) {

    }

    @Override
    public void executeCommand(@Nullable Project project, @NotNull Runnable runnable, @Nullable String s,
            @Nullable Object o) {

    }

    @Override
    public void executeCommand(@Nullable Project project, @NotNull Runnable runnable, @Nullable String s,
            @Nullable Object o, @Nullable Document document) {

    }

    @Override
    public void executeCommand(@Nullable Project project, @NotNull Runnable runnable, @Nullable String s,
            @Nullable Object o, @NotNull UndoConfirmationPolicy undoConfirmationPolicy) {

    }

    @Override
    public void executeCommand(@Nullable Project project, @NotNull Runnable runnable, @Nullable String s,
            @Nullable Object o, @NotNull UndoConfirmationPolicy undoConfirmationPolicy, @Nullable Document document) {

    }

    @Override
    public void executeCommand(@Nullable Project project, @NotNull Runnable runnable, @Nullable String s,
            @Nullable Object o, @NotNull UndoConfirmationPolicy undoConfirmationPolicy, boolean b) {

    }

    @Override
    public void setCurrentCommandName(@Nullable String s) {

    }

    @Override
    public void setCurrentCommandGroupId(@Nullable Object o) {

    }

    @Nullable
    @Override
    public Runnable getCurrentCommand() {
        return null;
    }

    @Nullable
    @Override
    public String getCurrentCommandName() {
        return null;
    }

    @Nullable
    @Override
    public Object getCurrentCommandGroupId() {
        return null;
    }

    @Nullable
    @Override
    public Project getCurrentCommandProject() {
        return null;
    }

    @Override
    public void runUndoTransparentAction(@NotNull Runnable runnable) {

    }

    @Override
    public boolean isUndoTransparentActionInProgress() {
        return false;
    }

    @Override
    public void markCurrentCommandAsGlobal(@Nullable Project project) {

    }

    @Override
    public void addAffectedDocuments(@Nullable Project project, @NotNull Document... documents) {

    }

    @Override
    public void addAffectedFiles(@Nullable Project project, @NotNull VirtualFile... virtualFiles) {

    }

    @Override
    public void addCommandListener(@NotNull CommandListener commandListener) {

    }

    @Override
    public void addCommandListener(@NotNull CommandListener commandListener, @NotNull Disposable disposable) {

    }

    @Override
    public void removeCommandListener(@NotNull CommandListener commandListener) {

    }
}
