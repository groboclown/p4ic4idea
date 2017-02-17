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
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.server.P4OptionsServerConnectionFactory;
import net.groboclown.idea.p4ic.server.exceptions.LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
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
 */
class AuthenticatedServer {
    private static final Logger LOG = Logger.getInstance(AuthenticatedServer.class);
    private static final Logger P4LOG = Logger.getInstance("p4");

    private static final AtomicInteger serverCount = new AtomicInteger(0);
    private static final AtomicInteger activeConnectionCount = new AtomicInteger(0);

    // At one point, the CONNECT_LOCK was a static variable.  However, there's
    // no need to make this anything other than a class variable, especially
    // now with the enforced logic of the server object checkout concept.
    // If future issues with the connections seem to pop up again, this
    // may be a good place to initially look for solutions (change it
    // back to static).  Note that by being static, there are certain
    // situations (such as startup time) where the lock can be in
    // contention and cause a possible deadlock.  New usage of the
    // lock (tryLock with a timer) should help keep this from being an issue,
    // though.
    private final Lock CONNECT_LOCK = new ReentrantLock();
    private static final long CONNECT_LOCK_TIMEOUT_MILLIS = 30 * 1000L;

    private final ServerAuthenticator authenticator = new ServerAuthenticator();
    private final ClientConfig config;
    private final int serverInstance = serverCount.getAndIncrement();
    private final File tempDir;

    @Nullable
    private final Project project;

    @Nullable
    private IOptionsServer server;

    private Thread checkedOutBy;

    private boolean hasValidatedAuthentication = false;
    private ServerAuthenticator.AuthenticationStatus invalidLoginStatus = null;

    // metrics for debugging
    private int loginFailedCount = 0;
    private int connectedCount = 0;
    private int disconnectedCount = 0;


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
    synchronized IOptionsServer checkoutServer()
            throws InterruptedException, P4JavaException, URISyntaxException {
        if (invalidLoginStatus != null) {
            if (invalidLoginStatus.getProblem() != null) {
                if (invalidLoginStatus.getProblem().getP4JavaException() != null) {
                    throw remakeException(invalidLoginStatus.getProblem().getP4JavaException());
                }
                throw new P4JavaException("Server Connection invalid", invalidLoginStatus.getProblem());
            }
            throw new P4JavaException("Server Connection invalid");
        }
        if (checkedOutBy != null) {
            throw new P4JavaException("P4ServerName object already checked out by " + checkedOutBy);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking out server " + this + " in " + Thread.currentThread());
        }

        if (server == null || ! server.isConnected()) {
            reconnect();
        }

        // This is to prevent lots of extra calls to the server for simple
        // validate-if-authenticated checks.  This might be a source for bugs, though.
        if (! hasValidatedAuthentication) {
            authenticate();
        }

        checkedOutBy = Thread.currentThread();
        return server;
    }

    synchronized void checkinServer(@NotNull IOptionsServer server) throws P4JavaException {
        if (checkedOutBy != Thread.currentThread()) {
            throw new P4JavaException("P4ServerName object not checked out by current thread (current thread: " +
                Thread.currentThread() + "; checked out by " + checkedOutBy + ")");
        }
        if (this.server != server) {
            throw new P4JavaException("Incorrect server instance check-in");
        }
        checkedOutBy = null;
        if (UserProjectPreferences.getReconnectWithEachRequest(project)) {
            // Note that this isn't going to be an absolute reconnect with
            // each request, but a general one.  One or more server requests
            // will actually be associated with this server object, but they
            // should all run within the same small time frame.

            disconnect();
        }
    }

    /**
     *
     * @return authentication result
     * @throws P4JavaException on authentication problem
     */
    ServerAuthenticator.AuthenticationStatus authenticate()
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

