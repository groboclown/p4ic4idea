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

package net.groboclown.idea.p4ic.v2.server;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigListener;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.state.AllClientsState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The owner for the {@link P4Server} instances.
 */
public class P4ServerManager implements ProjectComponent {
    private final Project project;
    private final MessageBusConnection messageBus;
    private final Map<ClientServerId, P4Server> servers = new HashMap<ClientServerId, P4Server>();
    private final AlertManager alertManager;
    private volatile boolean connectionsValid = false;


    // TODO keep track of the ServerConnection objects.


    public P4ServerManager(@NotNull final Project project)
            throws VcsException {
        this.project = project;
        this.alertManager = AlertManager.getInstance();
        this.messageBus = project.getMessageBus().connect();
    }


    @Nullable
    public P4Server getServerFor(@NotNull Client client) {
        if (connectionsValid) {
            synchronized (servers) {
                final P4Server ret = servers.get(ClientServerId.create(client));
                if (ret.isValid()) {
                    return ret;
                }
            }
        }
        return null;
    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        this.messageBus.subscribe(P4ConfigListener.TOPIC, new P4ConfigListener() {

            @Override
            public void configChanges(@NotNull final Project project, @NotNull final P4Config original,
                    @NotNull final P4Config config) {
                // Connections are potentially invalid.
                // There may also be new connections.  This keeps it all up-to-date.
                synchronized (servers) {
                    // client/servers that are not in the new list are marked invalid.
                    Set<ClientServerId> knownServers = new HashSet<ClientServerId>(servers.keySet());
                    for (Client client : P4Vcs.getInstance(project).getClients()) {
                        final ClientServerId id = ClientServerId.create(client);
                        if (knownServers.contains(id)) {
                            knownServers.remove(id);
                        } else {
                            try {
                                servers.put(id, new P4Server(project, client));
                            } catch (VcsException e) {
                                // FIXME
                                // correct this bundle link,

                                alertManager.addWarning(P4Bundle.message("server.failure"), e);
                            }
                        }
                    }
                    for (ClientServerId serverId : knownServers) {
                        servers.get(serverId).setValid(false);
                    }
                }
            }

            @Override
            public void configurationProblem(@NotNull final Project project, @NotNull final P4Config config,
                    @NotNull final P4InvalidConfigException ex) {
                // Connections are temporarily invalid.
                connectionsValid = false;
                synchronized (servers) {
                    for (P4Server server : servers.values()) {
                        server.setValid(false);
                    }
                }
            }

        });
    }

    @Override
    public void disposeComponent() {
        final AllClientsState clientState = AllClientsState.getInstance(project);
        for (P4Server p4Server : servers.values()) {
            if (!p4Server.isValid()) {
                // Remove from our cache, because the server isn't being used anymore.
                clientState.removeClientState(ClientServerId.create(p4Server.getClient()));
            }
            p4Server.dispose();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Perforce Server Manager";
    }
}
