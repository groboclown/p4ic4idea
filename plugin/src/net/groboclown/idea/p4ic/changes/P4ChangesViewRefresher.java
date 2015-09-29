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
package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesViewRefresher;
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode;
import com.intellij.openapi.vcs.changes.actions.RefreshAction;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;

public class P4ChangesViewRefresher implements ChangesViewRefresher {
    private static final Logger LOG = Logger.getInstance(ChangesViewRefresher.class);

    @Override
    public void refresh(@NotNull final Project project) {
        // Indirectly invoke the P4ChangeProvider
        ChangeListManager.getInstance(project).ensureUpToDate(true);
    }


    public static void refreshLater(@NotNull final Project project) {
        LOG.debug("Refreshing changelist view", new Throwable());

        // FIXME try just:
        // ChangeListManager.getInstance(project).schedule();

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ChangeListManager.getInstance(project).invokeAfterUpdate(new Runnable() {
                        @Override
                        public void run() {
                            // This will perform all the right logic to refresh the
                            // change lists and refresh the UI.
                            RefreshAction.doRefresh(project);
                        }
                    }, InvokeAfterUpdateMode.BACKGROUND_CANCELLABLE,
                    P4Bundle.getString("change.view.refresh.title"),
                    ModalityState.NON_MODAL);
            }
        });
    }
}
