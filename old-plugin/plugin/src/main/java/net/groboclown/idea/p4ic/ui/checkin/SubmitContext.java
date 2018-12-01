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

package net.groboclown.idea.p4ic.ui.checkin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.file.P4CheckinEnvironment;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

/**
 * M in the MVC for the submit UI.  Used by both the
 * {@link P4CheckinEnvironment} and the panel.  It also acts as a controller
 * in terms of reading values from the Perforce server for populating
 * initial values.
 */
public class SubmitContext {
    private static final Logger LOG = Logger.getInstance(SubmitContext.class);

    private final P4Vcs vcs;
    private final P4ChangeListMapping changeListMapping;
    private final List<P4ChangeListJob> jobs;
    private Set<String> acceptableJobStates = Collections.emptySet();

    // default status
    private String submitStatus = "closed";

    private final Map<P4Server, List<P4ChangeListId>> currentChanges;

    private boolean isJobAssociationValid;

    public SubmitContext(@NotNull P4Vcs vcs, Collection<Change> ideaChanges) {
        this.vcs = vcs;
        this.jobs = new ArrayList<P4ChangeListJob>();
        this.currentChanges = new HashMap<P4Server, List<P4ChangeListId>>();
        this.changeListMapping = P4ChangeListMapping.getInstance(vcs.getProject());
        refresh(ideaChanges);
    }


    /**
     *
     * @return true if the selected change lists are on the same P4 server.
     */
    public boolean isJobAssociationValid() {
        return isJobAssociationValid;
    }

    @NotNull
    public List<P4ChangeListJob> getJobs() {
        return Collections.unmodifiableList(jobs);
    }


    @Nullable
    public P4ChangeListJob addJobId(@NotNull String jobId) {
        if (isJobAssociationValid()) {
            for (P4Server server: currentChanges.keySet()) {
                P4ChangeListJob job = getJob(server, jobId);
                if (job != null) {
                    jobs.add(job);
                    return job;
                }
            }
            LOG.info("Could not find job " + jobId + " on any registered server for the active changelists " +
                    currentChanges);
            return null;
        } else {
            return null;
        }
    }


    // (controller style actions merged with the model)
    // TODO allow the user to create a job
    // TODO allow the user to search for jobs



    public void removeJob(@NotNull P4ChangeListJob jobId) {
        jobs.remove(jobId);
    }


    @NotNull
    public String getSubmitStatus() {
        return submitStatus;
    }


    public boolean setSubmitStatus(@NotNull String status) {
        if (getJobStatuses().contains(status)) {
            submitStatus = status;
            return true;
        }
        return false;
    }


    /**
     * This list can potentially refresh with each call to
     * {@link #setSelectedCurrentChanges(Collection)}.
     *
     * @return the list of valid job statuses
     */
    public synchronized Collection<String> getJobStatuses() {
        if (acceptableJobStates == null) {
            acceptableJobStates = new HashSet<String>();

            // special status used for check-ins, but is not a formal status.
            acceptableJobStates.add("same");

            acceptableJobStates.addAll(P4ChangeListJob.DEFAULT_JOB_STATUS);
        }
        return acceptableJobStates;
    }


    public synchronized boolean setSelectedCurrentChanges(Collection<Change> ideaChanges) {
        refresh(ideaChanges);
        return isJobAssociationValid();
    }


    public synchronized void refresh(Collection<Change> ideaChanges) {
        jobs.clear();
        currentChanges.clear();
        for (Map<P4Server, P4ChangeListId> mapping: getP4ChangesFor(ideaChanges).values()) {
            for (Entry<P4Server, P4ChangeListId> entry : mapping.entrySet()) {
                List<P4ChangeListId> changes = currentChanges.get(entry.getKey());
                if (changes == null) {
                    changes = new ArrayList<P4ChangeListId>();
                    currentChanges.put(entry.getKey(), changes);
                }
                changes.add(entry.getValue());
            }
        }
        for (Entry<P4Server, List<P4ChangeListId>> entry : currentChanges.entrySet()) {
            jobs.addAll(getJobsFor(entry.getKey(), entry.getValue()));
        }
        checkValidity();
    }


    @NotNull
    public Project getProject() {
        return vcs.getProject();
    }


    /**
     * Check if the selected changes are valid for job attachments.
     * If it's valid, then the corresponding states are also set
     * (jobServerId, isJobAssociationValid, acceptableJobStates).
     */
    private void checkValidity() {
        // If there are no changes, the state is invalid.
        isJobAssociationValid = ! currentChanges.isEmpty();
        acceptableJobStates = null;
        for (P4Server server: currentChanges.keySet()) {
            if (acceptableJobStates == null) {
                acceptableJobStates = findJobStatesFor(server);
            } else {
                // intersection of the two sets - get the minimal
                // job status states.
                acceptableJobStates.retainAll(findJobStatesFor(server));
            }
        }
        if (acceptableJobStates == null) {
            acceptableJobStates = Collections.emptySet();
        }
    }


    @Nullable
    private P4ChangeListJob getJob(@NotNull P4Server server, @NotNull final String jobId) {
        try {
            return server.getJobsForIds(Collections.singleton(jobId)).get(jobId);
        } catch (InterruptedException e) {
            LOG.warn(e);
            return null;
        }
    }


    @NotNull
    private List<P4ChangeListJob> getJobsFor(@NotNull P4Server server, @NotNull Collection<P4ChangeListId> changes) {
        try {
            final ArrayList<P4ChangeListJob> ret = new ArrayList<P4ChangeListJob>(server.getJobsInChangelists(changes));
            Collections.sort(ret);
            return ret;
        } catch (InterruptedException e) {
            LOG.warn(e);
            return Collections.emptyList();
        }
    }


    @NotNull
    private Map<Change, Map<P4Server, P4ChangeListId>> getP4ChangesFor(@NotNull final Collection<Change> changes) {
        // TODO look to moving this into P4ChangeListMapping
        Map<Change, Map<P4Server, P4ChangeListId>> ret = new HashMap<Change, Map<P4Server, P4ChangeListId>>();
        final ChangeListManager changeListManager = ChangeListManager.getInstance(vcs.getProject());
        final List<P4Server> servers = vcs.getP4Servers();
        for (Change change : changes) {
            final LocalChangeList idea = changeListManager.getChangeList(change);
            if (idea != null) {
                Map<P4Server, P4ChangeListId> mapping = new HashMap<P4Server, P4ChangeListId>();
                for (P4Server server : servers) {
                    P4ChangeListId changeListId = changeListMapping.getPerforceChangelistFor(server, idea);
                    if (changeListId != null) {
                        mapping.put(server, changeListId);
                    }
                }
                if (! mapping.isEmpty()) {
                    ret.put(change, mapping);
                }
            }
        }
        return ret;
    }


    @NotNull
    private Set<String> findJobStatesFor(@NotNull final P4Server server) {
        Set<String> jobStatuses = new HashSet<String>();

        // special status used for check-ins, but is not a formal status.
        jobStatuses.add("same");

        try {
            jobStatuses.addAll(server.getJobStatusValues());
        } catch (InterruptedException e) {
            LOG.info(e);
        }

        return jobStatuses;
    }
}
