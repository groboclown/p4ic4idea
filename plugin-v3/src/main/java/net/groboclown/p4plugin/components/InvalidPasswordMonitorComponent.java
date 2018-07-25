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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.server.LoginAction;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.extension.P4Vcs;
import net.groboclown.p4plugin.messages.UserMessage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class InvalidPasswordMonitorComponent
        implements ApplicationComponent, Disposable {
    private static final String COMPONENT_NAME = "p4ic: Invalid Password Monitor";

    // Note: without a server add/remove listener, this can be a potential memory leak.
    private final Map<P4ServerName, FailureType> lastFailureType = new HashMap<>();

    private boolean disposed = false;

    private enum FailureType {
        NONE, SSO, SESSION_EXPIRED, PASSWORD_INVALID, PASSWORD_UNNECESSARY
    }

    @Override
    public void initComponent() {
        MessageBusClient.ApplicationClient mbClient = MessageBusClient.forApplication(this);
        LoginFailureMessage.addListener(mbClient, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                if (shouldHandleProblem(config, FailureType.SSO)) {
                    // No explicit action to take
                    UserMessage.showNotification(null,
                            P4Bundle.message("error.loginsso.exec-failed",
                                    config.getLoginSso(), e.getLocalizedMessage()),
                            P4Bundle.message("error.loginsso.exec-failed.title"),
                            NotificationType.ERROR);

                    // TODO once the Single Sign On can take an argument, let this prompt for the argument.
                }
            }

            @Override
            public void singleSignOnExecutionFailed(@NotNull ServerConfig config,
                    @NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {
                // No explicit action to take
                UserMessage.showNotification(null,
                        P4Bundle.message("error.loginsso.exec-failed.long",
                                config.getLoginSso(), e.getExitCode(), e.getStdout(), e.getStderr()),
                        P4Bundle.message("error.loginsso.exec-failed.title"),
                        NotificationType.ERROR);

                // TODO once the Single Sign On can take an argument, let this prompt for the argument.
            }

            @Override
            public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                // Force a log-in.  If this fails, another message will be handled.
                if (shouldHandleProblem(config, FailureType.SESSION_EXPIRED)) {
                    P4ServerComponent.perform(findBestProject(), config,
                            new LoginAction());
                }
            }

            @Override
            public void passwordInvalid(@NotNull final ServerConfig config, @NotNull AuthenticationFailedException e) {
                // Remove the stored password.
                if (shouldHandleProblem(config, FailureType.PASSWORD_INVALID)) {
                    // Ask for the password.  Use the fancy notification stuff.
                    UserMessage.showNotification(null,
                            P4Bundle.message("login.password.error", config.getServerName().getDisplayName()),
                            P4Bundle.getString("login.password.error.title"),
                            NotificationType.ERROR,
                            (notification, hyperlinkEvent) -> {
                                forgetLoginProblem(config);
                                ApplicationPasswordRegistry.getInstance()
                                        .askForNewPassword(null, config);
                            },
                            () -> {
                                // Timed out waiting for a user action.  Reset the password login
                                // state, so the user can be asked again.
                                forgetLoginProblem(config);
                            }
                    );
                }
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                // Inform the user about the unnecessary password.
                if (shouldHandleProblem(config, FailureType.PASSWORD_UNNECESSARY)) {
                    UserMessage.showNotification(null,
                            P4Bundle.message("connection.password.unnecessary.details",
                                    config.getServerName().getDisplayName()),
                            P4Bundle.message("connection.password.unnecessary.title"),
                            NotificationType.INFORMATION);

                    // Update the password to remove the stored password.
                    ApplicationPasswordRegistry.getInstance().remove(config);
                }
            }
        });

        ServerConnectedMessage.addListener(mbClient, (serverConfig, loggedIn) -> {
            // Force a no-issue connection state.
            if (loggedIn) {
                forgetLoginProblem(serverConfig);
            }
        });

        // Add listeners for config added/removed.  Any change to the configuration, even if it
        // doesn't touch the server config, should clear the login state.
        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerListener() {
            @Override
            public void projectOpened(Project project) {
                MessageBusClient.ProjectClient projectMbClient =
                        MessageBusClient.forProject(project, InvalidPasswordMonitorComponent.this);
                ClientConfigAddedMessage.addListener(projectMbClient,
                        (root, clientConfig) -> forgetLoginProblem(clientConfig.getServerConfig()));
                ClientConfigRemovedMessage.addListener(projectMbClient,
                        event -> forgetLoginProblem(event.getClientConfig().getServerConfig()));
            }

            @Override
            public boolean canCloseProject(Project project) {
                return true;
            }

            @Override
            public void projectClosed(Project project) {
                // do nothing.
            }

            @Override
            public void projectClosing(Project project) {
                // do nothing.
            }
        }, this);
    }

    @Override
    public void disposeComponent() {
        dispose();
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            Disposer.dispose(this);
        }
    }

    public boolean isDisposed() {
        return disposed;
    }


    private boolean shouldHandleProblem(ServerConfig config, FailureType type) {
        // The idea here is to prevent the user from being bombarded with duplicate requests.
        FailureType previous;
        synchronized (lastFailureType) {
            previous = lastFailureType.get(config.getServerName());
            lastFailureType.put(config.getServerName(), type);
        }
        return (previous != type);
    }

    private void forgetLoginProblem(ServerConfig config) {
        synchronized (lastFailureType) {
            lastFailureType.remove(config.getServerName());
        }
    }


    @NotNull
    private static Project findBestProject() {
        // FIXME guessing at a project.
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            AbstractVcs vcs =
                    ProjectLevelVcsManager.getInstance(openProject).findVcsByName(P4Vcs.VCS_NAME);
            if (vcs != null) {
                return openProject;
            }
        }
        // Weird state - no project open that has an associated P4Vcs.
        return ProjectManager.getInstance().getDefaultProject();
    }
}
