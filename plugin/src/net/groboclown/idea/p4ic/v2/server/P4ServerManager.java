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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.state.AllClientsState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * The owner for the {@link P4Server} instances.
 */
public class P4ServerManager implements ProjectComponent {
    private static final Logger LOG = Logger.getInstance(P4ServerManager.class);

    private final Project project;
    private final MessageBusConnection appMessageBus;
    private final Map<ClientServerId, P4Server> servers = new HashMap<ClientServerId, P4Server>();
    private final AlertManager alertManager;
    private volatile boolean connectionsValid = true;


    // TODO keep track of the ServerConnection objects.


    /** should only be created by the P4Vcs object. */
    public P4ServerManager(@NotNull final Project project) {
        this.project = project;
        this.alertManager = AlertManager.getInstance();
        this.appMessageBus = ApplicationManager.getApplication().getMessageBus().connect();
    }


    @NotNull
    public List<P4Server> getServers() {
        if (connectionsValid) {
            List<P4Server> ret;
            synchronized (servers) {
                if (servers.isEmpty()) {
                    // FIXME this shouldn't happen - it should be loaded correctly and automatically.

                    // Attempt to reload them.
                    LOG.info("No servers known; attempting reload");
                    P4ConfigProject cp = P4ConfigProject.getInstance(project);
                    try {
                        for (ProjectConfigSource source: cp.loadProjectConfigSources()) {
                            final P4Server server = new P4Server(project, source);
                            servers.put(server.getClientServerId(), server);
                        }
                    } catch (P4InvalidConfigException e) {
                        LOG.info("source reload caused error", e);
                        return Collections.emptyList();
                    }
                }
                ret = new ArrayList<P4Server>(servers.size());
                for (P4Server server: servers.values()) {
                    if (server.isValid()) {
                        ret.add(server);
                    }
                }
            }
            return ret;
        } else {
            return Collections.emptyList();
        }
    }


    /**
     * @param files files
     * @return the matched mapping of files to the servers.  There might be a "null" server entry, which
     * contains a list of file paths that didn't map to a client.
     */
    @NotNull
    public Map<P4Server, List<FilePath>> mapFilePathsToP4Server(Collection<FilePath> files) {
        LOG.info("mapping to servers: " + files);
        if (connectionsValid) {
            Map<P4Server, List<FilePath>> ret = new HashMap<P4Server, List<FilePath>>();
            List<P4Server> servers = getServers();
            if (servers.isEmpty()) {
                LOG.info("no valid servers registered");
                return ret;
            }
            // Find the shallowest match.
            for (FilePath file : files) {
                int minDepth = Integer.MAX_VALUE;
                P4Server minDepthServer = null;
                for (P4Server server : servers) {
                    int depth = server.getFilePathMatchDepth(file);
                    if (depth < minDepth && depth >= 0) {
                        minDepth = depth;
                        minDepthServer = server;
                    }
                }
                List<FilePath> match = ret.get(minDepthServer);
                if (match == null) {
                    match = new ArrayList<FilePath>();
                    ret.put(minDepthServer, match);
                }
            }
            return ret;
        } else {
            LOG.info("configs not valid");
            return Collections.emptyMap();
        }
    }


    @Override
    public void projectOpened() {
        // intentionally empty
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        Events.appBaseConfigUpdated(appMessageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull final Project project,
                    @NotNull final List<ProjectConfigSource> sources) {
                // Connections are potentially invalid.  Because the primary project config may be no longer
                // valid, just mark all of the configs invalid.
                // There may also be new connections.  This keeps it all up-to-date.
                synchronized (servers) {
                    servers.clear();

                    // client/servers that are not in the new list are marked invalid.
                    Set<ClientServerId> knownServers = new HashSet<ClientServerId>(servers.keySet());

                    for (ProjectConfigSource source : sources) {
                        final ClientServerId id = source.getClientServerId();
                        if (knownServers.contains(id)) {
                            knownServers.remove(id);
                            servers.get(id).setValid(true);
                        } else {
                            final P4Server server = new P4Server(project, source);
                            servers.put(server.getClientServerId(), server);
                        }
                    }
                    for (ClientServerId serverId : knownServers) {
                        servers.get(serverId).setValid(false);
                        servers.remove(serverId);
                    }
                }
                connectionsValid = true;
            }
        });

        Events.appConfigInvalid(appMessageBus, new ConfigInvalidListener() {
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

        // FIXME initialize the servers
    }

    @Override
    public void disposeComponent() {
        final AllClientsState clientState = AllClientsState.getInstance();
        for (P4Server p4Server : servers.values()) {
            if (!p4Server.isValid()) {
                // Remove from our cache, because the server isn't being used anymore.
                clientState.removeClientState(p4Server.getClientServerId());
            }
            p4Server.dispose();
        }
        if (appMessageBus != null) {
            appMessageBus.disconnect();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Perforce Server Manager";
    }
}
