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
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CreateJobAction implements P4CommandRunner.ServerAction<CreateJobResult> {
    private final String actionId;
    private final P4Job job;

    public CreateJobAction(@NotNull P4Job job) {
        this(ActionUtil.createActionId(CreateJobAction.class), job);
    }

    public CreateJobAction(@NotNull String actionId, @NotNull P4Job job) {
        this.actionId = actionId;
        this.job = job;
    }

    @NotNull
    @Override
    public Class<? extends CreateJobResult> getResultType() {
        return CreateJobResult.class;
    }

    @Override
    public P4CommandRunner.ServerActionCmd getCmd() {
        return P4CommandRunner.ServerActionCmd.CREATE_JOB;
    }

    /**
     *
     * @return the underlying fields for the job.
     * @see com.perforce.p4java.impl.generic.core.Job
     */
    @NotNull
    public Map<String,Object> getFields() {
        Map<String, Object> ret = new HashMap<>(job.getRawDetails());

        ret.put("Job", job.getJobId());
        ret.put("Description", job.getDescription());

        return ret;
    }

    @Override
    public String getActionId() {
        return actionId;
    }

    @NotNull
    public P4Job getJob() {
        return job;
    }
}
