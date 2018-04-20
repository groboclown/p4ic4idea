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

package net.groboclown.p4.server.cache.sync;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.P4Bundle;
import net.groboclown.p4.changes.P4ChangeListJob;
import net.groboclown.p4.server.cache.ClientServerRef;
import net.groboclown.p4.server.cache.UpdateGroup;
import net.groboclown.p4.server.cache.state.CachedState;
import net.groboclown.p4.server.cache.state.JobStateList;
import net.groboclown.p4.server.cache.state.P4JobState;
import net.groboclown.p4.server.cache.state.PendingUpdateState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JobServerCacheSync extends CacheFrontEnd {
    // FIXME jobs need to be cached per server

    private final JobStateList jobs;
    private Date lastRefreshed;

    public JobServerCacheSync(@NotNull JobStateList jobs) {
        this.jobs = jobs;
        lastRefreshed = CachedState.NEVER_LOADED;
        for (P4JobState state : jobs) {
            if (state.getLastUpdated().after(lastRefreshed)) {
                lastRefreshed = state.getLastUpdated();
            }
        }
    }


    @Override
    protected void innerLoadServerCache(@NotNull final P4Exec2 exec,
            @NotNull final AlertManager alerts) {
        // Only reload the job status for the jobs we're asked to
        // care about.

        for (String jobId: jobs.copy().keySet()) {
            try {
                final P4JobState job = exec.getJobForId(jobId);
                if (job != null) {
                    jobs.add(job);
                }
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.job-refresh.title", jobId),
                        P4Bundle.message("error.job-refresh", jobId),
                        e, new FilePath[0]);
            }
        }
    }

    @Override
    protected void rectifyCache(@NotNull final Project project,
            @NotNull final Collection<PendingUpdateState> pendingUpdateStates,
            @NotNull final AlertManager alerts) {
        // Have this do something if job changing is ever implemented
    }

    @NotNull
    @Override
    protected Collection<UpdateGroup> getSupportedUpdateGroups() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshed;
    }

    @Override
    protected void checkLocalIntegrity(@NotNull final List<PendingUpdateState> pendingUpdates) {
        // do nothing - no local changes are stored.
    }

    @NotNull
    Collection<P4JobState> loadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts,
            @NotNull Collection<String> jobIds) {
        Set<P4JobState> ret = new HashSet<P4JobState>(jobIds.size());
        for (String jobId : jobIds) {
            try {
                final P4JobState job = exec.getJobForId(jobId);
                if (job != null) {
                    jobs.add(job);
                    ret.add(job);
                }
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.job-refresh.title", jobId),
                        P4Bundle.message("error.job-refresh", jobId),
                        e, new FilePath[0]);
            }
        }
        return ret;
    }

    Map<String, P4ChangeListJob> getCachedJobIds(@NotNull ClientServerRef clientServerRef,
            @NotNull Collection<String> jobIds) {
        Map<String, P4ChangeListJob> ret = new HashMap<String, P4ChangeListJob>();
        for (String jobId : jobIds) {
            P4JobState job = jobs.get(jobId);
            if (job == null) {
                ret.put(jobId, null);
            } else {
                ret.put(jobId, new P4ChangeListJob(clientServerRef, job));
            }
        }
        return ret;
    }

    @NotNull
    ServerQuery createRefreshQuery(@NotNull Collection<String> jobIds) {
        Date needsRefreshTime = new Date(System.currentTimeMillis() - MIN_REFRESH_INTERVAL_MS);
        final List<String> toRefresh = new ArrayList<String>(jobIds);
        Iterator<String> iter = toRefresh.iterator();
        while (iter.hasNext()) {
            final String next = iter.next();
            final P4JobState job = jobs.get(next);
            if (job != null && job.getLastUpdated().after(needsRefreshTime)) {
                iter.remove();
            }
        }

        return new ServerQuery() {
            @Nullable
            @Override
            public Object query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                    @NotNull final ServerConnection connection, @NotNull SynchronizedActionRunner runner,
                    @NotNull final AlertManager alerts)
                    throws InterruptedException {
                for (String jobId : toRefresh) {
                    try {
                        final P4JobState job = exec.getJobForId(jobId);
                        if (job != null) {
                            jobs.add(job);
                        } else {
                            jobs.remove(jobId);
                        }
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("error.job-refresh.title", jobId),
                                P4Bundle.message("error.job-refresh", jobId),
                                e, new FilePath[0]);
                    }
                }
                return null;
            }
        };
    }
}
