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
import net.groboclown.p4.server.api.commands.AbstractAction;
import net.groboclown.p4.server.api.commands.ActionUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MoveFileAction extends AbstractAction implements P4CommandRunner.ClientAction<MoveFileResult> {
    private final String actionId;
    private final FilePath source;
    private final FilePath target;
    private final P4ChangelistId changelistId;

    public MoveFileAction(@NotNull FilePath source, @NotNull FilePath target, P4ChangelistId changelistId) {
        this(createActionId(MoveFileAction.class), source, target, changelistId);
    }

    public MoveFileAction(@NotNull String actionId, @NotNull FilePath source, @NotNull FilePath target,
            P4ChangelistId changelistId) {
        this.actionId = actionId;
        this.source = source;
        this.target = target;
        this.changelistId = changelistId;
    }

    @NotNull
    @Override
    public Class<? extends MoveFileResult> getResultType() {
        return MoveFileResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.MOVE_FILE;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    public FilePath getSourceFile() {
        return source;
    }

    @NotNull
    public FilePath getTargetFile() {
        return target;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        if (changelistId != null) {
            return new String[] { changeId(changelistId) };
        }
        return EMPTY;
    }

    @NotNull
    @Override
    public List<FilePath> getAffectedFiles() {
        return Arrays.asList(source, target);
    }
}
