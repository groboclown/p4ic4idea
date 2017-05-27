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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.connection.ServerStatusController;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog.NO;
import static net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog.YES;

public class ConfigurationProblemHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(ConfigurationProblemHandler.class);

    public ConfigurationProblemHandler(@NotNull Project project, @NotNull ServerStatusController serverStatusController,
            @NotNull Exception ex) {
        super(project, serverStatusController, ex);
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Configuration problem for server " + getServerKey(), getException());

        if (isInvalid()) {
            return;
        }

        DistinctDialog.performOnYesNoDialog(
                DistinctDialog.key(this, getServerKey()),
                getProject(),
                P4Bundle.message("configuration.connection-problem-ask", getExceptionMessage()),
                P4Bundle.message("configuration.check-connection"),
                NotificationType.ERROR,
                new DistinctDialog.ChoiceActor() {
                    @Override
                    public void onChoice(int choice) {
                        if (choice == YES) {
                            // Signal to the API to try again only if
                            // the user selected "okay".
                            tryConfigChange();
                        } else if (choice == NO) {
                            // Work offline
                            goOffline();
                        }
                    }
                });
    }
}
