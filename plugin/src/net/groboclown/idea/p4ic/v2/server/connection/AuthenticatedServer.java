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
    };


    private static final Logger LOG = Logger.getInstance(AuthenticatedServer.class);
    private static final Logger P4LOG = Logger.getInstance("p4");
    private static final Lock CONNECT_LOCK = new ReentrantLock();

    private static final AtomicInteger serverCount = new AtomicInteger(0);
    private static final AtomicInteger activeConnectionCount = new AtomicInteger(0);
    public static final int RECONNECTION_WAIT_MILLIS = 10;

    private final ConnectionHandler connectionHandler;
    private final ServerConfig config;
    private final int serverInstance = serverCount.getAndIncrement();
    private final String clientName;
    private final File tempDir;

    @Nullable
    private final Project project;

    @NotNull
    private IOptionsServer server;

    private P4JavaException authenticationException = null;
    private boolean hasPassedAuthentication = false;
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

        // Note: at this point, the

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


    @NotNull
    IOptionsServer getServer() throws P4JavaException {
        if (isInvalidLogin) {
            throw remakeException(authenticationException);
        }
        if (! hasValidatedAuthentication) {
            // Note: does not check the authentication result.
            authenticate();
        }
        return server;
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
                    if (server.isConnected()) {
                        server.disconnect();
                    }
                    server = reconnect(project, clientName, connectionHandler, config, tempDir,
                            serverInstance);
                    connectedCount++;
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
            CONNECT_LOCK.lockInterruptibly();
            try {
                connectionHandler.forcedAuthentication(project, server, config, AlertManager.getInstance());
                forcedAuthenticationCount++;
            } finally {
                CONNECT_LOCK.unlock();
            }
        } catch (PasswordAccessedWrongException e) {
            // do not capture this specific exception
            authenticationException = null;
            throw e;
        } catch (P4JavaException e) {
            // capture the exception for future use
            authenticationException = e;
            throw e;
        } catch (InterruptedException e) {
            throw new P4JavaException(e);
        }
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();

        super.finalize();
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


    private static IOptionsServer reconnect(@Nullable Project project,
            @Nullable String clientName, @NotNull ConnectionHandler connectionHandler,
            @NotNull ServerConfig config, @NotNull File tempDir,
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
            CONNECT_LOCK.lockInterruptibly();
            try {
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
            } finally {
                CONNECT_LOCK.unlock();
            }
        } catch (InterruptedException e) {
            throw new P4JavaException(e);
        }
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
                ", passedLogin? " + hasPassedAuthentication +
                ", validatedLogin? " + hasValidatedAuthentication +
                ")";
    }

    @NotNull
    private static P4JavaException remakeException(@Nullable final P4JavaException authenticationException) {
        if (authenticationException == null) {
            return new P4JavaException("invalid login credentials");
        }
        // TODO This is an implicit tight coupling with ClientExec.  However, we want to
        // throw a fresh exception so we record the real source of the error and the
        // actual where-we-are-throwing-it-now location.
        if (authenticationException instanceof PasswordAccessedWrongException) {
            final PasswordAccessedWrongException ret = new PasswordAccessedWrongException();
            ret.initCause(authenticationException);
            return ret;
        }
        if (authenticationException instanceof LoginRequiresPasswordException) {
            return new LoginRequiresPasswordException(
                    (AccessException) ((LoginRequiresPasswordException) authenticationException).getCause());
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
