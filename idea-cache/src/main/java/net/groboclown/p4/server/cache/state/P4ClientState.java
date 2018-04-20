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

package net.groboclown.p4.server.cache.state;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static net.groboclown.idea.p4ic.v2.server.cache.state.CachedState.getAttribute;

/**
 * Very simple state representation of a Perforce client.  This is persisted locally, so that the plugin can
 * work whether it's online or offline.
 */
public class P4ClientState {
    private static final Logger LOG = Logger.getInstance(P4ClientState.class);


    private final boolean isServerCaseInsensitive;

    private final ClientServerRef clientServerRef;
    private final P4WorkspaceViewState workspace;
    private final Set<P4ChangeListState> changes = new HashSet<P4ChangeListState>();
    private final Set<P4FileSyncState> knownHave = new HashSet<P4FileSyncState>();
    private final FileUpdateStateList updatedFiles = new FileUpdateStateList();
    private final JobStatusListState jobStatusList;
    private final JobStateList jobs;
    private final UserSummaryStateList userStatusList;

    public P4ClientState(boolean isServerCaseInsensitive, @NotNull ClientServerRef clientServerRef,
            @NotNull P4WorkspaceViewState workspace, @NotNull JobStatusListState jobStatusList,
            @NotNull JobStateList jobs, @NotNull UserSummaryStateList userStatusList) {
        this.isServerCaseInsensitive = isServerCaseInsensitive;
        this.clientServerRef = clientServerRef;
        this.workspace = workspace;
        this.jobStatusList = jobStatusList;
        this.jobs = jobs;
        this.userStatusList = userStatusList;
    }


    /**
     * Empty out the values, and go back to a clean slate.
     */
    void flush() {
        // All read-only objects are not flushed.
        //      - workspace
        //      - job status
        changes.clear();
        knownHave.clear();
        updatedFiles.flush();
        jobs.flush();
        userStatusList.flush();
    }


    @NotNull
    public ClientServerRef getClientServerRef() {
        return clientServerRef;
    }

    public boolean isServerCaseInsensitive() {
        return isServerCaseInsensitive;
    }

    @NotNull
    public FileUpdateStateList getUpdatedFiles() {
        // must return the underlying data structure
        return updatedFiles;
    }

    @NotNull
    public P4WorkspaceViewState getWorkspaceView() {
        return workspace;
    }

    @NotNull
    public Set<P4ChangeListState> getChanges() {
        return changes;
    }

    @NotNull
    public JobStatusListState getJobStatusList() {
        return jobStatusList;
    }

    @NotNull
    public JobStateList getJobs() {
        return jobs;
    }

    @NotNull
    public UserSummaryStateList getUserStatusList() {
        return userStatusList;
    }

    @NotNull
    public Set<P4FileSyncState> getKnownHave() {
        return knownHave;
    }

    /**
     * Remove all the {@link UpdateRef} objects related to this state.
     *
     * @param pendingUpdateState state to remove referenced states
     */
    void stripStatesFor(@NotNull final PendingUpdateState pendingUpdateState) {
        Iterator<P4ChangeListState> iter = changes.iterator();
        while (iter.hasNext()) {
            final P4ChangeListState next = iter.next();
            if (next.getPendingUpdateRefId() == pendingUpdateState.getRefId()) {
                iter.remove();
            }
        }
        // we are iterating over a copy of the files, so this is fine.
        for (P4FileUpdateState file : updatedFiles) {
            if (file.getPendingUpdateRefId() == pendingUpdateState.getRefId()) {
                updatedFiles.remove(file);
            }
        }
    }

    public void serialize(@NotNull Element wrapper, @NotNull EncodeReferences refs) {
        wrapper.setAttribute("serverConnection", clientServerRef.getServerName().getFullPort());
        if (clientServerRef.getClientName() != null) {
            wrapper.setAttribute("clientName", clientServerRef.getClientName());
        }
        wrapper.setAttribute("isServerCaseInsensitive", Boolean.toString(isServerCaseInsensitive));

        {
            Element el = new Element("workspace");
            wrapper.addContent(el);
            workspace.serialize(el, refs);
        }

        {
            Element el = new Element("job-status");
            wrapper.addContent(el);
            jobStatusList.serialize(el, refs);
        }

        {
            Element el = new Element("users");
            wrapper.addContent(el);
            userStatusList.serialize(el, refs);
        }

        for (P4ChangeListState change : changes) {
            Element el = new Element("ch");
            wrapper.addContent(el);
            change.serialize(el, refs);
        }

        // Because the file state is a shared knowledge, just store
        // the shared IDs.
        for (P4FileSyncState fileState : knownHave) {
            Element el = new Element("h");
            wrapper.addContent(el);
            fileState.serialize(el, refs);
        }
        for (P4FileUpdateState fileState : updatedFiles) {
            Element el = new Element("u");
            wrapper.addContent(el);
            fileState.serialize(el, refs);
        }
    }

    public static P4ClientState deserialize(@NotNull Element state, @NotNull DecodeReferences refs) {
        boolean isServerCaseInsensitive = Boolean.parseBoolean(getAttribute(state, "isServerCaseInsensitive"));
        String serverConnection = getAttribute(state, "serverConnection");
        P4ServerName serverName = P4ServerName.forPortNotNull(serverConnection);
        String clientName = getAttribute(state, "clientName");

        Element workspaceEl = state.getChild("workspace");
        if (workspaceEl == null || serverConnection == null || clientName == null) {
            return null;
        }
        P4WorkspaceViewState workspace = P4WorkspaceViewState.deserialize(workspaceEl, refs);
        if (workspace == null) {
            return null;
        }

        Element jobStatusEl = state.getChild("job-status");
        JobStatusListState jobStatusList = null;
        if (jobStatusEl != null) {
            jobStatusList = JobStatusListState.deserialize(jobStatusEl, refs);
        }
        if (jobStatusList == null) {
            jobStatusList = new JobStatusListState();
        }

        Element usersEl = state.getChild("users");
        UserSummaryStateList userList = null;
        if (usersEl != null) {
            userList = UserSummaryStateList.deserialize(usersEl, refs);
        }
        if (userList == null) {
            userList = new UserSummaryStateList();
        }

        ClientServerRef clientServerRef = ClientServerRef.create(serverName, clientName);
        final P4ClientState ret = new P4ClientState(isServerCaseInsensitive, clientServerRef,
                workspace, jobStatusList, refs.getJobStateList(), userList);

        for (Element el: state.getChildren("ch")) {
            P4ChangeListState change = P4ChangeListState.deserialize(el, refs);
            if (change != null) {
                ret.changes.add(change);
            }
        }
        for (Element el: state.getChildren("h")) {
            P4FileSyncState file = P4FileSyncState.deserialize(el, refs);
            if (file != null) {
                ret.knownHave.add(file);
            }
        }
        for (Element el : state.getChildren("u")) {
            P4FileUpdateState file = P4FileUpdateState.deserialize(el, refs);
            if (file != null) {
                ret.updatedFiles.add(file);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Final list of updated files for " +
                    ret.clientServerRef + ": " + ret.updatedFiles);
        }
        return ret;
    }
}
