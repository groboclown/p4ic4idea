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
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.ExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.P4AccessException;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4JavaSSLStrengthException;
import net.groboclown.idea.p4ic.server.exceptions.P4LoginException;
import net.groboclown.idea.p4ic.server.exceptions.P4LoginRequiresPasswordException;
import net.groboclown.idea.p4ic.server.exceptions.P4SSLException;
import net.groboclown.idea.p4ic.server.exceptions.P4VcsConnectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;

/**
 * Handles authentication with the server.  It will not interact with the UI, so it can
 * be called from the EDT or a background thread without threat of deadlock.
 */
public class ServerAuthenticator {
    private static final Logger LOG = Logger.getInstance(ServerAuthenticator.class);

    public static final AuthenticationStatus DISPOSED = new AuthenticationStatus(
            null, true, false, false, false, false, false, false);
    public static final AuthenticationStatus REQUIRES_PASSWORD = new AuthenticationStatus(
            null, true, false, false, false, true, true, false);

    private static class AuthStatBuilder {
        private final P4VcsConnectionException problem;
        private boolean notConnected = false;
        private boolean passwordInvalid = false;
        private boolean sessionExpired = false;
        private boolean notLoggedIn = false;
        private boolean clientSetupProblem = false;
        private boolean needsPassword = false;
        private boolean passwordUnnecessary = false;

        private AuthStatBuilder(P4VcsConnectionException problem) {
            this.problem = problem;
        }

        private AuthStatBuilder notConnected() {
            notConnected = true;
            return this;
        }

        private AuthStatBuilder passwordInvalid() {
            passwordInvalid = true;
            return this;
        }

        private AuthStatBuilder sessionExpired() {
            sessionExpired = true;
            return this;
        }

        private AuthStatBuilder notLoggedIn() {
            notLoggedIn = true;
            return this;
        }

        private AuthStatBuilder clientSetupProblem() {
            clientSetupProblem = true;
            return this;
        }

        private AuthStatBuilder needsPassword() {
            needsPassword = true;
            return this;
        }

        private AuthStatBuilder passwordUnnecessary() {
            passwordUnnecessary = true;
            return this;
        }

        private AuthenticationStatus create() {
            return new AuthenticationStatus(problem,
                    notConnected, passwordInvalid, sessionExpired, clientSetupProblem, notLoggedIn, needsPassword,
                    passwordUnnecessary);
        }
    }

    private static AuthStatBuilder statBuilder(@NotNull P4VcsConnectionException problem) {
        return new AuthStatBuilder(problem);
    }

    private static AuthStatBuilder statBuilder() {
        return new AuthStatBuilder(null);
    }

    public static class AuthenticationStatus {
        private final P4VcsConnectionException problem;
        private final boolean notConnected;
        private final boolean passwordInvalid;
        private final boolean sessionExpired;
        private final boolean notLoggedIn;
        private final boolean clientSetupProblem;
        private final boolean needsPassword;
        private final boolean passwordUnnecessary;

        private AuthenticationStatus(@Nullable P4VcsConnectionException problem,
                boolean notConnected, boolean passwordInvalid,
                boolean sessionExpired, boolean clientSetupProblem, boolean notLoggedIn,
                boolean needsPassword, boolean passwordUnnecessary) {
            this.problem = problem;
            this.notConnected = notConnected;
            this.passwordInvalid = passwordInvalid;
            this.sessionExpired = sessionExpired;
            this.clientSetupProblem = clientSetupProblem;
            this.notLoggedIn = notLoggedIn;
            this.needsPassword = needsPassword;
            this.passwordUnnecessary = passwordUnnecessary;
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
                    this.passwordUnnecessary = false;
                    break;
                case PASSWORD_INVALID:
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = true;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = true;
                    this.passwordUnnecessary = false;
                    break;
                case PASSWORD_UNNECESSARY:
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = false;
                    this.notLoggedIn = false;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    this.passwordUnnecessary = true;
                    break;
                case SESSION_EXPIRED:
                    this.notConnected = false;
                    this.sessionExpired = true;
                    this.passwordInvalid = false;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    this.passwordUnnecessary = false;
                    break;
                case SSO_LOGIN:
                    this.notConnected = false;
                    this.sessionExpired = true;
                    this.passwordInvalid = false;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = false;
                    this.needsPassword = false;
                    this.passwordUnnecessary = false;
                    break;
                default:
                    // Assume it's a bad login
                    this.notConnected = false;
                    this.sessionExpired = false;
                    this.passwordInvalid = true;
                    this.notLoggedIn = true;
                    this.clientSetupProblem = true;
                    this.needsPassword = false;
                    this.passwordUnnecessary = false;
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
            // passwordUnnecessary means that we're authenticated.
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

        public boolean isPasswordUnnecessary() {
            return passwordUnnecessary;
        }

        @Nullable
        public P4VcsConnectionException getProblem() {
            return problem;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Auth Result: ");
            String sep = "";
            if (getProblem() != null) {
                sb.append(sep).append(getProblem().getMessage());
                sep = "; ";
            }
            if (isAuthenticated()) {
                sb.append(sep).append("authenticated");
                sep = "; ";
            }

            if (isNotConnected()) {
                sb.append(sep).append("not connected");
                sep = "; ";
            }
            if (isPasswordInvalid()) {
                sb.append(sep).append("password invalid");
                sep = "; ";
            }
            if (isSessionExpired()) {
                sb.append(sep).append("session expired");
                sep = "; ";
            }
            if (isNotLoggedIn()) {
                sb.append(sep).append("not logged in");
                sep = "; ";
            }
            if (isClientSetupProblem()) {
                sb.append(sep).append("client setup problem");
                sep = "; ";
            }
            if (isPasswordRequired()) {
                sb.append(sep).append("password required");
                sep = "; ";
            }
            if (isPasswordUnnecessary()) {
                sb.append(sep).append("password unnecessary");
                // sep = "; ";
            }
            return sb.toString();
        }
    }

