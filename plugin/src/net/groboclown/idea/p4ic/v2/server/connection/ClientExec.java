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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ClientConfigP4ProjectConfig;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4LoginException;
import net.groboclown.idea.p4ic.server.exceptions.P4RetryAuthenticationException;
import net.groboclown.idea.p4ic.server.exceptions.P4SSLFingerprintException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.authentication.PasswordManager;
import net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator;
import net.groboclown.idea.p4ic.v2.server.connection.ServerRunner.P4Runner;
import net.groboclown.idea.p4ic.v2.ui.alerts.DisconnectedHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.LoginFailedHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.RetryAuthenticationFailedHandler;
import net.groboclown.idea.p4ic.v2.ui.alerts.SSLFingerprintProblemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

/**
 * A client-aware connection to a Perforce server.
 */
public class ClientExec {
    private static final Logger LOG = Logger.getInstance(ClientExec.class);
    private static final AllServerCount SERVER_COUNT = new AllServerCount();

    private final Object sync = new Object();

    private final ServerStatusController connectedController;
    private final ClientConfig config;

    private boolean disposed = false;

    @Nullable
    private AuthenticatedServer cachedServer;

    @NotNull
    static ClientExec createFor(@NotNull ClientConfig config, @NotNull ServerStatusController statusController)
            throws P4InvalidConfigException {
        return new ClientExec(config, statusController);
    }


    private ClientExec(@NotNull ClientConfig config, @NotNull ServerStatusController connectedController) {
        this.connectedController = connectedController;
        this.config = config;
    }


    @Nullable
    public String getClientName() {
        return config.getClientName();
    }

