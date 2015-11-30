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

public class SSLFingerprintProblemHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(SSLFingerprintProblemHandler.class);


    public SSLFingerprintProblemHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("SSL fingerprint problem", getException());

        if (isInvalid()) {
            return;
        }

        int result = Messages.showYesNoDialog(getProject(),
                P4Bundle.message("configuration.ssl-fingerprint-problem.ask", getExceptionMessage()),
                P4Bundle.message("configuration.ssl-fingerprint-problem.title"),
                Messages.getErrorIcon());
        if (result == Messages.YES) {
            // Signal to the API to try again only if
            // the user selected "okay".
            tryConfigChange();
        } else {
            // Work offline
            goOffline();
        }
    }
}
