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

public class ClientConfigRemovedMessage extends ProjectMessage<ClientConfigRemovedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:client configuration registration removed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );

    public interface Listener {
        void clientConfigurationRemoved(@NotNull ClientConfig config);
    }

    public static void reportClientConfigRemoved(@NotNull Project project, @NotNull ClientConfig config) {
        if (canSendMessage(project)) {
            getListener(project, TOPIC).clientConfigurationRemoved(config);
        }
    }

    public static void addListener(@NotNull MessageBusClient client, @NotNull Listener listener) {
        addListener(client, TOPIC, listener);
    }
}
