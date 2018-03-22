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
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.*;
import net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;


/**
 * Executes a command with an IOptionsServer instance, and correctly handles the
 * exception handling.
 *
 * At one point, this logic was spread between the {@link AuthenticatedServer},
 * {@link ClientExec}, and a few other places.  This unifies all that error handling
 * logic in one place.
 */
class ServerRunner {
    private static final Logger LOG = Logger.getInstance(ServerRunner.class);


    interface P4Runner<T> {
        T run()
                throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                VcsException;
    }


    
    interface ErrorVisitorFactory {
        @NotNull
        ErrorVisitor getVisitorFor(@NotNull Project project);
    }


    interface ErrorVisitor {
        /**
         * Login is considered to be invalid.
         *
         * @param e source error
         * @throws VcsException exception
         * @throws CancellationException user canceled execution
         */
        void loginFailure(@NotNull P4LoginException e)
                throws VcsException, CancellationException;

        void loginRequiresPassword(@NotNull P4PasswordException cause,
                ClientConfig clientConfig)
                throws VcsException, CancellationException;

        void disconnectFailure(P4DisconnectedException e)
                throws VcsException, CancellationException;

        /**
         * Full handling of an invalid config.  Should include
         * AlertManager.getInstance().addCriticalError(new ConfigurationProblemHandler(project, connectedController, e), ex);
         *
         * @param e source error
         * @throws VcsException exception
         * @throws CancellationException user canceled execution
         */
        void configInvalid(P4InvalidConfigException e)
                throws VcsException, CancellationException;

        @NotNull
        P4SSLFingerprintException sslFingerprintError(ConnectionException e);

        P4SSLException sslHandshakeError(SslHandshakeException e);

        P4SSLException sslKeyStrengthError(SslHandshakeException e);

        void passwordUnnecessary(@NotNull ServerAuthenticator.AuthenticationStatus authenticationResult);
    }

    interface Connection {
        /**
         * Called when the connection is considered to be invalid, such as when it is
         * working in offline mode, or the server indicates that the connection is incorrect.
         */
        void disconnected();

        /**
         *
         * @return true if the connection to the server should not be made.
         */
        boolean isWorkingOffline();

        /**
         * When a non-error situation happens.
         */
        void onSuccessfulCall();
    }

