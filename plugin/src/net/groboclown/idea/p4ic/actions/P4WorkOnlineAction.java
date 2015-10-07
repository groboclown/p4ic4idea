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
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class P4WorkOnlineAction extends AnAction {
    public P4WorkOnlineAction() {
        super(P4Bundle.message("statusbar.connection.popup.reconnect"));
    }


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = getProject(e);
        if (project == null) {
            return;
        }
        // FIXME
        final P4Vcs vcs = P4Vcs.getInstance(getProject(e));
        Set<ServerConfig> offlineServers = new HashSet<ServerConfig>();

        // Tell the servers that they should be working online.
        ServerStoreService.getInstance().workOnline();

        // Force the configurations to be reloaded.
        vcs.reloadConfigs();
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
        e.getPresentation().setEnabled(! isWorkingOnline(getProject(e)));
    }


    @Nullable
    private Project getProject(@NotNull AnActionEvent e) {
        if (e.getProject() != null) {
            return e.getProject();
        }
        return null;
    }


    private boolean isWorkingOnline(@Nullable Project project) {
        if (project == null) {
            return false;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);
        // If any one client is offline, then report offline.
        List<Client> clients = vcs.getClients();
        // Likewise, if there are no clients, or it's a config problem, then report offline
        if (clients.isEmpty()) {
            return false;
        }
        for (Client client : clients) {
            if (client.isWorkingOffline()) {
                return false;
            }
        }

        return true;
    }
}
