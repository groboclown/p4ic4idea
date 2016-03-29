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

// THIS IS CURRENTLY A WORK-IN-PROGRESS.
// It's trying to rip out the exception and error handling logic into a single
// place.


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


//    interface P4Runner<T> {
//        T run() throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
//                P4Exception;
//    }
//
//
//    interface ErrorVisitor {
//        /**
//         * Login is considered to be invalid.
//         *
//         * @param e source error
//         * @return the actual exception to throw
//         * @throws VcsException
//         * @throws CancellationException
//         */
//        @NotNull
//        P4LoginException loginFailure(P4JavaException e)
//                throws VcsException, CancellationException;
//
//        /**
//         * Login is considered to be invalid.
//         *
//         * @param e
//         * @throws VcsException
//         * @throws CancellationException
//         */
//        void loginFailure(P4LoginException e)
//                throws VcsException, CancellationException;
//
//        /**
//         * The server refuses to reauthenticate a connection.
//         *
//         * @param e
//         * @throws VcsException
//         * @throws CancellationException
//         */
//        void retryAuthorizationFailure(P4RetryAuthenticationException e)
//                throws VcsException, CancellationException;
//
//        void disconnectFailure(P4DisconnectedException e)
//                throws VcsException, CancellationException;
//
//        /**
//         * Full handling of an invalid config.  Should include
//         * AlertManager.getInstance().addCriticalError(new ConfigurationProblemHandler(project, connectedController, e), ex);
//         *
//         * @param e
//         * @throws VcsException
//         * @throws CancellationException
//         */
//        void configInvalid(P4InvalidConfigException e)
//                throws VcsException, CancellationException;
//
//        @NotNull
//        P4SSLFingerprintException sslFingerprintError(ConnectionException e);
//    }
//
//    interface Connection {
//        /**
//         * Called when the connection is considered to be invalid, such as when it is
//         * working in offline mode, or the server indicates that the connection is incorrect.
//         */
//        void disconnected();
//
//        /**
//         *
//         * @return true if the connection to the server should not be made.
//         */
//        boolean isWorkingOffline();
//
//        /**
//         * When a non-error situation happens.
//         */
//        void onSuccessfulCall();
//
//
//        AuthenticationResult authenticate();
//    }
//
//
//    static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull Connection conn, @NotNull ErrorVisitor errorVisitor)
//            throws VcsException, CancellationException {
//        return p4RunFor(runner, conn, errorVisitor, 0);
//    }
//
//
//    private static <T> T p4RunFor(@NotNull P4Runner<T> runner, @NotNull Connection conn,
//            @NotNull ErrorVisitor errorVisitor, int retryCount)
//            throws VcsException, CancellationException {
//        try {
//            return p4RunWithSkippedPasswordCheck(runner, conn, errorVisitor, retryCount);
//        } catch (P4UnknownLoginException e) {
//            // FIXME don't know the kind of password problem.  It was a
//            // "password not set or invalid", which means that the server could
//            // have dropped the security token, and we need a new one.
//
//        } catch (P4DisconnectedException e) {
//
//            if (isPasswordProblem(e)) {
//                try {
//                    return onPasswordProblem(e, runner, errorVisitor, retryCount);
//                } catch (P4LoginException pwEx) {
//                    errorVisitor.loginFailure(pwEx);
//                    throw pwEx;
//                } catch (P4RetryAuthenticationException pwEx) {
//                    errorVisitor.retryAuthorizationFailure(pwEx);
//                    throw pwEx;
//                } catch (P4JavaException pwEx) {
//                    if (isPasswordProblem(pwEx)) {
//                        // just bad.
//                        throw errorVisitor.loginFailure(e);
//                    }
//                    throw new P4Exception(pwEx);
//                }
//            }
//            // Probably a problem with the user unable to perform an operation because
//            // they don't have explicit permissions to perform it (such as writing to
//            // a part of the depot that's protected).
//            throw new P4Exception(e);
//        }
//    }
//
//
//    private static <T> T p4RunWithSkippedPasswordCheck(@NotNull P4Runner<T> runner, @NotNull Connection conn,
//            @NotNull ErrorVisitor errorVisitor, int retryCount)
//            throws VcsException, CancellationException {
//        // Must check offline status
//        if (conn.isWorkingOffline()) {
//            // should never get to this point; the online/offline status should
//            // have already been determined before entering this method.
//            conn.disconnected();
//            throw new P4WorkingOfflineException();
//        }
//
//        try {
//            T ret = runner.run();
//            // Command executed without a problem; report the service connection as working.
//            conn.onSuccessfulCall();
//            return ret;
//        } catch (ClientError e) {
//            // Happens due to charset translation (input stream reading) failure
//            LOG.warn("ClientError in P4JavaApi", e);
//            throw new P4ApiException(e);
//        } catch (NullPointerError e) {
//            // Happens due to bug in code (invalid API)
//            LOG.warn("NullPointerException in P4JavaApi", e);
//            throw new P4ApiException(e);
//        } catch (ProtocolError e) {
//            // client API expectations of server responses failed
//            LOG.warn("ProtocolError in P4JavaApi", e);
//            throw new P4ServerProtocolException(e);
//        } catch (UnimplementedError e) {
//            // server probably supports functions that the client API doesn't
//            LOG.warn("Unimplemented API in P4JavaApi", e);
//            throw new P4ApiException(e);
//        } catch (P4JavaError e) {
//            // some other client API issue
//            LOG.warn("General error in P4JavaApi", e);
//            throw new P4ApiException(e);
//        } catch (PasswordAccessedWrongException e) {
//            LOG.info("Could not get the password yet");
//            // Don't explicitly tell the user about this; it will show up eventually.
//            // Also, this is not classified as a login error.
//            throw new P4TimingException(e);
//        } catch (LoginRequiresPasswordException e) {
//            LOG.info("No password known, and the server requires a password.");
//            // Bubble this up to the password handlers
//            throw new P4LoginException(e);
//        } catch (AccessException e) {
//            // Most probably a password problem.
//            LOG.info("Problem accessing resources (password problem?)", e);
//            if (isPasswordProblem(e)) {
//                // This is handled by the outside caller
//                throw new P4UnknownLoginException(e);
//            }
//            throw new P4AccessException(e);
//        } catch (ConfigException e) {
//            LOG.info("Problem with configuration", e);
//            P4InvalidConfigException ex = new P4InvalidConfigException(e);
//            errorVisitor.configInvalid(ex);
//            throw ex;
//        } catch (ConnectionNotConnectedException e) {
//            LOG.info("Wasn't connected", e);
//            conn.disconnected();
//            if (retryCount > 1) {
//                P4WorkingOfflineException ex = new P4WorkingOfflineException(e);
//                errorVisitor.disconnectFailure(ex);
//                throw ex;
//            } else {
//                return p4RunFor(runner, conn, errorVisitor, retryCount + 1);
//            }
//        } catch (TrustException e) {
//            LOG.info("SSL trust problem", e);
//            throw errorVisitor.sslFingerprintError(e);
//        } catch (ConnectionException e) {
//            LOG.info("Connection problem", e);
//            conn.disconnected();
//
//            if (isSSLFingerprintProblem(e)) {
//                // incorrect or not set trust fingerprint
//                throw errorVisitor.sslFingerprintError(e);
//            }
//
//
//            if (isSSLHandshakeProblem(e)) {
//                if (isUnlimitedStrengthEncryptionInstalled()) {
//                    // config not invalid
//                    P4DisconnectedException ex = new P4DisconnectedException(e);
//                    errorVisitor.disconnectFailure(ex);
//                    throw ex;
//                } else {
//                    // SSL extensions are not installed, so config is invalid.
//                    conn.disconnected();
//                    throw errorVisitor.sslFingerprintError(e);
//                }
//            }
//
//            // Ask the user if it should be a real disconnect, or if we should
//            // retry.
//            if (retryCount > 1) {
//                conn.disconnected();
//                P4DisconnectedException ex = new P4DisconnectedException(e);
//                errorVisitor.disconnectFailure(ex);
//                throw ex;
//            }
//            return p4RunFor(runner, conn, errorVisitor, retryCount + 1);
//        } catch (FileDecoderException e) {
//            // Server -> client encoding problem
//            LOG.info("File decoder problem", e);
//            throw new P4FileException(e);
//        } catch (FileEncoderException e) {
//            // Client -> server encoding problem
//            LOG.info("File encoder problem", e);
//            throw new P4FileException(e);
//        } catch (NoSuchObjectException e) {
//            // Bad arguments to API
//            LOG.info("No such object problem", e);
//            throw new P4Exception(e);
//        } catch (OptionsException e) {
//            // Bug in plugin
//            LOG.info("Input options problem", e);
//            throw new P4Exception(e);
//        } catch (RequestException e) {
//            LOG.info("Request problem", e);
//
//            if (isPasswordProblem(e)) {
//                // This could either be a real password problem, or
//                // a lost security token issue.
//                throw new P4UnknownLoginException(e);
//            }
//
//            // The other possibility is the client API implementation is bad.
//            throw new P4ApiException(e);
//        } catch (ResourceException e) {
//            // The ServerFactory doesn't have the resources available to create a
//            // new connection to the server.
//            LOG.info("Resource problem", e);
//            conn.disconnected();
//            P4DisconnectedException ex = new P4DisconnectedException(e);
//            errorVisitor.disconnectFailure(ex);
//            throw ex;
//        } catch (P4JavaException e) {
//            LOG.info("General Perforce problem", e);
//
//            // TODO have this inspect the underlying source of the problem.
//
//            throw new P4Exception(e);
//        } catch (IOException e) {
//            LOG.info("IO problem", e);
//            throw new P4Exception(e);
//        } catch (URISyntaxException e) {
//            LOG.info("Invalid URI", e);
//            P4InvalidConfigException ex = new P4InvalidConfigException(e);
//            errorVisitor.configInvalid(ex);
//            throw ex;
//        } catch (CancellationException e) {
//            // A user-requested cancellation of the action.
//            // no need to handle; it's part of the throw clause
//            LOG.info("Cancelled", e);
//            throw e;
//        } catch (InterruptedException e) {
//            // An API requested cancellation of the action.
//            // Change to a cancel
//            LOG.info("Cancelled", e);
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        } catch (TimeoutException e) {
//            // the equivalent of a cancel, because the limited time window
//            // ran out.
//            LOG.info("Timed out", e);
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        } catch (VcsException e) {
//            // Plugin code generated error
//            throw e;
//        } catch (ProcessCanceledException e) {
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        } catch (Throwable t) {
//            VcsExceptionUtil.alwaysThrown(t);
//            if (t.getMessage() != null &&
//                    t.getMessage().equals("Task was cancelled.")) {
//                CancellationException ce = new CancellationException(t.getMessage());
//                ce.initCause(t);
//                throw ce;
//            }
//            LOG.warn("Unexpected exception", t);
//            throw new P4Exception(t);
//        }
//    }
//
//
//    private static boolean isPasswordProblem(@NotNull P4JavaException ex) {
//        // TODO replace with error code checking
//
//        if (ex instanceof RequestException) {
//            RequestException rex = (RequestException) ex;
//            return (rex.hasMessageFragment("Your session has expired, please login again.")
//                    || rex.hasMessageFragment("Perforce password (P4PASSWD) invalid or unset."));
//        }
//        if (ex instanceof AccessException) {
//            AccessException aex = (AccessException) ex;
//            // see Server for a list of the authentication failure messages.
//            return aex.hasMessageFragment("Perforce password (P4PASSWD)");
//        }
//        //if (ex instanceof LoginRequiresPasswordException) {
//        //    LOG.info("No password specified, but one is needed", ex);
//        //    return false;
//        //}
//        return false;
//    }
//
//
//    private static <T> void handleConnectionException(
//            final @NotNull Connection conn, final @NotNull ErrorVisitor errorVisitor,
//            @NotNull Throwable e)
//            throws VcsException, CancellationException, Retry {
//        // Note that this is not as nice a way to handle exceptions,
//        // as the try/catch with multiple catches, but it allows for
//        // deep inspection of the exception cause for the cases where
//        // the real exception is wrapped.
//
//        // Some things must never be handled
//        VcsExceptionUtil.alwaysThrown(e);
//
//        // TODO has a big performance improvement, extract out the
//        // cause trace earlier, so we don't duplicate this effort and data
//        // construction.  It should probably be used as the constructor
//        // argument for ExH.
//
//        // A long chained call stack, rather than a set of if statements.
//        // Must take care that the order is correct for the heirarchy.  In a
//        // standard if block or exception handling block, the compilers/editors
//        // handle this for us; here, we don't have that luxury.
//        boolean handled = handle(ClientError.class, e, new ExH<ClientError>() {
//            @Override
//            public boolean on(@NotNull final ClientError e) throws P4ApiException {
//                // Happens due to charset translation (input stream reading) failure
//                LOG.warn("ClientError in P4JavaApi", e);
//                throw new P4ApiException(e);
//            }
//        })
//        || handle(NullPointerError.class, e, new ExH<NullPointerError>() {
//            @Override
//            public boolean on(@NotNull final NullPointerError e) throws VcsException, CancellationException {
//                // Happens due to bug in code (invalid API)
//                LOG.warn("NullPointerException in P4JavaApi", e);
//                throw new P4ApiException(e);
//            }
//        })
//        || handle(ProtocolError.class, e, new ExH<ProtocolError>() {
//            @Override
//            public boolean on(@NotNull final ProtocolError e) throws VcsException, CancellationException {
//                // client API expectations of server responses failed
//                LOG.warn("ProtocolError in P4JavaApi", e);
//                throw new P4ServerProtocolException(e);
//            }
//        })
//        || handle(UnimplementedError.class, e, new ExH<UnimplementedError>() {
//            @Override
//            public boolean on(@NotNull final UnimplementedError e) throws VcsException, CancellationException {
//                // server probably supports functions that the client API doesn't
//                LOG.warn("Unimplemented API in P4JavaApi", e);
//                throw new P4ApiException(e);
//            }
//        })
//        || handle(P4JavaError.class, e, new ExH<P4JavaError>() {
//            @Override
//            public boolean on(@NotNull final P4JavaError e) throws VcsException, CancellationException {
//                // some other client API issue
//                LOG.warn("General error in P4JavaApi", e);
//                throw new P4ApiException(e);
//            }
//        })
//        || handle(PasswordAccessedWrongException.class, e, new ExH<PasswordAccessedWrongException>() {
//            @Override
//            public boolean on(@NotNull final PasswordAccessedWrongException e)
//                    throws VcsException, CancellationException {
//                LOG.info("Could not get the password yet");
//                // Don't explicitly tell the user about this; it will show up eventually.
//                // Also, this is not classified as a login error.
//                throw new P4TimingException(e);
//            }
//        })
//        || handle(LoginRequiresPasswordException.class, e, new ExH<LoginRequiresPasswordException>() {
//            @Override
//            public boolean on(@NotNull final LoginRequiresPasswordException e)
//                    throws VcsException, CancellationException {
//                LOG.info("No password known, and the server requires a password.");
//                // Bubble this up to the password handlers
//                throw new P4LoginException(e);
//            }
//        })
//        || handle(AccessException.class, e, new ExH<AccessException>() {
//            @Override
//            public boolean on(@NotNull final AccessException e) throws VcsException, CancellationException {
//                // Most probably a password problem.
//                LOG.info("Problem accessing resources", e);
//                if (isPasswordProblem(e)) {
//                    // This is handled by the outside caller
//                    throw new P4UnknownLoginException(e);
//                }
//                throw new P4AccessException(e);
//            }
//        })
//        || handle(ConfigException.class, e, new ExH<ConfigException>() {
//            @Override
//            public boolean on(@NotNull final ConfigException e) throws VcsException, CancellationException {
//                LOG.info("Problem with configuration", e);
//                P4InvalidConfigException ex = new P4InvalidConfigException(e);
//                errorVisitor.configInvalid(ex);
//                throw ex;
//            }
//        })
//        || handle(ConnectionNotConnectedException.class, e, new ExH<ConnectionNotConnectedException>() {
//            @Override
//            public boolean on(@NotNull final ConnectionNotConnectedException e)
//                    throws VcsException, CancellationException, Retry {
//                LOG.info("Wasn't connected", e);
//                conn.disconnected();
//                throw new Retry(e);
//
//                // FIXME Retry handler!!!!
//                // if (retryCount > 1) {
//                //     P4WorkingOfflineException ex = new P4WorkingOfflineException(e);
//                //     errorVisitor.disconnectFailure(ex);
//                //     throw ex;
//            }
//        })
//        || handle(TrustException.class, e, new ExH<TrustException>() {
//            @Override
//            public boolean on(@NotNull final TrustException e) throws VcsException, CancellationException, Retry {
//                LOG.info("SSL trust problem", e);
//                throw errorVisitor.sslFingerprintError(e);
//            }
//        })
//        || handle(ConnectionException.class, e, new ExH<ConnectionException>() {
//            @Override
//            public boolean on(@NotNull final ConnectionException e) throws VcsException, CancellationException, Retry {
//                LOG.info("Connection problem", e);
//                conn.disconnected();
//
//                if (isSSLFingerprintProblem(e)) {
//                    // incorrect or not set trust fingerprint
//                    throw errorVisitor.sslFingerprintError(e);
//                }
//
//                if (isSSLHandshakeProblem(e)) {
//                    if (isUnlimitedStrengthEncryptionInstalled()) {
//                        // config not invalid
//                        P4DisconnectedException ex = new P4DisconnectedException(e);
//                        errorVisitor.disconnectFailure(ex);
//                        throw ex;
//                    } else {
//                        // SSL extensions are not installed, so config is invalid.
//                        conn.disconnected();
//                        throw errorVisitor.sslFingerprintError(e);
//                    }
//                }
//
//                // Ask the user if it should be a real disconnect, or if we should
//                // retry.
//                throw new Retry(e);
//            }
//        })
//        || handle(FileDecoderException.class, e, new ExH<FileDecoderException>() {
//            @Override
//            public boolean on(@NotNull final FileDecoderException e) throws VcsException, CancellationException, Retry {
//                // Server -> client encoding problem
//                LOG.info("File decoder problem", e);
//                throw new P4FileException(e);
//            }
//        })
//        || handle(FileEncoderException.class, e, new ExH<FileEncoderException>() {
//            @Override
//            public boolean on(@NotNull final FileEncoderException e) throws VcsException, CancellationException, Retry {
//                // Client -> server encoding problem
//                LOG.info("File encoder problem", e);
//                throw new P4FileException(e);
//            }
//        })
//        || handle(NoSuchObjectException.class, e, new ExH<NoSuchObjectException>() {
//            @Override
//            public boolean on(@NotNull final NoSuchObjectException e)
//                    throws VcsException, CancellationException, Retry {
//                // Bad arguments to API
//                LOG.info("No such object problem", e);
//                throw new P4Exception(e);
//            }
//        })
//        || handle(OptionsException.class, e, new ExH<OptionsException>() {
//            @Override
//            public boolean on(@NotNull final OptionsException e) throws VcsException, CancellationException, Retry {
//                // Bug in plugin
//                LOG.info("Input options problem", e);
//                throw new P4Exception(e);
//            }
//        })
//        || handle()
//        /*
//        } catch (RequestException e) {
//            LOG.info("Request problem", e);
//
//            if (isPasswordProblem(e)) {
//                // This could either be a real password problem, or
//                // a lost security token issue.
//                throw new P4UnknownLoginException(e);
//            }
//
//            // The other possibility is the client API implementation is bad.
//            throw new P4ApiException(e);
//        } catch (ResourceException e) {
//            // The ServerFactory doesn't have the resources available to create a
//            // new connection to the server.
//            LOG.info("Resource problem", e);
//            conn.disconnected();
//            P4DisconnectedException ex = new P4DisconnectedException(e);
//            errorVisitor.disconnectFailure(ex);
//            throw ex;
//        } catch (P4JavaException e) {
//            LOG.info("General Perforce problem", e);
//
//            // TODO have this inspect the underlying source of the problem.
//
//            throw new P4Exception(e);
//        } catch (IOException e) {
//            LOG.info("IO problem", e);
//            throw new P4Exception(e);
//        } catch (URISyntaxException e) {
//            LOG.info("Invalid URI", e);
//            P4InvalidConfigException ex = new P4InvalidConfigException(e);
//            errorVisitor.configInvalid(ex);
//            throw ex;
//        } catch (CancellationException e) {
//            // A user-requested cancellation of the action.
//            // no need to handle; it's part of the throw clause
//            LOG.info("Cancelled", e);
//            throw e;
//        } catch (InterruptedException e) {
//            // An API requested cancellation of the action.
//            // Change to a cancel
//            LOG.info("Cancelled", e);
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        } catch (TimeoutException e) {
//            // the equivalent of a cancel, because the limited time window
//            // ran out.
//            LOG.info("Timed out", e);
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        } catch (VcsException e) {
//            // Plugin code generated error
//            throw e;
//        } catch (ProcessCanceledException e) {
//            CancellationException ce = new CancellationException(e.getMessage());
//            ce.initCause(e);
//            throw ce;
//        }
//         */
//        || handle(VcsException.class, e, new ExH<VcsException>() {
//            @Override
//            public boolean on(@NotNull final VcsException e)
//                    throws VcsException, CancellationException {
//                // generic VcsException handler
//                throw e;
//            }
//        })
//        || handle(Error.class, e, new ExH<Error>() {
//            @Override
//            public boolean on(@NotNull final Error e) throws VcsException, CancellationException {
//                // Generic error handler
//                if ("Task was cancelled.".equals(e.getMessage())) {
//                    CancellationException ce = new CancellationException(t.getMessage());
//                    ce.initCause(e);
//                    throw ce;
//                }
//                throw e;
//            }
//        })
//        || handle(RuntimeException.class, e, new ExH<RuntimeException>() {
//            @Override
//            public boolean on(@NotNull final RuntimeException e) throws VcsException, CancellationException {
//                // Generic runtime exception handler
//                throw e;
//            }
//        });
//
//        // Generic throwable handler
//        // Generic error handler
//        if ("Task was cancelled.".equals(e.getMessage())) {
//            CancellationException ce = new CancellationException(t.getMessage());
//            ce.initCause(e);
//            throw ce;
//        }
//        LOG.warn("Unexpected exception", e);
//        throw new P4Exception(e);
//    }
//
//
//    private static <T extends Throwable> boolean handle(@NotNull Class<T> cause, @Nullable Throwable t,
//            ExH<T> callable) throws VcsException, CancellationException, Retry {
//        T actual = isThrownFromA(cause, t, new HashSet<Throwable>());
//        if (actual != null) {
//            LOG.info("Handling " + t.getClass().getSimpleName() + " as " +
//                actual.getClass().getSimpleName(), t);
//            return callable.on(actual);
//        }
//        return false;
//    }
//
//
//    @Nullable
//    private static <T extends Throwable> T isThrownFromA(@NotNull Class<T> cause,
//            @Nullable Throwable t, @NotNull Set<Throwable> causeStack) {
//        if (t == null) {
//            return null;
//        }
//        if (causeStack.contains(t)) {
//            return null;
//        }
//        if (cause.isInstance(t)) {
//            return cause.cast(t);
//        }
//        causeStack.add(t);
//        return isThrownFromA(cause, t.getCause(), causeStack);
//    }
//
//
//    private static <T> T onPasswordProblem(@NotNull final P4JavaException e, @NotNull final P4Runner<T> runner,
//            @NotNull ErrorVisitor errorVisitor, final int retryCount) throws VcsException, P4JavaException {
//        if (e.getCause() != null && (e.getCause() instanceof PasswordAccessedWrongException)) {
//            // not a login failure.
//            throw e;
//        }
//        final AuthenticatedServer server = cachedServer;
//        if (server == null) {
//            throw new P4JavaException("Disconnected server", e);
//        }
//        final AuthenticationResult authenticationResult =
//                p4RunWithSkippedPasswordCheck(project, new P4Runner<AuthenticationResult>() {
//                    @Override
//                    public AuthenticationResult run()
//                            throws P4JavaException, IOException, InterruptedException, TimeoutException,
//                            URISyntaxException,
//                            P4Exception {
//                        LOG.info("Encountered password issue; attempting to reauthenticate " + cachedServer);
//                        return server.authenticate();
//                    }
//                }, /* first attempt at this login attempt, so retry is 0 */ 0);
//        switch (authenticationResult) {
//            case AUTHENTICATED:
//                // Just fine; no errors.
//                return p4RunWithSkippedPasswordCheck(project, runner, retryCount + 1);
//            case INVALID_LOGIN:
//                throw loginFailure(project, getServerConfig(), getServerConnectedController(), e);
//            case NOT_CONNECTED:
//                throw disconnectedFailure(project, getServerConfig(), getServerConnectedController(), e);
//            case RELOGIN_FAILED:
//                // FIXME
//                throw new P4RetryAuthenticationException(project, config, e);
//            default:
//                throw e;
//        }
//    }
//
//
//    static P4LoginException loginFailure(@NotNull Project project, @NotNull ServerConfig config,
//            @NotNull ServerConnectedController controller, @NotNull final P4JavaException e) {
//        LOG.info("Gave up on trying to login.  Showing critical error.");
//        P4LoginException ex = new P4LoginException(project, config, e);
//        AlertManager.getInstance().addCriticalError(
//                new LoginFailedHandler(project, controller, config, e), ex);
//        return ex;
//    }
//
//
//    static P4DisconnectedException disconnectedFailure(@NotNull Project project, @NotNull ServerConfig config,
//            @NotNull ServerConnectedController controller, @NotNull final P4JavaException e) {
//        P4DisconnectedException ret = new P4DisconnectedException(project, config, e);
//        AlertManager.getInstance().addCriticalError(
//                new DisconnectedHandler(project, controller, ret), ret);
//        return ret;
//    }
//
//
//    private static void authenticateServer(@Nullable Project project, @NotNull AuthenticatedServer server,
//            @NotNull ServerConfig config, @Nullable P4JavaException e) throws P4JavaException, P4DisconnectedException {
//        switch (server.authenticate()) {
//            case AUTHENTICATED:
//                // Just fine; no errors.
//                return;
//            case INVALID_LOGIN:
//                throw new P4LoginException(project, config, e);
//            case NOT_CONNECTED:
//                throw new P4DisconnectedException(project, config, e);
//            case RELOGIN_FAILED:
//                throw new P4RetryAuthenticationException(project, config, e);
//            default:
//                throw e;
//        }
//    }
//
//
//    private static boolean isSSLHandshakeProblem(@NotNull final ConnectionException e) {
//        // This check isn't always right - it could be a fingerprint problem in disguise
//
//        String message = e.getMessage();
//        return (message != null &&
//                message.contains("invalid SSL session"));
//    }
//
//
//    private static boolean isUnlimitedStrengthEncryptionInstalled() {
//        try {
//            return Cipher.getMaxAllowedKeyLength("RC5") >= 256;
//        } catch (NoSuchAlgorithmException e) {
//            return false;
//        }
//    }
//
//
//    private static boolean isSSLFingerprintProblem(@NotNull final ConnectionException e) {
//        // TODO replace with error code checking
//
//        String message = e.getMessage();
//        return message != null &&
//                message.contains("The fingerprint for the public key sent to your client is");
//    }
//
//
//
//    private interface ExH<T extends Throwable> {
//        boolean on(@NotNull T e)
//            throws VcsException, CancellationException, Retry;
//    }
//
//
//    private static class Retry extends Throwable {
//        private final P4JavaException src;
//
//        private Retry(final P4JavaException src) {
//            this.src = src;
//        }
//    }
}
