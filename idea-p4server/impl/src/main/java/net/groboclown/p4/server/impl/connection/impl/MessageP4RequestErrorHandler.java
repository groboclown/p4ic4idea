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

package net.groboclown.p4.server.impl.connection.impl;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.ClientError;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.ConnectionNotConnectedException;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.FileSaveException;
import com.perforce.p4java.exception.InvalidImplementationException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.exception.UnknownServerException;
import com.perforce.p4java.exception.ZeroconfException;
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.messagebus.CancellationMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.FileErrorMessage;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.P4ServerErrorMessage;
import net.groboclown.p4.server.api.messagebus.P4WarningMessage;
import net.groboclown.p4.server.impl.connection.P4RequestErrorHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;


public abstract class MessageP4RequestErrorHandler
        extends P4RequestErrorHandler {
    private final Project project;

    public MessageP4RequestErrorHandler(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void handleOnDisconnectError(@NotNull ConnectionException e) {
        LOG.warn("Closing the connection to the Perforce server generated an error", e);
        P4WarningMessage.sendDisconnectCausedError(project, e);
    }

    @Override
    public void handleOnDisconnectError(@NotNull AccessException e) {
        LOG.warn("Closing the connection to the Perforce server generated an error", e);
        P4WarningMessage.sendDisconnectCausedError(project, e);
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    protected abstract String getMessage(@NonNls @NotNull String messageKey, @NotNull Throwable t,
            Object... arguments);

    @NotNull
    @Override
    protected P4CommandRunner.ServerResultException handleError(
            @NotNull ConnectionInfo info, @NotNull Error e) {
        if (e instanceof VirtualMachineError || e instanceof ThreadDeath) {
            throw e;
        }
        LOG.warn("Running an action with the Perforce server generated an error.", e);
        if (e instanceof ClientError) {
            // Happens due to charset translation (input stream reading) failure
            P4WarningMessage.sendCharsetTranslationError(project, (ClientError) e);
            return createServerResultException(e,
                    getMessage("error.ClientError", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof NullPointerError) {
            // Happens due to bug in code (invalid API)
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.NullPointerError", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof ProtocolError) {
            // client API expectations of server responses failed
            InternalErrorMessage.send(project).p4ApiInternalError(e);
            return createServerResultException(e,
                    getMessage("error.ProtocolError", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof UnimplementedError) {
            // server probably supports functions that the client API doesn't
            InternalErrorMessage.send(project).p4ApiInternalError(e);
            return createServerResultException(e,
                    getMessage("error.UnimplementedError", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof P4JavaError) {
            // some other client API issue
            InternalErrorMessage.send(project).p4ApiInternalError(e);
            return createServerResultException(e,
                    getMessage("error.P4JavaError", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        // Unexpected error
        LOG.info("Don't know how to handle this error.", e);
        InternalErrorMessage.send(project).internalError(e);
        return createServerResultException(e,
                getMessage("error.Error", e),
                P4CommandRunner.ErrorCategory.INTERNAL);
    }

    @Nonnull
    @Override
    protected P4CommandRunner.ServerResultException handleException(
            @NotNull ConnectionInfo info, @NotNull Exception e) {
        LOG.warn("Running an action with the Perforce server generated an error.", e);

        // Note: the "if" statements in here should be eliminated as much as possible,
        // and instead pushed into the P4Java plugin.
        if (e instanceof AuthenticationFailedException) {
            AuthenticationFailedException afe = (AuthenticationFailedException) e;
            return handleAuthenticationFailureType(info, e, afe);
        }
        if (e instanceof AccessException) {
            // This exception is abstract.  If we've
            // made it into this block, then that means a new exception type
            // was added, but not updated here.
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.AccessException", e),
                    P4CommandRunner.ErrorCategory.ACCESS_DENIED);
        }
        if (e instanceof ConnectionNotConnectedException) {
            // IOptionsServer was never connected to the server, or there
            // was a configuration error when attempting to reconnect to the
            // server.
            Throwable cause = e.getCause();
            if (cause instanceof ConfigException) {
                // use that cause as the real reason.
                e = (Exception) cause;
                // and fall through to let those config exception
                // checkers process the problem.
            } else {
                // At this point, the exception means that the server wasn't connected.
                // That means there's an API error somewhere that isn't connecting right.
                InternalErrorMessage.send(project).internalError(e);
                return createServerResultException(e,
                        getMessage("error.internal.connection", e),
                        P4CommandRunner.ErrorCategory.INTERNAL);
            }
        }
        if (e instanceof TrustException) {
            Throwable cause = e.getCause();
            if (cause instanceof ConfigException) {
                // There was an underlying config exception that was turned into
                // a trust exception, due to updating one of the client's local SSL
                // files.  Use those config exception handlers instead.
                e = (Exception) cause;
                // and fall through.
            } else {
                TrustException te = (TrustException) e;
                switch (te.getType()) {
                    case INSTALL:
                    case UNINSTALL:
                        // problem while saving the new trust fingerprint to the trust file.
                        // Should have been handled as a ConfigException.
                        InternalErrorMessage.send(project).internalError(e);
                        return createServerResultException(e,
                                getMessage("error.internal.missing-case", e),
                                P4CommandRunner.ErrorCategory.INTERNAL);
                    case NEW_CONNECTION:
                        // server isn't in the fingerprint registry, and the user didn't
                        // accept the new fingerprint.
                        if (info.hasServerConfig()) {
                            ConnectionErrorMessage.send().sslHostTrustNotEstablished(info.getServerConfig());
                            return createServerResultException(e,
                                    getMessage("error.TrustException.NEW_CONNECTION", e),
                                    P4CommandRunner.ErrorCategory.CONNECTION);
                        } else {
                            // No SSL information with the minimal setup.
                            InternalErrorMessage.send(project).internalError(e);
                            return createServerResultException(e,
                                    getMessage("error.internal.no-ssl-info", e),
                                    P4CommandRunner.ErrorCategory.INTERNAL);
                        }
                    case NEW_KEY:
                        // fingerprint on record doesn't match server's fingerprint.
                        if (info.hasServerConfig()) {
                            ConnectionErrorMessage.send().sslHostFingerprintMismatch(info.getServerConfig(), te);
                            return createServerResultException(e,
                                    getMessage("error.TrustException.NEW_KEY", e),
                                    P4CommandRunner.ErrorCategory.CONNECTION);
                        } else {
                            // No SSL information with the minimal setup.
                            InternalErrorMessage.send(project).internalError(e);
                            return createServerResultException(e,
                                    getMessage("error.internal.no-ssl-info", e),
                                    P4CommandRunner.ErrorCategory.INTERNAL);
                        }
                    default:
                        // An unknown trust type, which means that the API was updated
                        // without this block being updated.
                        InternalErrorMessage.send(project).internalError(e);
                        return createServerResultException(e,
                                getMessage("error.internal.trust", e),
                                P4CommandRunner.ErrorCategory.INTERNAL);
                }
            }
            // fall through to config handling.
        }
        if (e instanceof UnknownServerException) {
            ConnectionErrorMessage.send().unknownServer(
                    info.getServerName(), info.getServerConfig(), e);
            return createServerResultException(e,
                    getMessage("error.UnknownServerException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }
        if (e instanceof FileSaveException) {
            if (info.hasServerConfig()) {
                ConnectionErrorMessage.send().couldNotWrite(info.getServerConfig(), (FileSaveException) e);
            } else {
                // Shouldn't happen.  The information to write to a file should only be
                // contained in a ServerConfig.
                InternalErrorMessage.send(project).internalError(e);
            }
            return createServerResultException(e,
                    getMessage("error.FileSaveException", e),
                    P4CommandRunner.ErrorCategory.OS);
        }
        if (e instanceof ZeroconfException) {
            ConnectionErrorMessage.send().zeroconfProblem(
                    info.getServerName(), info.getServerConfig(), (ZeroconfException) e);
            return createServerResultException(e,
                    getMessage("error.ZeroconfException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }
        if (e instanceof InvalidImplementationException) {
            // The IOptionsServer or IClient implementation objects were not found,
            // or were otherwise setup wrong.
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.internal.api", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof SslHandshakeException) {
            if (!isUnlimitedStrengthEncryptionInstalled()) {
                // No such algorithm support - the user probably needs to install the
                // unlimited strength encryption library.
                ConnectionErrorMessage.send().sslAlgorithmNotSupported(
                        info.getServerName(), info.getServerConfig());
                return createServerResultException(e,
                        getMessage("error.SslAlgorithmNotSupportedException", e),
                        P4CommandRunner.ErrorCategory.CONNECTION);
            }

            // The peer was unverified with the ssl connection.
            // It's a SSLPeerUnverifiedException.
            // No certificate, cipher suite doesn't support authentication,
            // no peer authentication established during SSL handshake, user time
            // isn't close enough to the server time.
            ConnectionErrorMessage.send().sslPeerUnverified(
                    info.getServerName(), info.getServerConfig(), (SslHandshakeException) e);
            return createServerResultException(e,
                    getMessage("error.SslHandshakeException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }
        if (e instanceof SslException) {
            ConnectionErrorMessage.send().sslCertificateIssue(
                    info.getServerName(), info.getServerConfig(), (SslException) e);
            return createServerResultException(e,
                    getMessage("error.SslException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }
        if (e instanceof ConnectionException) {
            Throwable cause = e.getCause();
            if (cause instanceof ConfigException) {
                // Handle the exception as though it were the underlying
                // problem.
                e = (ConfigException) cause;
                // and fall through.
            } else if (cause instanceof UnknownHostException) {
                ConnectionErrorMessage.send().unknownServer(
                        info.getServerName(), info.getServerConfig(), e);
                return createServerResultException(e,
                        getMessage("error.UnknownServerException", e),
                        P4CommandRunner.ErrorCategory.CONNECTION);
            } else {
                // General problem with connection, such as socket disconnected mid-stream,
                // the server version is incompatible with the plugin, the server sends
                // garbled information, and so on.
                ConnectionErrorMessage.send().connectionError(
                        info.getServerName(), info.getServerConfig(), (ConnectionException) e);
                return createServerResultException(e,
                        getMessage("error.ConnectionException", e),
                        P4CommandRunner.ErrorCategory.CONNECTION);
            }
        }
        if (e instanceof FileDecoderException) {
            // The client had a problem with a file's character encoding.
            // client <- server encode problem
            FileErrorMessage.send(project).fileSendError(
                    info.getServerName(), info.getServerConfig(),
                    e);
            return createServerResultException(e,
                    getMessage("error.FileDecoderException", e),
                    P4CommandRunner.ErrorCategory.OS);
        }
        if (e instanceof FileEncoderException) {
            // Client -> server encoding problem
            FileErrorMessage.send(project).fileReceiveError(
                    info.getServerName(), info.getServerConfig(),
                    e);
            return createServerResultException(e,
                    getMessage("error.FileEncoderException", e),
                    P4CommandRunner.ErrorCategory.OS);
        }
        if (e instanceof NoSuchObjectException) {
            // Bad arguments to API
            // Signals missing objects within p4java; this is <i>not</i> used for missing objects on the Perforce
            // server side.
            InternalErrorMessage.send(project).p4ApiInternalError(e);
            return createServerResultException(e,
                    getMessage("error.internal.api", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof OptionsException) {
            // Bug in plugin
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.internal.api", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof RequestException) {
            RequestException re = (RequestException) e;
            IServerMessage msg = re.getServerMessage();
            if (msg != null) {
                // There might be some places where the underlying API doesn't perform the correct translation
                // of the IServerMessage to an authentication failed problem.  So add that in here, to be extra
                // careful.  If the p4java code is perfect, then this code snippet would never be used.
                AuthenticationFailedException.ErrorType authFailType = ResultMapParser.getAuthFailType(msg);
                if (authFailType != null) {
                    AuthenticationFailedException afe = new AuthenticationFailedException(authFailType, msg);
                    return handleAuthenticationFailureType(info, e, afe);
                }
                if (msg.isError()) {
                    P4ServerErrorMessage.send(project).requestCausedError(info.getServerName(), info.getServerConfig(),
                            msg, re);
                } else if (msg.isWarning()) {
                    P4ServerErrorMessage.send(project).requestCausedWarning(info.getServerName(), info.getServerConfig(),
                            msg, re);
                } else if (msg.isInfo()) {
                    P4ServerErrorMessage.send(project).requestCausedInfoMsg(info.getServerName(), info.getServerConfig(),
                            msg, re);
                } else {
                    // This state shouldn't happen.
                    InternalErrorMessage.send(project).internalError(e);
                }
            } else {
                Throwable cause = e.getCause();
                if (cause instanceof P4JavaException) {
                    // API wrapped a low-level error.
                    P4ServerErrorMessage.send(project).requestException(info.getServerName(), info.getServerConfig(),
                            (P4JavaException) cause);
                } else {
                    // API generated a pseudo-request error.
                    P4ServerErrorMessage.send(project).requestException(info.getServerName(), info.getServerConfig(),
                            re);
                }
            }
            return createServerResultException(e,
                    getMessage("error.RequestException", e),
                    P4CommandRunner.ErrorCategory.SERVER_ERROR);
        }
        if (e instanceof ResourceException) {
            // The ServerFactory doesn't have the resources available to create a
            // new connection to the server.
            ConnectionErrorMessage.send().resourcesUnavailable(info.getServerName(), info.getServerConfig(),
                    (ResourceException) e);
            return createServerResultException(e,
                    getMessage("error.ResourceException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }


        // Config exceptions must be some of the last exceptions to check, because so many
        // other exceptions are wrapped config exceptions
        if (e instanceof ConfigException) {
            // This exception is abstract.  If we've
            // made it into this block, then that means a new exception type
            // was added, but not updated here.
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.ConfigException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }


        if (e instanceof P4JavaException) {
            // If the p4java API works perfect, then this code is never called.  However, because
            // we can't guarantee that right now or in future updates to the API, we handle it
            // using the old, clunky way.
            AuthenticationFailedException.ErrorType authFailType = ResultMapParser.getAuthFailType(e.getMessage());
            if (authFailType != null) {
                IServerMessage msg = new ServerMessage(Collections.singletonList(
                        new ServerMessage.SingleServerMessage(e.getMessage())
                ));
                AuthenticationFailedException afe = new AuthenticationFailedException(authFailType, msg);
                return handleAuthenticationFailureType(info, e, afe);
            }

            // It's some other server error.
            P4ServerErrorMessage.send(project).requestException(info.getServerName(), info.getServerConfig(),
                    (P4JavaException) e);
            return createServerResultException(e,
                    getMessage("error.P4JavaException", e),
                    P4CommandRunner.ErrorCategory.SERVER_ERROR);
        }
        if (e instanceof IOException) {
            // This shouldn't be a server connection issue, because those are wrapped in ConnectionException classes.
            // That just leaves local file I/O problems.
            FileErrorMessage.send(project).localFileError(info.getServerName(), info.getServerConfig(),
                    (IOException) e);
            return createServerResultException(e,
                    getMessage("error.IOException", e),
                    P4CommandRunner.ErrorCategory.SERVER_ERROR);
        }
        if (e instanceof URISyntaxException) {
            ConnectionErrorMessage.send().connectionError(info.getServerName(), info.getServerConfig(),
                    new ConnectionException(e));
            return createServerResultException(e,
                    getMessage("error.URISyntaxException", e),
                    P4CommandRunner.ErrorCategory.CONNECTION);
        }
        if (e instanceof CancellationException) {
            // A user-requested cancellation of the action.
            CancellationMessage.send(project).cancelled((CancellationException) e);
            return createServerResultException(e,
                    getMessage("error.cancelled", e),
                    P4CommandRunner.ErrorCategory.TIMEOUT);
        }
        if (e instanceof InterruptedException ||
                e instanceof TimeoutException ||
                e instanceof ProcessCanceledException) {
            // InterruptedException: An API requested cancellation of the action.
            // TimeoutException: a limited time window ran out
            // ProcessCanceledException: user cancelled a dialog
            // Change to a cancel
            CancellationException ce = new CancellationException(e.getMessage());
            ce.initCause(e);
            CancellationMessage.send(project).cancelled(ce);
            return createServerResultException(e,
                    getMessage("error.cancelled", e),
                    P4CommandRunner.ErrorCategory.TIMEOUT);
        }
        if (e instanceof VcsException) {
            // Plugin code generated an error that was intended for the Idea VCS system.
            // This shouldn't happen.
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.internal.api", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }
        if (e instanceof RuntimeException) {
            // An API problem.
            InternalErrorMessage.send(project).internalError(e);
            return createServerResultException(e,
                    getMessage("error.internal.api", e),
                    P4CommandRunner.ErrorCategory.INTERNAL);
        }

        // Something else that we didn't anticipate.
        InternalErrorMessage.send(project).unexpectedError(e);
        return createServerResultException(e,
                getMessage("error.internal.api", e),
                P4CommandRunner.ErrorCategory.INTERNAL);
    }

    private static boolean isUnlimitedStrengthEncryptionInstalled() {
        try {
            return Cipher.getMaxAllowedKeyLength("RC5") >= 256;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private P4CommandRunner.ServerResultException handleAuthenticationFailureType(
            @NotNull ConnectionInfo info,
            @NotNull Exception sourceException,
            @NotNull AuthenticationFailedException sourceAfe) {
        AuthenticationFailedException.ErrorType errorType = sourceAfe.getErrorType();
        switch (errorType) {
            case SESSION_EXPIRED:
                if (info.hasServerConfig()) {
                    // FIXME could be handled at the source.
                    LoginFailureMessage.send().sessionExpired(info.getServerConfig(), sourceAfe);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.SESSION_EXPIRED", sourceException),
                            P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                } else {
                    // Internal API error, because there was an expectation for a session, but
                    // no session information was used in the connection.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.internal.login-required", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                }
            case PASSWORD_INVALID:
                if (info.hasServerConfig()) {
                    LoginFailureMessage.send().passwordInvalid(info.getServerConfig(), sourceAfe);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.PASSWORD_INVALID", sourceException),
                            P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                } else {
                    // Internal API error, because there was a login attempt, but
                    // no user information was used in the connection.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.internal.login-required", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                }
            case NOT_LOGGED_IN:
                if (info.hasServerConfig()) {
                    if (info.getServerConfig().usesStoredPassword()) {
                        // User required a password, but the user is considered not logged in.
                        // This usually means that the user specified an empty password, so log in isn't
                        // required, but the server wants a password.
                        LoginFailureMessage.send().passwordInvalid(info.getServerConfig(), sourceAfe);
                        return createServerResultException(sourceException,
                                getMessage("error.AuthenticationFailedException.PASSWORD_INVALID", sourceException),
                                P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                    }
                    // The API should have performed a log in.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.NOT_LOGGED_IN", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                } else {
                    // This is an internal API usage error.  The plugin code should
                    // have already logged the user in, or a command was run that didn't
                    // expect to need to log in, but it was required.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.NOT_LOGGED_IN", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                }
            case SSO_LOGIN:
                if (info.hasServerConfig()) {
                    LoginFailureMessage.send().singleSignOnFailed(info.getServerConfig(), sourceAfe);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.SSO_LOGIN", sourceException),
                            P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                } else {
                    // Tried accessing an SSO capable server but with only the server name.
                    // This is doomed to fail.  It also means that the internal API code needs
                    // to be fixed to take SSO information along with the server port.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.internal.login-required", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                }
            case PASSWORD_UNNECESSARY:
                if (info.hasServerConfig()) {
                    // By having an explicit message for an unnecessary password, the
                    // rest of the code could perform corrective action.
                    // FIXME could be handled at the connection level.
                    LoginFailureMessage.send().passwordUnnecessary(info.getServerConfig(), sourceAfe);
                    return createServerResultException(sourceException,
                            getMessage("error.AuthenticationFailedException.PASSWORD_UNNECESSARY", sourceException),
                            P4CommandRunner.ErrorCategory.ACCESS_DENIED);
                } else {
                    // Tried accessing an SSO capable server but with only the server name.
                    // This is doomed to fail.  It also means that the internal API code needs
                    // to be fixed to take SSO information along with the server port.
                    InternalErrorMessage.send(project).internalError(sourceException);
                    return createServerResultException(sourceException,
                            getMessage("error.internal.login-required", sourceException),
                            P4CommandRunner.ErrorCategory.INTERNAL);
                }
            default:
                // FIXME clean up
                throw new IllegalStateException("Unexpected error type", sourceException);
        }
    }
}
