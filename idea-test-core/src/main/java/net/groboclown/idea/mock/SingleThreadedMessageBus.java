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

package net.groboclown.idea.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.MessageHandler;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple message bus that runs a message immediately.
 */
public class SingleThreadedMessageBus
        implements MessageBus {
    private final String owner;
    private final MessageBus parent;
    private final Disposable connectionDisposable;
    private final List<InnerMessageBusConnection> connections = new ArrayList<>();
    private final Map<Topic<?>, Object> publishers = new HashMap<>();

    public SingleThreadedMessageBus(@Nullable MessageBus parent) {
        this.owner = parent + " of " + SingleThreadedMessageBus.class;
        this.connectionDisposable = Disposer.newDisposable(owner);
        this.parent = parent;
    }

    @Nullable
    @Override
    public MessageBus getParent() {
        return parent;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <L> L syncPublisher(@NotNull final Topic<L> topic) {
        L publisher = (L) publishers.get(topic);
        if (publisher == null) {
            Class<L> listenerClass = topic.getListenerClass();
            publisher = (L) Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class<?>[] {
                    listenerClass
            }, (proxy, method, args) -> {
                for (InnerMessageBusConnection connection : connections) {
                    connection.invoke(topic, method, args);
                }
                return null;
            });
            publishers.put(topic, publisher);
        }
        return publisher;
    }

    @Deprecated
    @NotNull
    // v183 - removed this method.
    //@Override
    public <L> L asyncPublisher(@NotNull Topic<L> topic) {
        return syncPublisher(topic);
    }

    @Override
    public void dispose() {

    }

    // v173 - introduced new method
    //@Override
    @SuppressWarnings("unused")
    public boolean isDisposed() {
        return false;
    }

    @NotNull
    @Override
    public MessageBusConnection connect() {
        return connect(connectionDisposable);
    }

    // v202 - introduced new method
    //@Override
    @SuppressWarnings("unused")
    public MessageBusConnection simpleConnect() {
        return connect();
    }

    @NotNull
    @Override
    public MessageBusConnection connect(@NotNull Disposable parentDisposable) {
        final InnerMessageBusConnection connection = new InnerMessageBusConnection();
        Disposer.register(parentDisposable, connection);
        connections.add(connection);
        return connection;
    }

    @Override
    public boolean hasUndeliveredEvents(@NotNull Topic<?> topic) {
        return false;
    }

    class InnerMessageBusConnection
            implements MessageBusConnection {
        final Map<Topic<?>, List<Object>> handlers = new HashMap<>();
        MessageHandler defaultHandler = null;

        void invoke(Topic<?> topic, Method m, Object[] args) {
            List<Object> listeners = handlers.get(topic);
            if (listeners != null) {
                for (Object listener : listeners) {
                    try {
                        m.invoke(listener, args);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        @Override
        public void dispose() {
            SingleThreadedMessageBus.this.connections.remove(this);
        }

        @Override
        public <L> void subscribe(@NotNull Topic<L> topic, @NotNull L handler)
                throws IllegalStateException {
            handlers.putIfAbsent(topic, new ArrayList<>());
            handlers.get(topic).add(handler);
        }

        @Override
        public <L> void subscribe(@NotNull Topic<L> topic)
                throws IllegalStateException {
            if (defaultHandler == null) {
                throw new IllegalStateException();
            }
            handlers.putIfAbsent(topic, new ArrayList<>());
            handlers.get(topic).add(defaultHandler);
        }

        @Override
        public void setDefaultHandler(@Nullable MessageHandler handler) {
            defaultHandler = handler;
        }

        @Override
        public void deliverImmediately() {
            // do nothing
        }

        @Override
        public void disconnect() {
            dispose();
            handlers.clear();
        }
    }
}
