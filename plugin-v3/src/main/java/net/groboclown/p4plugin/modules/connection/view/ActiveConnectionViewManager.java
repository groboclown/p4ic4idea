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

package net.groboclown.p4plugin.modules.connection.view;

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
public class ActiveConnectionViewManager implements ChangesViewContentProvider, Disposable {
    private final Project project;
    private ActiveConnectionPanel panel;
    private boolean disposed = false;

    public ActiveConnectionViewManager(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public JComponent initContent() {
        if (panel == null) {
            panel = new ActiveConnectionPanel(project, this);
        }
        return panel.getRoot();
    }

    @Override
    public void disposeContent() {
        dispose();
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            panel = null;
            Disposer.dispose(this);
        }
    }

    public boolean isDisposed() {
        return disposed;
    }
}
