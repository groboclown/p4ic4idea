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
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ListJobsQuery implements P4CommandRunner.ServerQuery<ListJobsResult> {
    private final String jobId;
    private final String username;
    private final String description;
    private final int maxResults;

    public ListJobsQuery(@Nullable String jobId, @Nullable String username,
            @Nullable String description, int maxResults) {
        this.jobId = jobId;
        this.username = username;
        this.description = description;
        this.maxResults = maxResults;
    }

    @NotNull
    @Override
    public Class<? extends ListJobsResult> getResultType() {
        return ListJobsResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return P4CommandRunner.ServerQueryCmd.LIST_JOBS;
    }

    public String getJobId() {
        return jobId;
    }

    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
