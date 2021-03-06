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
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ClientOpenCacheMessage
        extends AbstractCacheMessage<ClientOpenCacheMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:open cache update";
    private static final Topic<AbstractCacheMessage.TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void openFilesChangelistsUpdated(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::openFilesChangelistsUpdated);
    }

    public static void sendEvent(@NotNull Event e) {
        abstractSendEvent(TOPIC, e);
    }


    public static class Event extends AbstractCacheUpdateEvent<Event> {
        private final Collection<P4LocalFile> openedFiles;
        private final Collection<P4LocalChangelist> pendingChangelists;

        public Event(@NotNull ClientServerRef ref,
                Collection<P4LocalFile> openedFiles,
                Collection<P4LocalChangelist> pendingChangelists) {
            super(ref);
            this.openedFiles = openedFiles;
            this.pendingChangelists = pendingChangelists;
        }

        public Collection<P4LocalFile> getOpenedFiles() {
            return openedFiles;
        }

        public Collection<P4LocalChangelist> getPendingChangelists() {
            return pendingChangelists;
        }
    }
}
