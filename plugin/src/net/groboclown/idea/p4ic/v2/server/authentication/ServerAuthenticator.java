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

package net.groboclown.idea.p4ic.v2.server.authentication;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.ClientFileAccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.ExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4LoginException;
import net.groboclown.idea.p4ic.server.exceptions.P4SSLException;
import net.groboclown.idea.p4ic.server.exceptions.P4VcsConnectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Handles authentication with the server.  It will not interact with the UI, so it can
 * be called from the EDT or a background thread without threat of deadlock.
 */
public class ServerAuthenticator {
    private static final Logger LOG = Logger.getInstance(ServerAuthenticator.class);

    public static AuthenticationStatus DISPOSED = new AuthenticationStatus(
            null, true, false, false, false, false, false);

    public static class AuthenticationStatus {
        private final P4VcsConnectionException problem;
        private final boolean notConnected;
        private final boolean passwordInvalid;
        private final boolean sessionExpired;
        private final boolean notLoggedIn;
        private final boolean clientSetupProblem;
        private final boolean needsPassword;

        private AuthenticationStatus(@Nullable P4VcsConnectionException problem,
                boolean notConnected, boolean passwordInvalid,
                boolean sessionExpired, boolean clientSetupProblem, boolean notLoggedIn,
                boolean needsPassword) {
            this.problem = problem;
            this.notConnected = notConnected;
            this.passwordInvalid = passwordInvalid;
            this.sessionExpired = sessionExpired;
            this.clientSetupProblem = clientSetupProblem;
            this.notLoggedIn = notLoggedIn;
            this.needsPassword = needsPassword;
        }

        private AuthenticationStatus(@NotNull AuthenticationFailedException problem) {
            this.problem = new P4LoginException(problem);
            switch (problem.getErrorType()) {
                case NOT_LOGGED_IN:
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = false;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    break;
                case PASSWORD_INVALID:
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = true;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = true;
                    break;
                case SESSION_EXPIRED:
                    this.notConnected = false;
                    this.sessionExpired = true;
                    this.passwordInvalid = false;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    break;
                case SSO_LOGIN:
                    this.notConnected = false;
                    this.sessionExpired = true;
                    this.passwordInvalid = false;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    break;
                default:
                    // Assume it's a bad login
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = true;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = true;
                    this.needsPassword = false;
            }
        }

        public boolean isAuthenticated() {
            return ! passwordInvalid
                    && ! notConnected
                    && ! sessionExpired
                    && ! notLoggedIn
                    && ! clientSetupProblem
                    && ! needsPassword
                    && problem == null;
        }

        public boolean isNotConnected() {
            return notConnected;
        }

        public boolean isPasswordInvalid() {
            return passwordInvalid;
        }

        public boolean isSessionExpired() {
            return sessionExpired;
        }

        public boolean isNotLoggedIn() {
            return notLoggedIn;
        }

        public boolean isClientSetupProblem() {
            return clientSetupProblem;
        }

        public boolean isPasswordRequired() {
            return needsPassword;
        }

        @Nullable
        public P4VcsConnectionException getProblem() {
            return problem;
        }
    }

    public AuthenticationStatus createStatusFor(@NotNull URISyntaxException e) {
        return new AuthenticationStatus(new P4InvalidConfigException(e),
                true, false, false, true, false, false);
    }

    public AuthenticationStatus discoverAuthenticationStatus(@NotNull IOptionsServer server) {
        if (! server.isConnected()) {
            return new AuthenticationStatus(null, true, false, false, false, false, false);
        }
        return runExec(new ExecFunc<Void>() {
            @Nullable
            @Override
            public Void exec(@NotNull IOptionsServer server)
                    throws P4JavaException {
                server.getUsers(null, 1);
                return null;
            }
        }, server).status;
    }


    /**
     * Attempt to log into the server.  This should only be called if the connection
     * is not authenticated (the above call fails).
     *
     * @param server p4 api server instance
     * @param config server configuration settings
     * @param authenticationCheck the status from the call to {@link #discoverAuthenticationStatus(IOptionsServer)}
     *                            that triggered this call.  Used to figure out how to resolve the authentication
     *                            issue.
     * @param knownPassword password as known by the password manager
     * @return status of the authentication after the attempt.
     */
    public AuthenticationStatus authenticate(@NotNull IOptionsServer server, @NotNull final ServerConfig config,
            @NotNull final AuthenticationStatus authenticationCheck,
            @Nullable final String knownPassword) {
        return authenticate(server, config, authenticationCheck, knownPassword, false);
    }

