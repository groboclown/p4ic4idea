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
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.util.FilteredIterable;
import net.groboclown.p4.server.impl.cache.RootedClientConfigImpl;
import net.groboclown.p4.server.impl.cache.ServerStatusImpl;
import net.groboclown.p4plugin.modules.clientconfig.VcsRootConfigController;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public class ProjectConfigRegistryImpl
       extends ProjectConfigRegistry {
    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistryImpl.class);

    private final Object sync = new Object();

    // vcs root -> rooted client config is a many-to-one relationship (multiple vcs roots
    // may map to a single rooted client config).  Rather than having mappings for quick lookups,
    // only the single list is used.  The total number of VCS roots is expected to never be large
    // enough for the complications around maintaining multiple maps + the source of truth is not
    // sufficient.  This might be revisited later, though.
    private final List<RootedClientConfigImpl> registeredConfigs = new ArrayList<>();

    // server status, though, is shared between configs.
    // This is the server unique id -> status
    private final Map<String, ServerStatusRef> registeredServers = new HashMap<>();

    public ProjectConfigRegistryImpl(Project project) {
        super(project);
    }


    // this is public only for test support.
    public void addClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
        checkDisposed();
        RootedClientConfigImpl newRootConfig = null;
        RootedClientConfigImpl oldRootConfig = null;
        boolean rootCreated = false;
        boolean rootRemoved = false;

        synchronized (sync) {
            // 1a. Find any config that has the root registered.
            // 1b. Find if there is another client that this shares information.
            for (final RootedClientConfigImpl root: registeredConfigs) {
                if (root.getProjectVcsRootDirs().contains(vcsRootDir)) {
                    // Note: could loop through all the items and ensure that the
                    // root directory is only registered in one place, but that adds a performance
                    // penalty and lots of extra logic that we don't want to get into here.
                    oldRootConfig = root;
                }
                if (config.equals(root.getClientConfig())) {
                    if (newRootConfig != null) {
                        LOG.warn("Multiple clients with same client config: " + root);
                    } else {
                        newRootConfig = root;
                    }
                }
            }

            if (oldRootConfig != null && oldRootConfig == newRootConfig) {
                // 2. If old root is the same as the new root, then don't do anything.
                return;
            }
            if (oldRootConfig != null) {
                // 3. If the root changes configuration, then there's a chance that the old
                //    configuration is not used anymore.
                rootRemoved = removeVcsRootFromConfig(oldRootConfig, vcsRootDir);
            }
            if (newRootConfig == null) {
                // 4. If the root changes to a new config, then create it and send a message.
                rootCreated = true;
                newRootConfig = addRootedClientConfig(config, vcsRootDir);
            } else {
                newRootConfig.addVcsRoot(vcsRootDir);
            }

        }
        if (rootRemoved) {
            assert oldRootConfig != null;
            sendClientRemoved(oldRootConfig);
        }
        if (rootCreated) {
            assert newRootConfig != null;
            sendClientAdded(newRootConfig);
        }
    }

    // made public for tests
    @Override
    public void removeClientConfigAt(@NotNull VirtualFile vcsRootDir) {
        checkDisposed();
        RootedClientConfigImpl removed = null;
        synchronized (sync) {
            for (final RootedClientConfigImpl root : registeredConfigs) {
                if (root.getProjectVcsRootDirs().contains(vcsRootDir)) {
                    // Note: could loop through all the items and ensure that the
                    // root directory is only registered in one place, but that adds a performance
                    // penalty and lots of extra logic that we don't want to get into here.
                    removed = root;
                    break;
                }
            }
            if (removed != null) {
                if (! removeVcsRootFromConfig(removed, vcsRootDir)) {
                    // The config is still around, so do not send a message that it was removed.
                    removed = null;
                }
            }
        }
        if (removed != null) {
            sendClientRemoved(removed);
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
        final List<RootedClientConfigImpl> configs;
        final List<ServerStatusRef> serverStatusRefs;
        synchronized (sync) {
            // need a copy of the values, otherwise they'll be cleared when we
            // clear the registered configs.
            configs = List.copyOf(registeredConfigs);
            serverStatusRefs = List.copyOf(registeredServers.values());
            registeredConfigs.clear();
            registeredServers.clear();
        }
        LOG.info("Starting config tear-down loop");
        for (RootedClientConfig clientConfig : configs) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dispose: Call on " + clientConfig);
            }
            sendClientRemoved(clientConfig);
        }
        serverStatusRefs.forEach((r) -> r.state.dispose());
    }

    @NotNull
    @Override
    public List<RootedClientConfig> getRootedClientConfigs() {
        final List<RootedClientConfig> clients = new ArrayList<>();
        synchronized (sync) {
            for (final RootedClientConfigImpl config: registeredConfigs) {
                if (! config.isDisposed()) {
                    clients.add(config);
                }
            }
        }
        return clients;
    }

    @Override
    protected void onLoginExpired(@NotNull OptionalClientServerConfig config) {
        getServersFor(config.getServerConfig()).forEach(ServerStatusImpl::markSessionExpired);
    }

    @Override
    protected void onLoginError(@NotNull OptionalClientServerConfig config) {
        getServersFor(config.getServerConfig()).forEach(ServerStatusImpl::markLoginFailed);
    }

    @Override
    protected void onPasswordInvalid(@NotNull OptionalClientServerConfig config) {
        getServersFor(config.getServerConfig()).forEach(ServerStatusImpl::markBadPassword);
    }

    @Override
    protected void onPasswordUnnecessary(@NotNull OptionalClientServerConfig config) {
        getServersFor(config.getServerConfig()).forEach(ServerStatusImpl::markPasswordIsUnnecessary);
    }

    @Override
    protected void onHostConnectionError(@NotNull P4ServerName server) {
        getServersFor(server).forEach(ServerStatusImpl::markBadConnection);
    }

    @Override
    protected void onServerConnected(@NotNull ServerConnectedMessage.ServerConnectedEvent e) {
        getServersFor(e.getServerConfig()).forEach((state) -> {
            state.markServerConnected();
            if (e.isLoggedIn()) {
                state.markLoggedIn(e.isPasswordUsed());
            }
        });
    }

    @Override
    protected void onUserSelectedOffline(@NotNull UserSelectedOfflineMessage.OfflineEvent event) {
        getServersFor(event.getName()).forEach((sc) -> sc.setUserOffline(true));

    }

    @Override
    protected void onUserSelectedOnline(@NotNull ClientServerRef clientRef) {
        getServersFor(clientRef.getServerName()).forEach((sc) -> sc.setUserOffline(false));
    }

    @Override
    protected void onUserSelectedAllOnline() {
        synchronized (sync) {
            for (ServerStatusRef ref : registeredServers.values()) {
                ref.state.setUserOffline(false);
            }
        }
    }

    @Override
    protected void initializeRoots() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating VCS roots");
        }
        synchronized (sync) {
            if (! registeredServers.isEmpty() || ! registeredConfigs.isEmpty()) {
                LOG.warn("");
                return;
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
            }
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

    private Stream<ServerStatusImpl> getServersFor(@NotNull ServerConfig config) {
        ServerStatusRef ref;
        synchronized (sync) {
            ref = registeredServers.get(config.getServerId());
        }
        if (ref != null && !ref.state.isDisposed()) {
            return Stream.of(ref.state);
        }
        return Stream.of();
    }

    private Stream<ServerStatusImpl> getServersFor(@NotNull P4ServerName name) {
        final List<ServerStatusImpl> ret = new ArrayList<>();
        synchronized (sync) {
            for (final ServerStatusRef ssr : registeredServers.values()) {
                if (ssr.config.getServerName().equals(name)) {
                    ret.add(ssr.state);
                }
            }
        }
        return ret.stream();
    }

    // Low-level call, must be done from within a synchronized block.
    // Also, it must be done outside a loop over registered* elements.
    // Assumes the root is already registered in the config.
    private boolean removeVcsRootFromConfig(@NotNull RootedClientConfigImpl config, @NotNull VirtualFile root) {
        if (config.isOneRootRemaining()) {
            // remove the config
            registeredConfigs.remove(config);
            final ServerStatusRef serverRef = registeredServers.get(config.getServerConfig().getServerId());
            if (serverRef != null) {
                if (serverRef.removeClientConfig()) {
                    registeredServers.remove(config.getServerConfig().getServerId());
                    serverRef.state.dispose();
                }
            }
            return true;
        }
        config.removeVcsRoot(root);
        return false;
    }

    // Low-level call, must be done within a synchronized block.
    // Assumes the config is not already registered.
    private RootedClientConfigImpl addRootedClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
        ServerStatusRef serverRef = registeredServers.get(config.getServerConfig().getServerId());
        if (serverRef == null) {
            serverRef = new ServerStatusRef(config.getServerConfig(), new ServerStatusImpl(config.getServerConfig()));
            Disposer.register(this, serverRef.state);
            registeredServers.put(config.getServerConfig().getServerId(), serverRef);
        }
        serverRef.addClientConfigRef();
        RootedClientConfigImpl ret = new RootedClientConfigImpl(config, serverRef.state, vcsRootDir);
        registeredConfigs.add(ret);
        return ret;
    }

    private static class ServerStatusRef {
        private final AtomicInteger refCount = new AtomicInteger(0);
        final ServerStatusImpl state;
        final ServerConfig config;

        private ServerStatusRef(@NotNull ServerConfig config, @NotNull ServerStatusImpl state) {
            this.config = config;
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
