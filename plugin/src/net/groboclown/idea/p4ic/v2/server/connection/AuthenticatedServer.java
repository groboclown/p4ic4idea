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
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;
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
    enum AuthenticationResult {
        AUTHENTICATED,
        INVALID_LOGIN,
        RELOGIN_FAILED,
        NOT_CONNECTED
    }


    private static final Logger LOG = Logger.getInstance(AuthenticatedServer.class);
    private static final Logger P4LOG = Logger.getInstance("p4");

    private static final AtomicInteger serverCount = new AtomicInteger(0);
    private static final AtomicInteger activeConnectionCount = new AtomicInteger(0);
    private static final int RECONNECTION_WAIT_MILLIS = 10;

    private final Lock CONNECT_LOCK = new ReentrantLock();
    private static final long CONNECT_LOCK_TIMEOUT_MILLIS = 30 * 1000L;

    private final ConnectionHandler connectionHandler;
    private final ServerConfig config;
    private final int serverInstance = serverCount.getAndIncrement();
    private final String clientName;
    private final File tempDir;

    @Nullable
    private final Project project;

    @NotNull
    private IOptionsServer server;

    private Thread checkedOutBy;

    private P4JavaException authenticationException = null;
    private boolean hasValidatedAuthentication = false;
    private boolean isInvalidLogin = false;

    // metrics for debugging
    private int loginFailedCount = 0;
    private int forcedAuthenticationCount = 0;
    private int connectedCount = 0;
    private int disconnectedCount = 0;



    AuthenticatedServer(@Nullable Project project,
            @Nullable String clientName, @NotNull ConnectionHandler connectionHandler,
            @NotNull ServerConfig config, @NotNull File tempDir)
            throws P4JavaException, URISyntaxException {
        this.project = project;
        this.connectionHandler = connectionHandler;
        this.config = config;
        this.clientName = clientName;
        this.tempDir = tempDir;
        this.server = reconnect(project, clientName, connectionHandler,
                config, tempDir, serverInstance);

        connectedCount++;
    }


    boolean isDisconnected() {
        return ! server.isConnected();
    }


    void disconnect() throws ConnectionException, AccessException {
        if (server.isConnected()) {
            disconnectedCount++;
            activeConnectionCount.decrementAndGet();
            server.disconnect();
        }
    }

    // experimental workflow

    @NotNull
    synchronized IOptionsServer checkoutServer() throws P4JavaException, URISyntaxException {
        if (isInvalidLogin) {
            throw remakeException(authenticationException);
        }
        if (checkedOutBy != null) {
            throw new P4JavaException("Server object already checked out by " + checkedOutBy);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking out server " + this + " in " + Thread.currentThread());
        }

        if (! server.isConnected()) {
            reconnect();
        }
        //if (! hasValidatedAuthentication) {
            // Note: does not check the authentication result.
            authenticate();
        //}
        checkedOutBy = Thread.currentThread();
        return server;
    }

    synchronized void checkinServer(@NotNull IOptionsServer server) throws P4JavaException {
        if (checkedOutBy != Thread.currentThread()) {
            throw new P4JavaException("Server object not checked out by current thread (current thread: " +
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
     * @return true if authentication was successful.
     * @throws P4JavaException
     */
    AuthenticationResult authenticate() throws P4JavaException {
        if (! server.isConnected() || (project != null && project.isDisposed())) {
            return AuthenticationResult.NOT_CONNECTED;
        }

        if (isInvalidLogin) {
            LOG.info("Previous login attempts failed.  Assuming authentication is invalid.");
            return AuthenticationResult.INVALID_LOGIN;
        }

        // The default authentication has already run, but it may
        // be invalid.  However, all that checking will be in the
        // shared logic

        if (! validateLogin()) {
            loginFailedCount++;

            // the forced authentication has passed, but the
            // validation failed.

            if (! hasValidatedAuthentication) {
                // we have never been authenticated by the server,
                // and this situation means that we still aren't
                // even after a forced authentication.  So, we'll
                // report this connection as being unauthenticated.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Assuming actual authentication issue");
                }

                // This assumption that the authentication is invalid is too quick to judge.
                // Do not return not-authorized yet
            }

            // we have had a valid connection before, so we'll
            // try it again.
            // For some reason that needs to be figured out,
            // the server reports the connection as unauthorized.
            // We'll give it up to 3 retries with a slight delay
            // in between, in cas ethe error comes from too frequent
            // requests.

            for (int i = 0; i < getMaxAuthenticationRetries(); i++) {
                try {
                    // Sleeping a bit may cause the server to re-authenticate
                    // correctly.
                    reconnect();
                    if (validateLogin()) {
                        LOG.info("Authorization successful after " + i +
                                " unauthorized connections (but a valid one was seen earlier) for " +
                                this);
                        return AuthenticationResult.AUTHENTICATED;
                    }
                    forceAuthenticate(i);
                    if (validateLogin()) {
                        LOG.info("Authorization successful after " + i +
                                " unauthorized connections (but a valid one was seen earlier) for " +
                                this);
                        return AuthenticationResult.AUTHENTICATED;
                    }
                } catch (URISyntaxException e) {
                    throw new P4JavaException(e);
                }
            }
            // Don't keep trying the same bad config.
            isInvalidLogin = true;
            LOG.info("Failed authentication after " + getMaxAuthenticationRetries() +
                    " unauthorized connections (but a valid one was seen earlier) for " +
                    this);
            return AuthenticationResult.RELOGIN_FAILED;
        }
        hasValidatedAuthentication = true;
        return AuthenticationResult.AUTHENTICATED;
    }

    private void forceAuthenticate(int count) throws P4JavaException {
        try {
            // It looks like forced authentication can fail due to too many
            // quick requests to the server.  So wait a little bit to
            // give the server time to recover.
            Thread.sleep(RECONNECTION_WAIT_MILLIS * (count + 1));
            if (LOG.isDebugEnabled()) {
                LOG.debug("forcing authentication with " + this);
            }
            withConnectionLock(new WithConnectionLock<Void>() {
                @Override
                public Void call() throws P4JavaException {
                    connectionHandler.forcedAuthentication(project, server, config, AlertManager.getInstance());
                    forcedAuthenticationCount++;
                    return null;
                }
            });
        } catch (PasswordAccessedWrongException e) {
            // do not capture this specific exception
            authenticationException = null;
            throw e;
        } catch (P4JavaException e) {
            // capture the exception for future use
            authenticationException = e;
            throw e;
        } catch (InterruptedException e) {
            // TODO should not be wrapping this exception in a P4JavaException
            throw new P4JavaException(e);
        } catch (URISyntaxException e) {
            // Should not actually happen, but the withConnectionLock declares it...
            throw new P4JavaException(e);
        }
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();

        super.finalize();
    }


    private void reconnect() throws P4JavaException, URISyntaxException {
        if (checkedOutBy != null) {
            throw new P4JavaException("Server instance already checked out by " + checkedOutBy);
        }
        try {
            withConnectionLock(new WithConnectionLock<Void>() {
                @Override
                public Void call() throws P4JavaException, URISyntaxException {
                    disconnect();
                    server = reconnect(project, clientName, connectionHandler, config, tempDir,
                            serverInstance);
                    connectedCount++;
                    return null;
                }
            });
        } catch (InterruptedException e) {
            // TODO should not be wrapping in a P4JavaException
            throw new P4JavaException(e);
        }
    }


    private boolean validateLogin()
            throws ConnectionException, RequestException, AccessException {
        if (! server.isConnected()) {
            return false;
        }
        try {
            // Perform an operation that should succeed, and only fail if the
            // login is wrong.
            server.getUser(config.getUsername());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Basic authentication looks correct for " + this);
            }
            return true;
        } catch (AccessException ex) {
            LOG.info("Server forgot connection login", ex);
            // Do not disconnect here..  If it's not a real authentication
            // issue, then we will need to stay connected to re-authorize
            // the connection.
            // disconnect();
            return false;
        }
    }


    private IOptionsServer reconnect(@Nullable final Project project,
            @Nullable String clientName, final @NotNull ConnectionHandler connectionHandler,
            @NotNull final ServerConfig config, @NotNull File tempDir,
            int serverInstance)
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
                    P4LOG.debug("p4java info: " + infoString);
                }

                @Override
                public void internalStats(final String statsString) {
                    P4LOG.debug("p4java stats: " + statsString);
                }

                @Override
                public void internalTrace(final LogTraceLevel traceLevel, final String traceMessage) {
                    P4LOG.debug("p4java trace: " + traceMessage);
                }

                @Override
                public LogTraceLevel getTraceLevel() {
                    return P4LOG.isDebugEnabled() ? LogTraceLevel.ALL : LogTraceLevel.FINE;
                }
            });
        }


        final Properties properties;
        final String url;
        properties = connectionHandler.getConnectionProperties(config, clientName);
        properties.setProperty(PropertyDefs.P4JAVA_TMP_DIR_KEY, tempDir.getAbsolutePath());

        // For tracking purposes
        properties.setProperty(PropertyDefs.PROG_NAME_KEY,
                properties.getProperty(PropertyDefs.PROG_NAME_KEY) + " connection " +
                serverInstance);

        url = connectionHandler.createUrl(config);
        LOG.info("Opening connection " + serverInstance + " to " + url + " with " + config.getUsername());

        // see bug #61
        // Hostname as used by the Java code:
        //   Mac clients can incorrectly set the hostname.
        //   The underlying code will use:
        //      InetAddress.getLocalHost().getHostName()
        //   or from the UsageOptions passed into the
        //   server configuration `init` method.

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
                    final IOptionsServer server = connectionHandler.getOptionsServer(url, properties, config);

                    // These seem to cause issues.
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

                    // If there is a password problem, we still want
                    // to maintain our cached server, so a retry doesn't
                    // recreate the server connection again.

                    // However, the way this is written allows for
                    // an invalid password to cause the server connection
                    // to never be returned.

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("calling defaultAuthentication on " + connectionHandler.getClass().getSimpleName());
                    }
                    connectionHandler.defaultAuthentication(project, server, config);

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
     * @param callable
     * @param <T>
     * @return
     * @throws InterruptedException
     * @throws P4JavaException
     * @throws URISyntaxException
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
                        serverInstance == ((AuthenticatedServer) o).serverInstance;
    }

    @Override
    public int hashCode() {
        return serverInstance;
    }

    @Override
    public String toString() {
        return "Server" + serverInstance +
                " (loginFailed# " + loginFailedCount +
                ", connected# " + connectedCount +
                ", disconnected# " + disconnectedCount +
                ", forcedLogin# " + forcedAuthenticationCount +
                ", invalidLogin? " + isInvalidLogin +
                ", validatedLogin? " + hasValidatedAuthentication +
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
