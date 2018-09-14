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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientConfigRemovedMessage extends ProjectMessage<ClientConfigRemovedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:client configuration registration removed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class Event extends AbstractMessageEvent {
        // event source is here so that an object that sends this event can tell if
        // it receives the event it just sent out.
        private final Object eventSource;
        private final ClientConfig config;
        private final VirtualFile vcsRootDir;

        public Event(Object eventSource, ClientConfig config, VirtualFile vcsRootDir) {
            this.eventSource = eventSource;
            this.config = config;
            this.vcsRootDir = vcsRootDir;
        }

        public Object getEventSource() {
            return eventSource;
        }

        public ClientConfig getClientConfig() {
            return config;
        }

        public VirtualFile getVcsRootDir() {
            return vcsRootDir;
        }
    }

    public interface Listener {
        /**
         * @param event the details about the removed configuration.
         */
        void clientConfigurationRemoved(@NotNull Event event);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void clientConfigurationRemoved(@NotNull Event event) {
        }
    }

    public static void reportClientConfigRemoved(@NotNull Project project, @NotNull Object src,
            @NotNull ClientConfig config, @Nullable VirtualFile vcsRootDir) {
        getListener(project, TOPIC, DEFAULT_LISTENER).clientConfigurationRemoved(new Event(src, config, vcsRootDir));
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
