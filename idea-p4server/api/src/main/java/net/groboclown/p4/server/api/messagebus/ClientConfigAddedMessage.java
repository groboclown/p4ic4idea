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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientConfigAddedMessage extends ProjectMessage<ClientConfigAddedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:client configuration registration added";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    // This pattern is a little different.  Sending a client added message means sending a
    // server added to the application message bus.
    private static final String SERVER_DISPLAY_NAME = "p4ic4idea:server configuration registration added";
    private static final Topic<ServerListener> SERVER_TOPIC = new Topic<>(
            SERVER_DISPLAY_NAME, ServerListener.class, Topic.BroadcastDirection.TO_CHILDREN
    );

    public static class ClientConfigAddedEvent extends AbstractMessageEvent {
        private final VirtualFile root;
        private final ClientConfig clientConfig;

        ClientConfigAddedEvent(@Nullable VirtualFile root, @NotNull ClientConfig clientConfig) {
            this.root = root;
            this.clientConfig = clientConfig;
        }

        @Nullable
        public VirtualFile getRoot() {
            return root;
        }

        @NotNull
        public ClientConfig getClientConfig() {
            return clientConfig;
        }
    }

    public interface Listener {
        void clientConfigurationAdded(@NotNull ClientConfigAddedEvent e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void clientConfigurationAdded(@NotNull ClientConfigAddedEvent e) {
        }
    }

    public static class ServerConfigAddedEvent extends AbstractMessageEvent {
        private final ServerConfig serverConfig;

        ServerConfigAddedEvent(@NotNull ServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }

        @NotNull
        public ServerConfig getServerConfig() {
            return serverConfig;
        }
    }

    public interface ServerListener {
        void serverConfigAdded(@NotNull ServerConfigAddedEvent e);
    }

    /**
     * Should only be called by {@link net.groboclown.p4.server.api.ProjectConfigRegistry}.  Doesn't conform to
     * the "send(Project)" standard API, because it can trigger 2 events.
     *
     * @param project project to send the message on.
     */
    public static void reportClientConfigurationAdded(@NotNull Project project,
            @Nullable VirtualFile root, @NotNull ClientConfig clientConfig) {
        ServerListener serverListener = ApplicationMessage.getListener(SERVER_TOPIC);
        if (serverListener != null) {
            serverListener.serverConfigAdded(new ServerConfigAddedEvent(clientConfig.getServerConfig()));
        }
        getListener(project, TOPIC, DEFAULT_LISTENER).clientConfigurationAdded(new ClientConfigAddedEvent(
                root, clientConfig));
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }

    public static void addServerListener(@NotNull MessageBusClient.ApplicationClient client,
            @NotNull Object listenerOwner, @NotNull ServerListener listener) {
        ApplicationMessage.addTopicListener(client, SERVER_TOPIC, listener, ServerListener.class, listenerOwner);
    }
}
