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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.PasswordManager;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class LoginFailedHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(LoginFailedHandler.class);

    // Without the list of who is being asked for passwords, we get into a bad
    // state where the asking for password is active while another bad login
    // message is displayed.  This all stems from the issue where the dialog that
    // we're showing to ask the user for their action choice won't go away
    // if another dialog pops up.
    private static final Set<String> ACTIVE_LOGINS = Collections.synchronizedSet(new HashSet<String>());

    private final ServerConfig config;



    public LoginFailedHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull ServerConfig config,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
        this.config = config;
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Login problem", getException());

        if (isInvalid()) {
            return;
        }

        boolean handleEndSeparately = false;

        if (beginAction(config)) {
            try {
                int result = Messages.showDialog(getProject(),
                        P4Bundle.message("configuration.login-problem-ask", getExceptionMessage()),
                        P4Bundle.message("configuration.login-problem.title"),
                        new String[]{
                                P4Bundle.message("configuration.login-problem.yes"),
                                P4Bundle.message("configuration.login-problem.no"),
                                P4Bundle.message("configuration.login-problem.cancel")
                        },
                        0,
                        Messages.getErrorIcon());
                if (result == 0) { // first option: re-enter password
                    // This needs to run in another event, otherwise the
                    // message dialog will stay active forever.
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
                                endAction(config);
                            }
                        }
                    });
                    handleEndSeparately = true;
                } else if (result == 1) { // 2nd option: update server config
                    tryConfigChange();
                } else { // 3rd option: work offline
                    goOffline();
                }
            } finally {
                if (! handleEndSeparately) {
                    endAction(config);
                }
            }
        } else {
            LOG.info("Already handling login for config " + config);
        }
    }


    private static boolean beginAction(@NotNull ServerConfig config) {
        String key = getLoginKey(config);
        return ACTIVE_LOGINS.add(key);
    }

    private static void endAction(@NotNull ServerConfig config) {
        String key = getLoginKey(config);
        ACTIVE_LOGINS.remove(key);
    }


    @NotNull
    private static String getLoginKey(@NotNull ServerConfig config) {
        return config.getServiceName() + ">>>" + config.getUsername();
    }

}
