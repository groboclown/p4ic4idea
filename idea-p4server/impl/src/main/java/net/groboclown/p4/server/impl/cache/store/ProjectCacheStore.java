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

import com.intellij.openapi.util.Pair;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.LockTimeoutProvider;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.cache.PendingAction;
import net.groboclown.p4.server.impl.cache.PendingActionFactory;
import net.groboclown.p4.server.impl.config.LockTimeoutProviderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Central store for all the cached state information.  Does not store the VcsRootCacheStore, because that's
 * handled separately.
 */
public class ProjectCacheStore {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<P4ServerName, ServerQueryCacheStore> serverQueryCache = new HashMap<>();
    private final Map<ClientServerRef, ClientQueryCacheStore> clientQueryCache = new HashMap<>();
    private final List<PendingAction> pendingActions = new ArrayList<>();
    private final IdeChangelistCacheStore changelistCacheStore = new IdeChangelistCacheStore();

    private LockTimeoutProvider lockTimeout = new LockTimeoutProviderImpl();


    @SuppressWarnings("WeakerAccess")
    public static class State {
        // All the stuff cached from the server
        public List<ServerQueryCacheStore.State> serverState;
        public List<ClientQueryCacheStore.State> clientState;

        // All the pending actions
        public List<PendingAction.State> pendingActions;

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
    public void addPendingAction(PendingAction action) {
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
        for (PendingAction pendingAction : pendingActions) {
            ret.pendingActions.add(pendingAction.getState());
        }

        ret.changelistState = changelistCacheStore.getState();

        return ret;
    }

