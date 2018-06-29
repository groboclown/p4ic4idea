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

package net.groboclown.p4.server.api.commands.file;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.ActionUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteFileAction implements P4CommandRunner.ClientAction<DeleteFileResult> {
    private final String actionId;
    private final FilePath file;
    private final P4ChangelistId changelistId;


    public DeleteFileAction(@NotNull FilePath file, P4ChangelistId changelistId) {
        this(ActionUtil.createActionId(DeleteFileAction.class), file, changelistId);
    }

    public DeleteFileAction(@NotNull String actionId, @NotNull FilePath file,
            @Nullable P4ChangelistId changelistId) {
        this.actionId = actionId;
        this.file = file;
        this.changelistId = changelistId;
    }

    @NotNull
    @Override
    public Class<? extends DeleteFileResult> getResultType() {
        return DeleteFileResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.DELETE_FILE;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    public FilePath getFile() {
        return file;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }
}
