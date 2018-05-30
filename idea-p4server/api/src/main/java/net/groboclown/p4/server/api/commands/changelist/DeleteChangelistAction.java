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
import net.groboclown.p4.server.api.commands.ActionUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

public class DeleteChangelistAction implements P4CommandRunner.ClientAction<DeleteChangelistResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;

    public DeleteChangelistAction(P4ChangelistId changelistId) {
        this(ActionUtil.createActionId(DeleteChangelistAction.class), changelistId);
    }

    public DeleteChangelistAction(String actionId, P4ChangelistId changelistId) {
        this.actionId = actionId;
        this.changelistId = changelistId;
    }

    @NotNull
    @Override
    public Class<? extends DeleteChangelistResult> getResultType() {
        return DeleteChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.DELETE_CHANGELIST;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }
}