    @NotNull
    static IOptionsServer getServerFor(@NotNull AuthenticatedServer.ServerConnection servCon,
            @NotNull ErrorVisitor errorVisitor) throws VcsException {

        // Based on the authentication status, execute the right errorVisitor status and throw
        // the right exception.
        final ServerAuthenticator.AuthenticationStatus status = servCon.getAuthStatus();

        // Order is highly important here.
        if (status.isPasswordUnnecessary()) {
            errorVisitor.passwordUnnecessary(status);
            // Do not raise an exception.
        }
        if (status.isAuthenticated()) {
            if (servCon.getServer() != null) {
                return servCon.getServer();
            }
            throw new IllegalStateException("Authenticated status, but null server");
        }
        if (status.isClientSetupProblem()) {
            final P4InvalidConfigException problem;
            // TODO shouldn't need to do this casting work; instead, use the real problem.
            if (status.getProblem() != null) {
                problem = new P4InvalidConfigException(new P4JavaException(status.getProblem()));
            } else {
                problem = new P4InvalidConfigException(P4Bundle.message("exception.invalid.client",
                        // FIXME use a real parameter here.
                        "<unknown>"));
            }
            errorVisitor.configInvalid(problem);
            throw new HandledVcsException(problem);
        }
        if (status.isPasswordInvalid()) {
            P4VcsConnectionException ex = status.getProblem();
            final P4LoginException problem;
            // TODO shouldn't need to do this casting; instead, use the real problem.
            if (ex == null) {
                problem = new P4PasswordInvalidException(new P4JavaException());
            } else {
                problem = new P4PasswordInvalidException(new P4JavaException(ex));
            }
            errorVisitor.loginFailure(problem);
            throw new HandledVcsException(problem);
        }
        if (status.isPasswordRequired()) {
            // TODO shouldn't need to do this casting; instead, use the real problem.
            final P4PasswordException problem;
            if (status.getProblem() != null && status.getProblem() instanceof P4PasswordException) {
                problem = (P4PasswordException) status.getProblem();
            } else {
                problem = new P4LoginRequiresPasswordException(new P4JavaException());
            }
            // TODO currently investigating ways to prevent a persistent pestering for the password.
            // If it's the same client and server ID, then that means it's easy to skip (keep a record
            // of which configs has been asked; but that might be a memory issue).  Otherwise, this gets
            // trickier.
            LOG.info("Password is required for connection ID " + servCon.getClientConfig().getConfigVersion() + "."
                    + servCon.getClientConfig().getServerConfig().getConfigVersion() + ": " + problem.getMessage());
            errorVisitor.loginRequiresPassword(problem, servCon.getClientConfig());
            throw new HandledVcsException(problem);
        }
        if (status.isSessionExpired()) {
            // This means that the login session is expired, but the re-authentication
            // failed.  This shouldn't happen.
            P4VcsConnectionException ex = status.getProblem();
            final P4LoginException problem;
            // TODO shouldn't need to do this casting; instead, use the real problem.
            if (ex == null) {
                problem = new P4PasswordInvalidException(new P4JavaException());
            } else {
                problem = new P4PasswordInvalidException(new P4JavaException(ex));
            }
            errorVisitor.loginFailure(problem);
            throw new HandledVcsException(problem);
        }
        if (status.isNotConnected()) {
            final P4DisconnectedException problem;
            if (status.getProblem() == null) {
                problem = new P4DisconnectedException();
            } else {
                problem = new P4DisconnectedException(status.getProblem());
            }
            errorVisitor.disconnectFailure(problem);
            throw new HandledVcsException(problem);
        }
        if (status.isNotLoggedIn()) {
            // This status can only happen if the other errors above are true.
            // This shouldn't happen.
            final P4DisconnectedException problem;
            if (status.getProblem() == null) {
                problem = new P4DisconnectedException();
            } else {
                problem = new P4DisconnectedException(status.getProblem());
            }
            errorVisitor.disconnectFailure(problem);
            throw new HandledVcsException(problem);
        }

        // At this point, the status is bad.
        if (servCon.getServer() != null) {
            LOG.warn("Authentication status is in an unknown state: " + status);
            return servCon.getServer();
        }
        throw new IllegalStateException("Authentication status is in an unknown state: " + status);
    }


