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
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.server.P4OptionsServerConnectionFactory;
import net.groboclown.idea.p4ic.v2.server.authentication.PasswordManager;
import net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles the authentication and connection of the server object.
 * Access should still be monitored through the {@link ClientExec}
 * class.
 * <p>
 * The new, simplified approach is to always disconnect when the
 * server is checked in, and always reconnect when it is checked out.
 * All authentication is handled by {@link ServerAuthenticator}.
 */
class AuthenticatedServer {
    private static final Logger LOG = Logger.getInstance(AuthenticatedServer.class);
    private static final Logger P4LOG = Logger.getInstance("p4");

    private static final AtomicInteger serverCount = new AtomicInteger(0);
    private static final AtomicInteger activeConnectionCount = new AtomicInteger(0);

    // At one point, the connectLock was a static variable.  However, there's
    // no need to make this anything other than an instance variable, especially
    // now with the enforced logic of the server object checkout concept.
    // If future issues with the connections seem to pop up again, this
    // may be a good place to initially look for solutions (change it
    // back to static).  Note that by being static, there are certain
    // situations (such as startup time) where the lock can be in
    // contention and cause a possible deadlock.  New usage of the
    // lock (tryLock with a timer) should help keep this from being an issue,
    // though.
    private final Lock connectLock = new ReentrantLock();

    private final ServerAuthenticator authenticator = new ServerAuthenticator();
    private final ClientConfig config;
    private final int serverInstance = serverCount.getAndIncrement();
    private final File tempDir;

    @Nullable
    private final Project project;

    @Nullable
    private IOptionsServer server;

    private volatile Thread checkedOutBy;
    private Exception checkedOutStack;

    // metrics for debugging
    private int loginFailedCount = 0;
    private int connectedCount = 0;
    private int disconnectedCount = 0;

    // Keep track of whether we require a password for this connection or not.
    // If we require a password, but we can't get one (because the user hasn't
    // told us, or it wasn't saved), then we shouldn't be sending requests to
    // the server that we know will fail.
    private boolean requiresPassword = false;


    public static class ServerConnection {
        private final Project project;
        private final ClientConfig clientConfig;
        private final IOptionsServer server;
        private final ServerAuthenticator.AuthenticationStatus authStatus;

        public ServerConnection(@NotNull Project project, @NotNull ClientConfig config,
                @Nullable IOptionsServer server,
                @NotNull ServerAuthenticator.AuthenticationStatus authStatus) {
            this.project = project;
            this.clientConfig = config;
            this.server = server;
            this.authStatus = authStatus;
            if (authStatus.isAuthenticated() && server == null) {
                throw new IllegalStateException("authenticated status, null server");
            }
        }

        @NotNull
        public ClientConfig getClientConfig() {
            return clientConfig;
        }

        @Nullable
        public IOptionsServer getServer() {
            return server;
        }

        @NotNull
        ServerAuthenticator.AuthenticationStatus getAuthStatus() {
            return authStatus;
        }

        @NotNull
        public Project getProject() {
            return project;
        }
    }


    AuthenticatedServer(@Nullable Project project,
            @NotNull ClientConfig clientConfig, @NotNull File tempDir)
            throws P4JavaException, URISyntaxException {
        this.project = project;
        this.config = clientConfig;
        this.tempDir = tempDir;
        this.server = null;
    }


    boolean isDisconnected() {
        return server == null || ! server.isConnected();
    }


    void disconnect() throws ConnectionException, AccessException {
        if (server != null && server.isConnected()) {
            disconnectedCount++;
            activeConnectionCount.decrementAndGet();
            server.disconnect();
        }
    }

    @NotNull
    ServerConnection checkoutServer()
            throws InterruptedException, P4JavaException, URISyntaxException {
        if (! connectLock.tryLock(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)) {
            throw new InterruptedException();
        }
        if (this.project == null) {
            throw new IllegalStateException("Project not set");
        }
        final IOptionsServer retServer;
        final ServerAuthenticator.AuthenticationStatus retAuthStatus;
        try {
            if (checkedOutBy != null) {
                throw new P4JavaException("P4ServerName object already checked out by " + checkedOutBy,
                        checkedOutStack);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking out server " + this + " in " + Thread.currentThread());
            }

            retServer = reconnect(false);
            retAuthStatus = authenticate();

            // ONLY set the checked-out-by state if the reconnect and authenticate
            // worked.  If they threw exceptions, then the server isn't checked out.

            checkedOutBy = Thread.currentThread();
            checkedOutStack = new Exception();
            checkedOutStack.fillInStackTrace();
        } finally {
            connectLock.unlock();
        }
        return new ServerConnection(project, config, retServer, retAuthStatus);
    }

