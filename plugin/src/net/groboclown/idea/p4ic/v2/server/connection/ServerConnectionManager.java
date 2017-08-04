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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.events.ServerConnectionStateListener;
import net.groboclown.idea.p4ic.v2.server.cache.CentralCacheManager;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.state.JobStateList;
import net.groboclown.idea.p4ic.v2.server.cache.state.JobStatusListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4WorkspaceViewState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.UserSummaryStateList;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.connection.Synchronizer.ServerSynchronizer;
import net.groboclown.idea.p4ic.v2.ui.alerts.DisconnectedHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerConnectionManager implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(ServerConnectionManager.class);

    private final MessageBusConnection messageBus;
    private final CentralCacheManager cacheManager;
    private final AlertManager alerts;
    private final Lock serverCacheLock = new ReentrantLock();
    private final Map<ServerConfig, ServerConfigStatus> serverCache = new HashMap<ServerConfig, ServerConfigStatus>();

    public static ServerConnectionManager getInstance() {
        return ApplicationManager.getApplication().getComponent(ServerConnectionManager.class);
    }

    // Used by PicoContainer
    @SuppressWarnings("unused")
    public ServerConnectionManager() {
        this(new CentralCacheManager(), AlertManager.getInstance());
    }

    private ServerConnectionManager(@NotNull CentralCacheManager cacheManager,
            @NotNull AlertManager alerts) {
        this.cacheManager = cacheManager;
        this.alerts = alerts;
        this.messageBus = ApplicationManager.getApplication().getMessageBus().connect();
    }

    @Override
    public void initComponent() {
        Events.registerServerConnectionAppBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig newConfig,
                    @Nullable P4ProjectConfig previousConfiguration) {
                if (previousConfiguration != null) {
                    Set<ServerConfig> removedConfigs =
                            new HashSet<ServerConfig>(previousConfiguration.getServerConfigs());
                    removedConfigs.removeAll(newConfig.getServerConfigs());
                    invalidateServerConfigs(removedConfigs);
                }
            }
        });
        Events.registerServerConnectionAppConfigInvalid(messageBus, new ConfigInvalidListener() {
            @Override
            public void configurationProblem(@NotNull Project project, @NotNull P4ProjectConfig config,
                    @NotNull VcsConnectionProblem ex) {
                invalidateServerConfigs(config.getServerConfigs());
            }
        });
        Events.appServerConnectionState(messageBus, new ServerConnectionStateListener() {
            @Override
            public void connected(@NotNull final ServerConfig config) {
                setConnectionState(config, true);
            }

            @Override
            public void disconnected(@NotNull final ServerConfig config) {
                setConnectionState(config, false);
            }
        });
    }

    @Override
    public void disposeComponent() {
        messageBus.dispose();
        serverCacheLock.lock();
        try {
            for (ServerConfigStatus status: serverCache.values()) {
                status.dispose();
            }
            serverCache.clear();
        } finally {
            serverCacheLock.unlock();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4ServerName Connection Manager";
    }


    /**
     *
     *
     * @param clientConfig configuration for the client and server connection.
     * @return connection
     */
    @NotNull
    public ServerConnection getConnectionFor(@NotNull Project project,
            @NotNull ClientConfig clientConfig, boolean requiresClient)
            throws P4InvalidClientException {
        serverCacheLock.lock();
        try {
            ServerConfigStatus status = serverCache.get(clientConfig.getServerConfig());
            if (status == null) {
                status = new ServerConfigStatus(clientConfig.getServerConfig(), alerts.createServerSynchronizer());
                serverCache.put(clientConfig.getServerConfig(), status);
            }
            return status.getConnectionFor(project, clientConfig, alerts, cacheManager,
                    requiresClient);
        } finally {
            serverCacheLock.unlock();
        }
    }


    void flushCache(@NotNull ClientServerRef clientServerRef, boolean includeLocal, boolean force) {
        cacheManager.flushState(clientServerRef, includeLocal, force);
    }


    private void invalidateServerConfigs(@NotNull Collection<ServerConfig> serverConfigs) {
        if (serverConfigs.isEmpty()) {
            return;
        }
        serverCacheLock.lock();
        try {
            for (ServerConfig serverConfig : serverConfigs) {
                ServerConfigStatus status = serverCache.get(serverConfig);
                if (status != null) {
                    status.dispose();
                    serverCache.remove(serverConfig);
                }
            }
        } finally {
            serverCacheLock.unlock();
        }
    }


    private void setConnectionState(@NotNull ServerConfig config, boolean isOnline) {
        /* Bug #128: This was putting us in a deadlock.
        serverCacheLock.lock();
        try {
            final ServerConfigStatus status = serverCache.get(config);
            if (status != null) {
                // directly set the status; don't go through the events or
                // other connection stuff.
                if (isOnline) {
                    status.onConnected();
                } else {
                    status.onDisconnected();
                }
            }
        } finally {
            serverCacheLock.unlock();
        }
        */
        final ServerConfigStatus status;
        serverCacheLock.lock();
        try {
            status = serverCache.get(config);
        } finally {
            serverCacheLock.unlock();
        }
        if (status != null) {
            // directly set the status; don't go through the events or
            // other connection stuff.
            if (isOnline) {
                status.onConnected();
            } else {
                status.onDisconnected();
            }
        }
    }


    private static class ServerConfigStatus implements ServerStatusController {
        // List, not a set, so that we can have multiple registrations of the same client name;
        // especially useful for multiple projects with the same client.
        final Map<String, ServerConnection> clientNames = new HashMap<String, ServerConnection>();
        final ServerConfig config;
        final Synchronizer.ServerSynchronizer synchronizer;
        volatile boolean valid = true;
        volatile boolean disposed = false;

        // assume we're online at the start.
        volatile boolean online = true;

        private final Lock onlineStatusLock = new ReentrantLock();
        private final Condition onlineChangedCondition = onlineStatusLock.newCondition();


        ServerConfigStatus(@NotNull ServerConfig config, @NotNull ServerSynchronizer synchronizer) {
            this.config = config;
            this.synchronizer = synchronizer;

            // we are online by default
            synchronizer.wentOnline();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public boolean isValid() {
            return valid && ! disposed;
        }

        @Override
        public void disconnect() {
            if (setOffline()) {
                Events.serverDisconnected(config);
            }
        }

        @Override
        public void connect(@NotNull Project project) {
            if (setOnline(project)) {
                Events.serverConnected(config);
            }
        }

        @NotNull
        @Override
        public String getServerDescription() {
            return config.getServerName().getDisplayName();
        }

        @Override
        public boolean isWorkingOffline() {
            return disposed || ! valid || ! online;
        }

        @Override
        public boolean isWorkingOnline() {
            return ! disposed && valid && online;
        }

        @Override
        public void onConnected() {
            if (!disposed) {
                setOnline(null);
            }
        }

        @Override
        public void onDisconnected() {
            setOffline();
        }

        @Override
        public void onConfigInvalid() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Marking exec invalid: " + config, new Throwable("stack capture"));
            }
            valid = false;
        }

        void dispose() {
            disposed = true;
            for (ServerConnection connection: clientNames.values()) {
                connection.dispose();
            }
        }

        synchronized ServerConnection getConnectionFor(
                @NotNull final Project project,
                @NotNull final ClientConfig clientConfig,
                @NotNull AlertManager alerts, @NotNull CentralCacheManager cacheManager,
                boolean requiresClient)
                throws P4InvalidClientException {
            if (! clientConfig.getServerConfig().equals(config)) {
                throw new IllegalArgumentException("did not pass same server config");
            }
            ServerConnection conn = clientNames.get(clientConfig.getClientName());
            if (conn == null) {
                final ClientExec exec;
                try {
                    LOG.debug("Constructing new connection for " + clientConfig.getClientServerRef().getServerDisplayId());
                    exec = ClientExec.createFor(clientConfig, this);
                } catch (P4InvalidConfigException e) {
                    LOG.warn(e);
                    throw new P4InvalidClientException(clientConfig.getClientServerRef());
                }

                // Must share one ConnectionSynchronizer per ClientExec.
                ServerSynchronizer.ConnectionSynchronizer connectionSync =
                        synchronizer.createConnectionSynchronizer();

                if (! requiresClient && ! clientConfig.isWorkspaceCapable()) {
                    conn = new ServerConnection(alerts,
                            new ClientCacheManager(clientConfig,
                                    createEmptyClientLocalState(clientConfig.getClientServerRef())),
                            clientConfig, this, connectionSync, exec);
                    // Do not add the connection to the client names
                    // store, because we don't have a client.
                } else {
                    conn = new ServerConnection(alerts,
                            cacheManager.getClientCacheManager(
                                    clientConfig, new CaseInsensitiveCheck(project, exec, connectionSync)),
                            clientConfig, this, connectionSync, exec);
                    clientNames.put(clientConfig.getClientName(), conn);
                }
            }
            return conn;
        }

        synchronized boolean setOnline(@Nullable final Project project) {
            if (disposed) {
                // Does not need to be an error (#134)
                LOG.info("Tried to go online for a disposed connection");
            } else if (valid) {
                onlineStatusLock.lock();
                try {
                    if (!online) {
                        LOG.info("** went online: " + config);
                        online = true;
                        synchronizer.wentOnline();
                        onlineChangedCondition.signal();
                    } else if (LOG.isDebugEnabled()) {
                        LOG.debug("Already online; skipping going online: " + config);
                    }
                    return true;
                } finally {
                    onlineStatusLock.unlock();
                }
            } else if (project != null) {
                P4DisconnectedException ex = new P4DisconnectedException(
                        // Note: using the nice server name, rather than the whole ugly string (#116)
                        P4Bundle.message("disconnected.server-invalid",
                                config.getServerName().getDisplayName())
                );
                AlertManager.getInstance().addCriticalError(
                        new DisconnectedHandler(project, this, ex), ex);
            } else {
                LOG.warn("Could not go online; connection invalid for " + config);
            }
            return false;
        }

        synchronized boolean setOffline() {
            onlineStatusLock.lock();
            try {
                if (online) {
                    LOG.info("** went offline: " + config);
                    online = false;
                    synchronizer.wentOffline();
                    onlineChangedCondition.signal();
                    return valid;
                }
            } finally {
                onlineStatusLock.unlock();
            }
            return false;
        }
    }


    private static class CaseInsensitiveCheck implements Callable<Boolean> {
        private final Project project;
        private final ClientExec exec;
        private final ServerSynchronizer.ConnectionSynchronizer connectionSync;
        private P4Exec2 p4exec;

        private CaseInsensitiveCheck(@NotNull final Project project, @NotNull final ClientExec exec,
                @NotNull final ServerSynchronizer.ConnectionSynchronizer connectionSync) {
            this.project = project;
            this.exec = exec;
            this.connectionSync = connectionSync;
        }

        @Override
        public Boolean call() throws Exception {
            // This was causing AuthenticatedServer to sometimes have a
            // check-out called from two different threads.  It needs a
            // proper synchronization to prevent that from happening.

            if (p4exec == null) {
                p4exec = new P4Exec2(project, exec);
            }
            final VcsException[] ex = new VcsException[] { null };
            Boolean ret = connectionSync.runImmediateAction(new Synchronizer.ActionRunner<Boolean>() {
                @Override
                public Boolean perform(@NotNull SynchronizedActionRunner runner)
                        throws InterruptedException {
                    try {
                        return ! p4exec.getServerInfo().isCaseSensitive();
                    } catch (VcsException e) {
                        ex[0] = e;
                        return null;
                    }
                }
            });
            if (ex[0] != null) {
                throw ex[0];
            }
            return ret;
        }
    }


    private static ClientLocalServerState createEmptyClientLocalState(
            @NotNull final ClientServerRef clientServer) {
        // don't care what the case sensitivity is for this particular imitation
        // state.
        return new ClientLocalServerState(
                new P4ClientState(false, clientServer,
                        new P4WorkspaceViewState("workspace"),
                        new JobStatusListState(),
                        new JobStateList(),
                        new UserSummaryStateList()),
                new P4ClientState(false, clientServer,
                        new P4WorkspaceViewState("workspace"),
                        new JobStatusListState(),
                        new JobStateList(),
                        new UserSummaryStateList()),
                new ArrayList<PendingUpdateState>());
    }
}
