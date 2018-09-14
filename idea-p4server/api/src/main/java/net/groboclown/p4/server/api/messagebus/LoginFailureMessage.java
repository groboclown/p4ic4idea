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
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class LoginFailureMessage extends ApplicationMessage<LoginFailureMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:login failed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    private static final Listener DEFAULT_LISTENER = new ListenerAdapter();

    public static class SingleSignOnExecutionFailureEvent extends AbstractMessageEvent {
        private final ServerConfig config;
        private final String cmd;
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public SingleSignOnExecutionFailureEvent(@NotNull ServerConfig config, String cmd,
                int exitCode, String stdout, String stderr) {
            this.config = config;
            this.cmd = cmd;
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getCmd() {
            return cmd;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        @NotNull
        public ServerConfig getConfig() {
            return config;
        }
    }


    public interface Listener {
        void singleSignOnFailed(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e);
        void singleSignOnExecutionFailed(@NotNull SingleSignOnExecutionFailureEvent e);
        void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e);
        void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e);

        /**
         * The user supplied a password, but the login does not use one.  The configuration should be changed
         * to use an empty password.
         *
         * @param e source exception
         */
        void passwordUnnecessary(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void singleSignOnFailed(
                @NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
        }

        @Override
        public void singleSignOnExecutionFailed(@NotNull SingleSignOnExecutionFailureEvent e) {

        }

        @Override
        public void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {

        }

        @Override
        public void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {

        }

        @Override
        public void passwordUnnecessary(
                @NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {

        }
    }

    public static abstract class AllErrorListener implements Listener {
        @Override
        public void singleSignOnFailed(
                @NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
            onLoginFailure(e);
        }

        @Override
        public void singleSignOnExecutionFailed(@NotNull SingleSignOnExecutionFailureEvent e) {
            onLoginFailure(new ServerErrorEvent.ServerConfigErrorEvent<>(e.getConfig(),
                    new AuthenticationFailedException(AuthenticationFailedException.ErrorType.SSO_LOGIN,
                            new ServerMessage(new ArrayList<>()))));
        }

        @Override
        public void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
            onLoginFailure(e);
        }

        @Override
        public void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
            onLoginFailure(e);
        }

        @Override
        public void passwordUnnecessary(
                @NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
            onLoginFailure(e);
        }

        protected abstract void onLoginFailure(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e);
    }

    public static Listener send() {
        return getListener(TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addTopicListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
