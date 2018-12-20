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
import org.jetbrains.annotations.NotNull;

public class UserProjectPreferencesUpdatedMessage
        extends ProjectMessage<UserProjectPreferencesUpdatedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:user preferences updated";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class UserPreferencesUpdatedEvent extends AbstractMessageEvent {
        // Note that the project settings are not passed into this class, because
        // it's not in scope for this class!
        public UserPreferencesUpdatedEvent() {
        }
    }

    public interface Listener {
        void userPreferencesUpdated(@NotNull UserPreferencesUpdatedEvent e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void userPreferencesUpdated(@NotNull UserPreferencesUpdatedEvent e) {
        }
    }

    /**
     * Should only be called by {@link net.groboclown.p4.server.api.ProjectConfigRegistry}.  Doesn't conform to
     * the "send(Project)" standard API, because it can trigger 2 events.
     *
     * @param project project to send the message on.
     */
    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
