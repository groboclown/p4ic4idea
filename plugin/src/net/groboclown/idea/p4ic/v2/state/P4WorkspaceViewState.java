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

package net.groboclown.idea.p4ic.v2.state;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Keeps a copy of the user's workspace view mappings (the root, alt roots, and view).  This is used to
 * detect when the cached files should be thrown out and reloaded.
 */
public class P4WorkspaceViewState implements CachedState {
    private String name;
    private Set<String> roots;
    private Map<String, String> depotWorkspaceMapping;

    @Override
    public Date getLastUpdated() {
        return null;
    }

    /* TODO move this to the Cached Server State
    @Override
    public void refreshState(@NotNull final ServerExecutor server, @NotNull final List<VcsException> discoveredErrors) {
        final IClient client;
        try {
            client = server.getClient();
        } catch (VcsException e) {
            discoveredErrors.add(e);
            return;
        }
        name = client.getName();
        roots = new HashSet<String>();
        roots.add(client.getRoot());
        roots.addAll(client.getAlternateRoots());
        final ClientView view = client.getClientView();
        final List<IClientViewMapping> entries = view.getEntryList();
    }
    */
}
