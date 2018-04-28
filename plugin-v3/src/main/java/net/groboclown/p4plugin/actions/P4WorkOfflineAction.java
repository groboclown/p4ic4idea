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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This action is based on all the servers.
 */
public class P4WorkOfflineAction extends AnAction {
    public P4WorkOfflineAction() {
        super(P4Bundle.message("statusbar.connection.popup.offline-mode"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = getProject(e);
        if (project == null) {
            return;
        }
        final P4Vcs vcs = P4Vcs.getInstance(getProject(e));
        List<P4Server> onlineServers = new ArrayList<P4Server>();
        for (P4Server server: vcs.getP4Servers()) {
            if (server.isWorkingOnline()) {
                onlineServers.add(server);
            }
        }
        if (! onlineServers.isEmpty()) {
            if (Messages.showOkCancelDialog(e.getProject(),
                    P4Bundle.message("dialog.go-offline.message", P4Bundle.applicationName()),
                    P4Bundle.message("dialog.go-offline.title"),
                    Messages.getQuestionIcon()) == Messages.CANCEL) {
                return;
            }
            for (P4Server server: onlineServers) {
                server.workOffline();
            }
        }
    }


    /**
     * Updates the state of the action. Default implementation does nothing.
     * Override this method to provide the ability to dynamically change action's
     * state and(or) presentation depending on the context (For example
     * when your action state depends on the selection you can check for
     * selection and change the state accordingly).
     * This method can be called frequently, for instance, if an action is added to a toolbar,
     * it will be updated twice a second. This means that this method is supposed to work really fast,
     * no real work should be done at this phase. For example, checking selection in a tree or a list,
     * is considered valid, but working with a file system is not. If you cannot understand the state of
     * the action fast you should do it in the {@link #actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent)} method and notify
     * the user that action cannot be executed if it's the case.
     *
     * @param e Carries information on the invocation place and data available
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isWorkingOnline(getProject(e)));
    }


    @Nullable
    private Project getProject(@NotNull AnActionEvent e) {
        if (e.getProject() != null) {
            return e.getProject();
        }
        return null;
    }


    private boolean isWorkingOnline(@Nullable Project project) {
        if (project == null || project.isDisposed()) {
            return false;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);

        // only report online if no server reports offline.
        for (P4Server server : vcs.getP4Servers()) {
            if (server.isWorkingOffline()) {
                return false;
            }
        }
        return true;
    }
}
