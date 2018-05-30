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

package net.groboclown.p4.server.api.commands.changelist;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.ActionUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MoveFilesToChangelistAction implements P4CommandRunner.ClientAction<MoveFilesToChangelistResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;
    private final List<FilePath> files;

    public MoveFilesToChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull List<FilePath> files) {
        this(ActionUtil.createActionId(MoveFilesToChangelistAction.class), changelistId, files);
    }

    public MoveFilesToChangelistAction(@NotNull String actionId, @NotNull P4ChangelistId changelistId,
            @NotNull List<FilePath> files) {
        this.actionId = actionId;
        this.changelistId = changelistId;
        this.files = files;
    }

    @NotNull
    @Override
    public Class<? extends MoveFilesToChangelistResult> getResultType() {
        return MoveFilesToChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.MOVE_FILES_TO_CHANGELIST;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public List<FilePath> getFiles() {
        return files;
    }
}
