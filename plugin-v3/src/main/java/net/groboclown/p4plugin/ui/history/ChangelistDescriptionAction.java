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

package net.groboclown.p4plugin.ui.history;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.BackgroundAwtActionRunner;
import net.groboclown.idea.p4ic.ui.VcsDockedComponent;
import net.groboclown.idea.p4ic.v2.history.P4CommittedChangeListDetails;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;

@SuppressWarnings("ComponentNotRegistered")
public class ChangelistDescriptionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ChangelistDescriptionAction.class);

    public ChangelistDescriptionAction() {
        super(
                P4Bundle.getString("history.describe-change"),
                P4Bundle.getString("history.describe-change.description"),
                AllIcons.General.BalloonInformation);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);

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
        if (revision == null || ! (revision instanceof P4FileRevision)) {
            LOG.info("No revision information associated with changelist description action; ignoring (revision = "
                    + revision + ")");
            return;
        }
        final P4FileRevision p4rev = (P4FileRevision) revision;
        BackgroundAwtActionRunner.runBackgroundAwtAction(new BackgroundAwtActionRunner.BackgroundAwtAction<P4CommittedChangeListDetails>() {
            @Override
            public P4CommittedChangeListDetails runBackgroundProcess() {
                try {
                    return P4CommittedChangeListDetails.create(p4rev);
                } catch (InterruptedException e1) {
                    return null;
                }
            }

            @Override
            public void runAwtProcess(P4CommittedChangeListDetails value) {
                if (value != null) {
                    VcsDockedComponent.getInstance(project).addVcsTab(
                            P4Bundle.message("changelist.details.tab-title",
                                    value.getChangeListId() == null ? "" : value.getChangeListId().asString()),
                            new ChangelistDetails(value).getRoot(),
                            true,
                            true);
                }
            }
        });
    }
}
