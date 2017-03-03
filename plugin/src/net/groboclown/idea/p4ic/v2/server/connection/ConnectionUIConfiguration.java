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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4LoginException;
import net.groboclown.idea.p4ic.server.exceptions.P4RetryAuthenticationException;
import net.groboclown.idea.p4ic.server.exceptions.P4SSLFingerprintException;
import net.groboclown.idea.p4ic.server.exceptions.P4UnknownLoginException;
import net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator;
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
        final Project project = clientConfig.getProject();
        try {
            final ErrorCollectorVisitorFactory errorCollectorVisitorFactory = new ErrorCollectorVisitorFactory();
            final ServerConnection connection = connectionManager
                    .getConnectionFor(project, clientConfig, requiresClient);
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
        for (ClientConfig source : sources) {
            // FIXME DEBUG
            LOG.info("Loading clients for " + ConfigPropertiesUtil.toProperties(source));
            ErrorCollectorVisitorFactory errorCollectorVisitorFactory = new ErrorCollectorVisitorFactory();
            try {
                // Bug #115: getting the clients should not require that a
                // client is present.
                final ServerConnection connection =
                        connectionManager.getConnectionFor(source.getProject(),
                                source,false);
                final ClientExec exec = connection.oneOffClientExec(errorCollectorVisitorFactory);
                try {
                    final List<String> clients = new P4Exec2(source.getProject(), exec).
                            getClientNames();
                    // FIXME DEBUG
                    LOG.info(" - loaded clients " + clients);
                    ret.put(source, new ClientResult(clients));
                } finally {
                    exec.dispose();
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
            for (Exception problem : errorCollectorVisitorFactory.problems) {
                ret.put(source, new ClientResult(problem));
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

        @NotNull
        @Override
        public ServerRunner.ErrorVisitor getVisitorFor(@NotNull Project project) {
            return new ErrorCollectorVisitor(problems);
        }
    }

    private static class ErrorCollectorVisitor implements ServerRunner.ErrorVisitor {
        private final List<Exception> problems;

        private ErrorCollectorVisitor(List<Exception> problems) {
            this.problems = problems;
        }

        @NotNull
        @Override
        public P4LoginException loginFailure(@NotNull P4JavaException e)
                throws VcsException, CancellationException {
            LOG.info("General login failure", e);
            problems.add(e);
            return new P4LoginException(e);
        }

        @Override
        public void loginFailure(@NotNull P4LoginException e)
                throws VcsException, CancellationException {
            LOG.info("General login failure", e);
            problems.add(e);
        }

        @Override
        public void loginRequiresPassword()
                throws VcsException, CancellationException {
            LOG.info("Login requires password, but user didn't provide it.");
            problems.add(new P4UnknownLoginException(P4Bundle.getString("error.requires.password")));
        }

        @Override
        public void retryAuthorizationFailure(P4RetryAuthenticationException e)
                throws VcsException, CancellationException {
            LOG.info("retried authentication too many times", e);
            problems.add(new P4UnknownLoginException(P4Bundle.getString("error.authorization.retry")));
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
        public void passwordUnnecessary(@NotNull ServerAuthenticator.AuthenticationStatus authenticationResult) {
            LOG.info("User provided a password, but it was unnecessary: " + authenticationResult);
            problems.add(new P4UnknownLoginException("error.password.unnecessary"));
        }
    }
}
