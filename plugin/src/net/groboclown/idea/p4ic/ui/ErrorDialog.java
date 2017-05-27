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
package net.groboclown.idea.p4ic.ui;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.VcsExceptionUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4WorkingOfflineException;
import net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.CancellationException;

public class ErrorDialog {
    private static final Logger LOG = Logger.getInstance(ErrorDialog.class);

    public static void logError(@NotNull final Project project, @NotNull final String action, @NotNull final Throwable t) {
        // no response from the user is required, so it's fine
        // to run in an invoke later (there's no Future involved).

        // using ApplicationManager.getApplication().invokeLater
        // will cause the dialog to delay displaying until all
        // other active gui are dismissed.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!(t instanceof VcsException) && !(t instanceof CancellationException)) {
                    LOG.warn("Something threw an invalid exception without being properly wrapped", t);
                }
                if (
                        // User explicitly cancelled the operation
                        ! VcsExceptionUtil.isCancellation(t) &&

                        // User has already been warned of these
                        ! (t instanceof P4WorkingOfflineException) &&
                        ! (t instanceof P4DisconnectedException)) {
                    LOG.warn(t);
                    String message = t.getMessage();
                    if (message != null) {
                        // Some P4 exceptions can have trailing EOLs.
                        message = message.trim();
                    }
                    DistinctDialog.showMessageDialog(project,
                            P4Bundle.message("errordialog.message", message),
                            P4Bundle.message("errordialog.title", action),
                            NotificationType.ERROR);
                }
            }
        });
    }
}
