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

import com.intellij.util.messages.Topic;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public class LoginFailureMessage extends ApplicationMessage<LoginFailureMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:login failed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public interface Listener {
        void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e);
        void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e);

        void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e);

        /**
         * The user supplied a password, but the login does not use one.  The configuration should be changed
         * to use an empty password.
         *
         * @param config configuration that caused the issue.
         * @param e source exception
         */
        void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            // do nothing
        }

        @Override
        public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            // do nothing
        }

        @Override
        public void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            // do nothing
        }

        @Override
        public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            // do nothing
        }
    }

    public static abstract class AllErrorListener implements Listener {
        @Override
        public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            onLoginFailure(config, e);
        }

        @Override
        public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            onLoginFailure(config, e);
        }

        @Override
        public void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            onLoginFailure(config, e);
        }

        @Override
        public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
            onLoginFailure(config, e);
        }

        public abstract void onLoginFailure(@NotNull ServerConfig serverConfig,
                @NotNull AuthenticationFailedException e);
    }

    public static Listener send() {
        return getListener(TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client, @NotNull Listener listener) {
        addTopicListener(client, TOPIC, listener);
    }
}
