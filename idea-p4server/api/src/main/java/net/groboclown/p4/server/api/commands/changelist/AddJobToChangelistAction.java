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

public class AddJobToChangelistAction implements P4CommandRunner.ClientAction<AddJobToChangelistResult> {
    private final String actionId = ActionUtil.createActionId(AddJobToChangelistAction.class);
    private final P4ChangelistId changelistId;
    private final P4Job job;

    public AddJobToChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull P4Job job) {
        this.changelistId = changelistId;
        this.job = job;
    }

    @NotNull
    @Override
    public Class<? extends AddJobToChangelistResult> getResultType() {
        return AddJobToChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.ADD_JOB_TO_CHANGELIST;
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
