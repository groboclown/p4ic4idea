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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.LockTimeoutProvider;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.config.LockTimeoutProviderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Central store for all the cached state information.  Does not store the VcsRootCacheStore, because that's
 * handled separately.
 */
public class ProjectCacheStore {
    private static final Logger LOG = Logger.getInstance(ProjectCacheStore.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<P4ServerName, ServerQueryCacheStore> serverQueryCache = new HashMap<>();
    private final Map<ClientServerRef, ClientQueryCacheStore> clientQueryCache = new HashMap<>();
    private final List<ActionStore.PendingAction> pendingActions = new ArrayList<>();
    private final IdeChangelistCacheStore changelistCacheStore = new IdeChangelistCacheStore();

    private LockTimeoutProvider lockTimeout = new LockTimeoutProviderImpl();


    @SuppressWarnings("WeakerAccess")
    public static class State {
        // All the stuff cached from the server
        public List<ServerQueryCacheStore.State> serverState;
        public List<ClientQueryCacheStore.State> clientState;

        // All the pending actions
        public List<ActionStore.State> pendingActions;

        public IdeChangelistCacheStore.State changelistState;

    }

    @TestOnly
    public void addCache(ServerQueryCacheStore store) {
        serverQueryCache.put(store.getServerName(), store);
    }

    @TestOnly
    public void addCache(ClientQueryCacheStore store) {
        clientQueryCache.put(store.getClientServerRef(), store);
    }

    @TestOnly
    public void addPendingAction(ActionStore.PendingAction action) {
        pendingActions.add(action);
    }


    @Nullable
    public State getState()
            throws InterruptedException {
        State ret = new State();

        ret.serverState = new ArrayList<>(serverQueryCache.size());
        for (ServerQueryCacheStore entry : serverQueryCache.values()) {
            ret.serverState.add(entry.getState());
        }

        ret.clientState = new ArrayList<>(clientQueryCache.size());
        for (ClientQueryCacheStore value : clientQueryCache.values()) {
            ret.clientState.add(value.getState());
        }

        ret.pendingActions = new ArrayList<>(pendingActions.size());
        for (ActionStore.PendingAction pendingAction : pendingActions) {
            ret.pendingActions.add(pendingAction.getState());
        }

        ret.changelistState = changelistCacheStore.getState();

        return ret;
    }

    public void setState(@Nullable State state)
            throws InterruptedException {
        lockTimeout.withWriteLock(lock, () -> {
            serverQueryCache.clear();
            clientQueryCache.clear();
            pendingActions.clear();
            if (state == null) {
                changelistCacheStore.setState(null);
            } else {
                for (ServerQueryCacheStore.State serverState : state.serverState) {
                    ServerQueryCacheStore store = new ServerQueryCacheStore(serverState);
                    serverQueryCache.put(store.getServerName(), store);
                }
                for (ClientQueryCacheStore.State clientState : state.clientState) {
                    ClientQueryCacheStore store = new ClientQueryCacheStore(clientState);
                    clientQueryCache.put(store.getClientServerRef(), store);
                }
                for (ActionStore.State actionState : state.pendingActions) {
                    // TODO look into this weird bug; stuff was coming back null.  Is it still happening?
                    if (actionState.clientActionCmd != null || actionState.serverActionCmd != null) {
                        ActionStore.PendingAction action;
                        try {
                            action = ActionStore.read(actionState);
                            pendingActions.add(action);
                        } catch (PrimitiveMap.UnmarshalException e) {
                            LOG.warn("Problem reading state for " +
                                    (actionState.clientActionCmd == null
                                            ? actionState.serverActionCmd : actionState.clientActionCmd), e);
                        }
                    } else {
                        LOG.warn("Invalid action state " + actionState.actionId + ": " + actionState.data);
                    }
                }
                changelistCacheStore.setState(state.changelistState);
            }
        });
    }

    public IdeChangelistCacheStore getChangelistCacheStore() {
        return changelistCacheStore;
    }

    /**
     * query cache can change during a read, so the function runs inside the lock.
     *
     * @param config source
     * @param defaultValue value if the source is not registered to have a cache
     * @param fun mapping function
     * @param <T> return type
     * @return return value from the mapping function, or the default value
     * @throws InterruptedException thrown if the lock is not acquired in time.
     */
    @Nullable
    public <T> T read(ServerConfig config, T defaultValue, Function<ServerQueryCacheStore, T> fun)
            throws InterruptedException {
        return read(config.getServerName(), defaultValue, fun);
    }

    /**
     * query cache can change during a read, so the function runs inside the lock.
     *
     * @param config source
     * @param defaultValue value if the source is not registered to have a cache
     * @param fun mapping function
     * @param <T> return type
     * @return return value from the mapping function, or the default value
     * @throws InterruptedException thrown if the lock is not acquired in time.
     */
    @Nullable
    public <T> T read(P4ServerName config, T defaultValue, Function<ServerQueryCacheStore, T> fun)
            throws InterruptedException {
        return lockTimeout.withReadLock(lock, () -> {
            ServerQueryCacheStore store = serverQueryCache.get(config);
            if (store != null) {
                return fun.apply(store);
            }
            return defaultValue;
        });
    }


    @Nullable
    public <T> T read(ClientConfig config, T defaultValue, Function<ClientQueryCacheStore, T> fun)
            throws InterruptedException {
        return lockTimeout.withReadLock(lock, () -> {
            ClientQueryCacheStore store = clientQueryCache.get(config.getClientServerRef());
            if (store != null) {
                return fun.apply(store);
            }
            return defaultValue;
        });
    }


    public void read(ClientConfig config, Consumer<ClientQueryCacheStore> fun)
            throws InterruptedException {
        lockTimeout.withReadLock(lock, () -> {
            ClientQueryCacheStore store = clientQueryCache.get(config.getClientServerRef());
            if (store != null) {
                fun.accept(store);
            }
        });
    }

    public void write(ClientConfig config, Consumer<ClientQueryCacheStore> fun)
            throws InterruptedException {
        lockTimeout.withWriteLock(lock, () -> {
            ClientQueryCacheStore store = clientQueryCache.get(config.getClientServerRef());
            if (store == null) {
                store = new ClientQueryCacheStore(config.getClientServerRef());
                clientQueryCache.put(config.getClientServerRef(), store);
            }
            fun.accept(store);
        });
    }

    public void write(P4ServerName config, Consumer<ServerQueryCacheStore> fun)
            throws InterruptedException {
        lockTimeout.withWriteLock(lock, () -> {
            ServerQueryCacheStore store = serverQueryCache.get(config);
            if (store == null) {
                store = new ServerQueryCacheStore(config);
                serverQueryCache.put(config, store);
            }
            fun.accept(store);
        });
    }

    @NotNull
    public List<ActionStore.PendingAction> copyActions()
            throws InterruptedException {
        return lockTimeout.withReadLock(lock, () -> new ArrayList<>(pendingActions));
    }

    public void writeActions(Consumer<List<ActionStore.PendingAction>> fun)
            throws InterruptedException {
        lockTimeout.withWriteLock(lock, () -> {
            fun.accept(pendingActions);
        });
    }
}
