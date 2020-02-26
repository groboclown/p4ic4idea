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
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.ServerErrorEvent;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.util.FileTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Stores the registered configurations for a specific project.  The registry must
 * also inform the application server connection registry about the configurations, so that
 * the correct counters can be preserved.
 */
public abstract class ProjectConfigRegistry
        implements ProjectComponent, Disposable {
    public static final Class<ProjectConfigRegistry> COMPONENT_CLASS = ProjectConfigRegistry.class;
    public static final String COMPONENT_NAME = COMPONENT_CLASS.getName();

    private static final Logger LOG = Logger.getInstance(ProjectConfigRegistry.class);

    private final Project project;
    private final MessageBusClient.ProjectClient projectBusClient;
    private final MessageBusClient.ApplicationClient applicationBusClient;
    private boolean disposed = false;

    @Nullable
    public static ProjectConfigRegistry getInstance(@NotNull Project project) {
        return project.getComponent(COMPONENT_CLASS);
    }


    protected ProjectConfigRegistry(@NotNull Project project) {
        this.project = project;
        this.projectBusClient = MessageBusClient.forProject(project, this);
        this.applicationBusClient = MessageBusClient.forApplication(this);
        Disposer.register(project, this);
    }


    /**
     * Find the client that is closest to the {@literal file}.  If multiple clients
     * are in the same tree, then the client that is deepest is returned.
     *
     * @param file the file
     * @return the client that is the best match for the {@literal file}.
     */
    @Nullable
    public ClientConfigRoot getClientFor(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        int closestDepth = Integer.MAX_VALUE;
        ClientConfigRoot closest = null;
        for (ClientConfigRoot clientConfigRoot : getRegisteredStates()) {
            int depth = FileTreeUtil.getPathDepth(file, clientConfigRoot.getClientRootDir());
            if (depth >= 0 && depth < closestDepth) {
                closestDepth = depth;
                closest = clientConfigRoot;
            }
        }

        return closest;
    }


    @Nullable
    public ClientConfigRoot getClientFor(@Nullable FilePath file) {
        if (file == null) {
            return null;
        }
        // updateVcsRoots();
        int closestDepth = Integer.MAX_VALUE;
        ClientConfigRoot closest = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding best client root match for " + file);
        }
        for (ClientConfigRoot clientConfigRoot : getRegisteredStates()) {
            int depth = FileTreeUtil.getPathDepth(file, clientConfigRoot.getClientRootDir());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Root " + clientConfigRoot.getClientRootDir() + ": " + depth);
            }
            if (depth >= 0 && depth < closestDepth) {
                closestDepth = depth;
                closest = clientConfigRoot;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using client root " + closest.getClientRootDir() + " for " + file);
        }

        return closest;
    }

    public boolean isOnline(@Nullable ClientServerRef clientServerRef) {
        if (clientServerRef == null) {
            return false;
        }
        for (ClientConfigRoot configRoot: getRegisteredStates()) {
            if (clientServerRef.equals(configRoot.getClientConfig().getClientServerRef())) {
                return configRoot.isOnline();
            }
        }
        return false;
    }

    @NotNull
    public Collection<ClientConfigRoot> getClientConfigRoots() {
        // updateVcsRoots();
        return new ArrayList<>(getRegisteredStates());
    }


    /**
     * Retrieve the client configuration information about the client server ref.  Even though the
     * connections are registered application-wide, individual projects must register themselves
     *
     * @param ref client reference
     * @return the client config state, or null if it isn't registered.
     */
    @Nullable
    public abstract ClientConfig getRegisteredClientConfigState(@NotNull ClientServerRef ref);

    /**
     * Registers the client configuration to the project and the application.  If a configuration with the same
     * client-server reference is already registered, then it will be removed.  If that configuration is the exact
     * same as the requested added configuration, then it will still be removed then re-added.
     *
     * @param config configuration to register
     * @param vcsRootDir root directory for the configuration.
     */
    public abstract void addClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir);

    /**
     * Removes the client configuration registration with the given reference.  If it is registered, then
     * the appropriate messages will be sent out.
     *
     * @param vcsRootDir the VCS root directory to de-register; if it is not exactly a known root directory, this will
     *                   do nothing.
     * @return true if it was registered, false if not.
     */
    public abstract boolean removeClientConfigAt(@NotNull VirtualFile vcsRootDir);

    @Override
    public void projectOpened() {
        // Initial startup needs to read in the list of roots.
        updateVcsRoots();
    }

    @Override
    public final void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        LoginFailureMessage.addListener(applicationBusClient, this, new LoginFailureMessage.AllErrorListener() {
            @Override
            public void onLoginFailure(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                ProjectConfigRegistry.this.onLoginError(e.getConfig());
            }
        });
        ConnectionErrorMessage.addListener(applicationBusClient, this, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public <E extends Exception> void onHostConnectionError(@NotNull ServerErrorEvent<E> event) {
                ProjectConfigRegistry.this.onHostConnectionError(event.getName());
            }
        });
        ServerConnectedMessage.addListener(applicationBusClient, this, this::onServerConnected);
        ClientConfigRemovedMessage.addListener(projectBusClient, this, event -> {
            if (! ProjectConfigRegistry.this.equals(event.getEventSource())) {
                onClientRemoved(event.getClientConfig(), event.getVcsRootDir());
            }
        });
        UserSelectedOfflineMessage.addListener(projectBusClient, this, this::onUserSelectedOffline);
        //ClientActionMessage.addListener(appBus, cacheId, event -> refresh());
        //ServerActionCacheMessage.addListener(appBus, cacheId, event -> refresh());


        // New clients should only be registered by this component, which means that the "add client"
        // message should ONLY be sent by this class.  Therefore, this class doesn't need to listen
        // for add events, as that could cause recursive problems.
        //ClientConfigAddedMessage.addListener(projectBusClient, (r, c) -> {
        //    // Need to make sure we don't perform a recursive call here.
        //    if (r != null) {
        //        ClientConfigRoot existingClient = getClientFor(r);
        //        if (existingClient != null && !c.getClientServerRef().equals(existingClient.getClientConfig().getClientServerRef())) {
        //            addClientConfig(c, r);
        //        }
        //    }
        //});

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


        // Explicitly avoid the ProjectMessage, because this is an event owned by the IDE.
        projectBusClient.add(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, this::updateVcsRoots);
        projectBusClient.add(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED_IN_PLUGIN, this::updateVcsRoots);
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

    @Override
    public final void disposeComponent() {
        dispose();
    }

    @NotNull
    @Override
    public final String getComponentName() {
        return COMPONENT_NAME;
    }

    @NotNull
    protected final Project getProject() {
        return project;
    }

    /** Throws an error if disposed */
    protected final void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }

    protected final void sendClientRemoved(@Nullable ClientConfigRoot state) {
        if (state != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending notification that root was removed: " + state.getClientRootDir());
            }
            ClientConfigRemovedMessage.reportClientConfigRemoved(getProject(), this,
                    state.getClientConfig(), state.getProjectVcsRootDir());
        }
    }

    protected final void sendClientAdded(@Nullable ClientConfigRoot state) {
        if (state != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending notification that root was added: " + state.getClientRootDir());
            }
            ClientConfigAddedMessage.sendClientConfigurationAdded(getProject(),
                    state.getClientRootDir(),
                    state.getClientConfig());
        }
    }

    @NotNull
    protected abstract Collection<ClientConfigRoot> getRegisteredStates();

    protected abstract void onLoginError(@NotNull OptionalClientServerConfig config);

    protected abstract void onHostConnectionError(@NotNull P4ServerName server);

    protected abstract void onServerConnected(@NotNull ServerConnectedMessage.ServerConnectedEvent event);

    protected abstract void onClientRemoved(@NotNull ClientConfig config, @Nullable VirtualFile vcsRootDir);

    protected abstract void onUserSelectedOffline(@NotNull UserSelectedOfflineMessage.OfflineEvent event);

    protected abstract void onUserSelectedOnline(@NotNull ClientServerRef clientServerRef);

    protected abstract void onUserSelectedAllOnline();

    protected abstract void updateVcsRoots();
}
