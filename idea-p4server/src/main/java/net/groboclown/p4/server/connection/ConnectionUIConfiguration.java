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

package net.groboclown.p4.server.connection;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.SslHandshakeException;
import net.groboclown.idea.P4Bundle;
import net.groboclown.p4.server.config.ClientConfig;
import net.groboclown.p4.server.config.ConfigProblem;
import net.groboclown.p4.server.config.ConfigPropertiesUtil;
import net.groboclown.p4.server.config.ServerConfig;
import net.groboclown.p4.server.exceptions.P4DisconnectedException;
import net.groboclown.p4.server.exceptions.P4InvalidClientException;
import net.groboclown.p4.server.exceptions.P4InvalidConfigException;
import net.groboclown.p4.server.exceptions.P4JavaSSLStrengthException;
import net.groboclown.p4.server.exceptions.P4LoginException;
import net.groboclown.p4.server.exceptions.P4PasswordException;
import net.groboclown.p4.server.exceptions.P4SSLException;
import net.groboclown.p4.server.exceptions.P4SSLFingerprintException;
import net.groboclown.p4.server.exceptions.P4UnknownLoginException;
import net.groboclown.p4.server.exceptions.PasswordStoreException;
import net.groboclown.p4.server.authentication.PasswordManager;
import net.groboclown.p4.server.authentication.ServerAuthenticator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * Handles the server connection code when setting up an initial connection.
 */
public class ConnectionUIConfiguration {
    private static final Logger LOG = Logger.getInstance(ConnectionUIConfiguration.class);


