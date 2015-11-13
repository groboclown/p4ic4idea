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
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.events.ServerConnectionStateListener;
import net.groboclown.idea.p4ic.v2.server.cache.CentralCacheManager;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.connection.Synchronizer.ServerSynchronizer;
import net.groboclown.idea.p4ic.v2.ui.alerts.DisconnectedHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    public ServerConnectionManager() {
        this(new CentralCacheManager(), AlertManager.getInstance());
    }

    ServerConnectionManager(@NotNull CentralCacheManager cacheManager,
            @NotNull AlertManager alerts) {
        this.cacheManager = cacheManager;
        this.alerts = alerts;
        this.messageBus = ApplicationManager.getApplication().getMessageBus().connect();
    }

    @Override
    public void initComponent() {
        Events.appBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull final Project project,
                    @NotNull final List<ProjectConfigSource> sources) {
                // FIXME update ONLY for the project.
                invalidateAllConfigs();
            }
        });
        Events.appConfigInvalid(messageBus, new ConfigInvalidListener() {
            @Override
            public void configurationProblem(@NotNull final Project project, @NotNull final P4Config config,
                    @NotNull final VcsConnectionProblem ex) {
                // because this is selective on a config, we can safely ignore the project.
                invalidateConfig(config);
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
        return "Server Connection Manager";
    }


    /**
     *
     *
     * @param clientServerId client/server ID
     * @param config configuration for the server.
     * @return connection
     */
    @NotNull
    public ServerConnection getConnectionFor(@NotNull ClientServerId clientServerId, @NotNull ServerConfig config)
            throws P4InvalidClientException {
        serverCacheLock.lock();
        try {
            ServerConfigStatus status = serverCache.get(config);
            if (status == null) {
                status = new ServerConfigStatus(config, alerts.createServerSynchronizer());
                serverCache.put(config, status);
            }
            return status.getConnectionFor(clientServerId, alerts, cacheManager);
        } finally {
            serverCacheLock.unlock();
        }
    }


    void invalidateConfig(@NotNull P4Config client) {
        serverCacheLock.lock();
        try {
            List<ServerConfig> removedConfigs = new ArrayList<ServerConfig>();
            for (Entry<ServerConfig, ServerConfigStatus> entry: serverCache.entrySet()) {
                final ServerConfig config = entry.getKey();
                if (config.isSameConnectionAs(client)) {
                    if (entry.getValue().removeClient(client.getClientname())) {
                        removedConfigs.add(config);
                    }
                }
            }
            for (ServerConfig removedConfig: removedConfigs) {
                serverCache.get(removedConfig).dispose();
                serverCache.remove(removedConfig);
            }
        } finally {
            serverCacheLock.unlock();
        }
    }


    void invalidateAllConfigs() {
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


    void setConnectionState(@NotNull ServerConfig config, boolean isOnline) {
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
    }


    static class ServerConfigStatus implements ServerStatusController {
        // List, not a set, so that we can have multiple registrations of the same client name;
        // especially useful for multiple projects with the same client.
        final Map<String, ServerConnection> clientNames = new HashMap<String, ServerConnection>();
        final ServerConfig config;
        final Synchronizer.ServerSynchronizer synchronizer;
        boolean valid = true;
        boolean disposed = false;

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
        public boolean isAutoOffline() {
            return config.isAutoOffline();
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

        public boolean removeClient(@Nullable final String clientName) {
            clientNames.remove(clientName);
            return clientNames.isEmpty();
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
            setOnline(null);
        }

        @Override
        public void onDisconnected() {
            setOffline();
        }

        @Override
        public void onConfigInvalid() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Marking config invalid: " + config, new Throwable("stack capture"));
            }
            valid = false;
        }

        void dispose() {
            disposed = true;
            for (ServerConnection connection: clientNames.values()) {
                connection.dispose();
            }
        }

        synchronized ServerConnection getConnectionFor(@NotNull final ClientServerId clientServer,
                @NotNull AlertManager alerts, @NotNull CentralCacheManager cacheManager)
                throws P4InvalidClientException {
            ServerConnection conn = clientNames.get(clientServer.getClientId());
            if (conn == null) {
                conn = new ServerConnection(alerts, clientServer,
                        cacheManager.getClientCacheManager(clientServer, config, new CaseInsensitiveCheck(config)),
                        config, this, synchronizer.createConnectionSynchronizer());
                clientNames.put(clientServer.getClientId(), conn);
            }
            return conn;
        }

        synchronized boolean setOnline(@Nullable final Project project) {
            if (disposed) {
                LOG.error("Tried to go online for a disposed connection");
            } else if (valid) {
                onlineStatusLock.lock();
                try {
                    if (!online) {
                        LOG.info("** went online: " + config);
                        online = true;
                        synchronizer.wentOnline();
                        onlineChangedCondition.signal();
                        return true;
                    } else if (LOG.isDebugEnabled()) {
                        LOG.debug("Already online; skipping going online: " + config);
                    }
                } finally {
                    onlineStatusLock.unlock();
                }
                // fall through
            } else if (project != null) {
                P4DisconnectedException ex = new P4DisconnectedException(
                        P4Bundle.message("disconnected.server-invalid", config.getServiceName())
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
        private final ServerConfig config;

        private CaseInsensitiveCheck(final ServerConfig config) {
            this.config = config;
        }

        @Override
        public Boolean call() throws Exception {
            return ! ClientExec.getServerInfo(config).isCaseSensitive();
        }
    }

}
