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
package net.groboclown.idea.p4ic.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinChangeListSpecificComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class P4OnCheckinPanel implements CheckinChangeListSpecificComponent {
    private final Project project;
    private final CheckinProjectPanel panel;

    public P4OnCheckinPanel(@NotNull Project project, @NotNull CheckinProjectPanel panel) {
        this.project = project;
        this.panel = panel;
    }

    @Override
    public void onChangeListSelected(LocalChangeList list) {
        // TODO implement
    }

    @Override
    public JComponent getComponent() {
        // TODO add job selection panel

        return new JPanel();
    }

    @Override
    public void refresh() {
        // TODO implement
    }

    @Override
    public void saveState() {

    }

    @Override
    public void restoreState() {

    }
}
