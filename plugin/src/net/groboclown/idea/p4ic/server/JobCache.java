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

package net.groboclown.idea.p4ic.server;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Cache of the perforce Jobs per Server.  This helps reduce the load on the
 * server by preventing large number of requests against the server.
 *
 * TODO allow for querying jobs
 */
public class JobCache {
    private static final Logger LOG = Logger.getInstance(JobCache.class);

    // TODO this should be a user setting
    private static final int MAX_JOBS_RETURNED = 20;

    private final Object sync = new Object();
    private final Project project;
    private final RawServerExecutor server;
    private final Map<String, P4Job> jobs = new HashMap<String, P4Job>();
    private final List<String> jobStatusValues = new ArrayList<String>();

    public JobCache(@NotNull final Project project, @NotNull final RawServerExecutor server) {
        this.project = project;
        this.server = server;
    }


    public void invalidateCache() {
        synchronized (sync) {
            jobs.clear();
            jobStatusValues.clear();
        }
    }


    @Nullable
    public P4Job getCachedJob(@NotNull String jobId) {
        synchronized (sync) {
            return jobs.get(jobId);
        }
    }


    @NotNull
    public List<String> getJobStatusValues() throws VcsException {
        synchronized (sync) {
            if (jobStatusValues.isEmpty()) {
                jobStatusValues.addAll(server.getJobStatusValues(project));
            }
            return Collections.unmodifiableList(jobStatusValues);
        }
    }


    @Nullable
    public Collection<P4Job> getJobsForChangelist(final int changelistId) throws VcsException, CancellationException {
        // Bug #33: Calling the P4 api directly can cause issues.
        // Instead, go the indirect route by querying the underling
        // jobs directly.  This is what you need to do in p4 cli
        // anyway, so it's probably what the API is doing
        // as well.  It also gives us tighter control over job caching
        // and limiting the number of requests.

        final Collection<String> jobIds = server.getJobIdsForChangelist(project, changelistId);
        if (jobIds == null) {
            return null;
        }
        LOG.debug("Changelist " + changelistId + " has " + jobIds.size() + " jobs");
        final List<P4Job> jobs = new ArrayList<P4Job>(jobIds.size());
        for (String jobId : jobIds) {
            if (jobs.size() > MAX_JOBS_RETURNED) {
                // TODO allow user to pull in the large list of jobs if they want.
                // TODO this means telling the user in the UI that they exceeded this limit.
                LOG.warn(changelistId + " exceeded number of jobs to return; capping at " + MAX_JOBS_RETURNED);
                break;
            }
            P4Job job = getJob(jobId);
            if (job != null) {
                jobs.add(job);
            } else {
                LOG.warn("Changelist " + changelistId + " contains non-existent or invalid job id " + jobId);
            }
        }
        return jobs;
    }


    @Nullable
    public P4Job getJob(@NotNull String jobId) throws VcsException {
        P4Job job = getCachedJob(jobId);
        if (job != null) {
            return job;
        }
        // don't perform the p4 operation in a sync block.  If this
        // happens to be performed twice, there's no real dire
        // consequence.
        job = server.getJobForId(project, jobId);
        if (job != null) {
            synchronized (sync) {
                jobs.put(jobId, job);
            }
        }
        return job;
    }
}