    private AuthenticationStatus authenticate(@NotNull IOptionsServer server, @NotNull final ServerConfig config,
            @NotNull final AuthenticationStatus authenticationCheck,
            @Nullable final String knownPassword,
            boolean alreadyAttempted) {
        if (authenticationCheck.isAuthenticated()) {
            LOG.info("Called authenticate when already authenticated");
            return authenticationCheck;
        }
        if (authenticationCheck.clientSetupProblem) {
            LOG.info("Called authenticate when the issue was with the client setup");
            return authenticationCheck;
        }

        if (! server.isConnected()) {
            try {
                server.connect();
            } catch (ConnectionException e) {
                return new AuthenticationStatus(new P4DisconnectedException(e), true, false, false, false, false,
                        false);
            } catch (AccessException e) {
                LOG.debug("Connection failed due to authentication; going to try authentication", e);
            } catch (RequestException e) {
                return new AuthenticationStatus(new P4ApiException(e), true, false, false, false, false, false);
            } catch (ConfigException e) {
                return new AuthenticationStatus(new P4InvalidConfigException(e), true, false, false, false, false,
                        false);
            }
        }

        if (authenticationCheck.notConnected) {
            // Previous issue was due to not being connected.
            // Now that we're connected, check the authentication again.
            AuthenticationStatus nextCheck = discoverAuthenticationStatus(server);

            // If there was a problem that we won't be able to handle, or if the
            // connection is fine, return without a login attempt.
            if (nextCheck.notConnected
                    || nextCheck.isAuthenticated()
                    || nextCheck.clientSetupProblem
                    || alreadyAttempted) {
                return nextCheck;
            }
            // There was some other problem, but related to authentication.
            return authenticate(server, config, nextCheck, knownPassword, true);
        }

        final AuthenticationStatus ret = runExec(new ExecFunc<Void>() {
            @Nullable
            @Override
            public Void exec(@NotNull IOptionsServer server)
                    throws P4JavaException {
                LoginOptions loginOptions = new LoginOptions(false, true);
                StringBuffer authFileContents = null;
                if (config.getAuthTicket() != null) {
                    authFileContents = loadAuthTicketContents(config.getAuthTicket());
                }
                server.login(knownPassword, authFileContents, loginOptions);
                if (authFileContents != null) {
                    saveAuthTicketContents(authFileContents, config.getAuthTicket());
                }

                return null;
            }
        }, server).status;
        if (ret.notConnected && ! alreadyAttempted) {
            // Weird connection issue.  Try again.
            return authenticate(server, config, ret, knownPassword, true);
        }
        return ret;
    }

    @Nullable
    private StringBuffer loadAuthTicketContents(@NotNull File authTicket) {
        final StringBuffer ret = new StringBuffer();
        try {
            final FileReader inp = new FileReader(authTicket);
            try {
                char[] buff = new char[4096];
                int len;
                while ((len = inp.read(buff, 0, 4096)) > 0) {
                    ret.append(buff, 0, len);
                }
                return ret;
            } finally {
                inp.close();
            }
        } catch (IOException e) {
            LOG.info("Problem reading auth ticket file " + authTicket, e);
            return null;
        }
    }

    private void saveAuthTicketContents(@NotNull StringBuffer authFileContents, @NotNull File authTicket) {
        try {
            final FileWriter out = new FileWriter(authTicket);
            try {
                out.write(authFileContents.toString());
            } finally {
                out.close();
            }
        } catch (IOException e) {
            LOG.info("Problem writing auth ticket file " + authTicket, e);
        }
    }

    private interface ExecFunc<T> {
        @Nullable
        T exec(@NotNull IOptionsServer server) throws P4JavaException;
    }


    private static class ExecResult<T> {
        private final T value;
        @NotNull
        private final AuthenticationStatus status;

        private ExecResult(@Nullable T value) {
            this.value = value;
            this.status = new AuthenticationStatus(null, false, false, false, false, false, false);
        }

        private ExecResult(@NotNull AuthenticationStatus status) {
            this.value = null;
            this.status = status;
        }

        boolean isStatusProblem() {
            return ! status.isAuthenticated();
        }
    }


    private static <T> ExecResult<T> runExec(@NotNull ExecFunc<T> exec, @NotNull IOptionsServer server) {
        try {
            return new ExecResult<T>(exec.exec(server));
        } catch (SslException e) {
            return new ExecResult<T>(new AuthenticationStatus(
                    new P4SSLException(e),
                    true, false, false, false, true, false));
        } catch (ClientFileAccessException e) {
            // shouldn't happen for this call
            return new ExecResult<T>(new AuthenticationStatus(
                    new P4VcsConnectionException(e),
                    false, false, false, true, false, false));
        } catch (AuthenticationFailedException e) {
            return new ExecResult<T>(new AuthenticationStatus(e));
        } catch (ConnectionException e) {
            LOG.debug(e);
            Throwable cause = e.getCause();
            if (cause instanceof ConfigException) {
                return new ExecResult<T>(new AuthenticationStatus(
                        new P4InvalidConfigException((ConfigException) cause),
                        false, false, false, true, true, false));
            }
            return new ExecResult<T>(new AuthenticationStatus(
                    new P4VcsConnectionException(cause),
                    true, false, false, true, true, false));
        } catch (LoginRequiresPasswordException e) {
            return new ExecResult<T>(new AuthenticationStatus(new P4LoginException(e),
                    false, true, false, false, true, true));
        } catch (P4JavaException e) {
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                return new ExecResult<T>(new AuthenticationStatus(new P4LoginException(e),
                        false, true, false, false, true, true));
            }
            if (ExceptionUtil.isSessionExpiredProblem(e)) {
                return new ExecResult<T>(new AuthenticationStatus(new P4LoginException(e),
                        false, false, true, false, true, false));
            }
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e)) {
                return new ExecResult<T>(new AuthenticationStatus(new P4LoginException(e),
                        false, false, false, false, true, true));
            }

            // Generic error in the request
            LOG.info("Problem loading user list", e);
            return new ExecResult<T>(new AuthenticationStatus(
                    new P4ApiException(e),
                    true, false, false, false, false, false));
        }
    }
}
