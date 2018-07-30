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
import net.groboclown.p4.server.api.commands.AbstractNonCachedClientAction;
import net.groboclown.p4.server.api.commands.ActionUtil;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SubmitChangelistAction extends AbstractNonCachedClientAction<SubmitChangelistResult> {
    private final P4ChangelistId changelistId;
    private final Collection<P4Job> updatedJobs;
    private final String updatedDescription;
    private final JobStatus jobStatus;

    public SubmitChangelistAction(@NotNull P4ChangelistId changelistId,
            @Nullable Collection<P4Job> updatedJobs, @Nullable String updatedDescription,
            @Nullable JobStatus jobStatus) {
        this.changelistId = changelistId;
        this.updatedJobs = updatedJobs;
        this.updatedDescription = updatedDescription;
        this.jobStatus = jobStatus;
    }

    @NotNull
    @Override
    public Class<? extends SubmitChangelistResult> getResultType() {
        return SubmitChangelistResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.SUBMIT_CHANGELIST;
    }

    @NotNull
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @Nullable
    public Collection<P4Job> getUpdatedJobs() {
        return updatedJobs;
    }

    @Nullable
    public String getUpdatedDescription() {
        return updatedDescription;
    }

    @Nullable
    public JobStatus getJobStatus() {
        return jobStatus;
    }
}
