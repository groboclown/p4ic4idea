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
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SwarmErrorMessage extends ProjectMessage<SwarmErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:issue with swarm";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class SwarmEvent extends AbstractMessageEvent {
        private final P4ChangelistId changelist;

        public SwarmEvent(@Nullable P4ChangelistId changelist) {
            this.changelist = changelist;
        }

        @NotNull
        public Optional<P4ServerName> getName() {
            return Optional.ofNullable(changelist == null ? null : changelist.getServerName());
        }

        @NotNull
        public Optional<P4ChangelistId> getChangelistId() {
            return Optional.ofNullable(changelist);
        }
    }

    public interface Listener {
        void notOnServer(@NotNull SwarmEvent e, @NotNull ChangeList ideChangeList);
        void notNumberedChangelist(@NotNull SwarmEvent e);
        void problemContactingServer(@NotNull SwarmEvent swarmEvent, @NotNull Exception e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void notOnServer(@NotNull SwarmEvent e, @NotNull ChangeList ideChangeList) {

        }

        @Override
        public void notNumberedChangelist(@NotNull SwarmEvent e) {

        }

        @Override
        public void problemContactingServer(@NotNull SwarmEvent swarmEvent, @NotNull Exception e) {

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
