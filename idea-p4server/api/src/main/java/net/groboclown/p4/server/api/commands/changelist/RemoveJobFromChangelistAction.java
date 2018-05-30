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
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;

public class RemoveJobFromChangelistAction implements P4CommandRunner.ClientAction<RemoveJobFromChangelistResult> {
    private final String actionId;
    private final P4ChangelistId changelistId;
    private final P4Job job;

    public RemoveJobFromChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull P4Job job) {
        this(ActionUtil.createActionId(RemoveJobFromChangelistAction.class), changelistId, job);
    }

    public RemoveJobFromChangelistAction(@NotNull String actionId, @NotNull P4ChangelistId changelistId,
            @NotNull P4Job job) {
        this.actionId = actionId;
        this.changelistId = changelistId;
        this.job = job;
    }

    @NotNull
    @Override
    public Class<? extends RemoveJobFromChangelistResult> getResultType() {
        return RemoveJobFromChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.REMOVE_JOB_FROM_CHANGELIST;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public P4Job getJob() {
        return job;
    }
}
