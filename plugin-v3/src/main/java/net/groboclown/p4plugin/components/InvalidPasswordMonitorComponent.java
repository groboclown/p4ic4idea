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
import net.groboclown.p4.server.api.messagebus.ServerErrorEvent;
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
        LoginFailureMessage.addListener(mbClient, this, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                if (shouldHandleProblem(e.getConfig(), FailureType.SSO)) {
                    // No explicit action to take
                    UserMessage.showNotification(null, UserMessage.ERROR,
                            P4Bundle.message("error.loginsso.exec-failed",
                                    e.getConfig().getLoginSso(), e.getError().getLocalizedMessage()),
                            P4Bundle.message("error.loginsso.exec-failed.title"),
                            NotificationType.ERROR);

                    // TODO once the Single Sign On can take an argument, let this prompt for the argument.
                }
            }

            @Override
            public void singleSignOnExecutionFailed(@NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {
                // No explicit action to take
                UserMessage.showNotification(null, UserMessage.ERROR,
                        P4Bundle.message("error.loginsso.exec-failed.long",
                                e.getConfig().getLoginSso(), e.getExitCode(), e.getStdout(), e.getStderr()),
                        P4Bundle.message("error.loginsso.exec-failed.title"),
                        NotificationType.ERROR);

                // TODO once the Single Sign On can take an argument, let this prompt for the argument.
            }

            @Override
            public void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                // Force a log-in.  If this fails, another message will be handled.
                if (shouldHandleProblem(e.getConfig(), FailureType.SESSION_EXPIRED)) {
                    P4ServerComponent.perform(findBestProject(), e.getConfig(),
                            new LoginAction());
                }
            }

            @Override
            public void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                // Remove the stored password.
                if (shouldHandleProblem(e.getConfig(), FailureType.PASSWORD_INVALID)) {
                    // Ask for the password.  Use the fancy notification stuff.
                    UserMessage.showNotification(null, UserMessage.ERROR,
                            P4Bundle.message("login.password.error", e.getName().getDisplayName()),
                            P4Bundle.getString("login.password.error.title"),
                            NotificationType.ERROR,
                            (notification, hyperlinkEvent) -> {
                                forgetLoginProblem(e.getConfig());
                                ApplicationPasswordRegistry.getInstance()
                                        .askForNewPassword(null, e.getConfig());
                            },
                            () -> {
                                // Timed out waiting for a user action.  Reset the password login
                                // state, so the user can be asked again.
                                forgetLoginProblem(e.getConfig());
                            }
                    );
                }
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                // Inform the user about the unnecessary password.
                if (shouldHandleProblem(e.getConfig(), FailureType.PASSWORD_UNNECESSARY)) {
                    UserMessage.showNotification(null, UserMessage.WARNING,
                            P4Bundle.message("connection.password.unnecessary.details",
                                    e.getName().getDisplayName()),
                            P4Bundle.message("connection.password.unnecessary.title"),
                            NotificationType.INFORMATION);

                    // Update the password to remove the stored password.
                    ApplicationPasswordRegistry.getInstance().remove(e.getConfig());
                }
            }
        });

        ServerConnectedMessage.addListener(mbClient, this, (e) -> {
            // Force a no-issue connection state.
            if (e.isLoggedIn()) {
                forgetLoginProblem(e.getServerConfig());
            }
        });

        // Add listeners for config added/removed.  Any change to the configuration, even if it
        // doesn't touch the server config, should clear the login state.
        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerListener() {
            @Override
            public void projectOpened(Project project) {
                MessageBusClient.ProjectClient projectMbClient =
                        MessageBusClient.forProject(project, InvalidPasswordMonitorComponent.this);
                ClientConfigAddedMessage.addListener(projectMbClient, this,
                        e -> forgetLoginProblem(e.getClientConfig().getServerConfig()));
                ClientConfigRemovedMessage.addListener(projectMbClient, this,
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
