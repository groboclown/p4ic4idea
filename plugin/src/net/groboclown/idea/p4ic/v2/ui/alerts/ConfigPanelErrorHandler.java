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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.connection.CriticalErrorHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Date;

public class ConfigPanelErrorHandler implements CriticalErrorHandler {
    private final Project project;
    private final String title;
    private final String message;

    public ConfigPanelErrorHandler(@NotNull Project project,
            @NotNull @Nls final String title,
            @NotNull @Nls final String message) {
        this.project = project;
        this.title = title;
        this.message = message.length() > 0
                ? message
                : P4Bundle.message("config-panel.error.empty-message", title);
    }


    @Override
    public void handleError(@NotNull final Date when) {
        // using ApplicationManager.getApplication().invokeLater
        // will cause the dialog to delay displaying until all
        // other active dialogs are dismissed.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Don't use the Distinct Dialog.  The config panel
                // doesn't have these kinds of issues.

                Messages.showMessageDialog(project,
                        message,
                        title,
                        Messages.getErrorIcon());
            }
        });
    }
}
