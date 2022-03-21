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
import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;

public class P4ServerErrorMessage extends ProjectMessage<P4ServerErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:login failed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public interface Listener {

        /**
         * The server returned an error message
         * @param e source
         */
        void requestCausedError(@NotNull ServerErrorEvent.ServerMessageEvent e);

        void requestCausedWarning(@NotNull ServerErrorEvent.ServerMessageEvent e);

        void requestCausedInfoMsg(@NotNull ServerErrorEvent.ServerMessageEvent e);

        /**
         * The plugin generated a RequestException
         *
         * @param e source
         */
        void requestException(@NotNull ServerErrorEvent.ServerMessageEvent e);

        void requestException(@NotNull ServerErrorEvent.ServerNameErrorEvent<P4JavaException> e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void requestCausedError(@NotNull ServerErrorEvent.ServerMessageEvent e) {
        }

        @Override
        public void requestCausedWarning(@NotNull ServerErrorEvent.ServerMessageEvent e) {
        }

        @Override
        public void requestCausedInfoMsg(@NotNull ServerErrorEvent.ServerMessageEvent e) {
        }

        @Override
        public void requestException(@NotNull ServerErrorEvent.ServerMessageEvent e) {
        }

        @Override
        public void requestException(@NotNull ServerErrorEvent.ServerNameErrorEvent<P4JavaException> e) {
        }
    }

    public static void addListener(@NotNull MessageBusClient.ProjectClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }

    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }
}
