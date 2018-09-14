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
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileSaveException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.exception.ZeroconfException;
import org.jetbrains.annotations.NotNull;

public class ConnectionErrorMessage extends ApplicationMessage<ConnectionErrorMessage.Listener> {
    private static final String DISPLAY_NAME = "p4ic4idea:reconnect to server";
    private static final Topic<Listener> TOPIC = new Topic<>(
            DISPLAY_NAME,
            Listener.class,
            Topic.BroadcastDirection.TO_CHILDREN);
    public static final Listener DEFAULT_LISTENER = new ListenerAdapter();


    public interface Listener {
        /**
         * The server host could not be found.
         *
         * @param event source
         */
        void unknownServer(@NotNull ServerErrorEvent.ServerNameErrorEvent<Exception> event);

        /**
         * One of the files in the configuration could not be written to.
         *
         * @param event source
         */
        void couldNotWrite(@NotNull ServerErrorEvent.ServerConfigErrorEvent<FileSaveException> event);

        /**
         * A problem with the zerconf setup.
         *
         * @param event source
         */
        void zeroconfProblem(@NotNull ServerErrorEvent.ServerNameErrorEvent<ZeroconfException> event);

        void sslHostTrustNotEstablished(@NotNull ServerErrorEvent.ServerConfigProblemEvent event);

        void sslHostFingerprintMismatch(@NotNull ServerErrorEvent.ServerConfigErrorEvent<TrustException> event);

        /**
         * The user needs to install the unlimited strength encryption libraries.
         *
         * @param event source
         */
        void sslAlgorithmNotSupported(@NotNull ServerErrorEvent.ServerNameProblemEvent event);

        /**
         * The SSL peer (server) couldn't be connected due one of several potential issues.
         * It's a wrapped SSLPeerUnverifiedException.
         * Possible issues:
         * <ol>
         *     <li>No certificate</li>
         *     <li>cipher suite doesn't support authentication,</li>
         *     <li>no peer authentication established during SSL handshake</li>
         *     <li>user time isn't close enough to the server time.</li>
         *     <li>Server didn't send complete certificate chain</li>
         * </ol>
         *
         * @param event exception source
         */
        void sslPeerUnverified(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslHandshakeException> event);

        /**
         * A general certificate issue from the server.
         *
         * @param event exception source
         */
        void sslCertificateIssue(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslException> event);

        /**
         * General problem with connection, such as socket disconnected mid-stream,
         * the server version is incompatible with the plugin, the server sends
         * garbled information, and so on.
         *
         * @param event source
         */
        void connectionError(@NotNull ServerErrorEvent.ServerNameErrorEvent<ConnectionException> event);

        /**
         * Client doesn't have the resources necessary to open the connection to the server.
         *
         * @param event source
         */
        void resourcesUnavailable(@NotNull ServerErrorEvent.ServerNameErrorEvent<ResourceException> event);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void unknownServer(@NotNull ServerErrorEvent.ServerNameErrorEvent<Exception> event) {
        }

        @Override
        public void couldNotWrite(@NotNull ServerErrorEvent.ServerConfigErrorEvent<FileSaveException> event) {
        }

        @Override
        public void zeroconfProblem(@NotNull ServerErrorEvent.ServerNameErrorEvent<ZeroconfException> event) {
        }

        @Override
        public void sslHostTrustNotEstablished(@NotNull ServerErrorEvent.ServerConfigProblemEvent event) {
        }

        @Override
        public void sslHostFingerprintMismatch(@NotNull ServerErrorEvent.ServerConfigErrorEvent<TrustException> event) {
        }

        @Override
        public void sslAlgorithmNotSupported(@NotNull ServerErrorEvent.ServerNameProblemEvent event) {
        }

        @Override
        public void sslPeerUnverified(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslHandshakeException> event) {
        }

        @Override
        public void sslCertificateIssue(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslException> event) {
        }

        @Override
        public void connectionError(@NotNull ServerErrorEvent.ServerNameErrorEvent<ConnectionException> event) {
        }

        @Override
        public void resourcesUnavailable(@NotNull ServerErrorEvent.ServerNameErrorEvent<ResourceException> e) {
        }
    }

    public static abstract class AllErrorListener implements Listener {
        @Override
        public void unknownServer(@NotNull ServerErrorEvent.ServerNameErrorEvent<Exception> event) {
            onHostConnectionError(event);
        }

        @Override
        public void couldNotWrite(@NotNull ServerErrorEvent.ServerConfigErrorEvent<FileSaveException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void zeroconfProblem(@NotNull ServerErrorEvent.ServerNameErrorEvent<ZeroconfException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void sslHostTrustNotEstablished(@NotNull ServerErrorEvent.ServerConfigProblemEvent event) {
            onHostConnectionError(event);
        }

        @Override
        public void sslHostFingerprintMismatch(@NotNull ServerErrorEvent.ServerConfigErrorEvent<TrustException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void sslAlgorithmNotSupported(@NotNull ServerErrorEvent.ServerNameProblemEvent event) {
            onHostConnectionError(event);
        }

        @Override
        public void sslPeerUnverified(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslHandshakeException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void sslCertificateIssue(@NotNull ServerErrorEvent.ServerNameErrorEvent<SslException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void connectionError(@NotNull ServerErrorEvent.ServerNameErrorEvent<ConnectionException> event) {
            onHostConnectionError(event);
        }

        @Override
        public void resourcesUnavailable(@NotNull ServerErrorEvent.ServerNameErrorEvent<ResourceException> event) {
            onHostConnectionError(event);
        }

        public abstract <E extends Exception> void onHostConnectionError(@NotNull ServerErrorEvent<E> event);
    }

    public static Listener send() {
        return getListener(TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient.ApplicationClient client,
            @NotNull Object listenerOwner, @NotNull Listener listener) {
        addTopicListener(client, TOPIC, listener, Listener.class, listenerOwner);
    }
}
