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
import com.perforce.p4java.exception.TrustException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class SSLFingerprintProblemHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(SSLFingerprintProblemHandler.class);

    private final boolean trustError;


    public SSLFingerprintProblemHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
        trustError = exception instanceof TrustException;
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("SSL fingerprint problem", getException());

        if (isInvalid()) {
            return;
        }

        // TODO this needs to better handle the SSL fingerprint issues.
        final String message;
        if (trustError) {
            message = P4Bundle.message("configuration.ssl-trust-problem.ask",
                    System.getProperty("java.version") == null ? "<unknown>" : System.getProperty("java.version"),
                    System.getProperty("java.vendor") == null ? "<unknown>" : System.getProperty("java.vendor"),
                    System.getProperty("java.vendor.url") == null ? "<unknown>" : System.getProperty("java.vendor.url"),
                    System.getProperty("java.home") == null ? "<unknown>" : System.getProperty("java.home"),
                    getExceptionMessage()
            );
        } else {
            message = P4Bundle.message("configuration.ssl-fingerprint-problem.ask", getExceptionMessage());
        }

        int result = DistinctDialog.showYesNoDialog(
                DistinctDialog.key(this, getServerKey()),
                getProject(),
                message,
                P4Bundle.message("configuration.ssl-fingerprint-problem.title"),
                Messages.getErrorIcon());
        if (result == DistinctDialog.YES) {
            // Signal to the API to try again only if
            // the user selected "okay".
            tryConfigChange();
        } else if (result == DistinctDialog.NO) {
            // Work offline
            goOffline();
        }
    }
}