    void checkinServer(@NotNull IOptionsServer server) throws P4JavaException, InterruptedException {
        if (! connectLock.tryLock(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)) {
            throw new InterruptedException();
        }
        try {
            if (checkedOutBy != Thread.currentThread()) {
                // We'll allow it for now.  This is indicative that someone is doing something wrong with the
                // server; it should be handled within the same thread.
                LOG.error(
                        new P4JavaException("P4ServerName object not checked out by current thread (current thread: " +
                                Thread.currentThread() + "; checked out by " + checkedOutBy + ")", checkedOutStack));
            }
            if (this.server != server) {
                throw new P4JavaException("Incorrect server instance check-in");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking in server " + this + " in " + Thread.currentThread());
            }
            if (UserProjectPreferences.getReconnectWithEachRequest(project)) {
                disconnect();
            }
            checkedOutBy = null;
            checkedOutStack = null;
        } finally {
            connectLock.unlock();
        }
    }

    /**
     *
     * @return authentication result
     * @throws P4JavaException on authentication problem
     */
    private ServerAuthenticator.AuthenticationStatus authenticate()
            throws InterruptedException, P4JavaException {
        if (project != null && project.isDisposed()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't authenticate: Project disposed for " + this);
            }
            return ServerAuthenticator.DISPOSED;
        }
        if (server == null) {
            // The only place where this would matter is with the call to
            // ClientExec.ServerRunnerConnection.authenticate.  Even that
            // is called only when an unauthorized exception is called.
            // Other than that, this should always assume that it's connected.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can't authenticate: AuthenticatedServer is disposed for " + this);
            }
            return ServerAuthenticator.DISPOSED;
        }


        // Do not force a password prompt; let that be at
        // purview of the caller.
        LOG.debug("A password may be required.  Getting it.");
        OneUseString password =
                PasswordManager.getInstance().getPassword(project, config.getServerConfig(), false);
        if (LOG.isDebugEnabled() && password.isNullValue()) {
            LOG.debug(" - No password known");
        }
        if (requiresPassword && password.isNullValue()) {
            // Early quit - we don't need to keep pounding the server with unauthorized requests.
            LOG.debug("Password isn't known, but we know that we need the password, so early quit.");
            return ServerAuthenticator.REQUIRES_PASSWORD;
        }
        ServerAuthenticator.AuthenticationStatus status =
                password.use(new OneUseString.WithString<ServerAuthenticator.AuthenticationStatus>() {
                     @Override
                     public ServerAuthenticator.AuthenticationStatus with(@Nullable char[] passwd) {
                         final String knownPassword =
                                 passwd == null ? null : new String(passwd);
                         if (LOG.isDebugEnabled()) {
                             LOG.debug("Attempting to log in with config "
                                     + ConfigPropertiesUtil.toProperties(config)
                                     + "; has password? " + (knownPassword != null));
                         }
                         return authenticator.initialLogin(server, config.getServerConfig(), knownPassword);
                     }
                 });

        if (status.isAuthenticated()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Server seems authenticated this " + this);
            }
            return status;
        }
        if (status.isClientSetupProblem()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Server has client setup problems for " + this);
            }
            return status;
        }
        if (status.isNotConnected()) {
            // Could not connect.  Attempt to reconnect and retry the
            // authentication.  If it still fails to connect, then
            // report a problem (it may be a wrong port, or the server
            // could be down).
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not connected to server; reconnecting for " + this);
            }
            try {
                reconnect(true);
            } catch (URISyntaxException e) {
                return authenticator.createStatusFor(e);
            }
            status = authenticator.discoverAuthenticationStatus(server);
            if (status.isAuthenticated()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reconnected and seems authenticated for " + this);
                }
                return status;
            }
            if (status.isNotConnected()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reconnected and still not connected for " + this);
                }
                return status;
            }
            if (status.isClientSetupProblem()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reconnected and client setup problem for " + this);
                }
                return status;
            }
            // Fall through to continue check.
        }


        if (status.isAuthenticated()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection seems authenticated for " + this);
            }
            return status;
        }

        if (status.isPasswordUnnecessary()) {
            // This kind of status means that the user is probably authenticated,
            // so it needs to be handled before the isAuthenticated check.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Forgetting user password, because the user doesn't have one for the Perforce account.  " +
                        this);
            }
            PasswordManager.getInstance().forgetPassword(project, config.getServerConfig());

            // Fall through
        }

        if (status.isPasswordRequired()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User must enter the password for " + this);
            }
            // Mark this connection as needing a password, for future call optimization.
            requiresPassword = true;
            return status;
        }

        if (status.isPasswordInvalid()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Forgetting user password, because it's invalid.  " + this);
            }
            PasswordManager.getInstance().forgetPassword(project, config.getServerConfig());
            loginFailedCount++;
            return status;
        }

        if (status.isPasswordRequired()) {
            // Our password is wrong, perhaps.
            if (LOG.isDebugEnabled()) {
                LOG.debug("User must enter the password for " + this);
            }
            loginFailedCount++;
            return status;
        }
        if (status.isSessionExpired()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authorization failed due to session expiration for " + this);
            }
            // Attempted to login, it failed with "session expired",
            // so this means that the login didn't work.
            loginFailedCount++;
            return status;
        }
        if (status.isNotLoggedIn()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authorization failed, and the connection still needs a login for " + this);
            }
            // Attempted to login, but the server responded with the user needs to be
            // logged in.  Just abort.
            loginFailedCount++;
            return status;
        }

        loginFailedCount++;

        // Don't keep trying the same bad config.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failed authentication (but a valid one was seen earlier) for " + this);
        }
        return status;
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();

        super.finalize();
    }


    private IOptionsServer reconnect(boolean forceReconnect)
            throws P4JavaException, URISyntaxException, InterruptedException {
        if (checkedOutBy != null) {
            throw new P4JavaException("P4ServerName instance already checked out by " + checkedOutBy, checkedOutStack);
        }

        if (forceReconnect || server == null || !server.isConnected() ||
                UserProjectPreferences.getReconnectWithEachRequest(project)) {
            final OneUseString password =
                    PasswordManager.getInstance().getPassword(project, config.getServerConfig(), false);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetched the password.  Has it? " + ! password.isNullValue());
            }
            disconnect();
            try {
                server = reconnect(config, tempDir);
            } catch (P4JavaException e) {
                throw e;
            } catch (URISyntaxException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new P4JavaException(e);
            }
        }

        connectedCount++;
        return server;
    }


    private IOptionsServer reconnect(@NotNull final ClientConfig config, @NotNull final File tempDir)
            throws P4JavaException, URISyntaxException {
        // Setup logging
        if (Log.getLogCallback() == null) {
            Log.setLogCallback(new ILogCallback() {
                // errors are pushed up to the normal logging mechanisms,
                // so no need to mark it as error here.

                @Override
                public void internalError(final String errorString) {
                    P4LOG.warn("p4java error: " + errorString);
                }

                @Override
                public void internalException(final Throwable thr) {
                    P4LOG.info("p4java error", thr);
                }

                @Override
                public void internalWarn(final String warnString) {
                    P4LOG.info("p4java warning: " + warnString);
                }

                @Override
                public void internalInfo(final String infoString) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java info: " + infoString);
                    }
                }

                @Override
                public void internalStats(final String statsString) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java stats: " + statsString);
                    }
                }

                @Override
                public void internalTrace(final LogTraceLevel traceLevel, final String traceMessage) {
                    if (P4LOG.isDebugEnabled()) {
                        P4LOG.debug("p4java trace: " + traceMessage);
                    }
                }

                @Override
                public LogTraceLevel getTraceLevel() {
                    return P4LOG.isDebugEnabled() ? LogTraceLevel.ALL : LogTraceLevel.FINE;
                }
            });
        }

        // Use the ConnectionHandler so that mock objects can work better
        if (LOG.isDebugEnabled()) {
            LOG.debug("calling connectionHandler.getOptionsServer");
        }
        final IOptionsServer server =
                P4OptionsServerConnectionFactory.getInstance().createConnection(config, tempDir);

        // These cause issues.
        //server.registerCallback(new LoggingCommandCallback());
        //server.registerProgressCallback(new LoggingProgressCallback());

        if (LOG.isDebugEnabled()) {
            LOG.debug("calling connect");
        }
        server.connect();
        if (LOG.isDebugEnabled()) {
            LOG.debug("calling activeConnectionCount incrementAndGet");
        }
        activeConnectionCount.incrementAndGet();

        return server;
    }


    @Override
    public boolean equals(Object o) {
        return o == this ||
                !(o == null ||
                    !(o instanceof AuthenticatedServer)) &&
                        // note: identity equality
                        serverInstance == ((AuthenticatedServer) o).serverInstance;
    }

    @Override
    public int hashCode() {
        return serverInstance;
    }

    @Override
    public String toString() {
        return "P4ServerName" + serverInstance +
                " (loginFailed# " + loginFailedCount +
                ", connected# " + connectedCount +
                ", disconnected# " + disconnectedCount +
                ", clientId: " + config.getClientId() +
                ")";
    }
}
