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
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ListClientsForUserCacheMessage
        extends AbstractCacheMessage<ListClientsForUserCacheMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:client action copmleted";
    private static final Topic<TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void listClientsForUserUpdate(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::listClientsForUserUpdate);
    }

    public static void sendEvent(@NotNull Event e) {
        abstractSendEvent(TOPIC, e);
    }


    public static class Event
            extends AbstractCacheUpdateEvent<Event> {
        private final String user;
        private final Collection<P4WorkspaceSummary> clients;

        public Event(@NotNull P4ServerName ref,
                @NotNull String user, @NotNull Collection<P4WorkspaceSummary> clients) {
            super(ref);
            this.user = user;
            this.clients = clients;
        }

        @NotNull
        public String getUser() {
            return user;
        }

        @NotNull
        public Collection<P4WorkspaceSummary> getClients() {
            return clients;
        }
    }
}
