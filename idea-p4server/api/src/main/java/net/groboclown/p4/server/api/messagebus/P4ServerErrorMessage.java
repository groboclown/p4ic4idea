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
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4ServerErrorMessage extends ProjectMessage<P4ServerErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:login failed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    public static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public interface Listener {

        /**
         * The server returned an error message
         * @param msg message sent
         * @param re source
         */
        void requestCausedError(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re);

        void requestCausedWarning(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re);

        void requestCausedInfoMsg(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re);

        /**
         * The plugin generated a RequestException
         *
         * @param name
         * @param config
         * @param re
         */
        void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull RequestException re);

        void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull P4JavaException e);
    }

    public static class ListenerAdapter implements Listener {

        @Override
        public void requestCausedError(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re) {

        }

        @Override
        public void requestCausedWarning(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re) {

        }

        @Override
        public void requestCausedInfoMsg(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull IServerMessage msg, @NotNull RequestException re) {

        }

        @Override
        public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull RequestException re) {

        }

        @Override
        public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull P4JavaException e) {

        }
    }

    public static Listener send(@NotNull Project project) {
        return getListener(project, TOPIC, DEFAULT_LISTENER);
    }
}
