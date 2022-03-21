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
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;


/**
 * Report that a new client configuration was added.
 */
public class ClientConfigAddedMessage extends ProjectMessage<ClientConfigAddedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:client configuration registration added";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class ClientConfigAddedEvent extends AbstractMessageEvent {
        private final ClientConfig clientConfig;

        ClientConfigAddedEvent(@NotNull ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
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

    /**
     * Should only be called by {@link net.groboclown.p4.server.api.ProjectConfigRegistry}.
     *
     * @param project project to send the message on.
     */
    public static void reportClientConfigurationAdded(@NotNull Project project,
            @NotNull ClientConfig clientConfig) {
        getListener(project, TOPIC, DEFAULT_LISTENER).clientConfigurationAdded(new ClientConfigAddedEvent(
                clientConfig));
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
