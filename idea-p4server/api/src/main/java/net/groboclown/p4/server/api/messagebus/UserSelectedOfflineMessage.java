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
import net.groboclown.p4.server.api.P4ServerName;
import org.jetbrains.annotations.NotNull;

public class UserSelectedOfflineMessage extends ProjectMessage<UserSelectedOfflineMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:user selected a server to go offline";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class OfflineEvent extends AbstractMessageEvent {
        private final P4ServerName name;

        public OfflineEvent(@NotNull P4ServerName name) {
            this.name = name;
        }

        @NotNull
        public P4ServerName getName() {
            return name;
        }
    }

    public interface Listener {
        void userSelectedServerOffline(@NotNull OfflineEvent e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void userSelectedServerOffline(@NotNull OfflineEvent e) {
        }
    }


    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
