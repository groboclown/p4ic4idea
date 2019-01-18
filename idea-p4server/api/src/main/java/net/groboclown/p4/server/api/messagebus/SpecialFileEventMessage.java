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

import java.util.Collection;

public class SpecialFileEventMessage
        extends ProjectMessage<SpecialFileEventMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:special file event";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    public static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class SpecialFileEvent extends AbstractMessageEvent {
        private final Collection<String> affectedFiles;
        private final String reason;

        public SpecialFileEvent(Collection<String> files, String reason) {
            this.affectedFiles = files;
            this.reason = reason;
        }

        public Collection<String> getAffectedFiles() {
            return affectedFiles;
        }

        public String getReason() {
            return reason;
        }
    }

    public interface Listener {
        void fileReverted(@NotNull SpecialFileEvent e);

        void fileRevertSkipped(@NotNull SpecialFileEvent e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void fileReverted(@NotNull SpecialFileEvent e) {
        }

        @Override
        public void fileRevertSkipped(@NotNull SpecialFileEvent e) {
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
