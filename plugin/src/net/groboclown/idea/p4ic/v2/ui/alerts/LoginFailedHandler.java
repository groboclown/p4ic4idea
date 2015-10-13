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

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.ui.ErrorDialog;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class LoginFailedHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(LoginFailedHandler.class);

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
        LOG.warn("Configuration problem", getException());

        if (isInvalid()) {
            return;
        }

        int result = Messages.showYesNoCancelDialog(getProject(),
                P4Bundle.message("configuration.login-problem-ask", getExceptionMessage()),
                P4Bundle.message("configuration.login-problem.title"),
                P4Bundle.message("configuration.login-problem.yes"),
                P4Bundle.message("configuration.login-problem.no"),
                P4Bundle.message("configuration.login-problem.cancel"),
                Messages.getErrorIcon());
        boolean wentOffline = false;
        if (result == Messages.YES) {
            try {
                PasswordSafe.getInstance().removePassword(getProject(),
                        P4Vcs.class, config.getServiceName());
                Messages.showMessageDialog(getProject(),
                        P4Bundle.message("configuration.cleared-password.title"),
                        P4Bundle.message("configuration.cleared-password.message"),
                        Messages.getInformationIcon());
            } catch (PasswordSafeException e) {
                ErrorDialog.logError(getProject(), P4Bundle.message("configuration.cleared-password.error"), e);
            }
        } else if (result == Messages.NO) {
            wentOffline = ! tryConfigChange();
        } else {
            wentOffline = true;
        }

        if (wentOffline) {
            // Work offline
            goOffline();
            Messages.showMessageDialog(getProject(),
                    P4Bundle.message("dialog.offline.went-offline.message"),
                    P4Bundle.message("dialog.offline.went-offline.title"),
                    Messages.getInformationIcon());
        }

    }
}
