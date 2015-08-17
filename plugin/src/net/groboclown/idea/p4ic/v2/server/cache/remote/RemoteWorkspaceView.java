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

package net.groboclown.idea.p4ic.v2.server.cache.remote;

import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.impl.generic.client.ClientView;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.v2.server.cache.CachedServerState;
import net.groboclown.idea.p4ic.v2.server.cache.state.FileMappingRepo;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A representation of the server stored workspace view.  This class system doesn't support
 * changing the workspace view (must be done through the Perforce tools), but updates to
 * the view cause ripple effects in the rest of the system.
 */
public class RemoteWorkspaceView implements CachedServerState<RemoteWorkspaceView> {
    private P4WorkspaceViewState state;
    private final FileMappingRepo mappingRepo;


    public RemoteWorkspaceView(final FileMappingRepo mappingRepo) {
        this.mappingRepo = mappingRepo;
    }

    @Override
    public UpdatedCachedServerState<RemoteWorkspaceView> loadCurrentState(@NotNull final ServerExecutor server,
            @NotNull final List<VcsException> discoveredErrors) {
        final IClient client;
        try {
            client = server.getClient();
        } catch (VcsException e) {
            discoveredErrors.add(e);
            return null;
        }
        final RemoteWorkspaceView next = new RemoteWorkspaceView(mappingRepo);
        next.state = new P4WorkspaceViewState(client.getName());
        next.state.addRoot(client.getRoot());
        for (String root: client.getAlternateRoots()) {
            next.state.addRoot(root);
        }
        final ClientView view = client.getClientView();
        for (IClientViewMapping entry: view.getEntryList()) {
            next.state.addViewMapping(entry.getDepotSpec(false), entry.getClient(false));
        }
        return new UpdatedCachedServerState<RemoteWorkspaceView>(this, next);
    }
}
