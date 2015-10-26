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

public class DisconnectedHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(DisconnectedHandler.class);

    public DisconnectedHandler(@NotNull Project project, @NotNull ServerConnectedController connectedController,
            @NotNull Exception exception) {
        super(project, connectedController, exception);
        // When the disconnection happens, immediately go offline.
        connectedController.disconnect();
    }


    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("Disconnected from Perforce server");

        if (isInvalid() || isWorkingOnline()) {
            return;
        }

        // We may need to switch to automatically work offline due
        // to a user setting.
        if (isAutoOffline()) {
            LOG.info("User running in auto-offline mode.  Will silently work disconnected.");
            return;
        }

        // Ask the user if they want to disconnect.
        LOG.info("Asking user to reconnect");
        int choice = Messages.showDialog(getProject(),
                P4Bundle.message("dialog.offline.message"),
                P4Bundle.message("dialog.offline.title"),
                new String[]{
                        P4Bundle.message("dialog.offline.reconnect"),
                        P4Bundle.message("dialog.offline.offline-mode")
                },
                1,
                Messages.getErrorIcon());
        if (choice == 0) {
            connect();
        } else {
            goOffline();
            Messages.showMessageDialog(getProject(),
                    P4Bundle.message("dialog.offline.went-offline.message"),
                    P4Bundle.message("dialog.offline.went-offline.title"),
                    Messages.getInformationIcon());
        }
    }
}
