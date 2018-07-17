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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.actions.VcsQuickListContentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class P4QuickListContentProvider implements VcsQuickListContentProvider {
    @Nullable
    @Override
    public List<AnAction> getVcsActions(Project project, AbstractVcs activeVcs, DataContext dataContext) {
        if (activeVcs == null || !P4Vcs.VCS_NAME.equals(activeVcs.getName())) {
            return null;
        }

        final ActionManager manager = ActionManager.getInstance();
        final List<AnAction> actions = new ArrayList<AnAction>();

        actions.add(new Separator(activeVcs.getDisplayName()));
        add("CheckinProject", manager, actions);
        add("CheckinFiles", manager, actions);
        add("ChangesView.Revert", manager, actions);
        add("UpdateFiles", manager, actions);
        add("ChangesView.AddUnversioned", manager, actions);

        addSeparator(actions);
        add("Vcs.ShowTabbedFileHistory", manager, actions);
        add("Annotate", manager, actions);
        add("Compare.SameVersion", manager, actions);
        add("Compare.Selected", manager, actions);

        /* FIXME add shelve/unshelve capability.
        addSeparator(actions);
        add("P4.Shelve", manager, actions);
        add("P4.Unshelve", manager, actions);
        */

        /*
        add("P4.ResolveConflicts", manager, actions);
        */

        return actions;
    }

    @Nullable
    @Override
    public List<AnAction> getNotInVcsActions(@Nullable Project project, @Nullable DataContext dataContext) {
        // Don't do anything if it's not in source control
        return null;
    }

    @Override
    public boolean replaceVcsActionsFor(@NotNull AbstractVcs activeVcs, @Nullable DataContext dataContext) {
        if (!P4Vcs.VCS_NAME.equals(activeVcs.getName())) {
            return false;
        }
        return true;
    }

    private static void addSeparator(@NotNull final List<AnAction> actions) {
        actions.add(new Separator());
    }

    private static void add(String actionName, ActionManager manager, List<AnAction> actions) {
        final AnAction action = manager.getAction(actionName);
        assert action != null;
        actions.add(action);
    }
}
