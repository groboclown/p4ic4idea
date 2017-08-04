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
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

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

    private final Map<ClientServerRef, ClientLocalServerState> clientStates =
            new HashMap<ClientServerRef, ClientLocalServerState>();
    private MessageBusConnection messageBus;

    @NotNull
    public static AllClientsState getInstance() {
        return ApplicationManager.getApplication().getComponent(AllClientsState.class);
    }


    @Nullable
    public ClientLocalServerState getCachedStateForClient(@NotNull ClientServerRef clientServerRef) {
        synchronized (clientStates) {
            return clientStates.get(clientServerRef);
        }
    }


    @NotNull
    public ClientLocalServerState getStateForClient(@NotNull ClientServerRef clientServerRef,
            Callable<Boolean> isServerCaseInsensitiveCallable) throws P4InvalidClientException {
        if (clientServerRef.getClientName() == null) {
            throw new P4InvalidClientException(clientServerRef);
        }

        // This needs to be done carefully, so we don't have long running blocks
        // against the server.
        ClientLocalServerState ret;
        synchronized (clientStates) {
            ret = clientStates.get(clientServerRef);
        }
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
                    new P4ClientState(isServerCaseInsensitive,
                            clientServerRef, new P4WorkspaceViewState(clientServerRef.getClientName()),
                            new JobStatusListState(), new JobStateList(), new UserSummaryStateList()),
                    new P4ClientState(isServerCaseInsensitive,
                            clientServerRef, new P4WorkspaceViewState(clientServerRef.getClientName()),
                            new JobStatusListState(), new JobStateList(), new UserSummaryStateList()),
                    new ArrayList<PendingUpdateState>());
            // It's okay to overwrite any existing value here.  The initialization
            // work has already been done, and we shouldn't be getting an inconsistent
            // state if it did.  Besides, this one is probably newer and should therefore win.
            synchronized (clientStates) {
                clientStates.put(clientServerRef, ret);
            }
        }
        return ret;
    }

    public void removeClientState(@NotNull ClientServerRef client) {
        if (client.getClientName() == null) {
            // ignore
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing client cache " + client);
        }
        synchronized (clientStates) {
            final ClientLocalServerState state = clientStates.get(client);
            if (state != null) {
                clientStates.remove(client);
            }
        }
    }


    @Nullable
    @Override
    public Element getState() {
        synchronized (clientStates) {
            Element ret = new Element("all-clients-state");
            EncodeReferences refs = new EncodeReferences();
            for (Entry<ClientServerRef, ClientLocalServerState> entry : clientStates.entrySet()) {
                Element child = new Element("client-state");
                ret.addContent(child);
                entry.getKey().marshal(child);
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
                ClientServerRef id = ClientServerRef.deserialize(child);
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
        Events.registerAppBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig newConfig,
                    @Nullable P4ProjectConfig previousConfiguration) {
                if (previousConfiguration != null) {
                    for (ClientConfig clientConfig : previousConfiguration.getClientConfigs()) {
                        removeClientState(ClientServerRef.create(clientConfig));
                    }
                }
            }
        });
        Events.registerAppConfigInvalid(messageBus, new ConfigInvalidListener() {
            @Override
            public void configurationProblem(@NotNull Project project, @NotNull P4ProjectConfig config,
                    @NotNull VcsConnectionProblem ex) {
                for (ClientConfig clientConfig : config.getClientConfigs()) {
                    removeClientState(ClientServerRef.create(clientConfig));
                }
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