    public AuthenticationStatus createStatusFor(@NotNull URISyntaxException e) {
        return statBuilder(new P4InvalidConfigException(e))
                .notConnected()
                .clientSetupProblem()
                .create();
    }

    public AuthenticationStatus discoverAuthenticationStatus(@NotNull IOptionsServer server) {
        if (! server.isConnected()) {
            return statBuilder()
                    .notConnected()
                    .create();
        }
        return runExec(new ExecFunc<Void>() {
            @Nullable
            @Override
            public Void exec(@NotNull IOptionsServer server)
                    throws P4JavaException {
                // Originally, this was "getUsers", but it turns out that this
                // command can be run without setting a password, even if one
                // is required.
                server.getClients(new GetClientsOptions(1, null, null));
                return null;
            }
        }, server).status;
    }


    /**
     * After the server is created, attempt the login based on what we know
     *
     * @param server p4 server
     * @param knownPassword password that the user has told the plygin.
     * @return status after the initial login; can be used with the later authentication checks.
     */
    @NotNull
    public AuthenticationStatus initialLogin(@NotNull IOptionsServer server, @NotNull ServerConfig config,
            @Nullable final String knownPassword) {
        // As this is the initial login, we expect the connection to still be connected.

        // Initial login is only necessary if the auth ticket isn't set and the password is given.
        if (config.getAuthTicket() != null && config.getAuthTicket().isFile() && config.getAuthTicket().exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Trying authentication with existing auth ticket file " + config.getAuthTicket());
            }
            // If the session ticket is valid, then immediately return.  Otherwise, we default
            // to the standard password login (in some cases, it means a login to the server and
            // save the password to the auth ticket).
            final AuthenticationStatus status = discoverAuthenticationStatus(server);
            if (isAuthTicketAuthenticationValid(status, knownPassword != null)) {
                return status;
            }
        }
        // #147 if the user isn't logged in with an authentication ticket, but has P4LOGINSSO
        // set, then a simple password login attempt should be made.  The P4LOGINSSO will
        // ignore the password.
        if (config.hasLoginSso() || (knownPassword != null && ! knownPassword.isEmpty())) {
            LOG.debug("Logging into server with known password or sso script (" + config.getLoginSso() + ")");
            final LoginOptions loginOptions = new LoginOptions();
            final boolean useTicket = config.getAuthTicket() != null && ! config.getAuthTicket().isDirectory();
            loginOptions.setDontWriteTicket(! useTicket);
            return runExec(new ExecFunc<Void>() {
                @Nullable
                @Override
                public Void exec(@NotNull IOptionsServer server)
                        throws P4JavaException {
                    server.login(knownPassword, loginOptions);
                    return null;
                }
            }, server).status;
        }
        LOG.debug("No known password.  Checking authentication status rather than logging in.");
        return discoverAuthenticationStatus(server);
    }


    private boolean isAuthTicketAuthenticationValid(@NotNull AuthenticationStatus status, boolean hasPassword) {
        if (status.isSessionExpired() && ! hasPassword) {
            // Session is expired and we don't have a password, so just error out with a bad
            // session.
            LOG.debug("Session expired and we don't have a password");
            return true;
        } else if (!status.isSessionExpired()) {
            // We have a session-is-not-expired message, which is only half the answer.
            if (status.isAuthenticated()) {
                LOG.debug("Session is valid and we're logged in");
                return true;
            }
        }

        // The connection, without a login attempt, isn't valid.  Need to try again.
        return false;
    }


