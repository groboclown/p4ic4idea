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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Top level state storage for the view of all the clients.  This is per workspace.
 * Now, some workspaces may share clients, but if we allow this, then the cached
 * store of data could grow very large if the user removes a workspace.
 * The plugin should work fine under these circumstances, but it may have to do
 * more work than necessary.  To alleviate some of this, we can have application-wide
 * messaging of the objects for when the state changes.
 */
@State(
        name = "PerforceCachedServerState",
        storages = {
                @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/perforce-state.xml")
        }
)
public class AllClientsState implements ProjectComponent, PersistentStateComponent<Element> {
    private final Map<ClientServerId, ClientLocalServerState> clientStates =
            new HashMap<ClientServerId, ClientLocalServerState>();
    private MessageBusConnection messageBus;

    @NotNull
    public static AllClientsState getInstance(@NotNull Project project) {
        // FIXME
        throw new IllegalStateException("not registered in plugin.xml");

        //return project.getComponent(AllClientsState.class);
    }


    public ClientLocalServerState getStateForClient(@NotNull Client client) {
        // FIXME create a new state if one doesn't exist.
        throw new IllegalStateException("not implemented");
    }

    public void removeClientState(@NotNull ClientServerId client) {
        // FIXME Called when the client is no longer used.
        throw new IllegalStateException("not implemented");
    }


    @Nullable
    @Override
    public Element getState() {
        // FIXME;
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void loadState(final Element state) {
        clientStates.clear();

        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {

    }

    @Override
    public void initComponent() {
        messageBus = ApplicationManager.getApplication().getMessageBus().connect();

        // TODO listen to events, which trigger a simulated server update or local update.
        // That's a project for the far off future; for the moment, that makes things
        // way too complicated.
    }

    @Override
    public void disposeComponent() {
        messageBus.disconnect();
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "p4-client-state";
    }
}