    static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull Connection conn, @NotNull ErrorVisitor errorVisitor)
            throws VcsException, CancellationException {
        return p4RunFor(runner, conn, errorVisitor, 0);
    }


    private static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull final Connection conn,
            @NotNull ErrorVisitor errorVisitor, int retryCount)
            throws VcsException, CancellationException {

        // Authentication should be done when the connection is made, and NEVER
        // at this point.

        try {
            T ret = runner.run();
            // Command executed without a problem; report the service connection as working.
            conn.onSuccessfulCall();
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
            // Also, this is not classified as a login error.
            throw new P4TimingException(e);
        } catch (LoginRequiresPasswordException e) {
            LOG.info("No password known, and the server requires a password.");
            // Bubble this up to the password handlers
            throw new P4LoginRequiresPasswordException(e);
        } catch (AccessException e) {
            // Most probably a password problem.
            LOG.info("Problem accessing resources (password problem?)", e);
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e) || ExceptionUtil.isSessionExpiredProblem(e)) {
                // We may not have attempted login yet.
                throw new P4UnknownLoginException(e);
            }
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                // This is handled by the outside caller
                throw new P4UnknownLoginException(e);
            }
            throw new P4AccessException(e);
        } catch (ConfigException e) {
            LOG.info("Problem with configuration", e);
            P4InvalidConfigException ex = new P4InvalidConfigException(e);
            errorVisitor.configInvalid(ex);
            throw ex;
        } catch (ConnectionNotConnectedException e) {
            LOG.info("Wasn't connected", e);
            conn.disconnected();
            return retry(runner, conn, errorVisitor, retryCount, e);
        } catch (TrustException e) {
            LOG.info("SSL trust problem", e);
            throw errorVisitor.sslFingerprintError(e);
        } catch (SslHandshakeException e) {
            conn.disconnected();
            if (isUnlimitedStrengthEncryptionInstalled()) {
                throw errorVisitor.sslHandshakeError(e);
            }
            // SSL extensions are not installed, so config is invalid.
            throw errorVisitor.sslKeyStrengthError(e);
        } catch (ConnectionException e) {
            LOG.info("Connection problem", e);
            conn.disconnected();

            if (isSSLFingerprintProblem(e)) {
                // incorrect or not set trust fingerprint
                throw errorVisitor.sslFingerprintError(e);
            }

            if (isSSLHandshakeProblem(e)) {
                if (isUnlimitedStrengthEncryptionInstalled()) {
                    // config not invalid
                    P4DisconnectedException ex = new P4DisconnectedException(e);
                    errorVisitor.disconnectFailure(ex);
                    throw ex;
                } else {
                    // SSL extensions are not installed, so config is invalid.
                    conn.disconnected();
                    throw errorVisitor.sslFingerprintError(e);
                }
            }

            // Ask the user if it should be a real disconnect, or if we should
            // retry.
            return retry(runner, conn, errorVisitor, retryCount, e);
        } catch (FileDecoderException e) {
            // P4ServerName -> client encoding problem
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
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e) || ExceptionUtil.isSessionExpiredProblem(e)) {
                // Bubble this up to the password handlers
                LOG.info("No password known, but one is expected.");
                throw new P4LoginRequiresPasswordException(e);
            }
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                // This could either be a real password problem, or
                // a lost security token issue.
                LOG.info("Don't have a correct password.");
                throw new P4UnknownLoginException(e);
            }

            // The other possibility is the client API implementation is bad.
            LOG.info("Some generic request problem");
            throw new P4ApiException(e);
        } catch (ResourceException e) {
            // The ServerFactory doesn't have the resources available to create a
            // new connection to the server.
            LOG.info("Resource problem", e);
            conn.disconnected();
            P4DisconnectedException ex = new P4DisconnectedException(e);
            errorVisitor.disconnectFailure(ex);
            throw ex;
        } catch (P4JavaException e) {
            if (ExceptionUtil.isLoginRequiresPasswordProblem(e)) {
                // Bubble this up to the password handlers
                throw new P4LoginRequiresPasswordException(e);
            }
            if (ExceptionUtil.isSessionExpiredProblem(e)) {
                // Bubble this up to the password handlers
                throw new P4LoginException(e);
            }
            if (ExceptionUtil.isLoginPasswordProblem(e)) {
                // This could either be a real password problem, or
                // a lost security token issue.
                throw new P4UnknownLoginException(e);
            }

            // TODO have this inspect the underlying source of the problem.
            LOG.info("General Perforce problem", e);
            throw new P4Exception(e);
        } catch (IOException e) {
            LOG.info("IO problem", e);
            throw new P4Exception(e);
        } catch (URISyntaxException e) {
            LOG.info("Invalid URI", e);
            P4InvalidConfigException ex = new P4InvalidConfigException(e);
            errorVisitor.configInvalid(ex);
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
            LOG.info("General error", e);
            throw e;
        } catch (ProcessCanceledException e) {
            LOG.info("Cancelled", e);
            CancellationException ce = new CancellationException(e.getMessage());
            ce.initCause(e);
            throw ce;
        } catch (Throwable t) {
            // Throwable - never catch certain errors.
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

    private static <T> T retry(final P4Runner<T> runner, final Connection conn, final ErrorVisitor errorVisitor,
            final int retryCount, final Exception e) throws VcsException {
        P4WorkingOfflineException ex = new P4WorkingOfflineException(e);
        errorVisitor.disconnectFailure(ex);
        throw ex;
    }


    private static boolean isSSLHandshakeProblem(@NotNull final ConnectionException e) {
        // This check isn't always right - it could be a fingerprint problem in disguise

        String message = e.getMessage();
        return (message != null &&
                message.contains("invalid SSL session"));
    }


    private static boolean isUnlimitedStrengthEncryptionInstalled() {
        try {
            return Cipher.getMaxAllowedKeyLength("RC5") >= 256;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private final static String[] FINGERPRINT_PROBLEMS = {
            "The fingerprint for the public key sent to your client is",
            "The fingerprint for the mismatched key sent to your client is"
    };

    private static boolean isSSLFingerprintProblem(@NotNull final ConnectionException e) {
        // TODO replace with error code checking, but that means changing around the
        // ConnectionException class to support an error code.

        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        for (String fingerprintProblem : FINGERPRINT_PROBLEMS) {
            if (message.contains(fingerprintProblem)) {
                return true;
            }
        }
        return false;
    }
}
