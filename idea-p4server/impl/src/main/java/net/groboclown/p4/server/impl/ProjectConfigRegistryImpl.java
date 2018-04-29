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

package net.groboclown.p4.server.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.cache.ClientConfigState;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientConfigConnectionFailedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.impl.cache.ServerConfigStateImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public class ProjectConfigRegistryImpl
       extends ProjectConfigRegistry {
    public static final String COMPONENT_NAME = ProjectConfigRegistryImpl.class.getName();

    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistryImpl.class);

    private final Project project;
    private final Map<ClientServerRef, ClientConfigState> registeredConfigs = new HashMap<>();

    // the servers are stored based on the shared config.  This means that the connection
    // approach (e.g. password vs. auth ticket) is taken into account.  It has a potential
    // drawback where disconnects to the same server are spread as different messages, unless
    // we take care about checking messages.
    private final Map<String, ServerRef> registeredServers = new HashMap<>();

    private boolean disposed = false;


    public ProjectConfigRegistryImpl(Project project) {
        this.project = project;

        MessageBusClient mbClient = MessageBusClient.forProject(project, this);
        ClientConfigConnectionFailedMessage.addListener(mbClient, new ClientConfigConnectionFailedMessage.HostErrorListener() {
            @Override
            public void onLoginError(@NotNull ClientConfig config) {
                markServerConfigProblem(config.getServerConfig());
            }

            @Override
            public void onHostConnectionError(@NotNull ClientConfig config) {
                markServerConfigProblem(config.getServerConfig().getServerName());
            }
        });

        ClientConfigRemovedMessage.addListener(mbClient, config -> {
            removeClientWithoutMessage(config);
        });
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register themselves
     *
     * @param ref client reference
     * @return the client config, or null if it isn't registered.
     */
    @Nullable
    public ClientConfig getRegisteredClientConfig(@NotNull ClientServerRef ref) {
        ClientConfigState state;
        synchronized (registeredConfigs) {
            state = registeredConfigs.get(ref);
        }
        if (state == null) {
            return null;
        }
        return state.getClientConfig();
    }

    @Nullable
    @Override
    public ClientConfigState getRegisteredClientConfigState(@NotNull ClientServerRef ref) {
        // FIXME
        return null;
    }

    /**
     * Registers the client configuration to the project and the application.  If a configuration with the same
     * client-server reference is already registered, then it will be removed.  If that configuration is the exact
     * same as the requested added configuration, then it will still be removed then re-added.
     *
     * @param config configuration to register
     */
    public void addClientConfig(@NotNull ClientConfig config) {
        checkDisposed();
        ClientServerRef ref = config.getClientServerRef();
        ClientConfigState existing;
        synchronized (registeredConfigs) {
            existing = registeredConfigs.get(ref);
            registeredConfigs.put(ref, config);
        }
        if (existing != null) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, existing.getClientConfig());
            removeConfigFromApplication(project, config);
        }
        ClientConfigAddedMessage.reportClientConfigAdded(project, config);
        addConfigToApplication(project, config);
    }

    /**
     * Removes the client configuration registration with the given reference.  If it is registered, then
     * the appropriate messages will be sent out.
     *
     * @param ref the reference to de-register
     * @return true if it was registered, false if not.
     */
    public boolean removeClientConfig(@NotNull ClientServerRef ref) {
        checkDisposed();
        ClientConfigState removed;
        synchronized (registeredConfigs) {
            removed = registeredConfigs.remove(ref);
        }
        boolean registered = removed != null;
        if (registered) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, removed.getClientConfig());
            removeConfigFromApplication(project, removed);
        }
        // FIXME dispose the removed client config.
        return registered;
    }

    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void dispose() {
        disposed = true;
        final Collection<ClientConfig> configs;
        synchronized (registeredConfigs) {
            // need a copy of the values, otherwise they'll be cleared when we
            // clear the registered configs.
            configs = new ArrayList<>(registeredConfigs.values());
            registeredConfigs.clear();
        }
        for (ClientConfig clientConfig : configs) {
            ClientConfigRemovedMessage.reportClientConfigRemoved(project, clientConfig);
            removeConfigFromApplication(project, clientConfig);
        }
    }

    @Override
    public void disposeComponent() {
        dispose();
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    /** Throws an error if disposed */
    protected void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }

    private void registerServerForConfig(ClientConfig config) {
        synchronized (registeredServers) {
            ServerRef ref = registeredServers.get(config.getServerConfig().getServerId());
            if (ref == null) {
                ref = new ServerRef(createServerConfigState(config.getServerConfig()));
                registeredServers.put(config.getServerConfig().getServerId(), ref);
                // FIXME add message bus listeners
            }
            ref.addClientConfigRef();
        }
    }

    private void deregisterServerForConfig(ClientConfig config) {
        synchronized (registeredServers) {
            ServerRef ref = registeredServers.get(config.getServerConfig().getServerId());
            if (ref != null) {
                if (ref.removeClientConfig()) {
                    // FIXME dispose server ref
                    registeredServers.remove(config.getServerConfig().getServerId());
                }
            }
        }
    }


    private static class ServerRef {
        private final AtomicInteger refCount = new AtomicInteger(0);
        final ServerConfigStateImpl state;

        private ServerRef(@NotNull ServerConfig config) {
            this.state = new ServerConfigStateImpl(config);
        }

        void addClientConfigRef() {
            refCount.incrementAndGet();
        }

        boolean removeClientConfig() {
            return refCount.decrementAndGet() <= 0;
        }
    }
}
