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
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Rather than having a host of custom exceptions to handle all the different problems,
 * and have every handler have its own list of "instanceof" if statements, errors are
 * posted to the message bus with methods that explicitly define all the connection
 * problem types.
 * <p>
 * In some circumstances, and error will be thrown so that the Idea callers can react
 * accordingly.  In these cases, the message should still be posted to the bus.
 */
public class ClientConfigConnectionFailedMessage extends ProjectMessage<ClientConfigConnectionFailedMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:server connection failed";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME, Listener.class, Topic.BroadcastDirection.TO_CHILDREN
    );
    public interface Listener {
        void connectionTimedOut(@NotNull ClientConfig config);
        void hostNotFound(@NotNull ClientConfig config);
        void sslFingerprintMismatch(@NotNull ClientConfig config, @NotNull String discoveredFingerprint);
        void sslEncryptionStrengthProblem(@NotNull ClientConfig config);
        void incorrectPassword(@NotNull ClientConfig config, @Nullable String serverMessage);
        void passwordExpired(@NotNull ClientConfig config, @Nullable String serverMessage);
    }

    /**
     * Used only in cases where a class takes a {@link Listener} object instead of
     * posting to the message bus (to allow immediate result handling).  However, the
     * common use-case is for asynchronous communication.  To help with that, this
     * class can be passed into those methods to turn the call into a message bus
     * post.
     */
    public static class PostEventListener implements Listener {
        private final Project project;

        public PostEventListener(Project project) {
            this.project = project;
        }

        @Override
        public void connectionTimedOut(@NotNull ClientConfig config) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).connectionTimedOut(config);
            }
        }

        @Override
        public void hostNotFound(@NotNull ClientConfig config) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).hostNotFound(config);
            }
        }

        @Override
        public void sslFingerprintMismatch(@NotNull ClientConfig config, @NotNull String discoveredFingerprint) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).sslFingerprintMismatch(config, discoveredFingerprint);
            }
        }

        @Override
        public void sslEncryptionStrengthProblem(@NotNull ClientConfig config) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).sslEncryptionStrengthProblem(config);
            }
        }

        @Override
        public void incorrectPassword(@NotNull ClientConfig config, @Nullable String serverMessage) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).incorrectPassword(config, serverMessage);
            }
        }

        @Override
        public void passwordExpired(@NotNull ClientConfig config, @Nullable String serverMessage) {
            if (canSendMessage(project)) {
                project.getMessageBus().syncPublisher(TOPIC).passwordExpired(config, serverMessage);
            }
        }
    }

    /**
     * Helper class for those listeners that don't care about what the
     * problem was, but only that there was a problem.
     */
    public static abstract class AnyErrorListener
            implements Listener {

        public abstract void onError(@NotNull ClientConfig config);

        @Override
        public void connectionTimedOut(@NotNull ClientConfig config) {
            onError(config);
        }

        @Override
        public void hostNotFound(@NotNull ClientConfig config) {
            onError(config);
        }

        @Override
        public void sslFingerprintMismatch(@NotNull ClientConfig config, @NotNull String discoveredFingerprint) {
            onError(config);
        }

        @Override
        public void sslEncryptionStrengthProblem(@NotNull ClientConfig config) {
            onError(config);
        }

        @Override
        public void incorrectPassword(@NotNull ClientConfig config, @Nullable String serverMessage) {
            onError(config);
        }

        @Override
        public void passwordExpired(@NotNull ClientConfig config, @Nullable String serverMessage) {
            onError(config);
        }
    }

    public static void addListener(@NotNull MessageBusClient client, @NotNull Listener listener) {
        addListener(client, TOPIC, listener);
    }
}
