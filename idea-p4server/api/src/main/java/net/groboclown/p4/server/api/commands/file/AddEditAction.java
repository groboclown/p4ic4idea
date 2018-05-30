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
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddEditAction implements P4CommandRunner.ClientAction<AddEditResult> {
    private final String actionId;
    private final FilePath file;
    private final P4FileType type;
    private final P4ChangelistId changelistId;
    private final String charset;

    public AddEditAction(@NotNull FilePath file, @Nullable P4FileType type,
            P4ChangelistId changelistId, String charset) {
        this(ActionUtil.createActionId(AddEditAction.class), file, type, changelistId, charset);
    }

    public AddEditAction(@NotNull String actionId, @NotNull FilePath file, @Nullable P4FileType type,
            P4ChangelistId changelistId, String charset) {
        this.actionId = actionId;
        this.file = file;
        this.type = type;
        this.changelistId = changelistId;
        this.charset = charset;
    }

    @NotNull
    @Override
    public Class<? extends AddEditResult> getResultType() {
        return AddEditResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE;
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

    @Nullable
    public P4FileType getFileType() {
        return type;
    }

    @Nullable
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @Nullable
    public String getCharset() {
        return charset;
    }
}
