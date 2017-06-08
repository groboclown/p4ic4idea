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

package net.groboclown.idea.p4ic.v2.file;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.CommitSession;
import com.intellij.openapi.vcs.changes.CommitSessionContextAware;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.changes.ShelveChange;
import net.groboclown.idea.p4ic.v2.ui.alerts.DistinctDialog;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public class P4ShelveChangesCommitExecutor implements CommitExecutor {
    private final Project project;

    public P4ShelveChangesCommitExecutor(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getActionText() {
        return P4Bundle.getString("action.shelve");
    }

    @NotNull
    @Override
    public CommitSession createCommitSession() {
        return new ShelveCommitExecutor();
    }

    private class ShelveCommitExecutor implements CommitSession, CommitSessionContextAware {

        @Override
        public void execute(Collection<Change> changes, String commitMessage) {
            P4Vcs vcs = P4Vcs.getInstance(project);

            // TODO finish implementing.
            DistinctDialog.showMessageDialog(project, "P4Shelve not implemented yet.", "Shelve Problem",
                    NotificationType.ERROR);
        }

        @Override
        public boolean canExecute(Collection<Change> changes, String commitMessage) {
            if (changes.isEmpty() || commitMessage == null || commitMessage.isEmpty()) {
                return false;
            }
            for (Change change : changes) {
                if (change instanceof ShelveChange) {
                    return false;
                }
            }
            return true;
        }

        @Nullable
        @Override
        public JComponent getAdditionalConfigurationUI() {
            return null;
        }

        @Nullable
        @Override
        public JComponent getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage) {
            return null;
        }

        @Override
        public void executionCanceled() {

        }

        @Nullable
        @Override
        public String getHelpId() {
            return null;
        }

        // @Override
        public ValidationInfo validateFields() {
            return null;
        }

        @Override
        public void setContext(CommitContext context) {

        }
    }
}