    private interface ExecFunc<T> {
        @Nullable
        T exec(@NotNull IOptionsServer server) throws P4JavaException;
    }


    private static class ExecResult<T> {
        @NotNull
        private final AuthenticationStatus status;

        private ExecResult(@Nullable T value) {
            // this.value = value;
            this.status = statBuilder().create();
        }

        private ExecResult(@NotNull AuthenticationStatus status) {
            // this.value = null;
            this.status = status;
        }
    }


    private static <T> ExecResult<T> runExec(@NotNull ExecFunc<T> exec, @NotNull IOptionsServer server) {
        try {
            return new ExecResult<T>(exec.exec(server));
        } catch (SslHandshakeException e) {
            LOG.debug("Low-level SSL problem", e);
            return new ExecResult<T>(statBuilder(new P4JavaSSLStrengthException(e))
                    .notConnected()
                    .notLoggedIn()
                    .create());
        } catch (SslException e) {
            LOG.debug("Execution generated problem with SSL", e);
            return new ExecResult<T>(statBuilder(new P4SSLException(e))
                    .notConnected()
                    .notLoggedIn()
                    .create());
        } catch (ClientFileAccessException e) {
            // shouldn't happen for this call, but handle it just in case.
            LOG.debug("Execution encountered problem with accessing a file on the client", e);
            return new ExecResult<T>(statBuilder(new P4VcsConnectionException(e))
                    .clientSetupProblem()
                    .create());
        } catch (AuthenticationFailedException e) {
            LOG.debug("Execution generated a login failure", e);
            return new ExecResult<T>(new AuthenticationStatus(e));
        } catch (ConnectionException e) {
            LOG.debug("Execution encountered a connection failure", e);
            Throwable cause = e.getCause();
            if (cause instanceof ConfigException) {
                return new ExecResult<T>(statBuilder(new P4InvalidConfigException((ConfigException) cause))
                        .notConnected()
                        .clientSetupProblem()
                        .notLoggedIn()
                        .create());
            }
            return new ExecResult<T>(statBuilder(new P4VcsConnectionException(cause))
                    .notConnected()
                    .clientSetupProblem()
                    .notLoggedIn()
                    .create());
        } catch (LoginRequiresPasswordException e) {
            LOG.debug("Execution failed because login requires a password", e);
            return new ExecResult<T>(statBuilder(new P4LoginRequiresPasswordException(e))
                    .notLoggedIn()
                    .needsPassword()
                    .create());
        } catch (AccessException e) {
            if (ExceptionUtil.isLoginUnnecessary(e)) {
                // This isn't an error, but a warning.
                // It's generated when a "login" is attempted but
                // the user hasn't set a password.
                LOG.debug("User provided password, but the user never set a password", e);
                return new ExecResult<T>(statBuilder()
                        .passwordUnnecessary()
                        .create());
            }
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                LOG.debug("Incorrect password", e);
                return new ExecResult<T>(statBuilder(new P4LoginException(e))
                        .passwordInvalid()
                        .notLoggedIn()
                        .needsPassword()
                        .create());
            }
            if (ExceptionUtil.isSessionExpiredProblem(e)) {
                LOG.debug("Session expired", e);
                return new ExecResult<T>(statBuilder(new P4LoginException(e))
                        .sessionExpired()
                        .notLoggedIn()
                        .create());
            }
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e)) {
                LOG.debug("User requires a password and a login", e);
                return new ExecResult<T>(statBuilder(new P4LoginRequiresPasswordException(e))
                        .notLoggedIn()
                        .needsPassword()
                        .create());
            }

            // Special exception in the status
            LOG.debug("General access problem", e);
            return new ExecResult<T>(statBuilder(new P4AccessException(e))
                    .clientSetupProblem()
                    .notLoggedIn()
                    .create());
        } catch (P4JavaException e) {
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                LOG.debug("Incorrect password", e);
                return new ExecResult<T>(statBuilder(new P4LoginException(e))
                        .passwordInvalid()
                        .notLoggedIn()
                        .needsPassword()
                        .create());
            }
            if (ExceptionUtil.isSessionExpiredProblem(e)) {
                LOG.debug("Session expired", e);
                return new ExecResult<T>(statBuilder(new P4LoginException(e))
                        .sessionExpired()
                        .notLoggedIn()
                        .create());
            }
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e)) {
                LOG.debug("User requires a password and a login", e);
                return new ExecResult<T>(statBuilder(new P4LoginRequiresPasswordException(e))
                        .notLoggedIn()
                        .needsPassword()
                        .create());
            }

            // Generic error in the request
            LOG.debug("Problem loading user list", e);
            return new ExecResult<T>(statBuilder(new P4ApiException(e))
                    .create());
        }
    }
}
