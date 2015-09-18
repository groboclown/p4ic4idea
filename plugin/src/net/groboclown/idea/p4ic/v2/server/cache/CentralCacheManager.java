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

package net.groboclown.idea.p4ic.v2.server.cache;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.*;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.cache.state.AllClientsState;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CentralCacheManager {
    private static final Logger LOG = Logger.getInstance(CentralCacheManager.class);

    private final AllClientsState allClientState;
    private final MessageBusConnection messageBus;
    private final Map<ClientServerId, ClientCacheManager> clientManagers = new HashMap<ClientServerId, ClientCacheManager>();
    private final Lock cacheLock = new ReentrantLock();
    private boolean disposed = false;

    public CentralCacheManager() {
        allClientState = AllClientsState.getInstance();
        messageBus = ApplicationManager.getApplication().getMessageBus().connect();

        Events.appBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull final Project project,
                    @NotNull final List<ProjectConfigSource> sources) {

            }
        });

        // FIXME use new handlers.
        messageBus.subscribe(P4ClientsReloadedListener.TOPIC, new P4ClientsReloadedListener() {
            @Override
            public void clientsLoaded(@NotNull final Project project, @NotNull final List<Client> clients) {
                if (disposed) {
                    return;
                }
                cacheLock.lock();
                try {
                    clientManagers.clear();
                    // Don't create the new ones until we need it
                    // Also, don't remove existing cache objects.
                } finally {
                    cacheLock.unlock();
                }
            }
        });

        // FIXME old stuff
        messageBus.subscribe(P4ConfigListener.TOPIC, new P4ConfigListener() {
            @Override
            public void configChanges(@NotNull final Project project, @NotNull final P4Config original,
                    @NotNull final P4Config config) {
                if (disposed) {
                    return;
                }
                ClientServerId originalId = ClientServerId.create(project, original);
                ClientServerId newId = ClientServerId.create(project, config);
                if (originalId != null && ! originalId.equals(newId)) {
                    removeCache(originalId);
                }
                // Don't create the new one until we need it
            }

            @Override
            public void configurationProblem(@NotNull final Project project, @NotNull final P4Config config,
                    @NotNull final P4InvalidConfigException ex) {
                if (disposed) {
                    return;
                }
                ClientServerId id = ClientServerId.create(project, config);
                removeCache(id);
            }
        });
    }


    public void dispose() {
        messageBus.disconnect();
        disposed = true;
    }


    public boolean isDisposed() {
        return disposed;
    }


    /**
     *
     * @param clientServerId client / server name
     * @param config server configuration
     * @param isServerCaseInsensitiveCallable if the cached version of the client is not loaded,
     *                                        this will be called to discover whether the
     *                                        Perforce server is case sensitive or not.
     *                                        Errors will be reported to the log, and the local OS
     *                                        case sensitivity will be used instead.
     * @return the cache manager.
     */
    @NotNull
    public ClientCacheManager getClientCacheManager(@NotNull ClientServerId clientServerId, @NotNull ServerConfig config,
            @NotNull  Callable<Boolean> isServerCaseInsensitiveCallable) {
        if (disposed) {
            // Coding error; no bundled message
            throw new IllegalStateException("disposed");
        }
        ClientCacheManager cacheManager;
        cacheLock.lock();
        try {
            cacheManager = clientManagers.get(clientServerId);
            if (cacheManager == null) {
                final ClientLocalServerState state = allClientState.getStateForClient(clientServerId,
                        isServerCaseInsensitiveCallable);
                cacheManager = new ClientCacheManager(config, state);
                clientManagers.put(clientServerId, cacheManager);
            }
        } finally {
            cacheLock.unlock();
        }
        return cacheManager;
    }



    private void removeCache(ClientServerId id) {
        if (disposed) {
            // Coding error; no bundled message
            throw new IllegalStateException("disposed");
        }
        cacheLock.lock();
        try {
            clientManagers.remove(id);
            allClientState.removeClientState(id);
        } finally {
            cacheLock.unlock();
        }
        LOG.info("Removed cached state for " + id);
    }
}