    @NotNull
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return config.getServerConfig();
    }

    @NotNull
    public P4ServerName getServerName() {
        return config.getServerConfig().getServerName();
    }


    public void dispose() {
        LOG.info("Disposing ClientExec");

        // in the future, this may clean up open connections
        synchronized (sync) {
            if (! disposed) {
                disposed = true;
            }
            invalidateCache();
        }
    }


    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }


    <T> T runWithClient(@NotNull final Project project, @NotNull final WithClient<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                final AuthenticatedServer server = connectServer(project, getTempDir(project));

                // note: we're not caching the client
                IOptionsServer p4server = server.checkoutServer();
                try {
                    final IClient client = loadClient(p4server);
                    if (client == null) {
                        throw new ConfigException(
                                P4Bundle.message("error.run-client.invalid-client", getClientName()));
                    }

                    // disconnect happens as a separate activity.
                    return runner.run(p4server, client,
                            new WithClientCount(getServerName(), getClientName()));
                } finally {
                    server.checkinServer(p4server);
                }
            }
        });
    }

    <T> T runWithServer(@NotNull final Project project, @NotNull final WithServer<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                // disconnect happens as a separate activity.
                final AuthenticatedServer server = connectServer(project, getTempDir(project));
                IOptionsServer p4server = server.checkoutServer();
                try {
                    return runner.run(p4server,
                            new WithClientCount(getServerName()));
                } finally {
                    server.checkinServer(p4server);
                }
            }
        });
    }


    // Seems like a hack...
    @NotNull
    private static File getTempDir(@Nullable Project project) throws IOException {
        if (project == null) {
            return File.createTempFile("p4tempfile", "y");
        }
        return P4Vcs.getInstance(project).getTempDir();
    }


    private void invalidateCache() {
        synchronized (sync) {
            if (cachedServer != null) {
                try {
                    cachedServer.disconnect();
                } catch (ConnectionException e) {
                    LOG.debug("error on disconnect", e);
                } catch (AccessException e) {
                    LOG.debug("error on disconnect", e);
                } finally {
                    cachedServer = null;
                }
            }

        }
    }


    @NotNull
    private AuthenticatedServer connectServer(@Nullable Project project, @NotNull final File tempDir)
            throws P4JavaException, URISyntaxException {
        synchronized (sync) {
            if (disposed) {
                throw new ConnectionException(P4Bundle.message("error.p4exec.disposed"));
            }
            if (cachedServer != null && cachedServer.isDisconnected()) {
                cachedServer = null;
            }
            if (cachedServer == null) {
                cachedServer = connectTo(project, getClientConfig(), tempDir);
            }
        }

        return cachedServer;
    }


    @NotNull
    private static AuthenticatedServer connectTo(@Nullable Project project,
            @NotNull ClientConfig clientConfig, @NotNull File tempDir)
            throws P4JavaException, URISyntaxException {

        return new AuthenticatedServer(project, clientConfig, tempDir);
    }


    @Nullable
    private IClient loadClient(@NotNull final IOptionsServer server) throws ConnectionException, AccessException, RequestException {
        if (config.getClientName() == null) {
            return null;
        }
        IClient client = server.getClient(config.getClientName());
        if (client != null) {
            LOG.debug("Connected to client " + config.getClientName());
            server.setCurrentClient(client);
        }
        return client;
    }


    private <T> T p4RunFor(@NotNull Project project, @NotNull P4Runner<T> runner)
            throws VcsException, CancellationException {
        try {
            return ServerRunner.p4RunFor(runner,
                    new ServerRunnerConnection(project, getTempDir(project)),
                    new ServerRunnerErrorVisitor(project));
        } catch (IOException e) {
            throw new P4FileException(e);
        }
    }


    private class ServerRunnerConnection implements ServerRunner.Connection {
        private final Project project;
        private final File tempDir;

        ServerRunnerConnection(@NotNull final Project project, @NotNull final File tempDir) {
            this.project = project;
            this.tempDir = tempDir;
        }

        @Override
        public void disconnected() {
            invalidateCache();
        }

        @Override
        public boolean isWorkingOffline() {
            return connectedController.isWorkingOffline();
        }

        @Override
        public void onSuccessfulCall() {
            connectedController.onConnected();
        }

        @Override
        public ServerAuthenticator.AuthenticationStatus authenticate()
                throws InterruptedException, P4JavaException, URISyntaxException {
            return connectServer(project, tempDir).authenticate();
        }
    }

    private class ServerRunnerErrorVisitor implements ServerRunner.ErrorVisitor {
        private final Project project;

        ServerRunnerErrorVisitor(@NotNull final Project project) {
            this.project = project;
        }

        @NotNull
        @Override
        public P4LoginException loginFailure(@NotNull final P4JavaException e) throws VcsException, CancellationException {
            LOG.info("Incorrect login.", e);
            P4LoginException ex = new P4LoginException(project, config.getServerConfig(), e);
            AlertManager.getInstance().addCriticalError(
                    new LoginFailedHandler(project, connectedController, config.getServerConfig(), e), ex);
            return ex;
        }

        @Override
        public void loginFailure(@NotNull final P4LoginException e) throws VcsException, CancellationException {
            LOG.info("Gave up on trying to login.  Showing critical error.");
            AlertManager.getInstance().addCriticalError(
                    new LoginFailedHandler(project, connectedController, config.getServerConfig(), e), e);
        }

        @Override
        public void loginRequiresPassword() throws VcsException, CancellationException {
            Exception ex = new Exception("Login requires password");
            AlertManager.getInstance().addCriticalError(
                    new LoginFailedHandler(project, connectedController, config.getServerConfig(), ex), ex);
        }

        @Override
        public void retryAuthorizationFailure(final P4RetryAuthenticationException e)
                throws VcsException, CancellationException {
            LOG.warn("Incorrect handling of lost server authentication token", e);
            AlertManager.getInstance().addCriticalError(
                    new RetryAuthenticationFailedHandler(project, connectedController, config.getServerConfig(), e), e);
            throw e;
        }

        @Override
        public void disconnectFailure(final P4DisconnectedException e) throws VcsException, CancellationException {
            AlertManager.getInstance().addCriticalError(
                    new DisconnectedHandler(project, connectedController, e), e);
        }

        @Override
        public void configInvalid(final P4InvalidConfigException e) throws VcsException, CancellationException {
            Events.handledConfigInvalid(project, new ClientConfigP4ProjectConfig(config), e);
            connectedController.onConfigInvalid();
        }

        @NotNull
        @Override
        public P4SSLFingerprintException sslFingerprintError(final ConnectionException e) {
            P4SSLFingerprintException ex = new P4SSLFingerprintException(config.getServerConfig()
                    .getServerFingerprint(), e);
            AlertManager.getInstance().addCriticalError(
                    new SSLFingerprintProblemHandler(project, connectedController, e),
                    ex);
            return ex;
        }

        @Override
        public void passwordUnnecessary(@NotNull ServerAuthenticator.AuthenticationStatus authenticationResult) {
            try {
                PasswordManager.getInstance().forgetPassword(project, config.getServerConfig());
            } catch (PasswordStoreException e) {
                LOG.warn(e);
            }
            AlertManager.getInstance().addWarning(project,
                    P4Bundle.getString("connection.password.unnecessary.title"),
                    P4Bundle.message("connection.password.unnecessary.details",
                            config.getServerConfig().getServerName().getDisplayName()),
                    null, (FilePath[]) null);
        }
    }


    @NotNull
    ServerConnectedController getServerConnectedController() {
        return connectedController;
    }


    interface WithServer<T> {
        T run(@NotNull IOptionsServer server, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }


    interface WithClient<T> {
        T run(@NotNull IOptionsServer server, @NotNull IClient client, @NotNull ServerCount count) throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
    }



    interface ServerCount {
        void invoke(@NotNull String operation);
    }


    // TODO in the future, this can be a source for analytical information
    // regarding the activity of the plugin with the different servers, including
    // rate of invocations, number of invocations for different calls, and so on.
    private static class AllServerCount {
        final Map<P4ServerName, Map<String, Integer>> callCounts = new HashMap<P4ServerName, Map<String, Integer>>();

        synchronized void invoke(@NotNull String operation, @NotNull P4ServerName serverName, @NotNull String clientId) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("server " + serverName + "@" + clientId + " " + operation);
            }
            Map<String, Integer> clientCount = callCounts.get(serverName);
            if (clientCount == null) {
                clientCount = new HashMap<String, Integer>();
                callCounts.put(serverName, clientCount);
            }
            Integer count = clientCount.get(clientId);
            if (count == null) {
                count = 0;
            }
            clientCount.put(clientId, count + 1);
            if (count + 1 % 100 == 0) {
                LOG.info("Invocations against " + serverName + " " + clientId + " = " + (count + 1));
            }
        }
    }

    private static class WithClientCount implements ServerCount {
        private final P4ServerName serverName;
        private final String clientId;

        private WithClientCount(final P4ServerName serverName) {
            this(serverName, "");
        }

        private WithClientCount(final P4ServerName serverName, final String clientId) {
            this.serverName = serverName;
            this.clientId = clientId;
        }

        @Override
        public void invoke(@NotNull final String operation) {
            SERVER_COUNT.invoke(operation, serverName, clientId);
        }
    }
}
