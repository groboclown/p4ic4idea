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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

// FIXME the corresponding generated xml file seems to be
// removed either when the plugin is updated, or when the IDE is upgraded.

/**
 * Top level state storage for the view of all the clients.  This is per workspace.
 * Now, some workspaces may share clients, but if we allow this, then the cached
 * store of data could grow very large if the user removes a workspace.
 * The plugin should work fine under these circumstances, but it may have to do
 * more work than necessary.  To alleviate some of this, we can have application-wide
 * messaging of the objects for when the state changes.
 */
@State(
        name = "PerforceCachedClientServerState",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/perforce.xml")
        }
)
public class AllClientsState implements ApplicationComponent, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(AllClientsState.class);

    private final Map<ClientServerId, ClientLocalServerState> clientStates =
            new HashMap<ClientServerId, ClientLocalServerState>();
    private MessageBusConnection messageBus;

    @NotNull
    public static AllClientsState getInstance() {
        return ApplicationManager.getApplication().getComponent(AllClientsState.class);
    }


    @NotNull
    public ClientLocalServerState getStateForClient(@NotNull ClientServerId clientServerId,
            Callable<Boolean> isServerCaseInsensitiveCallable) {
        if (clientServerId.getClientId() == null) {
            throw new IllegalArgumentException("must supply a client name");
        }
        synchronized (clientStates) {
            ClientLocalServerState ret = clientStates.get(clientServerId);
            if (ret == null) {
                Boolean isServerCaseInsensitive = null;
                try {
                    isServerCaseInsensitive = isServerCaseInsensitiveCallable.call();
                } catch (Exception e) {
                    LOG.warn("Problem contacting Perforce server", e);
                }
                if (isServerCaseInsensitive == null) {
                    isServerCaseInsensitive = SystemInfo.isWindows;
                }
                ret = new ClientLocalServerState(
                        new P4ClientState(isServerCaseInsensitive, clientServerId, new P4WorkspaceViewState(clientServerId.getClientId())),
                        new P4ClientState(isServerCaseInsensitive, clientServerId, new P4WorkspaceViewState(clientServerId.getClientId())),
                        new ArrayList<PendingUpdateState>());
                clientStates.put(clientServerId, ret);
            }
            return ret;
        }
    }

    public void removeClientState(@NotNull ClientServerId client) {
        if (client.getClientId() == null) {
            // ignore
            return;
        }

        // FIXME it looks like this is too aggressively called.

        // FIXME debug
        LOG.info("Removing client cache " + client, new Exception("stack capture"));
        synchronized (clientStates) {
            final ClientLocalServerState state = clientStates.get(client);
            if (state != null) {
                clientStates.remove(client);
                LOG.warn("Removing client from cache storage: " + client, new Exception());
            }
        }
    }


    @Nullable
    @Override
    public Element getState() {
        synchronized (clientStates) {
            Element ret = new Element("all-clients-state");
            EncodeReferences refs = new EncodeReferences();
            for (Entry<ClientServerId, ClientLocalServerState> entry : clientStates.entrySet()) {
                Element child = new Element("client-state");
                ret.addContent(child);
                entry.getKey().serialize(child);
                entry.getValue().serialize(child, refs);
            }
            refs.serialize(ret);
            return ret;
        }
    }

    @Override
    public void loadState(@NotNull final Element state) {
        synchronized (clientStates) {
            clientStates.clear();
            DecodeReferences refs = DecodeReferences.deserialize(state);
            for (Element child : state.getChildren("client-state")) {
                ClientServerId id = ClientServerId.deserialize(child);
                if (id != null) {
                    ClientLocalServerState localServerState = ClientLocalServerState.deserialize(child, refs);
                    if (localServerState != null) {
                        clientStates.put(id, localServerState);
                    }
                }
            }
        }
    }

    @Override
    public void initComponent() {
        messageBus = ApplicationManager.getApplication().getMessageBus().connect();

        // TODO are these listeners necessary?
        Events.appBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull final Project project,
                    @NotNull final List<ProjectConfigSource> sources) {
                // FIXME implement
                // NOTE be project aware
            }
        });
        Events.appConfigInvalid(messageBus, new ConfigInvalidListener() {
            @Override
            public void configurationProblem(@NotNull final Project project, @NotNull final P4Config config,
                    @NotNull final P4InvalidConfigException ex) {
                // FIXME implement
                // NOTE be project aware
            }
        });
    }

    @Override
    public void disposeComponent() {
        if (messageBus != null) {
            messageBus.disconnect();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "PerforceCachedClientServerState";
    }
}