    @Nullable
    public static ConfigProblem checkConnection(@NotNull ClientConfig clientConfig,
            @NotNull ServerConnectionManager connectionManager, boolean requiresClient) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking the configuration for " + ConfigPropertiesUtil.toProperties(clientConfig));
        }
        final Project project = clientConfig.getProject();
        try {
            final ServerConnection connection = connectionManager
                    .getConnectionFor(project, clientConfig, requiresClient);
            final ErrorCollectorVisitorFactory errorCollectorVisitorFactory = new ErrorCollectorVisitorFactory(
                    clientConfig.getServerConfig());

            final ClientExec exec = connection.oneOffClientExec(errorCollectorVisitorFactory);
            try {
                new P4Exec2(clientConfig.getProject(), exec).getServerInfo();
            } finally {
                exec.dispose();
            }
            if (errorCollectorVisitorFactory.problems.isEmpty()) {
                return null;
            }
            // Just choose the first error
            return new ConfigProblem(null, errorCollectorVisitorFactory.problems.get(0));
        } catch (P4InvalidClientException e) {
            return new ConfigProblem(null, e);
        } catch (P4InvalidConfigException e) {
            return new ConfigProblem(null, e);
        } catch (VcsException e) {
            return new ConfigProblem(null, e);
        } catch (RuntimeException e) {
            return new ConfigProblem(null, e);
        }
    }


    @NotNull
    public static Map<ClientConfig, ClientResult> getClients(
            @Nullable Collection<ClientConfig> sources,
            @NotNull ServerConnectionManager connectionManager) {
        final Map<ClientConfig, ClientResult> ret = new HashMap<ClientConfig, ClientResult>();
        if (sources == null) {
            return Collections.emptyMap();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding clients for configs " + sources);
        }
        for (ClientConfig source : sources) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading clients for " + ConfigPropertiesUtil.toProperties(source));
            }
            try {
                // Bug #115: getting the clients should not require that a
                // client is present.
                final ServerConnection connection =
                        connectionManager.getConnectionFor(source.getProject(),
                                source,false);
                final ErrorCollectorVisitorFactory errorCollectorVisitorFactory = new ErrorCollectorVisitorFactory(
                        source.getServerConfig());
                try {
                    final ClientExec exec = connection.oneOffClientExec(errorCollectorVisitorFactory);
                    try {
                        final List<String> clients = new P4Exec2(source.getProject(), exec).
                                getClientNames();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(" - loaded clients " + clients);
                        }
                        ret.put(source, new ClientResult(clients));
                    } finally {
                        exec.dispose();
                    }
                } finally {
                    for (Exception problem : errorCollectorVisitorFactory.problems) {
                        ret.put(source, new ClientResult(problem));
                    }
                }
            } catch (P4InvalidConfigException e) {
                LOG.info(e);
                ret.put(source, new ClientResult(e));
            } catch (P4InvalidClientException e) {
                LOG.info(e);
                ret.put(source, new ClientResult(e));
            } catch (VcsException e) {
                LOG.info(e);
                ret.put(source, new ClientResult(e));
            }
        }
        return ret;
    }



    public static class ClientResult {
        private final List<String> clientNames;
        private final Exception connectionProblem;

        private ClientResult(@NotNull List<String> clientNames) {
            this.clientNames = clientNames;
            this.connectionProblem = null;
        }

        private ClientResult(@NotNull Exception ex) {
            this.clientNames = Collections.emptyList();
            this.connectionProblem = ex;
        }

        public boolean isInvalid() {
            return clientNames == null;
        }

        @NotNull
        public List<String> getClientNames() {
            return clientNames;
        }

        @Nullable
        public Exception getConnectionProblem() {
            return connectionProblem;
        }
    }


    private static class ErrorCollectorVisitorFactory implements ServerRunner.ErrorVisitorFactory {
        private final List<Exception> problems = new ArrayList<Exception>();
        private final ServerConfig serverConfig;

        private ErrorCollectorVisitorFactory(ServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }

        @NotNull
        @Override
        public ServerRunner.ErrorVisitor getVisitorFor(@NotNull Project project) {
            return new ErrorCollectorVisitor(project, problems, serverConfig);
        }
    }

    private static class ErrorCollectorVisitor implements ServerRunner.ErrorVisitor {
        private final List<Exception> problems;
        private final Project project;
        private final ServerConfig serverConfig;

        private ErrorCollectorVisitor(@NotNull Project project, @NotNull List<Exception> problems,
                @NotNull ServerConfig serverConfig) {
            this.project = project;
            this.problems = problems;
            this.serverConfig = serverConfig;
        }

        @Override
        public void loginFailure(@NotNull P4LoginException e)
                throws VcsException, CancellationException {
            LOG.info("General login failure", e);
            problems.add(e);
        }

        @Override
        public void loginRequiresPassword(
                @NotNull P4PasswordException cause, ClientConfig clientConfig)
                throws VcsException, CancellationException {
            LOG.info("Login requires password");
            problems.add(cause);

            // This is a really weird situation.  The usual approach of using the
            // AlertManager won't work, because the specific connection is ephemeral.
            // Instead, we should have a password prompt, and the user can rerun the
            // refresh.  This is pulled out from the LoginFailedHandler class.

            // FIXME This just sits around and waits for the configuration dialog
            // to go away... sometimes.
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.info("Prompting user for the password");
                        PasswordManager.getInstance().askPassword(project, serverConfig);
                    } catch (PasswordStoreException e) {
                        LOG.warn(e);
                        problems.add(e);
                        AlertManager.getInstance().addWarning(project,
                                P4Bundle.message("password.store.error.title"),
                                P4Bundle.message("password.store.error"),
                                e, new FilePath[0]);
                    }
                }
            });
        }

        @Override
        public void disconnectFailure(P4DisconnectedException e)
                throws VcsException, CancellationException {
            LOG.info("disconnected", e);
            problems.add(e);
        }

        @Override
        public void configInvalid(P4InvalidConfigException e)
                throws VcsException, CancellationException {
            LOG.info("config invalid", e);
            problems.add(e);
        }

        @NotNull
        @Override
        public P4SSLFingerprintException sslFingerprintError(ConnectionException e) {
            LOG.info("ssl fingerprint error", e);
            problems.add(e);
            return new P4SSLFingerprintException(null, e);
        }

        @Override
        public P4SSLException sslHandshakeError(SslHandshakeException e) {
            LOG.info("ssl handshake error", e);
            problems.add(e);
            return new P4SSLException(e);
        }

        @Override
        public P4SSLException sslKeyStrengthError(SslHandshakeException e) {
            LOG.info("ssl key strength library error", e);
            problems.add(e);
            return new P4JavaSSLStrengthException(e);
        }

        @Override
        public void passwordUnnecessary(@NotNull ServerAuthenticator.AuthenticationStatus authenticationResult) {
            LOG.info("User provided a password, but it was unnecessary: " + authenticationResult);
            problems.add(new P4UnknownLoginException("error.password.unnecessary"));
        }
    }
}
