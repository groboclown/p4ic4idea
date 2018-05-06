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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.cache.ClientConfigRootImpl;
import net.groboclown.p4.server.impl.cache.ServerStatusImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public class ProjectConfigRegistryImpl
       extends ProjectConfigRegistry {
    private final Map<ClientServerRef, ClientConfigRootImpl> registeredClients = new HashMap<>();

    // the servers are stored based on the shared config.  This means that the connection
    // approach (e.g. password vs. auth ticket) is taken into account.  It has a potential
    // drawback where disconnects to the same server are spread as different messages, unless
    // we take care about checking messages.
    private final Map<String, ServerRef> registeredServers = new HashMap<>();


    public ProjectConfigRegistryImpl(Project project) {
        super(project);
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register themselves
     *
     * @param ref client reference
     * @return the client config, or null if it isn't registered.
     */
    @Override
    @Nullable
    public ClientConfigRoot getRegisteredClientConfigState(@NotNull ClientServerRef ref) {
        if (isDisposed()) {
            // do not throw an error.
            return null;
        }

        ClientConfigRoot state;
        synchronized (registeredClients) {
            state = registeredClients.get(ref);
        }
        if (state == null || state.isDisposed()) {
            return null;
        }
        return state;
    }

    @Override
    public void addClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
        checkDisposed();
        ClientServerRef ref = config.getClientServerRef();
        ClientConfigRootImpl updated = createClientConfigState(config, vcsRootDir);
        ClientConfigRoot existing;
        synchronized (registeredClients) {
            existing = registeredClients.get(ref);
            registeredClients.put(ref, updated);
        }
        if (existing != null) {
            sendClientRemoved(existing);
        }
        sendClientAdded(updated);
    }

    /**
     * Removes the client configuration registration with the given reference.  If it is registered, then
     * the appropriate messages will be sent out.
     *
     * @param ref the reference to de-register
     * @return true if it was registered, false if not.
     */
    @Override
    public boolean removeClientConfig(@NotNull ClientServerRef ref) {
        checkDisposed();
        ClientConfigRoot removed;
        synchronized (registeredClients) {
            removed = registeredClients.remove(ref);
        }
        boolean registered = removed != null;
        if (registered) {
            sendClientRemoved(removed);
            cleanupClientState(removed);
        }
        // FIXME dispose the removed client config.
        return registered;
    }

    @Override
    public void dispose() {
        super.dispose();
        final Collection<ClientConfigRoot> configs;
        synchronized (registeredClients) {
            // need a copy of the values, otherwise they'll be cleared when we
            // clear the registered configs.
            configs = new ArrayList<>(registeredClients.values());
            registeredClients.clear();
        }
        for (ClientConfigRoot clientConfig : configs) {
            sendClientRemoved(clientConfig);
        }
    }

    @Nonnull
    @Override
    protected Collection<ClientConfigRoot> getRegisteredStates() {
        List<ClientConfigRoot> clients;
        synchronized (registeredClients) {
            clients = new ArrayList<>(registeredClients.values());
        }
        return clients.stream().filter((ccs) -> !ccs.isDisposed()).collect(Collectors.toList());
    }

    @Override
    protected void onLoginError(@NotNull ServerConfig config) {
        // Note: does not check disposed state.

        ServerStatusImpl state = getServerConfigState(config);
        if (state != null) {
            state.setServerLoginProblem(true);
        }
    }

    @Override
    protected void onHostConnectionError(@NotNull P4ServerName server) {
        // Note: does not check disposed state.

        getServersFor(server).forEach((state) -> state.setServerHostProblem(true));
   }

    @Override
    protected void onServerConnected(@NotNull ServerConfig server) {
        // Note: does not check disposed state.

        getServersFor(server.getServerName()).forEach((state) -> {
            state.setServerHostProblem(false);
            // If the connectivity and login all lines up, then the login works.
            if (server.getServerId().equals(state.getServerConfig().getServerId())) {
                state.setServerLoginProblem(false);
            }
        });
    }

    @Override
    protected void onClientRemoved(@NotNull ClientConfig config, @Nullable VirtualFile vcsRootDir) {
        // Note: does not check disposed state.

        ClientConfigRoot removedState;
        synchronized (registeredClients) {
            removedState = registeredClients.remove(config.getClientServerRef());
        }

        // If we have it, then clean it up
        if (removedState != null) {
            cleanupClientState(removedState);
        }
    }

    @Override
    protected void onUserSelectedOffline(@NotNull P4ServerName serverName) {
        // Note: does not check disposed state.

        getServersFor(serverName)
                .forEach((sc) -> sc.setUserOffline(true));

    }

    protected void onUserSelectedOnline(@NotNull ClientServerRef clientRef) {
        getServersFor(clientRef.getServerName())
                .forEach((sc) -> sc.setUserOffline(false));
    }

    protected void onUserSelectedAllOnline() {
        getAllServers()
                .forEach((sc) -> sc.setUserOffline(false));
    }


    private void cleanupClientState(@NotNull ClientConfigRoot removed) {
        // Note: does not check disposed state.

        deregisterServerForConfig(removed.getClientConfig());
        removed.dispose();
    }

    private ClientConfigRootImpl createClientConfigState(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
        ServerStatusImpl serverState;
        synchronized (registeredServers) {
            ServerRef ref = registeredServers.get(config.getServerConfig().getServerId());
            if (ref == null || ref.state.isDisposed()) {
                ref = new ServerRef(createServerConfigState(config.getServerConfig()));
                registeredServers.put(config.getServerConfig().getServerId(), ref);
            }
            ref.addClientConfigRef();
            serverState = ref.state;
        }
        return new ClientConfigRootImpl(config, serverState, vcsRootDir);
    }

    private ServerStatusImpl createServerConfigState(ServerConfig serverConfig) {
        ServerStatusImpl state = new ServerStatusImpl(serverConfig);
        Disposer.register(this, state);
        return state;
    }

    private void deregisterServerForConfig(ClientConfig config) {
        synchronized (registeredServers) {
            ServerRef ref = registeredServers.get(config.getServerConfig().getServerId());
            if (ref != null) {
                if (ref.removeClientConfig()) {
                    registeredServers.remove(config.getServerConfig().getServerId());
                    ref.state.dispose();
                }
            }
        }
    }

    private ServerStatusImpl getServerConfigState(@NotNull ServerConfig config) {
        ServerRef ref;
        synchronized (registeredServers) {
            ref = registeredServers.get(config.getServerId());
        }
        if (ref != null && !ref.state.isDisposed()) {
            return ref.state;
        }
        return null;
    }

    private Stream<ServerStatusImpl> getServersFor(@NotNull P4ServerName name) {
        return getAllServers()
                .filter((sc) -> name.equals(sc.getServerConfig().getServerName()));
    }

    private Stream<ServerStatusImpl> getAllServers() {
        Collection<ServerRef> servers;
        synchronized (registeredServers) {
            servers = new ArrayList<>(registeredServers.values());
        }
        return servers.stream()
                .filter((sr) -> !sr.state.isDisposed())
                .map((sr) -> sr.state);
    }

    private static class ServerRef {
        private final AtomicInteger refCount = new AtomicInteger(0);
        final ServerStatusImpl state;

        private ServerRef(@NotNull ServerStatusImpl state) {
            this.state = state;
        }

        void addClientConfigRef() {
            refCount.incrementAndGet();
        }

        boolean removeClientConfig() {
            return refCount.decrementAndGet() <= 0;
        }
    }
}
