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

package net.groboclown.p4plugin.components;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.ClientError;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileSaveException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.exception.ZeroconfException;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.CancellationMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.FileErrorMessage;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.P4ServerErrorMessage;
import net.groboclown.p4.server.api.messagebus.P4WarningMessage;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4plugin.messages.UserMessage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Reports errors to the user
 */
public class UserErrorComponent implements ProjectComponent {
    private static final Logger LOG = Logger.getInstance(UserErrorComponent.class);
    private static final String COMPONENT_NAME = "p4ic4idea:User Error Component";

    private final Project project;

    public UserErrorComponent(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        // skip
    }

    @Override
    public void projectClosed() {

    }

    // FIXME these are placeholder messages.

    @Override
    public void initComponent() {
        MessageBusClient.ApplicationClient appClient = MessageBusClient.forApplication(project);
        MessageBusClient.ProjectClient projClient = MessageBusClient.forProject(project, project);
        CancellationMessage.addListener(projClient, new CancellationMessage.Listener() {
            @Override
            public void cancelled(@NotNull CancellationException e) {
                simpleInfo("Cancelled operation", "Operation Cancelled");
                LOG.info(e);
            }
        });
        ConnectionErrorMessage.addListener(appClient, new ConnectionErrorMessage.Listener() {
            @Override
            public void unknownServer(@NotNull P4ServerName name, @Nullable ServerConfig config, @NotNull Exception e) {
                simpleError("Unknown server " + name, "Could Not Connect to Server");
                LOG.warn(e);
            }

            @Override
            public void couldNotWrite(@NotNull ServerConfig config, @NotNull FileSaveException e) {
                simpleError("Could not write " + config.getServerName(), "Could Not Write to Server");
                LOG.warn(e);
            }

            @Override
            public void zeroconfProblem(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull ZeroconfException e) {
                simpleError("zeroconf problem for " + name, "Zeroconf Problem");
                LOG.warn(e);
            }

            @Override
            public void sslHostTrustNotEstablished(@NotNull ServerConfig serverConfig) {
                simpleError("SSL Host trust not established to " + serverConfig.getServerName(),
                        "SSL Host Trust Issue");
            }

            @Override
            public void sslHostFingerprintMismatch(@NotNull ServerConfig serverConfig, @NotNull TrustException e) {
                simpleError("SSL host fingerprint did not match known value for " + serverConfig.getServerName(),
                        "SSL Host Fingerprint Mismatch");
                LOG.warn(e);
            }

            @Override
            public void sslAlgorithmNotSupported(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig) {
                simpleError("Did you install the extended cryptography package?", "SSL Algorithm Not Supported");
            }

            @Override
            public void sslPeerUnverified(@NotNull P4ServerName name, @Nullable ServerConfig serverConfig,
                    @NotNull SslHandshakeException e) {
                simpleError("SSL peer unverified for " + name, "SSL Error");
                LOG.warn(e);
            }

            @Override
            public void sslCertificateIssue(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull SslException e) {
                simpleError("SSL certificate error for " + serverName, "SSL Certificate Error");
                LOG.warn(e);
            }

            @Override
            public void connectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull ConnectionException e) {
                simpleError("Connection to Perforce server " + serverName + " failed: " + e.getMessage(),
                        "Perforce Connection Error");
                LOG.warn(e);
            }

            @Override
            public void resourcesUnavailable(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull ResourceException e) {
                simpleError("Resources were unavailable for " + serverName, "Perforce Resources Unavailable");
                LOG.warn(e);
            }
        });
        FileErrorMessage.addListener(projClient, new FileErrorMessage.Listener() {
            @Override
            public void fileReceiveError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull Exception e) {
                simpleError("Failed to receive files from " + serverName, "Perforce File Receive Error");
                LOG.warn(e);
            }

            @Override
            public void fileSendError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull Exception e) {
                simpleError("Failed to send a file to server " + serverName, "Perforce File Send Error");
                LOG.warn(e);
            }

