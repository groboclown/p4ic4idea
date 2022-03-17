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

package net.groboclown.p4plugin.modules.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.util.FilteredIterable;
import net.groboclown.p4.server.impl.cache.ClientConfigRootImpl;
import net.groboclown.p4.server.impl.cache.ServerStatusImpl;
import net.groboclown.p4.server.impl.config.part.EnvCompositePart;
import net.groboclown.p4plugin.modules.clientconfig.VcsRootConfigController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistryImpl.class);

    // NOTE: synchronize on the clients map, not the roots.
    private final Map<VirtualFile, ClientConfigRootImpl> registeredRoots = new HashMap<>();
    private final Map<ClientServerRef, ClientRef> registeredClients = new HashMap<>();

    // the servers are stored based on the shared config.  This means that the connection
    // approach (e.g. password vs. auth ticket) is taken into account.  It has a potential
    // drawback where disconnects to the same server are spread as different messages, unless
    // we take care about checking messages.
    private final Map<String, ServerRef> registeredServers = new HashMap<>();

    // If needs to synchronize on both clients and servers, sync on servers first, then clients.


    public ProjectConfigRegistryImpl(Project project) {
        super(project);
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register their own copy.
     *
     * @param ref client reference
     * @return the client config, or null if it isn't registered.
     */
    @Override
    @Nullable
    public ClientConfig getRegisteredClientConfigState(@NotNull ClientServerRef ref) {
        if (isDisposed()) {
            // do not throw an error.
            return null;
        }

        ClientRef client;
        synchronized (registeredClients) {
            client = registeredClients.get(ref);
        }
        if (client == null || client.isDisposed()) {
            return null;
        }
        return client.state;
    }


    // this is public only for test support.
    public void addClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
        checkDisposed();
        ClientServerRef ref = config.getClientServerRef();
        ClientConfigRootImpl updated;
        synchronized (registeredClients) {
            ClientConfigRootImpl oldRoot = registeredRoots.get(vcsRootDir);
            if (oldRoot != null) {
                if (oldRoot.getClientConfig().getClientServerRef().equals(ref)) {
                    // Old root is the same as the new root.  Don't do anything.
                    return;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(vcsRootDir + ": replacing " + oldRoot.getClientConfig() + " with " + config);
                }
                removeClientConfigAt(vcsRootDir);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug(vcsRootDir + ": adding " + config);
            }
            updated = createClientConfigState(config, vcsRootDir);
            registeredRoots.put(vcsRootDir, updated);
        }
        sendClientAdded(updated);
    }

    // made public for tests
    @Override
    public void removeClientConfigAt(@NotNull VirtualFile vcsRootDir) {
        checkDisposed();
        ClientConfigRoot removed;
        synchronized (registeredClients) {
            removed = registeredRoots.remove(vcsRootDir);
        }
        boolean registered = removed != null;
        if (registered) {
            sendClientRemoved(removed);
            cleanupClientState(removed);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed client config " + vcsRootDir);
            }
        }
    }

    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        super.dispose();
        final Collection<ClientConfigRoot> configs;
        synchronized (registeredClients) {
            // need a copy of the values, otherwise they'll be cleared when we
            // clear the registered configs.
            configs = new ArrayList<>(registeredRoots.values());
            registeredClients.clear();
        }
        LOG.info("Starting config loop");
        for (ClientConfigRoot clientConfig : configs) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dispose: Call on " + clientConfig);
            }
            sendClientRemoved(clientConfig);
        }
    }

    @Nonnull
    @Override
    protected Collection<ClientConfigRoot> getRegisteredStates() {
        List<ClientConfigRoot> clients;
        synchronized (registeredClients) {
            clients = new ArrayList<>(registeredRoots.values());
        }
        return clients.stream().filter((ccs) -> !ccs.isDisposed()).collect(Collectors.toList());
    }

    @Override
    protected void onLoginError(@NotNull OptionalClientServerConfig config) {
        // Note: does not check disposed state.

        ServerStatusImpl state = getServerConfigState(config.getServerConfig());
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
    protected void onServerConnected(@NotNull ServerConnectedMessage.ServerConnectedEvent e) {
        // Note: does not check disposed state.

        getServersFor(e.getServerConfig().getServerName()).forEach((state) -> {
            state.setServerHostProblem(false);
            // If the connectivity and login all lines up, then the login works.
            if (e.isLoggedIn() && e.getServerConfig().getServerId().equals(state.getServerConfig().getServerId())) {
                state.setServerLoginProblem(false);
            }
        });
    }

    @Override
    protected void onClientRemoved(@NotNull ClientConfig config, @Nullable VirtualFile vcsRootDir) {
        if (vcsRootDir != null) {
            // Need to double check that the config is the same at the root, because since this call the
            // root might have already changed to something else.
            synchronized (registeredClients) {
                ClientConfigRootImpl existing = registeredRoots.get(vcsRootDir);
                if (existing != null && config.getClientServerRef().equals(existing.getClientConfig().getClientServerRef())) {
                    removeClientConfigAt(vcsRootDir);
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping removal of " + vcsRootDir +
                            ": existing registered config (" + existing + ") does not match requested removal config (" +
                            config + ")");
                }
            }
        } else {
            LOG.warn("Skipping removal of client config " + config + " at null root");
        }
    }

    @Override
    protected void onUserSelectedOffline(@NotNull UserSelectedOfflineMessage.OfflineEvent event) {
        // Note: does not check disposed state.

        getServersFor(event.getName())
                .forEach((sc) -> sc.setUserOffline(true));

    }

    @Override
    protected void onUserSelectedOnline(@NotNull ClientServerRef clientRef) {
        getServersFor(clientRef.getServerName())
                .forEach((sc) -> sc.setUserOffline(false));
    }

    @Override
    protected void onUserSelectedAllOnline() {
        getAllServers()
                .forEach((sc) -> sc.setUserOffline(false));
    }

    @Override
    protected void initializeRoots() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating VCS roots");
        }
        synchronized (registeredServers) {
            final Set<VirtualFile> oldRoots;
            synchronized (registeredClients) {
                oldRoots = new HashSet<>(registeredRoots.keySet());
            }
            for (VcsDirectoryMapping directoryMapping : getDirectoryMappings()) {
                final VirtualFile rootDir = VcsUtil.getVirtualFile(directoryMapping.getDirectory());
                if (rootDir == null) {
                    LOG.info("Skipping VCS directory mapping with no root directory");
                    continue;
                }
                final List<ConfigPart> parts =
                        VcsRootConfigController.getInstance().getConfigPartsForRoot(getProject(), rootDir);
                if (parts != null) {
                    updateClientConfigAt(rootDir, parts);
                }
                oldRoots.remove(rootDir);
            }
            oldRoots.forEach(this::removeClientConfigAt);
        }
    }

    @Override
    protected void updateClientConfigAt(@NotNull VirtualFile root, @NotNull List<ConfigPart> parts) {
        MultipleConfigPart parentPart = new MultipleConfigPart("Project Registry", parts);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Add mapping for " + root + " -> " + parentPart);
        }
        try {
            if (ServerConfig.isValidServerConfig(parentPart)) {
                ServerConfig serverConfig = ServerConfig.createFrom(parentPart);
                if (ClientConfig.isValidClientConfig(serverConfig, parentPart)) {
                    ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, parentPart);
                    addClientConfig(clientConfig, root);
                    return;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(root + ": skipping invalid config " + parentPart);
            }
        } catch (IllegalArgumentException e) {
            LOG.info("Problem with config under " + root + ": " + parentPart, e);
        }

        // Invalid config.
        removeClientConfigAt(root);
    }

    @NotNull
    private Iterable<VcsDirectoryMapping> getDirectoryMappings() {
        return new FilteredIterable<>(
                ProjectLevelVcsManager.getInstance(getProject()).getDirectoryMappings(),
                (mapping) -> mapping != null && P4VcsKey.VCS_NAME.equals(mapping.getVcs()));
    }

    private void cleanupClientState(@NotNull ClientConfigRoot removed) {
        ClientServerRef clientServerRef = removed.getClientConfig().getClientServerRef();
        boolean didRemove = false;
        synchronized (registeredClients) {
            ClientRef ref = registeredClients.get(clientServerRef);
            if (ref != null && ref.removeClientConfigRoot()) {
                registeredClients.remove(clientServerRef);
                didRemove = true;
            }
        }
        if (didRemove) {
            deregisterServerForConfig(removed.getClientConfig());
        }
        removed.dispose();
    }

    private ClientConfigRootImpl createClientConfigState(@NotNull ClientConfig inpConfig, @NotNull VirtualFile vcsRootDir) {
        ServerStatusImpl serverState;
        synchronized (registeredServers) {
            ServerRef ref = registeredServers.get(inpConfig.getServerConfig().getServerId());
            if (ref == null || ref.state.isDisposed()) {
                ref = new ServerRef(createServerConfigState(inpConfig.getServerConfig()));
                registeredServers.put(inpConfig.getServerConfig().getServerId(), ref);
            }
            ref.addClientConfigRef();
            serverState = ref.state;
        }
        ClientConfig config;
        synchronized (registeredClients) {
            ClientRef ref = registeredClients.get(inpConfig.getClientServerRef());
            if (ref == null || ref.isDisposed()) {
                ref = new ClientRef(inpConfig);
                registeredClients.put(inpConfig.getClientServerRef(), ref);
            }
            ref.addClientConfigRootRef();
            config = ref.state;
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

    private static ClientConfig createDefaultClientConfig(@NotNull VirtualFile vcsRootDir) {
        ConfigPart part = new EnvCompositePart(vcsRootDir);
        ServerConfig serverConfig = ServerConfig.createFrom(part);
        return ClientConfig.createFrom(serverConfig, part);
    }

    private static class ClientRef {
        private final AtomicInteger refCount = new AtomicInteger(0);
        final ClientConfig state;

        private ClientRef(ClientConfig state) {
            this.state = state;
        }

        void addClientConfigRootRef() {
            refCount.incrementAndGet();
        }

        boolean removeClientConfigRoot() {
            return refCount.decrementAndGet() <= 0;
        }

        boolean isDisposed() {
            return refCount.get() <= 0;
        }
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
