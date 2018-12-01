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

package net.groboclown.idea.p4ic.v2.ui.alerts;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import net.groboclown.idea.p4ic.v2.server.authentication.PasswordManager;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoginFailedHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(LoginFailedHandler.class);

    // A memory leak of sorts, but it's limited to the distinct number of server IDs.
    private static final Map<String, Integer> ACTIVE_DIALOGS = new HashMap<String, Integer>();

    private final ServerConfig config;
    private final int activeId;


    public LoginFailedHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull ServerConfig config,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
        LOG.info("Generated login failed error for server " + config.getServerId(), exception);
        this.config = config;
        synchronized (ACTIVE_DIALOGS) {
            final Integer topId = ACTIVE_DIALOGS.get(getServerKey());
            if (topId == null) {
                this.activeId = 0;
            } else {
                this.activeId = topId + 1;
            }
            ACTIVE_DIALOGS.put(getServerKey(), activeId);
        }
        LOG.debug("Created dialog " + activeId);
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Login problem for server " + config.getServerId(), getException());

        // Attempt to limit the number of "you need a password" dialog prompts that can
        // pop up all at once.  If there was a more recent LoginFailed error, then don't
        // show this older one.
        synchronized (ACTIVE_DIALOGS) {
            final Integer topId = ACTIVE_DIALOGS.get(getServerKey());
            LOG.debug("Showing dialog " + activeId + "; most recent dialog is " + topId);
            if (topId != null && topId > activeId) {
                LOG.warn("The login failed dialog appears active, but showing it anyway.");

                // There appears to be a bug in this.  The GUI is no longer being activated.
                // See #154, #153, #151.
                // return;
            }
        }

        if (isInvalid()) {
            LOG.debug("Cannot handle password problem in this context; not in dispatch thread or project disposed.");
            return;
        }

        // First, ensure that the password was in fact cleared.
        // If it is set, then that means the user has already been prompted
        // for the password in another connection.

        try {
            OneUseString password =
                    PasswordManager.getInstance().getPassword(getProject(), config, false);
            if (! password.isNullValue()) {
                // password was reset.
                LOG.info("Not asking user for password, as it is currently set.");
                // Try to reconnect in the background.  If the connection has already been made,
                // then this should be a no-op.
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                    }
                });
                return;
            }
        } catch (PasswordStoreException e) {
            // Ignore.  This means that the password was most likely not set.
            LOG.debug(e);
        } catch (PasswordAccessedWrongException e) {
            // Ignore.  This means that the password was most likely not set.
            LOG.debug(e);
        }

        LOG.debug("Asking the user about the password problem in a dialog.");
        DistinctDialog.performOnDialog(
                DistinctDialog.key(this, config.getServerName(), config.getUsername()),
                getProject(),
                P4Bundle.message("configuration.login-problem-ask", getExceptionMessage()),
                P4Bundle.message("configuration.login-problem.title"),
                new String[]{
                        P4Bundle.message("configuration.login-problem.yes"),
                        P4Bundle.message("configuration.login-problem.no"),
                        P4Bundle.message("configuration.login-problem.cancel")
                },
                NotificationType.ERROR,
                new DistinctDialog.AsyncChoiceActor() {
                    @Override
                    public void onChoice(int choice, @NotNull final DistinctDialog.OnEndHandler onEndHandler) {
                        switch (choice) {
                            case 0:
                                // first option: re-enter password
                                // This needs to run in another event, otherwise the
                                // message dialog will stay active forever.
                                LOG.debug("Asking the user for the password");
                                onEndHandler.handleInOtherThread();
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            PasswordManager.getInstance().askPassword(getProject(), config);
                                            connect();
                                        } catch (PasswordStoreException e) {
                                            AlertManager.getInstance().addWarning(getProject(),
                                                    P4Bundle.message("password.store.error.title"),
                                                    P4Bundle.message("password.store.error"),
                                                    e, new FilePath[0]);
                                        } finally {
                                            onEndHandler.end();
                                        }
                                    }
                                });
                                break;
                            case 1:
                                // 2nd option: update server config
                                LOG.debug("Starting up the configuration UI");
                                tryConfigChange();
                                break;
                            default:
                                // 3rd option: work offline
                                LOG.debug("Going offline");
                                goOffline();
                                break;
                        }
                    }
                }
        );
    }


    @Override
    @NotNull
    String getExceptionMessage() {
        String msg = super.getExceptionMessage();

        // Clear out misleading messages.
        // TODO make this better.
        if ("Perforce password (%'P4PASSWD'%) invalid or unset.".equals(msg)) {
            return P4Bundle.getString("error.config.requires-password");
        }
        msg = msg.replace("%'", "'").replace("'%", "'");
        return msg;
    }
}
