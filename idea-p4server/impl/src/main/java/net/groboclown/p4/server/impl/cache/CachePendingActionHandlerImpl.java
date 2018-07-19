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

package net.groboclown.p4.server.impl.cache;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.cache.CachePendingActionHandler;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CachePendingActionHandlerImpl implements CachePendingActionHandler {
    private final ProjectCacheStore cache;

    public CachePendingActionHandlerImpl(@NotNull ProjectCacheStore projectCache) {
        this.cache = projectCache;
    }

    @Override
    public <R> R readActions(@NotNull ClientConfig clientConfig, @NotNull Function<Stream<ActionChoice>, R> f)
            throws InterruptedException {
        return f.apply(copyActions(clientConfig));
    }

    @Override
    public void readActionItems(@NotNull ClientConfig clientConfig, @NotNull Consumer<ActionChoice> f)
            throws InterruptedException {
        copyActions(clientConfig).forEach(f);
    }

    @Override
    public Stream<ActionChoice> copyActions(ClientConfig clientConfig)
            throws InterruptedException {
        final String clientId = ActionStore.getSourceId(clientConfig);
        final String serverId = ActionStore.getSourceId(clientConfig.getServerConfig());
        return cache.copyActions().stream()
                .filter((a) -> (a.clientAction != null && a.sourceId.equals(clientId)) ||
                        (a.serverAction != null && a.sourceId.equals(serverId)))
                .map((a) -> new ActionChoice(a.clientAction, a.serverAction));
    }

    @Override
    public void writeActions(ClientServerRef clientConfig, Consumer<WriteActionCache> fun)
            throws InterruptedException {
        cache.writeActions((actions) -> fun.accept(new WriteActionCacheImpl(clientConfig, actions)));
    }

    @Override
    public void writeActions(P4ServerName config, Consumer<WriteActionCache> fun)
            throws InterruptedException {
        cache.writeActions((actions) -> fun.accept(new WriteActionCacheImpl(config, actions)));
    }

    private class WriteActionCacheImpl implements WriteActionCache {
        @Nullable private final ClientServerRef clientServerRef;
        private final P4ServerName serverName;
        @Nullable private final String clientSourceId;
        private final String serverSourceId;
        private final List<ActionStore.PendingAction> actions;

        private WriteActionCacheImpl(@NotNull ClientServerRef clientServerRef,
                List<ActionStore.PendingAction> actions) {
            this.clientServerRef = clientServerRef;
            this.serverName = clientServerRef.getServerName();
            this.clientSourceId = ActionStore.getSourceId(clientServerRef);
            this.serverSourceId = ActionStore.getSourceId(this.serverName);
            this.actions = actions;
        }

        private WriteActionCacheImpl(@NotNull P4ServerName serverName,
                List<ActionStore.PendingAction> actions) {
            this.clientServerRef = null;
            this.serverName = serverName;
            this.clientSourceId = null;
            this.serverSourceId = ActionStore.getSourceId(serverName);
            this.actions = actions;
        }

        @Override
        public Stream<ActionChoice> getActions() {
            return actions.stream()
                    .filter(this::isInSources)
                    .map((a) -> new ActionChoice(a.clientAction, a.serverAction));
        }

        @Override
        public Optional<P4CommandRunner.ClientAction<?>> getClientActionById(@NotNull String actionId) {
            for (ActionStore.PendingAction action: actions) {
                if (action.clientAction != null && action.sourceId.equals(clientSourceId) && action.clientAction.getActionId().equals(actionId)) {
                    return Optional.of(action.clientAction);
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean removeActionById(@NotNull String actionId) {
            Iterator<ActionStore.PendingAction> iter = actions.iterator();
            while (iter.hasNext()) {
                ActionStore.PendingAction action = iter.next();
                if (isInSources(action) && actionId.equals(
                        action.clientAction != null
                            ? action.clientAction.getActionId()
                            : action.serverAction != null
                                ? action.serverAction.getActionId()
                                : null)) {
                    // FIXME should this send out an event?
                    // This would affect the ActionConnectionPanel.
                    iter.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void addAction(@NotNull P4CommandRunner.ClientAction<?> action) {
            if (clientSourceId == null) {
                throw new IllegalStateException("writer is configured for server actions");
            }
            // Ensure it's not already present
            for (ActionStore.PendingAction pendingAction: actions) {
                if (pendingAction.clientAction != null && clientSourceId.equals(pendingAction.sourceId) &&
                        action.getActionId().equals(pendingAction.clientAction.getActionId())) {
                    return;
                }
            }
            actions.add(ActionStore.createPendingAction(clientServerRef, action));
        }

        @Override
        public void addAction(@NotNull P4CommandRunner.ServerAction<?> action) {
            // Ensure it's not already present
            for (ActionStore.PendingAction pendingAction: actions) {
                if (pendingAction.serverAction != null && serverSourceId.equals(pendingAction.sourceId) &&
                        action.getActionId().equals(pendingAction.serverAction.getActionId())) {
                    return;
                }
            }
            actions.add(ActionStore.createPendingAction(serverName, action));
        }

        @NotNull
        @Override
        public Iterator<ActionChoice> iterator() {
            final Iterator<ActionStore.PendingAction> proxy = actions.iterator();
            return new ActionChoiceIterator(proxy);
        }

        private boolean isInSources(ActionStore.PendingAction a) {
            return (a.clientAction != null && a.sourceId.equals(clientSourceId)) ||
                    (a.serverAction != null && a.sourceId.equals(serverSourceId));
        }

        private class ActionChoiceIterator
                implements Iterator<ActionChoice> {
            private final Iterator<ActionStore.PendingAction> proxy;
            private  ActionChoice next;

            public ActionChoiceIterator(Iterator<ActionStore.PendingAction> proxy) {
                this.proxy = proxy;
                findNext();
            }

            private void findNext() {
                while (this.proxy.hasNext()) {
                    ActionStore.PendingAction v = this.proxy.next();
                    if (isInSources(v)) {
                        this.next = new ActionChoice(v.clientAction, v.serverAction);
                    }
                }
                next = null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public ActionChoice next() {
                ActionChoice ret = next;
                if (ret == null) {
                    throw new NoSuchElementException();
                }
                findNext();
                return ret;
            }

            @Override
            public void remove() {
                proxy.remove();
            }
        }
    }
}
