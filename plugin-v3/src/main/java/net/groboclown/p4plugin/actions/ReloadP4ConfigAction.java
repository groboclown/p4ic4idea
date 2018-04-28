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
package net.groboclown.idea.p4ic.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4ProjectConfigComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This action is based on all the servers.
 */
public class ReloadP4ConfigAction
        extends AnAction {
    public ReloadP4ConfigAction() {
        super(P4Bundle.message("statusbar.connection.popup.reload"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = getProject(e);
        if (project == null) {
            return;
        }

        // The announcement can take some time, specifically because it can read files.
        // So make sure it does not run on the EDT.
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                P4ProjectConfigComponent.getInstance(project).announceBaseConfigUpdated();
            }
        });
    }


    @Nullable
    private Project getProject(@NotNull AnActionEvent e) {
        if (e.getProject() != null) {
            return e.getProject();
        }
        return null;
    }
}
