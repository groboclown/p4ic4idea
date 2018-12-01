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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ui.ChangeNodeDecorator;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTreeList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.Collections;
import java.util.List;

public class PendingChangesTreeList
        extends ChangesTreeList<PendingChangelist> {
    PendingChangesTreeList(@NotNull Project project,
            boolean highlightProblems, @Nullable Runnable inclusionListener) {
        super(project, Collections.<PendingChangelist>emptyList(),
                false, highlightProblems, inclusionListener,
                null);
        setEmptyText(P4Bundle.getString("pending-changes-view.tree.empty-text"));
    }

    @NotNull
    PendingChangeItemSet getSelectedItems() {
        // FIXME
        return new PendingChangeItemSet(
                Collections.<FilePath>emptyList(),
                Collections.<P4ShelvedFile>emptyList(),
                Collections.<P4JobState>emptyList()
        );
    }

    @Override
    protected DefaultTreeModel buildTreeModel(List<PendingChangelist> list, ChangeNodeDecorator changeNodeDecorator) {
        final PendingTreeModel ret = new PendingTreeModel(myProject);
        ret.load(list);
        return ret;
    }

    //@Override
    // FIXME 2017.2
    //protected List<PendingChangelist> getSelectedObjects(ChangesBrowserNode<PendingChangelist> changesBrowserNode) {
    // FIXME 2017.1
    //protected List<PendingChangelist> getSelectedObjects(ChangesBrowserNode<?> changesBrowserNode) {
    protected List<PendingChangelist> getSelectedObjects(ChangesBrowserNode changesBrowserNode) {
        return changesBrowserNode.getAllObjectsUnder(PendingChangelist.class);
    }

    @Nullable
    @Override
    protected PendingChangelist getLeadSelectedObject(ChangesBrowserNode changesBrowserNode) {
        Object userObject = changesBrowserNode.getUserObject();
        return userObject instanceof PendingChangelist ? (PendingChangelist) userObject : null;
    }

}
