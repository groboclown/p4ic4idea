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

package net.groboclown.p4.server.api.messagebus;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-scoped request, because the client-server connections are shared between
 * projects.
 */
public class ReconnectRequestMessage
        extends ProjectMessage<ReconnectRequestMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:reconnect to server";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class ReconnectAllEvent extends AbstractMessageEvent {
        private final boolean mayDisplayDialogs;

        public ReconnectAllEvent(boolean mayDisplayDialogs) {
            this.mayDisplayDialogs = mayDisplayDialogs;
        }

        public boolean mayDisplayDialogs() {
            return mayDisplayDialogs;
        }
    }

    public static class ReconnectEvent extends AbstractMessageEvent {
        private final ClientServerRef ref;
        private final boolean mayDisplayDialogs;

        public ReconnectEvent(@NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
            this.ref = ref;
            this.mayDisplayDialogs = mayDisplayDialogs;
        }

        public boolean mayDisplayDialogs() {
            return mayDisplayDialogs;
        }

        @NotNull
        public ClientServerRef getRef() {
            return ref;
        }
    }

    public interface Listener {
        void reconnectToAllClients(@NotNull ReconnectAllEvent e);

        void reconnectToClient(@NotNull ReconnectEvent e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void reconnectToAllClients(@NotNull ReconnectAllEvent e) {
        }

        @Override
        public void reconnectToClient(@NotNull ReconnectEvent e) {
        }
    }


    public static void requestReconnectToAllClients(@NotNull Project project, boolean mayDisplayDialogs) {
        if (canSendMessage(project)) {
            getListener(project, TOPIC, DEFAULT_LISTENER).reconnectToAllClients(new ReconnectAllEvent(mayDisplayDialogs));
        }
    }


    public static void requestReconnectToClient(@NotNull Project project,
            @NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
        if (canSendMessage(project)) {
            getListener(project, TOPIC, DEFAULT_LISTENER).reconnectToClient(new ReconnectEvent(ref, mayDisplayDialogs));
        }
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