        if (invalidLoginStatus != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Previous login attempts failed.  Assuming authentication is invalid for " + this);
            }
            return invalidLoginStatus;
        }

        ServerAuthenticator.AuthenticationStatus status = authenticator.discoverAuthenticationStatus(server);
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
                reconnect();
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


        if (status.isPasswordRequired()) {
            // FIXME there is a situation where this can be triggered when the
            // password has been entered, but hasn't been used.  That situation
            // happens wit the "Perforce password (%'P4PASSWD'%) invalid or unset." error.
            // Need to fix the status generator to have the right status with that
            // error message.

            if (LOG.isDebugEnabled()) {
                LOG.debug("User must enter the password for " + this);
            }
            hasValidatedAuthentication = false;
            return status;
        }

        if (status.isAuthenticated()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection seems authenticated for " + this);
            }
            hasValidatedAuthentication = true;
            return status;
        }

        // Attempt a login before increasing the login failed count.

        if (! hasValidatedAuthentication) {
            // we have never been authenticated by the server,
            // and this situation means that we still aren't
            // even after a forced authentication.  So, we'll
            // report this connection as being unauthenticated.
            LOG.debug("Assuming actual authentication issue for " + this);

            // This assumption that the authentication is invalid is too quick to judge.
            // Do not return not-authorized yet
        }

        // For some reason that needs to be figured out,
        // the server can report the connection as unauthorized.
        // We'll give it up to 3 retries with a slight delay
        // in between, in cas ethe error comes from too frequent
        // requests.

        boolean first = true;

        for (int i = 0; i < getMaxAuthenticationRetries(); i++) {
            if (first) {
                first = false;
            } else {
                // Try connecting again, first reopening the connection.  Just in
                // case the state is a bit wacky.
                try {
                    reconnect();
                } catch (URISyntaxException e) {
                    return authenticator.createStatusFor(e);
                }
            }

            // Do not force a password prompt; let that be at
            // purview of the caller.
            OneUseString password =
                    PasswordManager.getInstance().getPassword(project, config.getServerConfig(), false);
            if (status.isPasswordRequired() && password.isNullValue()) {
                // We don't have a password, but one is required.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Don't have a password, but one is required for " + this);
                }
                return status;
            }

            // FIXME this may only be necessary for "session timed out"
            // Try explicit login.
            final ServerAuthenticator.AuthenticationStatus previousStatus = status;
            status = password.use(new OneUseString.WithString<ServerAuthenticator.AuthenticationStatus>() {
                @Override
                public ServerAuthenticator.AuthenticationStatus with(@Nullable char[] passwd) {
                    final String knownPassword =
                            passwd == null ? null : new String(passwd);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Authenticating using known password for " + this);
                    }
                    return authenticator.authenticate(server, config.getServerConfig(),
                            previousStatus, knownPassword);
                }
            });

            if (status.isPasswordUnnecessary()) {
                // This kind of status means that the user is probably authenticated,
                // so it needs to be handled before the isAuthenticated check.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Forgetting user password, because the user doesn't have one for the Perforce account.  " +
                            this);
                }
                PasswordManager.getInstance().forgetPassword(project, config.getServerConfig());
            }
            if (status.isAuthenticated()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authorization successful after " + i +
                            " unauthorized connections (but a valid one was seen earlier) for " +
                            this);
                }
                hasValidatedAuthentication = true;
                return status;
            }
            if (status.isPasswordRequired()) {
                // Our password is wrong, perhaps.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("User must enter the password for " + this);
                }
                hasValidatedAuthentication = false;
                loginFailedCount++;
                return status;
            }
            if (status.isSessionExpired()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authorization failed due to session expiration for " + this);
                }
                // Attempted to login, it failed with "session expired",
                // so this means that the login didn't work.
                hasValidatedAuthentication = false;
                loginFailedCount++;
                return status;
            }
            if (status.isNotLoggedIn()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authorization failed, and the connection still needs a login for " + this);
                }
                // Attempted to login, but the server responded with the user needs to be
                // logged in.  Just abort.
                hasValidatedAuthentication = false;
                loginFailedCount++;
                return status;
            }

            LOG.debug("Login failed.  Trying again.");
            loginFailedCount++;
        }
        // Don't keep trying the same bad config.
        invalidLoginStatus = status;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failed authentication after " + getMaxAuthenticationRetries() +
                    " unauthorized connections (but a valid one was seen earlier) for " +
                    this);
        }
        return status;
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();

        super.finalize();
    }


    private void reconnect()
            throws P4JavaException, URISyntaxException, InterruptedException {
        if (checkedOutBy != null) {
            throw new P4JavaException("P4ServerName instance already checked out by " + checkedOutBy);
        }
        final OneUseString password =
                PasswordManager.getInstance().getPassword(project, config.getServerConfig(), false);
        withConnectionLock(new WithConnectionLock<Void>() {
            @Override
            public Void call() throws P4JavaException, URISyntaxException {
                disconnect();
                try {
                    server = password.use(new OneUseString.WithStringThrows<IOptionsServer, Exception>() {
                        @Override
                        public IOptionsServer with(@Nullable char[] passwd)
                                throws Exception {
                            final String knownPassword =
                                    passwd == null ? null : new String(passwd);
                            return reconnect(config, tempDir, knownPassword);
                        }
                    });
                } catch (P4JavaException e) {
                    throw e;
                } catch (URISyntaxException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new P4JavaException(e);
                }

                connectedCount++;
                return null;
            }
        });
    }

    private IOptionsServer reconnect(@NotNull final ClientConfig config, @NotNull final File tempDir,
            @Nullable final String knownPassword)
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

        // The login command can cause some really strange
        // issues, such as "password invalid" when using
        // a valid password.  This
        try {
            return withConnectionLock(new WithConnectionLock<IOptionsServer>() {
                @Override
                public IOptionsServer call() throws P4JavaException, URISyntaxException {
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
                    // Will need to re-authenticate, because we're re-connecting.
                    hasValidatedAuthentication = false;
                    server.connect();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("calling activeConnectionCount incrementAndGet");
                    }
                    activeConnectionCount.incrementAndGet();

                    // Authentication will still be done, but
                    // outside this call.

                    return server;
                }
            });
        } catch (InterruptedException e) {
            throw new P4JavaException(e);
        }
    }


    /**
     * Ensures that the CONNECT_LOCK is correctly used so it doesn't cause deadlocks.
     *
     * @param callable locked object
     * @param <T> return type
     * @return callable's return
     * @throws InterruptedException thread interrupted
     * @throws P4JavaException p4 communication error
     * @throws URISyntaxException server uri wrong
     */
    private <T> T withConnectionLock(WithConnectionLock<T> callable)
            throws InterruptedException, P4JavaException, URISyntaxException {
        final boolean locked = CONNECT_LOCK.tryLock(CONNECT_LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        if (! locked) {
            // no need for finally stuff here.
            throw new InterruptedException("Could not acquire connection lock in time.");
        }
        try {
            return callable.call();
        } finally {
            CONNECT_LOCK.unlock();
        }
    }


    private interface WithConnectionLock<T> {
        T call() throws P4JavaException, URISyntaxException;
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
                ", invalidLogin " + invalidLoginStatus +
                ", validatedLogin? " + hasValidatedAuthentication +
                ", clientId: " + config.getClientId() +
                ")";
    }

    @NotNull
    private static P4JavaException remakeException(@Nullable final P4JavaException authenticationException) {
        if (authenticationException == null) {
            return new P4JavaException("invalid login credentials");
        }
        // TODO This is an implicit tight coupling with ServerRunner.  However, we want to
        // throw a fresh exception so we record the real source of the error and the
        // actual where-we-are-throwing-it-now location.
        if (authenticationException instanceof PasswordAccessedWrongException) {
            final PasswordAccessedWrongException ret = new PasswordAccessedWrongException();
            ret.initCause(authenticationException);
            return ret;
        }
        if (authenticationException instanceof LoginRequiresPasswordException) {
            return new LoginRequiresPasswordException(
                    (AccessException) authenticationException.getCause());
        }
        if (authenticationException instanceof AccessException) {
            return new AccessException((AccessException) authenticationException);
        }
        if (authenticationException instanceof ConfigException) {
            return new ConfigException(authenticationException);
        }
        if (authenticationException instanceof ConnectionNotConnectedException) {
            return new ConnectionNotConnectedException(authenticationException);
        }
        if (authenticationException instanceof TrustException) {
            return new TrustException((TrustException) authenticationException);
        }
        if (authenticationException instanceof ConnectionException) {
            return new ConnectionException(authenticationException);
        }
        if (authenticationException instanceof RequestException) {
            return new RequestException(
                    ((RequestException) authenticationException).getServerMessage(),
                    authenticationException);
        }

        // generic p4 java exception.  The other kinds are sort of ignored.
        return new P4JavaException(authenticationException);
    }


    private int getMaxAuthenticationRetries() {
        return UserProjectPreferences.getMaxAuthenticationRetries(project);
    }

}
