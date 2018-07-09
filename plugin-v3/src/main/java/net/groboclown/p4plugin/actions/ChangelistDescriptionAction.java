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

package net.groboclown.p4plugin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.history.ChangelistDetails;

public class ChangelistDescriptionAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ChangelistDescriptionAction.class);

    public ChangelistDescriptionAction() {
        super(
                P4Bundle.getString("history.describe-change"),
                P4Bundle.getString("history.describe-change.description"),
                AllIcons.General.BalloonInformation);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = getEventProject(e);
        if (project == null || project.isDisposed()) {
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return;
        }

        final VirtualFile file;
        {
            final Boolean nonLocal = e.getData(VcsDataKeys.VCS_NON_LOCAL_HISTORY_SESSION);
            if (Boolean.TRUE.equals(nonLocal)) {
                LOG.info("non-local VCS history session; ignoring changelist description action");
                return;
            }
            file = e.getData(VcsDataKeys.VCS_VIRTUAL_FILE);
            if (file == null || file.isDirectory()) {
                LOG.info("No VCS virtual file associated with changelist description action; ignoring request.");
                return;
            }
        }

        final VcsFileRevision revision = e.getData(VcsDataKeys.VCS_FILE_REVISION);
        if (!(revision instanceof P4HistoryVcsFileRevision)) {
            LOG.info("No file revision associated with file " + file);
            return;
        }
        P4HistoryVcsFileRevision p4Rev = (P4HistoryVcsFileRevision) revision;
        ServerConfig serverConfig = p4Rev.getServerConfig();

        P4ChangelistId changelistId = p4Rev.getChangelistId();

        P4ServerComponent.getInstance(project)
                .getCommandRunner()
                .query(serverConfig, new DescribeChangelistQuery(changelistId))
                .whenCompleted((r) -> {
                    if (r.getRemoteChangelist() != null) {
                        ChangelistDetails.showDocked(project, r.getRemoteChangelist());
                    }
                });
    }
}