    public void setState(@Nullable State state)
            throws InterruptedException {
        lockTimeout.withLock(lock.writeLock(), () -> {
            serverQueryCache.clear();
            clientQueryCache.clear();
            pendingActions.clear();
            if (state == null) {
                changelistCacheStore.setState(null);
            } else {
                List<ServerConfig> knownServerConfigs = new ArrayList<>();
                for (ServerQueryCacheStore.State serverState : state.serverState) {
                    ServerQueryCacheStore store = new ServerQueryCacheStore(serverState);
                    serverQueryCache.put(store.getServerName(), store);
                }
                for (ClientQueryCacheStore.State clientState : state.clientState) {
                    ClientQueryCacheStore store = new ClientQueryCacheStore(clientState);
                    clientQueryCache.put(store.getClientServerRef(), store);
                }
                for (PendingAction.State actionState : state.pendingActions) {
                    PendingAction action = PendingActionFactory.read(actionState);
                    pendingActions.add(action);
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
        return lockTimeout.withLock(lock.readLock(), () -> {
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
        return lockTimeout.withLock(lock.readLock(), () -> {
            ClientQueryCacheStore store = clientQueryCache.get(config.getClientServerRef());
            if (store != null) {
                return fun.apply(store);
            }
            return defaultValue;
        });
    }


    public void read(ClientConfig config, Consumer<ClientQueryCacheStore> fun)
            throws InterruptedException {
        lockTimeout.withLock(lock.readLock(), () -> {
            ClientQueryCacheStore store = clientQueryCache.get(config.getClientServerRef());
            if (store != null) {
                fun.accept(store);
            }
        });
    }

    @Nullable
    public <T> T readClientActions(ServerConfig config, Function<List<P4CommandRunner.ServerAction<?>>, T> fun)
            throws InterruptedException {
        final String sourceId = PendingActionFactory.getSourceId(config);
        final List<P4CommandRunner.ServerAction<?>> actions = new ArrayList<>();
        lockTimeout.withLock(lock.readLock(), () -> {
            for (PendingAction pendingAction : pendingActions) {
                if (pendingAction.isServerAction() && sourceId.equals(pendingAction.getSourceId())) {
                    actions.add(pendingAction.getServerAction(config));
                }
            }
        });
        return fun.apply(actions);
    }


    @NotNull
    public List<P4CommandRunner.ClientAction<?>> readClientActions(ClientConfig config)
            throws InterruptedException {
        final String sourceId = PendingActionFactory.getSourceId(config);
        final List<P4CommandRunner.ClientAction<?>> actions = new ArrayList<>();
        lockTimeout.withLock(lock.readLock(), () -> {
            for (PendingAction pendingAction : pendingActions) {
                if (pendingAction.isClientAction() && sourceId.equals(pendingAction.getSourceId())) {
                    actions.add(pendingAction.getClientAction(config));
                }
            }
        });
        return actions;
    }


    @NotNull
    public List<Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>>> readActions(ClientConfig config)
            throws InterruptedException {
        final String clientSourceId = PendingActionFactory.getSourceId(config);
        final String serverSourceId = PendingActionFactory.getSourceId(config.getServerConfig());
        final List<Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>>> actions = new ArrayList<>();
        lockTimeout.withLock(lock.readLock(), () -> {
            for (PendingAction pendingAction : pendingActions) {
                if (pendingAction.isClientAction() && clientSourceId.equals(pendingAction.getSourceId())) {
                    actions.add(new Pair<>(pendingAction.getClientAction(config), null));
                } else if (pendingAction.isServerAction() && serverSourceId.equals(pendingAction.getSourceId())) {
                    actions.add(new Pair<>(null, pendingAction.getServerAction(config.getServerConfig())));
                }
            }
        });
        return actions;
    }

    public void writeActions(ClientServerRef config, Consumer<WriteActionCache> fun)
            throws InterruptedException {
        WriteActionCache arg = new WriteActionCache(config);
        lockTimeout.withLock(lock.writeLock(), () -> fun.accept(arg));
    }


    public class WriteActionCache
            implements Iterable<Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>>> {
        private final ClientServerRef ref;
        private final String clientSourceId;
        private final String serverSourceId;

        private WriteActionCache(ClientServerRef config) {
            this.ref = config;
            this.clientSourceId = PendingActionFactory.getSourceId(config);
            this.serverSourceId = PendingActionFactory.getSourceId(config.getServerName());
        }

        public List<P4CommandRunner.ClientAction<?>> getClientActions(ClientConfig config) {
            if (!ref.equals(config.getClientServerRef())) {
                return Collections.emptyList();
            }
            List<P4CommandRunner.ClientAction<?>> actions = new ArrayList<>();
            for (PendingAction pendingAction : pendingActions) {
                if (pendingAction.isClientAction() && clientSourceId.equals(pendingAction.getSourceId())) {
                    actions.add(pendingAction.getClientAction(config));
                }
            }
            return actions;
        }

        public Optional<P4CommandRunner.ClientAction<?>> getClientActionById(ClientConfig config, String actionId) {
            if (!ref.equals(config.getClientServerRef())) {
                return Optional.empty();
            }
            for (PendingAction pendingAction : pendingActions) {
                if (pendingAction.isClientAction() && clientSourceId.equals(pendingAction.getSourceId())) {
                    return Optional.of(pendingAction.getClientAction(config));
                }
            }
            return Optional.empty();
        }

        public boolean removeActionById(String actionId) {
            Iterator<PendingAction> iter = pendingActions.iterator();
            while (iter.hasNext()) {
                PendingAction next = iter.next();
                if (actionId.equals(next.getActionId()) &&
                        (
                            (next.isClientAction() && clientSourceId.equals(next.getSourceId()))
                            || (next.isServerAction() && serverSourceId.equals(next.getSourceId()))
                        )) {
                    iter.remove();
                    return true;
                }
            }
            return false;
        }

        public void addAction(P4CommandRunner.ClientAction<?> action) {
            pendingActions.add(PendingActionFactory.create(ref, action));
        }

        public void addAction(P4CommandRunner.ServerAction<?> action) {
            pendingActions.add(PendingActionFactory.create(ref.getServerName(), action));
        }

        @NotNull
        @Override
        public Iterator<Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>>> iterator() {
            final Iterator<PendingAction> proxy = pendingActions.iterator();
            return new Iterator<Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>>>() {
                @Override
                public boolean hasNext() {
                    return proxy.hasNext();
                }

                @Override
                public Pair<P4CommandRunner.ClientAction<?>, P4CommandRunner.ServerAction<?>> next() {
                    PendingAction pendingAction = proxy.next();
                    // FIXME return something other than a pair; say, an object that can construct the right
                    // value?  Return the underlying PendingAction?
                    P4CommandRunner.ClientAction<?> clientAction = null;
                    P4CommandRunner.ServerAction<?> serverAction = null;
                    if (pendingAction.isClientAction()) {
                        throw new IllegalStateException("FIXME No ClientConfig known");
                        //clientAction = pendingAction.getClientAction();
                    } else {
                        throw new IllegalStateException("FIXME No ServerConfig known");
                        //serverAction = pendingAction.getServerAction();
                    }
                    //return new Pair<>(clientAction, serverAction);
                }

                @Override
                public void remove() {
                    proxy.remove();
                }
            };
        }
    }
}
