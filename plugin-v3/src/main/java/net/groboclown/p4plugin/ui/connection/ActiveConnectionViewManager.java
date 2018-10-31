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

package net.groboclown.p4plugin.ui.connection;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Allows for using the VCS extension mechanism to add the active connection panel to the
 * version control docked view.
 */
public class ActiveConnectionViewManager implements ChangesViewContentProvider {
    private final Project project;
    private final Disposable disposable;
    private ActiveConnectionPanel panel;

    public ActiveConnectionViewManager(@NotNull Project project) {
        this.project = project;
        this.disposable = () -> {
            // the connection panel performs its own disposing.
            panel = null;
        };
        Disposer.register(project, disposable);
    }

    @Override
    public JComponent initContent() {
        if (panel == null) {
            panel = new ActiveConnectionPanel(project, disposable);
        }
        return panel.getRoot();
    }

    @Override
    public void disposeContent() {
        Disposer.dispose(disposable);
    }
}
