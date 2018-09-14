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
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DescribeChangelistCacheMessage
        extends AbstractCacheMessage<DescribeChangelistCacheMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:client action completed";
    private static final Topic<TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void describeChangelistUpdate(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::describeChangelistUpdate);
    }

    public static void sendEvent(@NotNull Event e) {
        abstractSendEvent(TOPIC, e);
    }


    public static class Event extends AbstractCacheUpdateEvent<Event> {
        private final P4ChangelistId requestedChangelist;
        private final P4RemoteChangelist updatedChangelist;

        public Event(@NotNull P4ServerName ref,
                @NotNull P4ChangelistId requestedChangelist,
                @Nullable P4RemoteChangelist updatedChangelist) {
            super(ref);
            this.requestedChangelist = requestedChangelist;
            this.updatedChangelist = updatedChangelist;
        }

        @NotNull
        public P4ChangelistId getRequestedChangelist() {
            return requestedChangelist;
        }

        @Nullable
        public P4RemoteChangelist getUpdatedChangelist() {
            return updatedChangelist;
        }
    }
}
