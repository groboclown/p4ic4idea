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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.*;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.ui.alerts.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

/**
 * A client-aware connection to a Perforce server.
 */
class ClientExec {
    private static final Logger LOG = Logger.getInstance(ClientExec.class);
    private static final AllServerCount SERVER_COUNT = new AllServerCount();

    private final Object sync = new Object();

    private final ServerStatusController connectedController;
    private final ServerConfig config;

    @Nullable
    private final String clientName;

    @NotNull
    private final ConnectionHandler connectionHandler;

    private boolean disposed = false;

    @Nullable
    private AuthenticatedServer cachedServer;


    ClientExec(@NotNull ServerConfig config, @NotNull ServerStatusController connectedController,
            @Nullable String clientName)
            throws P4InvalidConfigException {
        this.connectedController = connectedController;
        this.config = config;
        this.clientName = clientName;
        this.connectionHandler = ConnectionHandler.getHandlerFor(config);
        connectionHandler.validateConfiguration(null, config);
    }


    @Nullable
    public String getClientName() {
        return clientName;
    }


    @NotNull
    public ServerConfig getServerConfig() {
        return config;
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


    /**
     *
     * @param project
     * @param config
     * @return
     * @throws IOException
     * @throws P4JavaException
     * @throws URISyntaxException
     * @throws P4LoginException
     * @deprecated this uses a static context, which means it must create
     *    a new server object for each call.  This should instead be moved
     *    into the ClientExec non-static.
     */
    @Deprecated
    static IServerInfo getServerInfo(@NotNull Project project, @NotNull ServerConfig config)
            throws IOException, P4JavaException, URISyntaxException, P4LoginException {
        ConnectionHandler connectionHandler = ConnectionHandler.getHandlerFor(config);

        final AuthenticatedServer server = connectTo(project, null, connectionHandler, config,
                getTempDir(project));
        try {
            try {
                return server.getServer().getServerInfo();
            } catch (P4JavaException e) {
                if (isPasswordProblem(e)) {
                    if (!server.authenticate()) {
                        throw new P4LoginException(project, config, e);
                    }
                    return server.getServer().getServerInfo();
                } else {
                    throw e;
                }
            }
        } finally {
            server.disconnect();
        }
    }


    /**
     *
     * @param project
     * @param config
     * @param statusController
     * @deprecated this call creates a new server object, which should instead
     *      only be done within a ClientExec instance (so that the connection can
     *      be cached).
     * @return
     */
    @Deprecated
    static boolean checkIfOnline(@NotNull Project project, @NotNull ServerConfig config,
            @NotNull ServerStatusController statusController) {
        Exception exception = null;
        boolean online = false;
        boolean needsAuthentication = false;
        try {
            final IServerInfo info = getServerInfo(project, config);
            if (info != null) {
                online = true;
            }
        } catch (IOException e) {
            exception = e;
        } catch (AccessException e) {
            needsAuthentication = true;
            exception = e;
        } catch (P4JavaException e) {
            exception = e;
        } catch (URISyntaxException e) {
            exception = e;
        } catch (P4LoginException e) {
            needsAuthentication = true;
            exception = e;
        }
        if (! online) {
            statusController.onDisconnected();
            final CriticalErrorHandler errorHandler;
            if (needsAuthentication) {
                errorHandler = new LoginFailedHandler(project, statusController, config, exception);
            } else {
                final P4DisconnectedException ex;
                if (exception == null) {
                    ex = new P4DisconnectedException();
                } else {
                    ex = new P4DisconnectedException(exception);
                }
                exception = ex;
                errorHandler = new DisconnectedHandler(project, statusController, ex);
            }
            AlertManager.getInstance().addCriticalError(errorHandler, exception);
        }
        return online;
    }


    /**
     *
     * @param project
     * @param config
     * @return
     * @throws IOException
     * @throws P4JavaException
     * @throws URISyntaxException
     * @throws P4LoginException
     * @deprecated this is called in a static context, which means that it creates a
     *      new server object, which should really only be done within a ClientExec.
     */
    @Deprecated
    static List<String> getClientNames(@Nullable Project project, @NotNull ServerConfig config)
            throws IOException, P4JavaException, URISyntaxException, P4LoginException {
        ConnectionHandler connectionHandler = ConnectionHandler.getHandlerFor(config);
        final AuthenticatedServer server = connectTo(project, null, connectionHandler, config,
                getTempDir(project));
        try {
            try {
                return getClientNames(server.getServer(), config.getUsername());
            } catch (P4JavaException e) {
                if (isPasswordProblem(e)) {
                    if (! server.authenticate()) {
                        throw new P4LoginException(project, config, e);
                    }
                    return getClientNames(server.getServer(), config.getUsername());
                }
                throw e;
            }
        } finally {
            server.disconnect();
        }
    }


    private static List<String> getClientNames(@NotNull IOptionsServer server, @NotNull String username)
            throws ConnectionException, AccessException, RequestException {
        final List<IClientSummary> clients =
                server.getClients(username, null, 0);
        List<String> ret = new ArrayList<String>(clients.size());
        for (IClientSummary client : clients) {
            if (client != null && client.getName() != null) {
                ret.add(client.getName());
            }
        }
        return ret;
    }


    <T> T runWithClient(@NotNull final Project project, @NotNull final WithClient<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                final AuthenticatedServer server = connectServer(project, getTempDir(project));

                // note: we're not caching the client
                final IClient client = loadClient(server.getServer());
                if (client == null) {
                    throw new ConfigException(P4Bundle.message("error.run-client.invalid-client", clientName));
                }

                // disconnect happens as a separate activity.
                return runner.run(server.getServer(), client,
                        new WithClientCount(config.getServiceName(), clientName));
            }
        });
    }

    <T> T runWithServer(@NotNull final Project project, @NotNull final WithServer<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, new P4Runner<T>() {
            @Override
            public T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception {
                // disconnect happens as a separate activity.
                return runner.run(connectServer(project, getTempDir(project)).getServer(),
                        new WithClientCount(config.getServiceName()));
            }
        }, 0);
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
                // cachedServer.disconnect();
                cachedServer = null;
            }
            if (cachedServer == null) {
                cachedServer = connectTo(project, clientName, connectionHandler, config, tempDir);
            }
        }

        return cachedServer;
    }


    @NotNull
    private static AuthenticatedServer connectTo(@Nullable Project project,
            @Nullable String clientName, @NotNull ConnectionHandler connectionHandler,
            @NotNull ServerConfig config, @NotNull File tempDir)
            throws P4JavaException, URISyntaxException {

        return new AuthenticatedServer(project, clientName, connectionHandler,
                config, tempDir);
    }


    @Nullable
    private IClient loadClient(@NotNull final IOptionsServer server) throws ConnectionException, AccessException, RequestException {
        if (clientName == null) {
            return null;
        }
        IClient client = server.getClient(clientName);
        if (client != null) {
            LOG.debug("Connected to client " + clientName);
            server.setCurrentClient(client);
        }
        return client;
    }


    private <T> T p4RunFor(@NotNull Project project, @NotNull P4Runner<T> runner)
            throws VcsException, CancellationException {
        return p4RunFor(project, runner, 0);
    }


    private <T> T p4RunFor(@NotNull Project project, @NotNull P4Runner<T> runner, int retryCount)
            throws VcsException, CancellationException {
        try {
            return p4RunWithSkippedPasswordCheck(project, runner, retryCount);
        } catch (P4JavaException e) {
            if (isPasswordProblem(e)) {
                try {
                    return onPasswordProblem(project, e, runner, retryCount);
                } catch (P4JavaException pwEx) {
                    if (isPasswordProblem(pwEx)) {
                        // just bad.
                        throw loginFailure(project, getServerConfig(), getServerConnectedController(), e);
                    }
                    throw new P4Exception(pwEx);
                }
            }
            // Probably a problem with the user unable to perform an operation because
            // they don't have explicit permissions to perform it (such as writing to
            // a part of the depot that's protected).
            throw new P4Exception(e);
        }
    }

    private <T> T p4RunWithSkippedPasswordCheck(@NotNull Project project, @NotNull P4Runner<T> runner, int retryCount)
            throws VcsException, CancellationException, P4JavaException {
        // Must check offline status
        if (connectedController.isWorkingOffline()) {
            // should never get to this point; the online/offline status should
            // have already been determined before entering this method.
            invalidateCache();
            throw new P4WorkingOfflineException();
        }

        try {
            T ret = runner.run();
            // Command executed without a problem; report the service connection as working.
            connectedController.onConnected();
            return ret;
        } catch (ClientError e) {
            // Happens due to charset translation (input stream reading) failure
            LOG.warn("ClientError in P4JavaApi", e);
            throw new P4ApiException(e);
        } catch (NullPointerError e) {
            // Happens due to bug in code (invalid API)
            LOG.warn("NullPointerException in P4JavaApi", e);
            throw new P4ApiException(e);
        } catch (ProtocolError e) {
            // client API expectations of server responses failed
            LOG.warn("ProtocolError in P4JavaApi", e);
            throw new P4ServerProtocolException(e);
        } catch (UnimplementedError e) {
            // server probably supports functions that the client API doesn't
            LOG.warn("Unimplemented API in P4JavaApi", e);
            throw new P4ApiException(e);
        } catch (P4JavaError e) {
            // some other client API issue
            LOG.warn("General error in P4JavaApi", e);
            throw new P4ApiException(e);
        } catch (PasswordAccessedWrongException e) {
            LOG.info("Could not get the password yet");
            // Don't explicitly tell the user about this; it will show up eventually.
            throw new P4LoginException(e);
        } catch (LoginRequiresPasswordException e) {
            LOG.info("No password known, and the server requires a password.");
            P4LoginException ex = new P4LoginException(e);
            // Don't explicitly tell the user about this; it will show up eventually.
            throw ex;
        } catch (AccessException e) {
            // Most probably a password problem.
            LOG.info("Problem accessing resources (password problem?) with " + cachedServer, e);
            // This is handled by the outside caller
            throw e;
        } catch (ConfigException e) {
            LOG.info("Problem with configuration", e);
            P4InvalidConfigException ex = new P4InvalidConfigException(e);
            configInvalid(project, ex);
            AlertManager.getInstance().addCriticalError(new ConfigurationProblemHandler(project, connectedController, e), ex);
            throw ex;
        } catch (ConnectionNotConnectedException e) {
            LOG.info("Wasn't connected", e);
            invalidateCache();
            if (retryCount > 1) {
                connectedController.onDisconnected();
                P4WorkingOfflineException ex = new P4WorkingOfflineException(e);
                AlertManager.getInstance()
                        .addCriticalError(new DisconnectedHandler(project, connectedController, ex), ex);
                throw ex;
            } else {
                return p4RunFor(project, runner, retryCount + 1);
            }
        } catch (TrustException e) {
            LOG.info("SSL trust problem", e);
            P4SSLFingerprintException ex = new P4SSLFingerprintException(config.getServerFingerprint(), e);
            configInvalid(project, ex);
            AlertManager.getInstance().addCriticalError(
                    new SSLFingerprintProblemHandler(project, connectedController, e),
                    ex);
            throw ex;
        } catch (ConnectionException e) {
            LOG.info("Connection problem", e);
            invalidateCache();

            if (isSSLFingerprintProblem(e)) {
                // incorrect or not set trust fingerprint
                P4SSLFingerprintException ex =
                       new P4SSLFingerprintException(config.getServerFingerprint(), e);
                configInvalid(project, ex);
                AlertManager.getInstance().addCriticalError(
                        new SSLFingerprintProblemHandler(project, connectedController, e),
                        ex);
                throw ex;
            }


            if (isSSLHandshakeProblem(e)) {
                if (isUnlimitedStrengthEncryptionInstalled()) {
                    // config not invalid
                    P4DisconnectedException ex = new P4DisconnectedException(e);
                    AlertManager.getInstance()
                            .addCriticalError(new DisconnectedHandler(project, connectedController, ex), ex);
                    throw ex;
                } else {
                    // SSL extensions are not installed, so config is invalid.
                    P4JavaSSLStrengthException ex = new P4JavaSSLStrengthException(e);
                    configInvalid(project, ex);
                    AlertManager.getInstance().addCriticalError(
                            new SSLKeyStrengthProblemHandler(project, connectedController, e),
                            ex);
                    throw ex;
                }
            }

            // Ask the user if it should be a real disconnect, or if we should
            // retry.
            if (retryCount > 1) {
                connectedController.onDisconnected();
                P4DisconnectedException ex = new P4DisconnectedException(e);
                AlertManager.getInstance().addCriticalError(new DisconnectedHandler(project, connectedController, ex), ex);
                throw ex;
            }
            return p4RunFor(project, runner, retryCount + 1);
        } catch (FileDecoderException e) {
            // Server -> client encoding problem
            LOG.info("File decoder problem", e);
            throw new P4FileException(e);
        } catch (FileEncoderException e) {
            // Client -> server encoding problem
            LOG.info("File encoder problem", e);
            throw new P4FileException(e);
        } catch (NoSuchObjectException e) {
            // Bad arguments to API
            LOG.info("No such object problem", e);
            throw new P4Exception(e);
        } catch (OptionsException e) {
            // Bug in plugin
            LOG.info("Input options problem", e);
            throw new P4Exception(e);
        } catch (RequestException e) {
            LOG.info("Request problem", e);

            // This is handled by the parent, as it's most
            // likely a password problem.  The other possibility
            // is the client API implementation is bad.
            throw e;
        } catch (ResourceException e) {
            // The ServerFactory doesn't have the resources available to create a
            // new connection to the server.
            LOG.info("Resource problem", e);
            connectedController.onDisconnected();
            throw new P4Exception(e);
        } catch (P4JavaException e) {
            LOG.info("General Perforce problem", e);
            throw new P4Exception(e);
        } catch (IOException e) {
            LOG.info("IO problem", e);
            throw new P4Exception(e);
        } catch (URISyntaxException e) {
            LOG.info("Invalid URI", e);
            P4InvalidConfigException ex = new P4InvalidConfigException(e);
            configInvalid(project, ex);
            AlertManager.getInstance().addCriticalError(new ConfigurationProblemHandler(project, connectedController, e), ex);
            throw ex;
        } catch (CancellationException e) {
            // A user-requested cancellation of the action.
            // no need to handle; it's part of the throw clause
            LOG.info("Cancelled", e);
            throw e;
        } catch (InterruptedException e) {
            // An API requested cancellation of the action.
            // Change to a cancel
            LOG.info("Cancelled", e);
            CancellationException ce = new CancellationException(e.getMessage());
            ce.initCause(e);
            throw ce;
        } catch (TimeoutException e) {
            // the equivalent of a cancel, because the limited time window
            // ran out.
            LOG.info("Timed out", e);
            CancellationException ce = new CancellationException(e.getMessage());
            ce.initCause(e);
            throw ce;
        } catch (VcsException e) {
            // Plugin code generated error
            throw e;
        } catch (ProcessCanceledException e) {
            CancellationException ce = new CancellationException(e.getMessage());
            ce.initCause(e);
            throw ce;
        } catch (Throwable t) {
            VcsExceptionUtil.alwaysThrown(t);
            if (t.getMessage() != null &&
                    t.getMessage().equals("Task was cancelled.")) {
                CancellationException ce = new CancellationException(t.getMessage());
                ce.initCause(t);
                throw ce;
            }
            LOG.warn("Unexpected exception", t);
            throw new P4Exception(t);
        }
    }

    private void configInvalid(@NotNull Project project, @NotNull VcsConnectionProblem e) {
        Events.handledConfigInvalid(project, new ManualP4Config(config, clientName), e);
        connectedController.onConfigInvalid();
    }

    private <T> T onPasswordProblem(@NotNull final Project project, @NotNull final P4JavaException e,
            @NotNull final P4Runner<T> runner, final int retryCount) throws VcsException, P4JavaException {
        if (e.getCause() != null && (e.getCause() instanceof PasswordAccessedWrongException)) {
            // not a login failure.
            throw e;
        }
        if (p4RunWithSkippedPasswordCheck(project, new P4Runner<Boolean>() {
            @Override
            public Boolean run()
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {
                LOG.info("Encountered password issue; attempting to reauthenticate " + cachedServer);
                return cachedServer.authenticate();
            }
        }, /* first attempt at this login attempt, so retry is 0 */0)) {
            return p4RunWithSkippedPasswordCheck(project, runner, retryCount + 1);
        }
        throw loginFailure(project, getServerConfig(), getServerConnectedController(), e);
    }


    static P4LoginException loginFailure(@NotNull Project project, @NotNull ServerConfig config,
            @NotNull ServerConnectedController controller, @NotNull final P4JavaException e) {
        LOG.info("Gave up on trying to login.  Showing critical error.");
        P4LoginException ex = new P4LoginException(project, config, e);
        AlertManager.getInstance().addCriticalError(
                new LoginFailedHandler(project, controller, config, e), ex);
        return ex;
    }


    private static boolean isPasswordProblem(@NotNull P4JavaException ex) {
        // TODO replace with error code checking

        if (ex instanceof RequestException) {
            RequestException rex = (RequestException) ex;
            return (rex.hasMessageFragment("Your session has expired, please login again.")
                    || rex.hasMessageFragment("Perforce password (P4PASSWD) invalid or unset."));
        }
        if (ex instanceof AccessException) {
            AccessException aex = (AccessException) ex;
            // see Server for a list of the authentication failure messages.
            return aex.hasMessageFragment("Perforce password (P4PASSWD)");
        }
        //if (ex instanceof LoginRequiresPasswordException) {
        //    LOG.info("No password specified, but one is needed", ex);
        //    return false;
        //}
        return false;
    }


    private boolean isSSLHandshakeProblem(@NotNull final ConnectionException e) {
        // This check isn't always right - it could be a fingerprint problem in disguise

        String message = e.getMessage();
        return (message != null &&
                message.contains("invalid SSL session"));
    }


    private boolean isUnlimitedStrengthEncryptionInstalled() {
        try {
            return Cipher.getMaxAllowedKeyLength("RC5") >= 256;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }


    private boolean isSSLFingerprintProblem(@NotNull final ConnectionException e) {
        // TODO replace with error code checking

        String message = e.getMessage();
        return message != null &&
                message.contains("The fingerprint for the public key sent to your client is");
    }

    @NotNull
    ServerConnectedController getServerConnectedController() {
        return connectedController;
    }


    interface P4Runner<T> {
        T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException, P4Exception;
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
        final Map<String, Map<String, Integer>> callCounts = new HashMap<String, Map<String, Integer>>();

        synchronized void invoke(@NotNull String operation, @NotNull String serverId, @NotNull String clientId) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("server " + serverId + "@" + clientId + " " + operation);
            }
            Map<String, Integer> clientCount = callCounts.get(serverId);
            if (clientCount == null) {
                clientCount = new HashMap<String, Integer>();
                callCounts.put(serverId, clientCount);
            }
            Integer count = clientCount.get(clientId);
            if (count == null) {
                count = 0;
            }
            clientCount.put(clientId, count + 1);
            if (count + 1 % 100 == 0) {
                LOG.info("Invocations against " + serverId + " " + clientId + " = " + (count + 1));
            }
        }
    }

    private static class WithClientCount implements ServerCount {
        private final String serverId;
        private final String clientId;

        private WithClientCount(final String serverId) {
            this(serverId, "");
        }

        private WithClientCount(final String serverId, final String clientId) {
            this.serverId = serverId;
            this.clientId = clientId;
        }

        @Override
        public void invoke(@NotNull final String operation) {
            SERVER_COUNT.invoke(operation, serverId, clientId);
        }
    }

}
