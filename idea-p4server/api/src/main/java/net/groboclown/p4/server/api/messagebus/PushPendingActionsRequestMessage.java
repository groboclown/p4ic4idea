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
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * After a server is connected, the pending actions generally need to be sent to the server.
 */
public class PushPendingActionsRequestMessage
        extends ProjectMessage<PushPendingActionsRequestMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:request push pending actions";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static final class PushPendingActionsRequestEvent extends AbstractMessageEvent {
        private final VirtualFile vcsRoot;

        public PushPendingActionsRequestEvent(@NotNull VirtualFile vcsRoot) {
            this.vcsRoot = vcsRoot;
        }

        @NotNull
        public VirtualFile getVcsRoot() {
            return this.vcsRoot;
        }
    }

    public interface Listener {
        /**
         *
         * @param event the configuration that requests the action push.
         */
        void pushPendingActions(@NotNull PushPendingActionsRequestEvent event);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void pushPendingActions(@NotNull PushPendingActionsRequestEvent event) {

        }
    }

    public static void requestPush(
            @NotNull Project project, @NotNull VirtualFile vcsRoot) {
        getListener(project, TOPIC, DEFAULT_LISTENER).pushPendingActions(new PushPendingActionsRequestEvent(vcsRoot));
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
