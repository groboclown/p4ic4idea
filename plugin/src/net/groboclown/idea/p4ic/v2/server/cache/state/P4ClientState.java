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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static net.groboclown.idea.p4ic.v2.server.cache.state.CachedState.getAttribute;

/**
 * Very simple state representation of a Perforce client.  This is persisted locally, so that the plugin can
 * work whether it's online or offline.
 */
public class P4ClientState {
    private final boolean isServerCaseInsensitive;

    private final ClientServerId clientServerId;
    private final P4WorkspaceViewState workspace;
    private final Set<P4ChangeListState> changes = new HashSet<P4ChangeListState>();
    private final Set<P4FileSyncState> knownHave = new HashSet<P4FileSyncState>();
    private final Set<P4FileUpdateState> updatedFiles = new HashSet<P4FileUpdateState>();
    private final JobStatusListState jobStatusList;

    public P4ClientState(final boolean isServerCaseInsensitive, @NotNull final ClientServerId clientServerId,
            @NotNull final P4WorkspaceViewState workspace, final JobStatusListState jobStatusList) {
        this.isServerCaseInsensitive = isServerCaseInsensitive;
        this.clientServerId = clientServerId;
        this.workspace = workspace;
        this.jobStatusList = jobStatusList;
    }

    @NotNull
    public ClientServerId getClientServerId() {
        return clientServerId;
    }

    public boolean isServerCaseInsensitive() {
        return isServerCaseInsensitive;
    }

    @NotNull
    public Set<P4FileUpdateState> getUpdatedFiles() {
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


    public void serialize(@NotNull Element wrapper, @NotNull EncodeReferences refs) {
        wrapper.setAttribute("serverConnection", clientServerId.getServerConfigId());
        if (clientServerId.getClientId() != null) {
            wrapper.setAttribute("clientName", clientServerId.getClientId());
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

        for (P4ChangeListState change : changes) {
            Element el = new Element("ch");
            wrapper.addContent(el);
            change.serialize(el, refs);
        }

        // Because the file state is a shared knowledge, just store
        // the shared IDs.
        for (P4FileSyncState fileState : knownHave) {
            Element el = new Element("h");
            fileState.serialize(el, refs);
        }
        for (P4FileUpdateState fileState : updatedFiles) {
            Element el = new Element("u");
            fileState.serialize(el, refs);
        }
    }

    public static P4ClientState deserialize(@NotNull Element state, @NotNull DecodeReferences refs) {
        boolean isServerCaseInsensitive = Boolean.parseBoolean(getAttribute(state, "isServerCaseInsensitive"));
        String serverConnection = getAttribute(state, "serverConnection");
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
        JobStatusListState jobStatusList = new JobStatusListState();
        if (jobStatusEl != null) {
            jobStatusList = JobStatusListState.deserialize(state, refs);
        }

        ClientServerId clientServerId = ClientServerId.create(serverConnection, clientName);
        final P4ClientState ret = new P4ClientState(isServerCaseInsensitive, clientServerId,
                workspace, jobStatusList);

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

        return ret;
    }
}
