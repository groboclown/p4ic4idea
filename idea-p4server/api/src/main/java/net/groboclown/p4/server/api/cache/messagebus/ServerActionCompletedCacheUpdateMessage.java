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
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;

public class ServerActionCompletedCacheUpdateMessage
        extends AbstractCacheMessage<ServerActionCompletedCacheUpdateMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:server action completed";
    private static final Topic<TopicListener<ServerActionCompletedCacheUpdateMessage.Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void serverActionCompleted(@NotNull ServerActionCompletedCacheUpdateMessage.Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull ServerActionCompletedCacheUpdateMessage.Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::serverActionCompleted);
    }

    public static void sendEvent(@NotNull ServerActionCompletedCacheUpdateMessage.Event e) {
        abstractSendEvent(TOPIC, e);
    }


    public static class Event extends AbstractCacheUpdateEvent<ServerActionCompletedCacheUpdateMessage.Event> {
        private final P4CommandRunner.ServerAction action;

        public Event(@NotNull P4ServerName ref,
                @NotNull P4CommandRunner.ServerAction action) {
            super(ref);
            this.action = action;
        }

        public P4CommandRunner.ServerAction getAction() {
            return action;
        }
    }
}
