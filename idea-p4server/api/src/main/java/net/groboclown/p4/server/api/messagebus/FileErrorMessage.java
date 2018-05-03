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
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class FileErrorMessage extends ProjectMessage<FileErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:reconnect to server";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    public static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public interface Listener {

        void fileReceiveError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull Exception e);

        void fileSendError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull Exception e);

        void localFileError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull IOException e);
    }

    public static class ListenerAdapter implements Listener {

        @Override
        public void fileReceiveError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull Exception e) {

        }

        @Override
        public void fileSendError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull Exception e) {

        }

        @Override
        public void localFileError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull IOException e) {

        }
    }

    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }
}
