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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.util.messages.Topic;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Indicates that the user requested an action on a file.
 * Different than an action on another item.
 */
public class FileActionMessage
        extends AbstractCacheMessage<FileActionMessage.Event> {
    private static final String DISPLAY_NAME = "p4ic4idea:pending file action";
    private static final Topic<TopicListener<Event>> TOPIC = createTopic(DISPLAY_NAME);

    public interface Listener {
        void pendingFileAction(@NotNull Event event);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull String cacheId,
            @NotNull Listener listener) {
        abstractAddListener(client, TOPIC, cacheId, listener::pendingFileAction);
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
        private final FilePath file;
        private final P4FileAction action;
        private final P4FileType type;
        private final P4CommandRunner.ClientAction<?> serverAction;
        private final ActionState state;
        private final P4CommandRunner.ServerResultException problem;
        private final Throwable error;

        // Need the result for completed actions for situations where we don't know if the
        // request performed an add or edit, or other kinds of results.
        private final P4CommandRunner.ClientResult result;

        public Event(@NotNull ClientServerRef ref, @NotNull FilePath file, @NotNull P4FileAction action,
                @Nullable P4FileType type, @NotNull P4CommandRunner.ClientAction<?> serverAction) {
            super(ref);
            this.file = file;
            this.action = action;
            this.type = type;
            this.serverAction = serverAction;
            this.state = ActionState.PENDING;
            this.result = null;
            this.problem = null;
            this.error = null;
        }

        public Event(@NotNull ClientServerRef ref, @NotNull FilePath file, @NotNull P4FileAction action,
                @Nullable P4FileType type, @NotNull P4CommandRunner.ClientAction<?> serverAction,
                @NotNull P4CommandRunner.ClientResult serverResult) {
            super(ref);
            this.file = file;
            this.action = action;
            this.type = type;
            this.serverAction = serverAction;
            this.state = ActionState.COMPLETED;
            this.result = serverResult;
            this.problem = null;
            this.error = null;
        }

        public Event(@NotNull ClientServerRef ref, @NotNull FilePath file, @NotNull P4FileAction action,
                @Nullable P4FileType type, @NotNull P4CommandRunner.ClientAction<?> serverAction,
                @NotNull Throwable error) {
            super(ref);
            this.file = file;
            this.action = action;
            this.type = type;
            this.serverAction = serverAction;
            this.state = ActionState.FAILED;
            this.result = null;
            if (error instanceof P4CommandRunner.ServerResultException) {
                this.problem = (P4CommandRunner.ServerResultException) error;
                this.error = null;
            } else {
                this.problem = null;
                this.error = error;
            }
        }
    }
}