            @Override
            public void localFileError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @NotNull IOException e) {
                simpleError("Problem with local file for " + serverName, "Local File Issue");
                LOG.warn(e);
            }
        });
        InternalErrorMessage.addListener(projClient, new InternalErrorMessage.Listener() {
            @Override
            public void internalError(@NotNull Throwable t) {
                simpleError("Internal error: " + t.getMessage(), "P4 Plugin Error");
                LOG.warn(t);
            }

            @Override
            public void p4ApiInternalError(@NotNull Throwable t) {
                simpleError("Internal error: " + t.getMessage(), "P4 Plugin Error");
                LOG.warn(t);
            }

            @Override
            public void unexpectedError(@NotNull Throwable t) {
                simpleError("Internal error: " + t.getMessage(), "P4 Plugin Error");
                LOG.warn(t);
            }
        });
        LoginFailureMessage.addListener(appClient, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                simpleError("Single sign on failed for " + config.getServerName(), "Login Failure");
                LOG.warn(e);
            }

            @Override
            public void singleSignOnExecutionFailed(@NotNull ServerConfig config,
                    @NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {
                simpleError("Single sign on execution failed for " + config.getServerName(), "Login Failure");
                LOG.warn("SSO error for cmd: " + e.getCmd());
                LOG.warn("Stdout: " + e.getStdout());
                LOG.warn("StdErr: " + e.getStderr());
            }

            @Override
            public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                simpleError("Session expired for " + config.getServerName(), "Login Failure");
                LOG.warn(e);
            }

            @Override
            public void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                simpleError("Password invalid for " + config.getServerName(), "Login Failure");
                LOG.warn(e);
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                simpleError("Password unnecessary for " + config.getServerName(), "Login Failure");
                LOG.warn(e);
            }
        });
        P4ServerErrorMessage.addListener(projClient, new P4ServerErrorMessage.Listener() {
            @Override
            public void requestCausedError(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull IServerMessage msg, @NotNull RequestException re) {
                simpleError("Request error: " + msg.getAllMessages(), "Perforce Server Error");
                LOG.warn(re);
            }

            @Override
            public void requestCausedWarning(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull IServerMessage msg, @NotNull RequestException re) {
                simpleWarning(msg.getAllMessages().toString(), "Perforce Server Warning");
                LOG.warn(re);
            }

            @Override
            public void requestCausedInfoMsg(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull IServerMessage msg, @NotNull RequestException re) {
                simpleInfo(msg.getAllMessages().toString(), "Perforce Server Information");
                LOG.warn(re);
            }

            @Override
            public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull RequestException re) {
                simpleError("Request error: " + re.getMessage(), "Perforce Server Error");
                LOG.warn(re);
            }

            @Override
            public void requestException(@NotNull P4ServerName name, @Nullable ServerConfig config,
                    @NotNull P4JavaException e) {
                simpleError("Request error: " + e.getMessage(), "Perforce Server Error");
                LOG.warn(e);
            }
        });
        P4WarningMessage.addListener(projClient, new P4WarningMessage.Listener() {
            @Override
            public void disconnectCausedError(@NotNull Exception e) {
                simpleWarning("Disconnection from server caused problem: " + e.getLocalizedMessage(),
                        "Disconnected from Perforce Server Error");
                LOG.warn(e);
            }

            @Override
            public void charsetTranslationError(@NotNull ClientError e) {
                simpleWarning("Problem : " + e.getLocalizedMessage(),
                        "Disconnected from Perforce Server Error");
                LOG.warn(e);
            }
        });
        ServerConnectedMessage.addListener(appClient, new ServerConnectedMessage.Listener() {
            @Override
            public void serverConnected(@NotNull ServerConfig serverConfig, boolean loggedIn) {
                // This can be spammy.
                //simpleInfo("Connected to " + serverConfig.getServerName() +
                //                (loggedIn ? " and logged in." : ", but not logged in."),
                //        "Perforce Server Connected");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connected to " + serverConfig.getServerName() + ".  Logged in? " + loggedIn);
                }
            }
        });
        UserSelectedOfflineMessage.addListener(projClient, new UserSelectedOfflineMessage.Listener() {
            @Override
            public void userSelectedServerOffline(@NotNull P4ServerName name) {
                simpleInfo("You selected to go offline for " + name,
                        "Perforce Server Disconnect");
            }
        });
        ReconnectRequestMessage.addListener(projClient, new ReconnectRequestMessage.Listener() {
            @Override
            public void reconnectToAllClients(boolean mayDisplayDialogs) {
                simpleInfo("Requested to go online for all connections.",
                        "Perforce Server Connect");
            }

            @Override
            public void reconnectToClient(@NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
                simpleInfo("Requested to go online for " + ref  + ".",
                        "Perforce Server Connect");
            }
        });
    }

    @Override
    public void disposeComponent() {
        // TODO Perform listener dispose?
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    private void simpleError(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        simpleMessage(message, title, NotificationType.ERROR);
    }

    private void simpleWarning(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        simpleMessage(message, title, NotificationType.WARNING);
    }

    private void simpleInfo(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        simpleMessage(message, title, NotificationType.INFORMATION);
    }

    private void simpleMessage(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message,
            @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title,
            @NotNull NotificationType icon) {
        UserMessage.showNotification(project, message, title, icon);
    }
}
