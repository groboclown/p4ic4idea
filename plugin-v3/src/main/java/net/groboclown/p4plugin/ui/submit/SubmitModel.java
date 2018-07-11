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

package net.groboclown.p4plugin.ui.submit;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.ListTableModel;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecQuery;
import net.groboclown.p4.server.api.commands.changelist.ListJobsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListJobsResult;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.JobStatusNames;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4JobSpec;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.values.JobStatusNamesImpl;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SubmitModel {
    private static final Logger LOG = Logger.getInstance(SubmitModel.class);

    private final Object sync = new Object();
    private final Project project;
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<P4Job> jobs = new ArrayList<>();
    private final List<ServerConfig> activeConfigs = new ArrayList<>();
    private ListTableModel<P4Job> jobModel;

    private JobStatus status;

    public SubmitModel(@NotNull Project project) {
        this.project = project;
    }

    void addListener(Runnable r) {
        this.listeners.add(r);
    }

    public void setSelectedCurrentChanges(Collection<Change> changes) {
        final List<ServerConfig> configs = getConfigsForChanges(changes);
        synchronized (sync) {
            activeConfigs.clear();
            activeConfigs.addAll(configs);
        }
        fireChange();
    }

    void setJobModel(@NotNull ListTableModel<P4Job> model) {
        this.jobModel = model;
    }

    public void resetState() {
        synchronized (sync) {
            jobs.clear();
        }
        fireChange();
    }

    void setStatus(@NotNull JobStatus status) {
        this.status = status;
    }

    public void saveState(@NotNull PairConsumer<Object, Object> dataConsumer) {
        dataConsumer.consume("jobIds", jobs);
        dataConsumer.consume("jobStatus", status);
    }

    @NotNull
    public List<P4Job> getJobs() {
        synchronized (sync) {
            return new ArrayList<>(jobs);
        }
    }

    @Nullable
    public JobStatus getJobStatus() {
        return status;
    }



    @SuppressWarnings("unchecked")
    public static List<P4Job> getJobs(@NotNull NullableFunction<Object, Object> dataSupplier) {
        Object ret = dataSupplier.fun("jobIds");
        if (ret != null) {
            return (List<P4Job>) ret;
        }
        return Collections.emptyList();
    }

    @Nullable
    public static JobStatus getSubmitStatus(@NotNull NullableFunction<Object, Object> dataSupplier) {
        final Object ret = dataSupplier.fun("jobStatus");
        if (ret != null) {
            return (JobStatus) ret;
        }
        return null;
    }


    /**
     * Discover the first job ID matching on a single server.  Colliding job IDs across multiple registered
     * servers is a problem for the user to figure out.
     *
     * @param jobId job ID on the server.
     * @return job, or null if none matching the ID is found.
     */
    @Nullable
    P4Job findJob(@NotNull String jobId) {
        jobId = jobId.trim();
        List<ServerConfig> configs;
        synchronized (sync) {
            configs = new ArrayList<>(activeConfigs);
        }
        for (ServerConfig config : configs) {
            try {
                ListJobsResult res = P4ServerComponent.getInstance(project)
                        .getCommandRunner()
                        .query(config, new ListJobsQuery(jobId, null, null, 1))
                        .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
                for (P4Job job : res.getJobs()) {
                    if (jobId.equalsIgnoreCase(job.getJobId())) {
                        return job;
                    }
                }
            } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
                // TODO better exception handling?
                LOG.warn(e);
            }
        }
        return null;
    }

    @NotNull
    P4CommandRunner.QueryAnswer<List<P4Job>> searchJobs(@NotNull String queryPart, int maxResultsPerServer) {
        List<ServerConfig> configs;
        synchronized (sync) {
            configs = new ArrayList<>(activeConfigs);
        }
        P4CommandRunner.QueryAnswer<List<P4Job>> ret = new DoneQueryAnswer<>(new ArrayList<>());
        for (ServerConfig config : configs) {
            ret = ret.mapQueryAsync((jobs) -> P4ServerComponent.getInstance(project)
                    .getCommandRunner()
                    .query(config, new ListJobsQuery(null, null, queryPart, maxResultsPerServer))
                    .mapQuery((r) -> {
                        jobs.addAll(r.getJobs());
                        return jobs;
                    }));
        }
        return ret;
    }

    @NotNull
    P4CommandRunner.QueryAnswer<JobStatusNames> loadJobStatusNames() {
        List<ServerConfig> configs;
        synchronized (sync) {
            configs = new ArrayList<>(activeConfigs);
        }
        P4CommandRunner.QueryAnswer<Set<JobStatus>> res = new DoneQueryAnswer<>(new HashSet<>());
        for (ServerConfig config : configs) {
            res = res.mapQueryAsync((statuses) -> P4ServerComponent.getInstance(project)
                    .getCommandRunner()
                    .query(config, new GetJobSpecQuery())
                    .mapQuery((r) -> {
                        P4JobSpec jobSpec = r.getJobSpec();
                        if (jobSpec != null) {
                            statuses.addAll(JobStatusNamesImpl.load(jobSpec).getJobStatusNames());
                        }
                        return statuses;
                    }));
        }
        return res.mapQuery((r) -> {
            if (r.isEmpty()) {
                return JobStatusNamesImpl.DEFAULT_STATUSES;
            }
            return new JobStatusNamesImpl(r);
        });
    }

    void addJob(@NotNull P4Job job) {
        boolean changed = false;
        synchronized (sync) {
            if (!jobs.contains(job)) {
                jobs.add(job);
                changed = true;
            }
        }
        if (changed) {
            // Note: because the commit dialog is shown, we can't use ApplicationManager invokeLater.
            SwingUtilities.invokeLater(() -> {
                LOG.info("Adding job to table: " + job);
                jobModel.addRow(job);
            });
            fireChange();
        }
    }

    void removeJobs(final List<P4Job> jobList) {
        synchronized (sync) {
            this.jobs.removeAll(jobList);
        }

        // Note: because the commit dialog is shown, we can't use ApplicationManager invokeLater.
        SwingUtilities.invokeLater(() -> {
            boolean changed = false;
            for (P4Job job : jobList) {
                int index = jobModel.indexOf(job);
                if (index >= 0) {
                    jobModel.removeRow(index);
                    changed = true;
                }
            }
            if (changed) {
                fireChange();
            }
        });
    }

    List<P4Job> getListedJobs() {
        synchronized (sync) {
            return new ArrayList<>(jobs);
        }
    }


    private void fireChange() {
        listeners.forEach(Runnable::run);
    }

    private List<ServerConfig> getConfigsForChanges(Collection<Change> changes) {
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return Collections.emptyList();
        }
        List<FilePath> files = new ArrayList<>(changes.size() * 2);
        for (Change change : changes) {
            ContentRevision before = change.getBeforeRevision();
            if (before != null) {
                files.add(before.getFile());
            }
            ContentRevision after = change.getAfterRevision();
            if (after != null) {
                files.add(after.getFile());
            }
        }
        Set<ServerConfig> configs = new HashSet<>();
        for (FilePath file : files) {
            ClientConfigRoot config = registry.getClientFor(file);
            if (config != null) {
                configs.add(config.getServerConfig());
            }
        }
        return new ArrayList<>(configs);
    }

    public Project getProject() {
        return project;
    }
}
