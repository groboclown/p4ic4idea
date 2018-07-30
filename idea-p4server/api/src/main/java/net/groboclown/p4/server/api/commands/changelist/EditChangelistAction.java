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

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.AbstractAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

public class EditChangelistAction extends AbstractAction implements P4CommandRunner.ClientAction<EditChangelistResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;
    private final String comment;

    public EditChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull String comment) {
        this(createActionId(EditChangelistAction.class), changelistId, comment);
    }

    public EditChangelistAction(@NotNull String actionId, @NotNull P4ChangelistId changelistId,
            @NotNull String comment) {
        this.actionId = actionId;
        this.changelistId = changelistId;
        this.comment = comment;
    }

    @NotNull
    @Override
    public Class<? extends EditChangelistResult> getResultType() {
        return EditChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.EDIT_CHANGELIST_DESCRIPTION;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public String getComment() {
        return comment;
    }

    @NotNull
    @Override
    public String[] getDisplayParameters() {
        return new String[] { changeId(changelistId) };
    }
}
