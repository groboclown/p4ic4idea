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

    public interface Listener {
        void clientConfigurationAdded(@Nullable VirtualFile root, @NotNull ClientConfig clientConfig);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void clientConfigurationAdded(@Nullable VirtualFile root, @NotNull ClientConfig clientConfig) {

        }
    }

    public interface ServerListener {
        void serverConfigAdded(@NotNull ServerConfig serverConfig);
    }

    /**
     * Should only be called by {@link net.groboclown.p4.server.api.ProjectConfigRegistry}.
     *
     * @param project project to send the message on.
     * @return the listener proxy for this message
     */
    public static void sendClientConfigurationAdded(@NotNull Project project,
            @Nullable VirtualFile root, @NotNull ClientConfig clientConfig) {
        ServerListener serverListener = ApplicationMessage.getListener(SERVER_TOPIC);
        if (serverListener != null) {
            serverListener.serverConfigAdded(clientConfig.getServerConfig());
        }
        getListener(project, TOPIC, DEFAULT_LISTENER).clientConfigurationAdded(root, clientConfig);
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client, @NotNull Listener listener) {
        addListener(client, TOPIC, listener);
    }

    public static void addServerListener(@NotNull MessageBusClient.ApplicationClient client,
            @NotNull ServerListener listener) {
        client.add(SERVER_TOPIC, listener);
    }
}
