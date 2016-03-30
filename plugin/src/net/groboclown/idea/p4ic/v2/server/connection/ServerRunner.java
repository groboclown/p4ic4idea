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
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.exception.*;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.*;
import net.groboclown.idea.p4ic.v2.server.connection.AuthenticatedServer.AuthenticationResult;
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
public class ServerRunner {
    private static final Logger LOG = Logger.getInstance(ServerRunner.class);


    interface P4Runner<T> {
        T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                P4Exception;
    }


    interface ErrorVisitor {
        /**
         * Login is considered to be invalid.
         *
         * @param e source error
         * @return the actual exception to throw
         * @throws VcsException
         * @throws CancellationException
         */
        @NotNull
        P4LoginException loginFailure(P4JavaException e)
                throws VcsException, CancellationException;

        /**
         * Login is considered to be invalid.
         *
         * @param e
         * @throws VcsException
         * @throws CancellationException
         */
        void loginFailure(P4LoginException e)
                throws VcsException, CancellationException;

        /**
         * The server refuses to reauthenticate a connection.
         *
         * @param e
         * @throws VcsException
         * @throws CancellationException
         */
        void retryAuthorizationFailure(P4RetryAuthenticationException e)
                throws VcsException, CancellationException;

        void disconnectFailure(P4DisconnectedException e)
                throws VcsException, CancellationException;

        /**
         * Full handling of an invalid config.  Should include
         * AlertManager.getInstance().addCriticalError(new ConfigurationProblemHandler(project, connectedController, e), ex);
         *
         * @param e
         * @throws VcsException
         * @throws CancellationException
         */
        void configInvalid(P4InvalidConfigException e)
                throws VcsException, CancellationException;

        @NotNull
        P4SSLFingerprintException sslFingerprintError(ConnectionException e);
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


        AuthenticationResult authenticate()
                throws P4JavaException, URISyntaxException;
    }


    static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull Connection conn, @NotNull ErrorVisitor errorVisitor)
            throws VcsException, CancellationException {
        return p4RunFor(runner, conn, errorVisitor, 0);
    }


    private static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull final Connection conn,
            @NotNull ErrorVisitor errorVisitor, int retryCount)
            throws VcsException, CancellationException {
        try {
            return p4RunWithSkippedPasswordCheck(runner, conn, errorVisitor, retryCount);
        } catch (P4UnknownLoginException e) {
            // Don't know the kind of password problem.  It was a
            // "password not set or invalid", which means that the server could
            // have dropped the security token, and we need a new one.

            final AuthenticationResult authenticationResult =
                    p4RunWithSkippedPasswordCheck(new P4Runner<AuthenticationResult>() {
                        @Override
                        public AuthenticationResult run()
                                throws P4JavaException, IOException, InterruptedException, TimeoutException,
                                URISyntaxException,
                                P4Exception {
                            LOG.info("Encountered password issue; attempting to reauthenticate");
                            return conn.authenticate();
                        }
                    }, conn, errorVisitor,
                    /* first attempt at this login attempt, so retry is 0 */ 0);
            switch (authenticationResult) {
                case AUTHENTICATED: {
                    // Just fine; no errors.
                    return retry(runner, conn, errorVisitor, retryCount, e);
                }
                case INVALID_LOGIN: {
                    P4LoginException ex = new P4LoginException(e);
                    errorVisitor.loginFailure(ex);
                    throw ex;
                }
                case NOT_CONNECTED: {
                    P4DisconnectedException ex = new P4DisconnectedException(e);
                    errorVisitor.disconnectFailure(ex);
                    throw ex;
                }
                case RELOGIN_FAILED: {
                    final P4RetryAuthenticationException ex = new P4RetryAuthenticationException(e);
                    errorVisitor.retryAuthorizationFailure(ex);
                    return retry(runner, conn, errorVisitor, retryCount, e);
                }
                default: {
                    throw e;
                }
            }
        } catch (P4LoginException e) {
            errorVisitor.loginFailure(e);
            throw e;
        } catch (P4RetryAuthenticationException e) {
            errorVisitor.retryAuthorizationFailure(e);
            return retry(runner, conn, errorVisitor, retryCount, e);
        }
    }


    private static <T> T p4RunWithSkippedPasswordCheck(@NotNull P4Runner<T> runner, @NotNull Connection conn,
            @NotNull ErrorVisitor errorVisitor, int retryCount)
            throws VcsException, CancellationException {
        // Must check offline status
        if (conn.isWorkingOffline()) {
            // should never get to this point; the online/offline status should
            // have already been determined before entering this method.
            conn.disconnected();
            throw new P4WorkingOfflineException();
        }

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
            throw new P4LoginException(e);
        } catch (AccessException e) {
            // Most probably a password problem.
            LOG.info("Problem accessing resources (password problem?)", e);
            if (isPasswordProblem(e)) {
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

            if (isPasswordProblem(e)) {
                // This could either be a real password problem, or
                // a lost security token issue.
                throw new P4UnknownLoginException(e);
            }

            // The other possibility is the client API implementation is bad.
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
            LOG.info("General Perforce problem", e);

            // TODO have this inspect the underlying source of the problem.

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

    private static <T> T retry(final P4Runner<T> runner, final Connection conn, final ErrorVisitor errorVisitor,
            final int retryCount, final Exception e) throws VcsException {
        if (retryCount > 1) {
            P4WorkingOfflineException ex = new P4WorkingOfflineException(e);
            errorVisitor.disconnectFailure(ex);
            throw ex;
        }
        return p4RunFor(runner, conn, errorVisitor, retryCount + 1);
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


    private static boolean isSSLFingerprintProblem(@NotNull final ConnectionException e) {
        // TODO replace with error code checking

        String message = e.getMessage();
        return message != null &&
                message.contains("The fingerprint for the public key sent to your client is");
    }
}
