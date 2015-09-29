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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4Job;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.file.P4CheckinEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * M in the MVC for the submit UI.  Used by both the
 * {@link P4CheckinEnvironment} and the panel.  It also acts as a controller
 * in terms of reading values from the Perforce server for populating
 * initial values.
 */
public class SubmitContext {
    private static final Logger LOG = Logger.getInstance(SubmitContext.class);

    private final P4Vcs vcs;
    private final List<P4Job> jobs;
    private List<String> acceptableJobStates = Collections.emptyList();
    private String jobServerId;

    // default status
    private String submitStatus = "closed";

    private Client currentClient;

    private final Set<P4ChangeListId> currentChanges;

    private boolean isJobAssociationValid;

    public SubmitContext(@NotNull P4Vcs vcs, Collection<Change> ideaChanges) {
        this.vcs = vcs;
        this.jobs = new ArrayList<P4Job>();
        this.currentChanges = new HashSet<P4ChangeListId>();
        refresh(ideaChanges);
    }


    /**
     *
     * @return true if the selected change lists are on the same P4 server.
     */
    public boolean isJobAssociationValid() {
        return isJobAssociationValid;
    }

    public List<P4Job> getJobs() {
        return Collections.unmodifiableList(jobs);
    }


    public P4Job addJobId(@NotNull String jobId) {
        if (isJobAssociationValid()) {
            P4Job job = getJob(jobId);
            if (job == null) {
                return null;
            }
            jobs.add(job);
            return job;
        } else {
            return null;
        }
    }


    // (controller style actions merged with the model)
    // TODO allow the user to create a job
    // TODO allow the user to search for jobs



    public void removeJob(@NotNull P4Job jobId) {
        jobs.remove(jobId);
    }


    @NotNull
    public String getSubmitStatus() {
        return submitStatus;
    }


    public boolean setSubmitStatus(@NotNull String status) {
        if (getJobStatuses().contains(status)) {
            submitStatus = status;
        }
        return false;
    }


    /**
     * This list can potentially refresh with each call to
     * {@link #setSelectedCurrentChanges(Collection)}.
     *
     * @return the list of valid job statuses
     */
    public synchronized List<String> getJobStatuses() {
        if (acceptableJobStates == null) {
            acceptableJobStates = new ArrayList<String>();

            // special status used for check-ins, but is not a formal status.
            acceptableJobStates.add("same");

            acceptableJobStates.addAll(P4Exec.DEFAULT_JOB_STATUS);
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
        for (Collection<P4ChangeListId> changeLists : getP4ChangesFor(ideaChanges).values()) {
            currentChanges.addAll(changeLists);
            jobs.addAll(getJobsFor(changeLists));
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
        // Acceptable job states are only refreshed when absolutely
        // necessary.

        String previousServerId = jobServerId;

        // If there are no changes, the state is invalid.
        isJobAssociationValid = false;
        jobServerId = null;
        String serverId = null;
        currentClient = null;
        for (Client client: vcs.getClients()) {
            for (P4ChangeListId p4id : currentChanges) {
                if (p4id.isIn(client)) {
                    String serviceName = client.getConfig().getServiceName();
                    if (serverId == null) {
                        serverId = serviceName;
                        currentClient = client;
                    } else if (! serverId.equals(serviceName)) {
                        isJobAssociationValid = false;
                        currentClient = null;
                        return;
                    }
                }
            }
        }
        jobServerId = serverId;
        isJobAssociationValid = true;

        // serverId and jobClient should not be null at this point,
        // but the logic above doesn't make it clear to the compiler,
        // so that means a potential future issue, so we add the
        // additional logic protection.

        if (jobServerId != null && ! jobServerId.equals(previousServerId)) {
            acceptableJobStates = findJobStatesFor(currentClient);
        }
    }


    @Nullable
    private P4Job getJob(@NotNull final String jobId) {
        if (currentClient == null) {
            return null;
        }
        try {
            return currentClient.getServer().getJobForId(jobId);
        } catch (VcsException e) {
            LOG.info(e);
            return null;
        }
    }


    @NotNull
    private List<P4Job> getJobsFor(final Collection<P4ChangeListId> changes) {
        Set<P4Job> allJobs = new HashSet<P4Job>();

        for (Client client: vcs.getClients()) {
            for (P4ChangeListId change : changes) {
                if (change.isIn(client)) {
                    try {
                        final Collection<P4Job> jobs = client.getServer().getJobsForChangelist(change.getChangeListId());
                        if (jobs != null) {
                            allJobs.addAll(jobs);
                        }
                    } catch (P4InvalidConfigException e) {
                        LOG.info(e);
                    } catch (VcsException e) {
                        LOG.info(e);
                    }
                }
            }
        }

        List<P4Job> ret = new ArrayList<P4Job>(allJobs);
        Collections.sort(ret);
        return ret;
    }


    @NotNull
    private Map<Change, Collection<P4ChangeListId>> getP4ChangesFor(@NotNull final Collection<Change> changes) {
        // TODO look to moving this into P4ChangeListMapping
        Map<Change, Collection<P4ChangeListId>> ret = new HashMap<Change, Collection<P4ChangeListId>>();
        for (Change change : changes) {
            final LocalChangeList idea = ChangeListManager.getInstance(vcs.getProject()).getChangeList(change);
            Collection<P4ChangeListId> p4Changes = vcs.getChangeListMapping().getPerforceChangelists(idea);
            if (p4Changes == null) {
                p4Changes = Collections.emptyList();
            }
            ret.put(change, p4Changes);
        }
        return ret;
    }


    @NotNull
    private List<String> findJobStatesFor(@NotNull final Client client) {
        List<String> jobStatuses = new ArrayList<String>();

        // special status used for check-ins, but is not a formal status.
        jobStatuses.add("same");

        try {
            jobStatuses.addAll(client.getServer().getJobStatusValues());
        } catch (VcsException e) {
            LOG.info(e);
        }

        return Collections.unmodifiableList(jobStatuses);
    }
}
