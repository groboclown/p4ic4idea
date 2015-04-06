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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * For now, this action is based on all the servers.
 * TODO allow this to be one action per server.
 */
public class P4WorkOfflineAction extends AnAction {
    private final static Logger LOG = Logger.getInstance(P4WorkOfflineAction.class);

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
        Set<ServerConfig> onlineServers = new HashSet<ServerConfig>();
        for (Client client: vcs.getClients()) {
            if (client.isWorkingOnline()) {
                onlineServers.add(client.getConfig());
            }
        }
        if (! onlineServers.isEmpty()) {
            if (Messages.showOkCancelDialog(e.getProject(),
                    P4Bundle.message("dialog.go-offline.message"),
                    P4Bundle.message("dialog.go-offline.title"),
                    Messages.getQuestionIcon()) == Messages.CANCEL) {
                return;
            }
            for (ServerConfig config: onlineServers) {
                try {
                    ServerStoreService.getInstance().getServerStatus(project, config).
                            forceDisconnect();
                } catch (P4InvalidConfigException ex) {
                    LOG.info(ex);
                }
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
