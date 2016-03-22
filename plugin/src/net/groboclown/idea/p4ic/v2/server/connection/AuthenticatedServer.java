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
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ILogCallback;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
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
    private static final Logger LOG = Logger.getInstance(AuthenticatedServer.class);
    private static final Logger P4LOG = Logger.getInstance("p4");
    private static final Lock CONNECT_LOCK = new ReentrantLock();
    public static final int MAX_AUTHENTICATION_RETRIES = 3;

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

    private boolean forcedAuthentication = false;
    private boolean hasPassedAuthentication = false;

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


    @NotNull
    IOptionsServer getServer() {
        return server;
    }


    /**
     *
     * @return true if authentication was successful.
     * @throws P4JavaException
     */
    boolean authenticate() throws P4JavaException {
        if (! server.isConnected() || (project != null && project.isDisposed())) {
            return false;
        }
        if (! forcedAuthentication) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempting to force an authentication for " + this);
            }
            forceAuthenticate();
        }
        if (! testLogin()) {
            loginFailedCount++;
            if (! hasPassedAuthentication) {
                // we have never been authenticated by the server,
                // and this situation means that we still aren't
                // even after a forced authentication.  So, we'll
                // report this connection as being unauthenticated.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Assuming actual authentication issue");
                }
                forcedAuthentication = false;
                return false;
            }

            // we have had a valid connection before, so we'll
            // try it again.
            // For some reason that needs to be figured out,
            // the server reports the connection as unauthorized.
            // We'll give it up to 3 retries with a slight delay
            // in between, in cas ethe error comes from too frequent
            // requests.

            for (int i = 0; i < MAX_AUTHENTICATION_RETRIES; i++) {
                try {
                    // Sleeping a bit may cause the server to reauthenticate
                    // correctly.
                    server = reconnect(project, clientName, connectionHandler, config, tempDir,
                            serverInstance);
                    connectedCount++;
                    forceAuthenticate();
                    if (testLogin()) {
                        LOG.info("Authorization successful after " + i +
                                " unauthorized connections (but a valid one was seen earlier) for " +
                                this);
                        return true;
                    }
                } catch (URISyntaxException e) {
                    throw new P4JavaException(e);
                }
            }
            LOG.info("Failed authentication after " + MAX_AUTHENTICATION_RETRIES +
                    " unauthorized connections (but a valid one was seen earlier) for " +
                    this);
            return false;
        }
        hasPassedAuthentication = true;
        return true;
    }

    private void forceAuthenticate() throws P4JavaException {
        try {
            // It looks like forced authentication can fail due to too many
            // quick requests to the server.  So wait a little bit to
            // give the server time to recover.
            Thread.sleep(RECONNECTION_WAIT_MILLIS);
            CONNECT_LOCK.lockInterruptibly();
            try {
                connectionHandler.forcedAuthentication(project, server, config, AlertManager.getInstance());
                forcedAuthenticationCount++;
                forcedAuthentication = true;
            } finally {
                CONNECT_LOCK.unlock();
            }
        } catch (InterruptedException e) {
            throw new P4JavaException(e);
        }
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();

        super.finalize();
    }


    private boolean testLogin()
            throws ConnectionException, RequestException, AccessException {
        try {
            // Perform an operation that should succeed, and only fail if the
            // login is wrong.
            server.getUser(config.getUsername());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Basic authentication looks correct for " + this);
            }
            return true;
        } catch (AccessException ex) {
            LOG.info("Authentication failed", ex);
            disconnect();
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
                LOG.debug("calling connectionHandler.getOptionsServer");
                final IOptionsServer server = connectionHandler.getOptionsServer(url, properties, config);

                // These seem to cause issues.
                //server.registerCallback(new LoggingCommandCallback());
                //server.registerProgressCallback(new LoggingProgressCallback());

                LOG.debug("calling connect");
                server.connect();
                LOG.debug("calling activeConnectionCount incrementAndGet");
                activeConnectionCount.incrementAndGet();

                // If there is a password problem, we still want
                // to maintain our cached server, so a retry doesn't
                // recreate the server connection again.

                // However, the way this is written allows for
                // an invalid password to cause the server connection
                // to never be returned.

                LOG.debug("calling defaultAuthentication on " + connectionHandler.getClass().getSimpleName());
                connectionHandler.defaultAuthentication(project, server, config);

                LOG.debug("Returning the authenticated server object");
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
                " (lf#: " + loginFailedCount +
                ", c#: " + connectedCount +
                ", d#: " + disconnectedCount +
                ", fa#: " + forcedAuthenticationCount +
                ")";
    }
}
