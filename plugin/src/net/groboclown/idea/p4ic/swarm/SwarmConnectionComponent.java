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

package net.groboclown.idea.p4ic.swarm;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.P4ServerManager;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.SwarmClientFactory;
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import net.groboclown.p4.simpleswarm.exceptions.InvalidSwarmServerException;
import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwarmConnectionComponent implements ApplicationComponent, Disposable {
    private static final Logger LOG = Logger.getInstance(SwarmConnectionComponent.class);

    private final Map<ClientServerRef, SwarmClient> swarmClients = new HashMap<ClientServerRef, SwarmClient>();

    @NotNull
    public static SwarmConnectionComponent getInstance() {
        return ApplicationManager.getApplication().getComponent(SwarmConnectionComponent.class);
    }

    public SwarmConnectionComponent() {
    }

    @Override
    public void initComponent() {
        Events.registerAppBaseConfigUpdated(
                ApplicationManager.getApplication().getMessageBus().connect(this),
                new BaseConfigUpdatedListener() {
                    @Override
                    public void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig newConfig,
                            @Nullable P4ProjectConfig previousConfiguration) {
                        refreshSwarmConfigsFor(project);
                    }
                });
    }

    @Override
    public void disposeComponent() {
        synchronized (swarmClients) {
            swarmClients.clear();
        }
        Disposer.dispose(this);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "SwarmConnectionComponent";
    }

    @Nullable
    public SwarmClient getClientFor(ClientServerRef ref) {
        synchronized (swarmClients) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Has swarm clients for refs " + swarmClients.keySet() + "; request for " + ref);
            }
            return swarmClients.get(ref);
        }
    }

    @NotNull
    public Map<ClientServerRef, SwarmClient> getClientsFor(@NotNull Project project) {
        List<ClientServerRef> refs = P4Vcs.getInstance(project).getClientServerRefs();
        Map<ClientServerRef, SwarmClient> ret = new HashMap<ClientServerRef, SwarmClient>();
        for (ClientServerRef ref : refs) {
            synchronized (swarmClients) {
                SwarmClient client = swarmClients.get(ref);
                if (client != null) {
                    ret.put(ref, client);
                }
            }
        }
        return ret;
    }

    public void refreshSwarmConfigsFor(@NotNull Project project) {
        final Map<ClientServerRef, SwarmClient> validClients;
        synchronized (swarmClients) {
            // In case one project has client-servers that are not in another project,
            // we want to preserve those.
            validClients = new HashMap<ClientServerRef, SwarmClient>(swarmClients);
        }
        for (P4Server server: P4ServerManager.getInstance(project).getServers()) {
            // Always override the existing swarm client settings for this project.
            // Note that if a previous refresh had client A, and this one has client B,
            // then this leads to a memory leak.  The proper solution is to associate
            // the clients with the project, and clear those out first (if only
            // associated with the one project). However, that
            // leads to its own issue of needing to clean up the memory when the
            // project is removed, which this is not aware of.
            validClients.put(server.getClientServerId(), null);
            try {
                final SwarmConfig config = server.createSwarmConfig();
                if (config != null) {
                    config.withLogger(SWARM_LOGGER);
                    LOG.info("Trying Swarm Server " + config.getUri() + "; username " +
                            config.getUsername());
                    final SwarmClient client = SwarmClientFactory.createSwarmClient(config);
                    validClients.put(server.getClientServerId(), client);
                    LOG.info("Found valid swarm server: " + config);
                } else {
                    LOG.info("Could not find Swarm Server for " + server.getServerConfig());
                }
            } catch (InterruptedException e) {
                LOG.warn(e);
            } catch (UnauthorizedAccessException e) {
                LOG.info("Failed to authenticate to swarm server for " + server.getServerConfig(), e);
            } catch (InvalidSwarmServerException e) {
                LOG.warn("Swarm server seems invalid for " + server.getServerConfig(), e);
            } catch (IOException e) {
                LOG.warn("Swarm server seems invalid for " + server.getServerConfig(), e);
            }
        }
        synchronized (swarmClients) {
            swarmClients.putAll(validClients);
        }
    }

    @Override
    public void dispose() {
        synchronized (swarmClients) {
            swarmClients.clear();
        }
    }

    private static class SwarmLoggerImpl implements SwarmLogger {

        @Override
        public boolean isDebugEnabled() {
            return LOG.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            LOG.debug(msg);
        }

        @Override
        public void debug(Throwable e) {
            LOG.debug(e);
        }

        @Override
        public void debug(String msg, Throwable e) {
            LOG.debug(msg, e);
        }

        @Override
        public void info(String msg) {
            LOG.info(msg);
        }

        @Override
        public void info(Throwable e) {
            LOG.info(e);
        }

        @Override
        public void info(String msg, Throwable e) {
            LOG.info(msg, e);
        }

        @Override
        public void warn(String msg) {
            LOG.warn(msg);
        }

        @Override
        public void warn(Throwable e) {
            LOG.warn(e);
        }

        @Override
        public void warn(String msg, Throwable e) {
            LOG.warn(msg, e);
        }

        @Override
        public void error(String msg) {
            LOG.error(msg);
        }

        @Override
        public void error(Throwable e) {
            LOG.error(e);
        }

        @Override
        public void error(String msg, Throwable e) {
            LOG.error(msg, e);
        }
    }
    private static final SwarmLogger SWARM_LOGGER = new SwarmLoggerImpl();
}
