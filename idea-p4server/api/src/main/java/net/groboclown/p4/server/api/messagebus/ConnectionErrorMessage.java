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
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileSaveException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.exception.ZeroconfException;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
         * @param e source
         */
        void unknownServer(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull Exception e);

        /**
         * One of the files in the configuration could not be written to.
         *
         * @param e source
         */
        void couldNotWrite(@NotNull ServerConfig config, @NotNull FileSaveException e);

        /**
         * A problem with the zerconf setup.
         *
         * @param e source
         */
        void zeroconfProblem(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull ZeroconfException e);

        void sslHostTrustNotEstablished(@NotNull ServerConfig serverConfig);

        void sslHostFingerprintMismatch(@NotNull ServerConfig serverConfig, @NotNull TrustException e);

        /**
         * The user needs to install the unlimited strength encryption libraries.
         *
         * @param serverConfig source
         */
        void sslAlgorithmNotSupported(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig);

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
         * @param serverConfig source
         * @param e exception source
         */
        void sslPeerUnverified(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig,
                @NotNull SslHandshakeException e);

        /**
         * A general certificate issue from the server.
         *
         * @param serverName source name
         * @param serverConfig source config
         * @param e exception source
         */
        void sslCertificateIssue(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull SslException e);

        /**
         * General problem with connection, such as socket disconnected mid-stream,
         * the server version is incompatible with the plugin, the server sends
         * garbled information, and so on.
         *
         * @param serverName   name
         * @param serverConfig config
         * @param e            source
         */
        void connectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull ConnectionException e);

        /**
         * Client doesn't have the resources necessary to open the connection to the server.
         *
         * @param serverName   name
         * @param serverConfig config
         * @param e            source
         */
        void resourcesUnavailable(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull ResourceException e);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void unknownServer(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull Exception e) {

        }

        @Override
        public void couldNotWrite(@NotNull ServerConfig config, @NotNull FileSaveException e) {

        }

        @Override
        public void zeroconfProblem(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull ZeroconfException e) {

        }

        @Override
        public void sslHostTrustNotEstablished(@NotNull ServerConfig serverConfig) {

        }

        @Override
        public void sslHostFingerprintMismatch(@NotNull ServerConfig serverConfig, @NotNull TrustException e) {

        }

        @Override
        public void sslAlgorithmNotSupported(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig) {

        }

        @Override
        public void sslPeerUnverified(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig,
                @NotNull SslHandshakeException e) {

        }

        @Override
        public void sslCertificateIssue(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull SslException e) {

        }

        @Override
        public void connectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull ConnectionException e) {

        }

        @Override
        public void resourcesUnavailable(@NotNull P4ServerName serverName, ServerConfig serverConfig, @NotNull ResourceException e) {

        }
    }

    public static abstract class AllErrorListener implements Listener {
        @Override
        public void unknownServer(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull Exception e) {
            onHostConnectionError(name, config, e);
        }

        @Override
        public void couldNotWrite(@NotNull ServerConfig config, @NotNull FileSaveException e) {
            onHostConnectionError(config.getServerName(), config, e);
        }

        @Override
        public void zeroconfProblem(@NotNull P4ServerName name, @Nullable ServerConfig config,
                @NotNull ZeroconfException e) {
            onHostConnectionError(name, config, e);

        }

        @Override
        public void sslHostTrustNotEstablished(@NotNull ServerConfig serverConfig) {
            onHostConnectionError(serverConfig.getServerName(), serverConfig, null);
        }

        @Override
        public void sslHostFingerprintMismatch(@NotNull ServerConfig serverConfig, @NotNull TrustException e) {
            onHostConnectionError(serverConfig.getServerName(), serverConfig, e);
        }

        @Override
        public void sslAlgorithmNotSupported(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig) {
            onHostConnectionError(name, serverConfig, null);
        }

        @Override
        public void sslPeerUnverified(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig,
                @NotNull SslHandshakeException e) {
            onHostConnectionError(name, serverConfig, e);
        }

        @Override
        public void sslCertificateIssue(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull SslException e) {
            onHostConnectionError(serverName, serverConfig, e);
        }

        @Override
        public void connectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull ConnectionException e) {
            onHostConnectionError(serverName, serverConfig, e);
        }

        @Override
        public void resourcesUnavailable(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                @NotNull ResourceException e) {
            onHostConnectionError(serverName, serverConfig, e);
        }

        public abstract void onHostConnectionError(@NotNull P4ServerName serverName,
                @Nullable ServerConfig serverConfig, @Nullable Exception e);
    }

    public static Listener send() {
        return getListener(TOPIC, DEFAULT_LISTENER);
    }

    public static void addListener(@NotNull MessageBusClient client, @NotNull Listener listener) {
        addTopicListener(client, TOPIC, listener);
    }
}
