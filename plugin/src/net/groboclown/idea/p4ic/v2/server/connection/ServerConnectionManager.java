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
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigListener;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.CentralCacheManager;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerConnectionManager implements ApplicationComponent {
    private final MessageBusConnection messageBus;
    private final CentralCacheManager cacheManager;
    private final AlertManager alerts;
    private final Lock serverCacheLock = new ReentrantLock();
    private final Map<ServerConfig, ServerConfigStatus> serverCache = new HashMap<ServerConfig, ServerConfigStatus>();

    public static ServerConnectionManager getInstance() {
        // FIXME
        throw new IllegalStateException("not registered in plugin.xml");

        // return ApplicationManager.getApplication().getComponent(ServerConnectionManager.class);
    }

    public ServerConnectionManager() {
        this(new CentralCacheManager(), AlertManager.getInstance());
    }

    ServerConnectionManager(@NotNull CentralCacheManager cacheManager,
            @NotNull AlertManager alerts) {
        this.cacheManager = cacheManager;
        this.alerts = alerts;
        this.messageBus = ApplicationManager.getApplication().getMessageBus().connect();

        messageBus.subscribe(P4ConfigListener.TOPIC, new P4ConfigListener() {
            @Override
            public void configChanges(@NotNull final Project project, @NotNull final P4Config original,
                    @NotNull final P4Config config) {
                invalidateConfig(original);
            }

            @Override
            public void configurationProblem(@NotNull final Project project, @NotNull final P4Config config,
                    @NotNull final P4InvalidConfigException ex) {
                invalidateConfig(config);
            }
        });
    }

    @Override
    public void initComponent() {

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
    public ServerConnection getConnectionFor(@NotNull ClientServerId clientServerId, @NotNull ServerConfig config) {
        serverCacheLock.lock();
        try {
            ServerConfigStatus status = serverCache.get(config);
            if (status == null) {
                status = new ServerConfigStatus(config);
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


    static class ServerConfigStatus implements ServerStatusController {
        // List, not a set, so that we can have multiple registrations of the same client name;
        // especially useful for multiple projects with the same client.
        final Map<String, ServerConnection> clientNames = new HashMap<String, ServerConnection>();
        final ServerConfig config;
        boolean valid = true;
        volatile boolean online;

        ServerConfigStatus(@NotNull final ServerConfig config) {
            this.config = config;
        }

        @Override
        public boolean isAutoOffline() {
            return config.isAutoOffline();
        }

        @Override
        public void disconnect() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void connect() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void waitForOnline(final long timeout, final TimeUnit unit) throws InterruptedException {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        public boolean removeClient(@Nullable final String clientName) {
            clientNames.remove(clientName);
            return clientNames.isEmpty();
        }

        @Override
        public boolean isWorkingOffline() {
            return ! valid || ! online;
        }

        @Override
        public boolean isWorkingOnline() {
            return valid && online;
        }

        @Override
        public void onConnected() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void onDisconnected() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void onConfigInvalid() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        void dispose() {
            valid = false;
            for (ServerConnection connection: clientNames.values()) {
                connection.dispose();
            }
        }

        ServerConnection getConnectionFor(@NotNull final ClientServerId clientServer,
                @NotNull AlertManager alerts, @NotNull CentralCacheManager cacheManager) {
            ServerConnection conn = clientNames.get(clientServer.getClientId());
            if (conn == null) {
                conn = new ServerConnection(alerts, clientServer, cacheManager.getClientCacheManager(clientServer, config),
                        config, this);
                clientNames.put(clientServer.getClientId(), conn);
            }
            return conn;
        }
    }
}
