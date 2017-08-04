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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class VcsDockedComponent implements Disposable, ProjectComponent {
    private final Project project;

    public static VcsDockedComponent getInstance(@NotNull Project project) {
        return project.getComponent(VcsDockedComponent.class);
    }

    private VcsDockedComponent(final Project project) {
        this.project = project;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (project.isDisposed()) {
                    return;
                }
                final ToolWindow toolWindow = getToolWindow();
                if (toolWindow == null) {
                    // can happen if the project isn't fully initialized yet
                    return;
                }
                final ContentManager contentManager = toolWindow.getContentManager();
                contentManager.addContentManagerListener(new ContentManagerAdapter() {
                    @Override
                    public void contentRemoved(ContentManagerEvent event) {
                        final JComponent component = event.getContent().getComponent();
                        if (component instanceof Disposable) {
                            Disposer.dispose((Disposable) component);
                        }
                    }
                });
                toolWindow.installWatcher(contentManager);
            }
        });
    }

    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        Disposer.dispose(this);
    }

    @Override
    public void initComponent() {
        // do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getSimpleName();
    }


    @Nullable
    private ToolWindow getToolWindow() {
        return ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
    }

    @Override
    public void dispose() {
        // TODO release any registered windows.
    }


    public void addVcsTab(@NotNull @NonNls String title,
            @NotNull JComponent component,
            boolean showTab,
            boolean replaceExistingComponent) {
        if (component instanceof Disposable) {
            Disposer.register(this, (Disposable) component);
        }
        final ToolWindow toolW = getToolWindow();
        if (toolW == null) {
            // cannot do anything
            return;
        }
        final ContentManager contentManager = getToolWindow().getContentManager();
        final Content existingContent = contentManager.findContent(title);
        if (existingContent != null) {
            if (!replaceExistingComponent) {
                contentManager.setSelectedContent(existingContent);
                return;
            }
            else if (!existingContent.isPinned()) {
                contentManager.removeContent(existingContent, true);
                existingContent.release();
            }
        }

        final Content content = contentManager.getFactory().createContent(component, title, false);
        contentManager.addContent(content);
        if (showTab) {
            getToolWindow().activate(null, false);
        }

        /*
        final CvsTabbedWindowComponent newComponent =
                new CvsTabbedWindowComponent(component, addDefaultToolbar, toolbarActions, contentManager, helpId);
        final Content content = contentManager.getFactory().createContent(newComponent.getShownComponent(), title,
                false);
        newComponent.setContent(content);
        contentManager.addContent(content);
        if (showTab) {
            getToolWindow().activate(null, false);
        }
        */
    }
}
