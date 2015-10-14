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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class SSLKeyStrengthProblemHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(SSLKeyStrengthProblemHandler.class);


    public SSLKeyStrengthProblemHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("SSL key strength problem", getException());

        if (isInvalid()) {
            return;
        }

        int result = Messages.showYesNoDialog(getProject(),
                P4Bundle.message("exception.java.ssl.keystrength-ask",
                        System.getProperty("java.version") == null ? "<unknown>" : System.getProperty("java.version"),
                        System.getProperty("java.vendor") == null ? "<unknown>" : System.getProperty("java.vendor"),
                        System.getProperty("java.vendor.url") == null
                                ? "<unknown>"
                                : System.getProperty("java.vendor.url"),
                        System.getProperty("java.home") == null ? "<unknown>" : System.getProperty("java.home"),
                        getExceptionMessage()),
                P4Bundle.message("exception.java.ssl.keystrength-ask.title"),
                Messages.getErrorIcon());
        boolean changed = false;
        if (result == Messages.YES) {
            // Signal to the API to try again only if
            // the user selected "okay".
            changed = tryConfigChange();
        }
        if (!changed) {
            // Work offline
            goOffline();
            Messages.showMessageDialog(getProject(),
                    P4Bundle.message("dialog.offline.went-offline.message"),
                    P4Bundle.message("dialog.offline.went-offline.title"),
                    Messages.getInformationIcon());
        }
    }
}
