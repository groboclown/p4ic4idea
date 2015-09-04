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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.compat.UICompat;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.connection.CriticalErrorHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class InvalidClientHandler implements CriticalErrorHandler {
    private final Project project;
    private final String clientName;
    private final String errorMessage;

    public InvalidClientHandler(@NotNull final Project project, @NotNull final String clientName,
            @NotNull final String errorMessage) {
        this.project = project;
        this.clientName = clientName;
        this.errorMessage = errorMessage;
    }

    @Override
    public void handleError(@NotNull final Date when) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        int result = Messages.showYesNoDialog(project,
                P4Bundle.message("configuration.connection-problem-ask", errorMessage),
                P4Bundle.message("configuration.check-connection"),
                Messages.getErrorIcon());
        boolean changed = false;
        if (result == Messages.YES) {
            // Signal to the API to try again only if
            // the user selected "okay".
            P4Vcs vcs = P4Vcs.getInstance(project);
            changed = UICompat.getInstance().editVcsConfiguration(project, vcs.getConfigurable());
        }
        if (!changed) {
            // Work offline
            Messages.showMessageDialog(project,
                    P4Bundle.message("dialog.offline.went-offline.message"),
                    P4Bundle.message("dialog.offline.went-offline.title"),
                    Messages.getInformationIcon());
        }
    }
}
