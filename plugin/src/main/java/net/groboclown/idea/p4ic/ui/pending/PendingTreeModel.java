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

package net.groboclown.idea.p4ic.ui.pending;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangeListRemoteState;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserChangeListNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.ui.SimpleTextAttributes;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.List;

class PendingTreeModel extends DefaultTreeModel {
    private static final Logger LOG = Logger.getInstance(PendingTreeModel.class);


    private final Project project;

    PendingTreeModel(@NotNull Project project) {
        super(ChangesBrowserNode.create(project,
                P4Bundle.getString("pending-changes-view.root.text")));
        this.project = project;
    }

    public void load(@NotNull List<PendingChangelist> list) {
        List<ChangesBrowserNode> newNodes = new ArrayList<ChangesBrowserNode>(list.size());
        for (PendingChangelist value : list) {
            newNodes.add(createFor(value));
        }

        ChangesBrowserNode root = (ChangesBrowserNode) getRoot();
        root.removeAllChildren();
        for (ChangesBrowserNode newNode : newNodes) {
            root.add(newNode);
        }
    }

    @NotNull
    private ChangesBrowserChangeListNode createFor(@NotNull PendingChangelist value) {
        PendingChangelistNode ret = new PendingChangelistNode(project,
                value, new ChangeListRemoteState(1));
        // TODO this isn't showing the list of files for some reason.  Are they just not
        // loaded?
        for (Change change : value.getChanges()) {
            ret.add(createChangeNode(change));
        }
        for (P4FileAction action : value.getNonProjectFiles()) {
            ret.add(createNonProjectNode(action));
        }
        ret.add(createShelvedFilesNode(value));
        ret.add(createJobNodes(value));
        return ret;
    }

    @NotNull
    private MutableTreeNode createChangeNode(@NotNull Change change) {
        return ChangesBrowserNode.create(project, change);
    }

    @NotNull
    private MutableTreeNode createNonProjectNode(P4FileAction action) {
        return new ChangesBrowserRemoteNode(action);
    }

    @NotNull
    private MutableTreeNode createJobNodes(@NotNull PendingChangelist value) {
        // TODO allow drag + drop
        ChangesBrowserStringNode jobsRoot = new ChangesBrowserStringNode(
                P4Bundle.getString("changes.browser.jobs.root")
        );
        jobsRoot.setItemCountLabel("changes.browser.jobs.count");
        jobsRoot.setAttributes(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        for (P4JobState job : value.getJobs()) {
            // TODO make full flavored.
            ChangesBrowserStringNode node = new ChangesBrowserStringNode(job.getId());
            node.setAttributes(SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
            // TODO improve icon
            node.setIcon(AllIcons.Nodes.Tag);
            jobsRoot.add(node);
        }
        return jobsRoot;
    }

    @NotNull
    private MutableTreeNode createShelvedFilesNode(@NotNull PendingChangelist value) {
        ChangesBrowserStringNode shelvedRoot = new ChangesBrowserStringNode(
                P4Bundle.getString("changes.browser.shelved.root")
        );
        shelvedRoot.setItemCountLabel("changes.browser.shelved.count");
        shelvedRoot.setAttributes(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        for (P4ShelvedFile shelvedFile : value.getShelvedFiles()) {
            // TODO make full flavored.
            ChangesBrowserStringNode node = new ChangesBrowserStringNode(shelvedFile.getDepotPath());
            node.setAttributes(
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC,
                            shelvedFile.getStatus().getColor()));
            // TODO improve icon
            node.setIcon(AllIcons.FileTypes.Any_type);
            shelvedRoot.add(node);
        }
        return shelvedRoot;
    }


}
