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
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
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
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                    // This happens at startup before the system has loaded,
                    // or right after an announcement is sent out, before
                    // we had a chance to reload our server connections.

                    LOG.info("No server connections known for project " + project.getName() + "; forcing an early reload");

                    initializeServers();
                    // Only the events will reload our servers.
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found server configs " + servers);
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
    public Map<P4Server, List<FilePath>> mapFilePathsToP4Server(Collection<FilePath> files)
            throws InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("mapping to servers: " + new ArrayList<FilePath>(files));
        }
        if (connectionsValid) {
            Map<P4Server, List<FilePath>> ret = new HashMap<P4Server, List<FilePath>>();
            List<P4Server> servers = getServers();
            if (servers.isEmpty()) {
                LOG.info("no valid servers registered");
                return ret;
            }
            // Find the shallowest match.
            for (FilePath file : files) {
                P4Server minDepthServer = getServerForPath(servers, file);
                List<FilePath> match = ret.get(minDepthServer);
                if (match == null) {
                    match = new ArrayList<FilePath>();
                    ret.put(minDepthServer, match);
                }
                match.add(file);
            }
            return ret;
        } else {
            LOG.info("configs not valid");
            return Collections.emptyMap();
        }
    }


    @Nullable
    public P4Server getForFilePath(@NotNull FilePath fp) throws InterruptedException {
        if (connectionsValid) {
            return getServerForPath(getServers(), fp);
        } else {
            LOG.info("configs not valid");
            return null;
        }
    }

    public P4Server getForVirtualFile(@NotNull VirtualFile vf) throws InterruptedException {
        if (connectionsValid) {
            return getServerForPath(getServers(), FilePathUtil.getFilePath(vf));
        } else {
            LOG.info("configs not valid");
            return null;
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

        // The servers need to be loaded initially, but they can't be loaded
        // at this point in time, because the file system isn't fully
        // initialized yet.  So, register a post-startup action.
        StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                initializeServers();
            }
        });

        // Make sure the events are correctly registered, too.

        Events.appBaseConfigUpdated(appMessageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull final Project project,
                    @NotNull final List<ProjectConfigSource> sources) {
                if (project != P4ServerManager.this.project) {
                    // Does not affect this project
                    return;
                }


                // Connections are potentially invalid.  Because the primary project config may be no longer
                // valid, just mark all of the configs invalid.
                // There may also be new connections.  This keeps it all up-to-date.
                synchronized (servers) {
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
                    final AllClientsState clientState = AllClientsState.getInstance();
                    for (ClientServerId serverId : knownServers) {
                        // FIXME check if this is too aggressive.
                        servers.get(serverId).setValid(false);
                        clientState.removeClientState(serverId);
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
                if (project == P4ServerManager.this.project) {
                    final AllClientsState clientState = AllClientsState.getInstance();

                    // Connections are temporarily invalid.
                    connectionsValid = false;
                    synchronized (servers) {
                        // FIXME examine whether this is appropriate to keep calling.
                        for (P4Server server : servers.values()) {
                            server.setValid(false);
                            clientState.removeClientState(server.getClientServerId());
                        }
                    }
                }
            }
        });
    }

    @Override
    public void disposeComponent() {
        for (P4Server p4Server : servers.values()) {
            // Note: don't remove the server from the cache at this point, because
            // it can be used later
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


    @Nullable
    private P4Server getServerForPath(@NotNull List<P4Server> servers, @NotNull FilePath file)
            throws InterruptedException {
        int minDepth = Integer.MAX_VALUE;
        P4Server minDepthServer = null;
        for (P4Server server : servers) {
            int depth = server.getFilePathMatchDepth(file);
            if (LOG.isDebugEnabled()) {
                LOG.debug(" --- server " + server + " match depth: " + depth);
            }
            if (depth < minDepth && depth >= 0) {
                minDepth = depth;
                minDepthServer = server;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Matched " + file + " to " + minDepthServer);
        }
        return minDepthServer;
    }


    private void initializeServers() {
        P4ConfigProject cp = P4ConfigProject.getInstance(project);
        try {
            final List<ProjectConfigSource> sources = cp.loadProjectConfigSources();
            synchronized (servers) {
                servers.clear();
                for (ProjectConfigSource source : sources) {
                    final P4Server server = new P4Server(project, source);
                    servers.put(server.getClientServerId(), server);
                }
            }
        } catch (P4InvalidConfigException e) {
            LOG.info("source load caused error", e);
            synchronized (servers) {
                servers.clear();
            }
        }
    }
}
