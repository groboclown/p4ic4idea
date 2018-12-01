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
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.cache.state.AllClientsState;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CentralCacheManager {
    private static final Logger LOG = Logger.getInstance(CentralCacheManager.class);

    private final AllClientsState allClientState;
    private final MessageBusConnection messageBus;
    private final Map<ClientServerRef, ClientCacheManager> clientManagers = new HashMap<ClientServerRef, ClientCacheManager>();
    private final Lock cacheLock = new ReentrantLock();
    private boolean disposed = false;

    public CentralCacheManager() {
        allClientState = AllClientsState.getInstance();
        messageBus = ApplicationManager.getApplication().getMessageBus().connect();

        Events.registerAppBaseConfigUpdated(messageBus, new BaseConfigUpdatedListener() {
            @Override
            public void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig newConfig,
                    @Nullable P4ProjectConfig previousConfiguration) {
                if (disposed) {
                    return;
                }
                if (previousConfiguration != null) {
                    for (ClientConfig clientConfig : previousConfiguration.getClientConfigs()) {
                        // Yes, we're removing the old cache objects.  Practice shows that
                        // there's a high chance that it's out of date or maybe even
                        // corrupted.
                        removeCache(ClientServerRef.create(clientConfig));
                    }
                }
                // Do not create entries for the new configuration;
                // let those be lazy loaded.
            }
        });

        Events.registerAppConfigInvalid(messageBus, new ConfigInvalidListener() {
            @Override
            public void configurationProblem(@NotNull Project project, @NotNull P4ProjectConfig config,
                    @NotNull VcsConnectionProblem ex) {
                if (disposed) {
                    return;
                }
                for (ClientConfig clientConfig : config.getClientConfigs()) {
                    // Only invalid clients are given to this method, so there's
                    // a very good chance that the client ID will be null.
                    ClientServerRef id = ClientServerRef.create(clientConfig);
                    removeCache(id);
                }
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


    // NOTE this must be done inside a ServerConnection
    public void flushState(@NotNull ClientServerRef clientServerRef,
            boolean includeLocal, boolean force) {
        if (disposed) {
            // Coding error; no bundled message
            throw new IllegalStateException("disposed");
        }
        final ClientLocalServerState state;
        cacheLock.lock();
        try {
            state = allClientState.getCachedStateForClient(clientServerRef);
            if (state == null) {
                LOG.info("No state to clear");
                return;
            }
            state.flush(includeLocal, force);
        } finally {
            cacheLock.unlock();
        }
    }


    /**
     *
     * @param clientConfig server configuration
     * @param isServerCaseInsensitiveCallable if the cached version of the client is not loaded,
     *                                        this will be called to discover whether the
     *                                        Perforce server is case sensitive or not.
     *                                        Errors will be reported to the log, and the local OS
     *                                        case sensitivity will be used instead.
     * @return the cache manager.
     */
    @NotNull
    public ClientCacheManager getClientCacheManager(@NotNull ClientConfig clientConfig,
            @NotNull Callable<Boolean> isServerCaseInsensitiveCallable) throws P4InvalidClientException {
        if (disposed) {
            // Coding error; no bundled message
            throw new IllegalStateException("disposed");
        }
        if (clientConfig.getClientName() == null) {
            throw new P4InvalidClientException(clientConfig.getClientServerRef());
        }
        ClientCacheManager cacheManager;
        cacheLock.lock();
        try {
            cacheManager = clientManagers.get(clientConfig.getClientServerRef());
            if (cacheManager == null) {
                final ClientLocalServerState state = allClientState.getStateForClient(
                        clientConfig.getClientServerRef(),
                        isServerCaseInsensitiveCallable);
                cacheManager = new ClientCacheManager(clientConfig, state);
                clientManagers.put(clientConfig.getClientServerRef(), cacheManager);
            }
        } finally {
            cacheLock.unlock();
        }
        return cacheManager;
    }



    private void removeCache(@NotNull ClientServerRef id) {
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
