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

package net.groboclown.p4.server.api;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.ServerErrorEvent;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.messagebus.VcsRootClientPartsMessage;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 * <p>
 * This service sends the Client Added and Client Removed messages.  It maintains the online / offline
 * state by listening to connection messages.
 */
@Service(Service.Level.PROJECT)
public abstract class ProjectConfigRegistry
        implements Disposable {
    public static final Class<ProjectConfigRegistry> COMPONENT_CLASS = ProjectConfigRegistry.class;

    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistry.class);

    private final Project project;
    private final MessageBusClient.ProjectClient projectBusClient;
    private final MessageBusClient.ApplicationClient applicationBusClient;
    private boolean disposed = false;

    @Nullable
    public static ProjectConfigRegistry getInstance(@NotNull Project project) {
        return project.getService(COMPONENT_CLASS);
    }


    protected ProjectConfigRegistry(@NotNull Project project) {
        this.project = project;
        this.projectBusClient = MessageBusClient.forProject(project, this);
        this.applicationBusClient = MessageBusClient.forApplication(this);
        Disposer.register(project, this);
    }


    /**
     * Find all client configs for the given reference.  There may be situations where a
     * user has multiple VCS roots, each with different ways to connect to the same client +
     * server.  Many of the commands used by the plugin don't care about the root that is
     * referenced, but only the client + server, so multiple of these may need to be attempted.
     *
     * @param ref client reference
     * @return all rooted client configs for the given client server reference.
     */
    @NotNull
    public List<RootedClientConfig> getClientConfigsForRef(@NotNull ClientServerRef ref) {
        if (isDisposed()) {
            return List.of();
        }
        final List<RootedClientConfig> ret = new ArrayList<>();
        for (RootedClientConfig config: getRootedClientConfigs()) {
            if (ref.equals(config.getClientConfig().getClientServerRef())) {
                ret.add(config);
            }
        }
        return ret;
    }



    /**
     * Find all client configs for the given server.
     *
     * @param ref client reference
     * @return all rooted client configs for the given client server reference.
     */
    @NotNull
    public List<RootedClientConfig> getClientConfigsForServer(@NotNull P4ServerName ref) {
        if (isDisposed()) {
            return List.of();
        }
        final List<RootedClientConfig> ret = new ArrayList<>();
        for (RootedClientConfig config: getRootedClientConfigs()) {
            if (ref.equals(config.getServerConfig().getServerName())) {
                ret.add(config);
            }
        }
        return ret;
    }


    /**
     * Find the client that is closest to the {@literal file}.  If multiple clients
     * are in the same tree, then the client that is deepest is returned.
     *
     * @param file the file
     * @return the client that is the best match for the {@literal file}.
     */
    @Nullable
    public RootedClientConfig getClientConfigFor(@Nullable final VirtualFile file) {
        if (file == null) {
            return null;
        }
        int closestDepth = Integer.MAX_VALUE;
        RootedClientConfig closest = null;
        for (final RootedClientConfig rootedClientConfig : getRootedClientConfigs()) {
            // Match on the VCS root, not the client root.  Client root may be very different.
            for (final VirtualFile vcsRoot : rootedClientConfig.getProjectVcsRootDirs()) {
                int depth = FileTreeUtil.getPathDepth(file, vcsRoot);
                if (depth >= 0 && depth < closestDepth) {
                    closestDepth = depth;
                    closest = rootedClientConfig;
                }
            }
        }

        return closest;
    }

    /**
     * Each VCS root has at most 1 client config.  This searches the list of registered
     * configurations for the best-match of the file to the VCS root.
     *
     * @param file a file in the project.
     * @return the best match client config, or, if the file is not under a VCS root, null.
     */
    @Nullable
    public RootedClientConfig getClientConfigFor(@Nullable FilePath file) {
        // This behavior needs to match up with the VirtualFile implementation.
        if (file == null) {
            return null;
        }
        int closestDepth = Integer.MAX_VALUE;
        RootedClientConfig closest = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding best client root match for " + file);
        }
        for (RootedClientConfig rootedClientConfig : getRootedClientConfigs()) {
            // Match on the VCS root, not the client root.  Client root may be very different.
            for (final VirtualFile vcsRoot : rootedClientConfig.getProjectVcsRootDirs()) {
                int depth = FileTreeUtil.getPathDepth(file, vcsRoot);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Root " + vcsRoot + ": " + depth);
                }
                if (depth >= 0 && depth < closestDepth) {
                    closestDepth = depth;
                    closest = rootedClientConfig;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using client root " + (closest == null ? null : closest.getProjectVcsRootDirs()) + " for " + file);
        }

        return closest;
    }

    /**
     * Are any of the servers for the given client ref online?
     *
     * @param clientServerRef reference
     * @return true if at least one is online.
     */
    public boolean isOnline(@Nullable ClientServerRef clientServerRef) {
        if (clientServerRef == null) {
            return false;
        }
        for (RootedClientConfig configRoot: getClientConfigsForRef(clientServerRef)) {
            return configRoot.isOnline();
        }
        return false;
    }

    @NotNull
    public abstract List<RootedClientConfig> getRootedClientConfigs();

    public void initializeService() {
        LoginFailureMessage.addListener(applicationBusClient, this, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                ProjectConfigRegistry.this.onLoginError(e.getConfig());
            }

            @Override
            public void singleSignOnExecutionFailed(@NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {
                ProjectConfigRegistry.this.onLoginError(e.getConfig());
            }

            @Override
            public void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                ProjectConfigRegistry.this.onLoginExpired(e.getConfig());
            }

            @Override
            public void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                ProjectConfigRegistry.this.onPasswordInvalid(e.getConfig());
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                ProjectConfigRegistry.this.onPasswordUnnecessary(e.getConfig());
            }
        });
        ConnectionErrorMessage.addListener(applicationBusClient, this, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public <E extends Exception> void onHostConnectionError(@NotNull ServerErrorEvent<E> event) {
                ProjectConfigRegistry.this.onHostConnectionError(event.getName());
            }
        });
        ServerConnectedMessage.addListener(applicationBusClient, this, this::onServerConnected);
        UserSelectedOfflineMessage.addListener(projectBusClient, this, this::onUserSelectedOffline);

        ReconnectRequestMessage.addListener(projectBusClient, this, new ReconnectRequestMessage.Listener() {
            @Override
            public void reconnectToAllClients(@NotNull ReconnectRequestMessage.ReconnectAllEvent e) {
                onUserSelectedAllOnline();
            }

            @Override
            public void reconnectToClient(@NotNull ReconnectRequestMessage.ReconnectEvent e) {
                onUserSelectedOnline(e.getRef());
            }
        });

        VcsRootClientPartsMessage.addListener(projectBusClient, this, new VcsRootClientPartsMessage.Listener() {
            @Override
            public void vcsRootClientPartsRemoved(
                    @Nonnull VcsRootClientPartsMessage.VcsRootClientPartsRemovedEvent event) {
                removeClientConfigAt(event.getVcsRoot());
            }

            @Override
            public void vcsRootUpdated(@Nonnull VcsRootClientPartsMessage.VcsRootClientPartsUpdatedEvent event) {
                updateClientConfigAt(event.getVcsRoot(), event.getParts());
            }
        });

        initializeRoots();
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            Disposer.dispose(this);
        }
    }

    @NotNull
    protected final Project getProject() {
        return project;
    }

    /** Throws an error if disposed */
    protected final void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }

    protected final void sendClientRemoved(@Nullable RootedClientConfig state) {
        if (state != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending notification that root was removed: " + state.getProjectVcsRootDirs());
            }
            ClientConfigRemovedMessage.reportClientConfigRemoved(getProject(), this,
                    state.getClientConfig());
        }
    }

    protected final void sendClientAdded(@Nullable RootedClientConfig state) {
        if (state != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending notification that client was added: " + state.getClientConfig());
            }
            ClientConfigAddedMessage.reportClientConfigurationAdded(getProject(),
                    state.getClientConfig());
        }
    }

    protected abstract void onLoginExpired(@NotNull OptionalClientServerConfig config);

    protected abstract void onLoginError(@NotNull OptionalClientServerConfig config);

    protected abstract void onPasswordInvalid(@NotNull OptionalClientServerConfig config);

    protected abstract void onPasswordUnnecessary(@NotNull OptionalClientServerConfig config);

    protected abstract void onHostConnectionError(@NotNull P4ServerName server);

    protected abstract void onServerConnected(@NotNull ServerConnectedMessage.ServerConnectedEvent event);

    protected abstract void onUserSelectedOffline(@NotNull UserSelectedOfflineMessage.OfflineEvent event);

    protected abstract void onUserSelectedOnline(@NotNull ClientServerRef clientServerRef);

    protected abstract void onUserSelectedAllOnline();

    protected abstract void updateClientConfigAt(@NotNull VirtualFile vcsRoot, @NotNull List<ConfigPart> parts);

    protected abstract void removeClientConfigAt(@NotNull VirtualFile vcsRootDir);

    protected abstract void initializeRoots();
}
