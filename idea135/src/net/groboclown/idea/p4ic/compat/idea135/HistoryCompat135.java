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

package net.groboclown.idea.p4ic.compat.idea135;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.DiffFromHistoryHandler;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryUtil;
import net.groboclown.idea.p4ic.compat.HistoryCompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HistoryCompat135 extends HistoryCompat {
    @Override
    public DiffFromHistoryHandler createDiffFromHistoryHandler() {
        return new DiffFromHistoryHandler() {
            public void showDiffForOne(@NotNull AnActionEvent e, @NotNull FilePath filePath, @NotNull VcsFileRevision previousRevision, @NotNull VcsFileRevision revision) {
                VcsHistoryUtil.showDifferencesInBackground(getProject(e), filePath, previousRevision, revision, true);
            }

            public void showDiffForTwo(@NotNull FilePath filePath, @NotNull VcsFileRevision revision1, @NotNull VcsFileRevision revision2) {
                VcsHistoryUtil.showDifferencesInBackground(
                        ProjectLocator.getInstance().guessProjectForFile(filePath.getVirtualFile()),
                        filePath, revision1, revision2, true);
            }
        };
    }

    @Nullable
    private static Project getProject(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            project = e.getData(CommonDataKeys.PROJECT);
        }
        return project;
    }
}
