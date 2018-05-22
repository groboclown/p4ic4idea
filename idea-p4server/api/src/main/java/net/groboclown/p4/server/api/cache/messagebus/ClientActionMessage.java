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

package net.groboclown.p4.server.api.cache.messagebus;

import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;

public class ClientActionMessage
        extends AbstractCacheMessage<ClientActionMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:client action pending";
    private static final Topic<TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void clientActionUpdate(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::clientActionUpdate);
    }

    public static void sendEvent(@NotNull Event e) {
        abstractSendEvent(TOPIC, e);
    }


    public enum ActionState {
        PENDING,
        COMPLETED,
        FAILED
    }


    public static class Event extends AbstractCacheUpdateEvent<Event> {
        private final P4CommandRunner.ClientAction action;
        private final ActionState state;

        public Event(@NotNull ClientServerRef ref,
                @NotNull P4CommandRunner.ClientAction action,
                ActionState state) {
            super(ref);
            this.action = action;
            this.state = state;
        }

        public P4CommandRunner.ClientAction getAction() {
            return action;
        }

        public ActionState getState() {
            return state;
        }
    }
}