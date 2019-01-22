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
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import org.jetbrains.annotations.NotNull;

public class InternalErrorMessage extends ProjectMessage<InternalErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:reconnect to server";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    public static final Listener DEFAULT_LISTENER  = new ListenerAdapter();

    public interface Listener {
        /**
         * An internal API in the plugin caused a problem.
         *
         * @param t error
         */
        void internalError(@NotNull ErrorEvent<Throwable> t);

        /**
         * the P4 API layer of code caused a problem.
         *
         * @param t error
         */
        void p4ApiInternalError(@NotNull ErrorEvent<Throwable> t);

        /**
         * Some exception that was not expected was thrown.
         *
         * @param t error
         */
        void unexpectedError(@NotNull ErrorEvent<Throwable> t);

        void cacheLockTimeoutError(@NotNull ErrorEvent<VcsInterruptedException> t);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void internalError(@NotNull ErrorEvent<Throwable> t) {
        }

        @Override
        public void p4ApiInternalError(@NotNull ErrorEvent<Throwable> t) {
        }

        @Override
        public void unexpectedError(@NotNull ErrorEvent<Throwable> t) {
        }

        @Override
        public void cacheLockTimeoutError(@NotNull ErrorEvent<VcsInterruptedException> t) {
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
