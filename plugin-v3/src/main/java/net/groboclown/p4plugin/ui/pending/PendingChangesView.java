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

package net.groboclown.p4plugin.ui.pending;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DumbAwareActionButton;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.BackgroundAwtActionRunner;
import net.groboclown.idea.p4ic.ui.VcsDockedComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class PendingChangesView
        implements ProjectComponent {
    private static final Logger LOG = Logger.getInstance(PendingChangesView.class);

    private final Project project;

    private JComponent view;

    private volatile boolean createdTab = false;

    public PendingChangesView(Project project) {
        this.project = project;
        LOG.debug("Created pending view component");
    }


    @Override
    public void initComponent() {
        // do nothing; wait for the project to be opened.
        LOG.debug("initializing");
    }

    public void projectOpened() {
        LOG.debug("adding pending changes tab");
        createTab();
    }

    private JComponent createView() {
        if (view != null) {
            return view;
        }

        final PendingChangesTreeList changeTree = new PendingChangesTreeList(project,
                false, null);
        final ChangeTreeButton[] buttons = createActionButtons(changeTree);
        for (ChangeTreeButton button : buttons) {
            button.setContextComponent(changeTree);
        }

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(changeTree);

        boolean decorateButtons = UIUtil.isUnderAquaLookAndFeel() && BorderLayout.SOUTH.equals(getToolbarPlacement());
        ActionManagerEx mgr = (ActionManagerEx) ActionManager.getInstance();
        final ActionToolbar toolbar = mgr.createActionToolbar("ToolbarDecorator",
                new DefaultActionGroup(buttons),
                isToolbarVertical(),
                decorateButtons);
        toolbar.getComponent().setBorder(null);

        JPanel root = new JPanel(new BorderLayout()) {
            public void addNotify() {
                super.addNotify();
                updateButtons(changeTree, toolbar, buttons);
            }
        };
        // Initialize the buttons
        updateButtons(changeTree, toolbar, buttons);

        root.add(scrollPane, BorderLayout.CENTER);
        root.add(toolbar.getComponent(), getToolbarPlacement());

        view = root;
        return root;
    }

    @NotNull
    private ChangeTreeButton[] createActionButtons(@NotNull final PendingChangesTreeList changeTree) {
        final SpinnerChangeTreeButton spinner = new SpinnerChangeTreeButton();
        return new ChangeTreeButton[] {
                spinner,
                new BackgroundChangeTreeButton<List<PendingChangelist>>(
                        P4Bundle.getString("pending-changes-view.button.refresh.name"),
                        P4Bundle.getString("pending-changes-view.button.refresh.desc"),
                        AllIcons.Actions.Refresh,
                        spinner
                ) {
                    @Nullable
                    @Override
                    protected List<PendingChangelist> background(@NotNull PendingChangeItemSet selectedItems) {
                        return PendingChangelist.getPendingChanges(project);
                    }

                    @Override
                    protected void after(@Nullable List<PendingChangelist> value) {
                        if (value == null) {
                            value = Collections.emptyList();
                        }
                        changeTree.setChangesToDisplay(value);
                    }

                    @Override
                    boolean checkEnabled(@NotNull PendingChangeItemSet items) {
                        return true;
                    }
                },
                new BackgroundChangeTreeButton<Object>(
                        P4Bundle.getString("pending-changes-view.button.delete.name"),
                        P4Bundle.getString("pending-changes-view.button.delete.desc"),
                        AllIcons.Actions.Delete,
                        spinner
                ) {
                    @Nullable
                    @Override
                    protected Object background(@NotNull PendingChangeItemSet selectedItems) {
                        return null;
                    }

                    @Override
                    protected void after(@Nullable Object value) {

                    }

                    @Override
                    boolean checkEnabled(@NotNull PendingChangeItemSet items) {
                        return false;
                    }
                }
        };
    }


    @NotNull
    private String getToolbarPlacement() {
        return SystemInfo.isMac ? BorderLayout.SOUTH : BorderLayout.WEST;
    }

    private boolean isToolbarVertical() {
        return ! (BorderLayout.WEST.equals(getToolbarPlacement()) || BorderLayout.EAST.equals(getToolbarPlacement()));
    }

    private void createTab() {
        if (! createdTab) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    JComponent component = createView();
                    createdTab = VcsDockedComponent.getInstance(project).addVcsTab(
                            P4Bundle.getString("pending-changes-view.title"),
                            component,
                            false,
                            true
                    );
                    createTab();
                }
            });
        }
    }

    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void disposeComponent() {
        VcsDockedComponent.getInstance(project).removeFromVcsTab(P4Bundle.getString("pending-changes-view.title"));
    }

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getSimpleName();
    }



    private static void updateButtons(PendingChangesTreeList changeTree,
            ActionToolbar toolbar, ChangeTreeButton[] buttons) {
        PendingChangeItemSet items = changeTree.getSelectedItems();
        for (ChangeTreeButton button : buttons) {
            button.updateState(changeTree, items);
        }
        toolbar.updateActionsImmediately();
    }

    private static abstract class ChangeTreeButton extends DumbAwareActionButton {
        ChangeTreeButton(@NotNull @NonNls String name, @NotNull @NonNls String description, @Nullable Icon icon) {
            super(name, description, icon);
        }

        abstract boolean checkEnabled(@NotNull PendingChangeItemSet items);

        void updateState(@NotNull PendingChangesTreeList tree, @NotNull PendingChangeItemSet items) {
            // Does nothing
        }
    }

    private static class SpinnerChangeTreeButton extends ChangeTreeButton implements CustomComponentAction {
        final AsyncProcessIcon icon;
        private int runningCount = 0;

        SpinnerChangeTreeButton() {
            super("", "", null);
            this.icon = new AsyncProcessIcon("Background action in progress");
            stopSpinner();
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            // do nothing
        }

        String getPosition(@NotNull String toolbarPosition) {
            if (BorderLayout.SOUTH.equals(toolbarPosition) || BorderLayout.NORTH.equals(toolbarPosition)) {
                return BorderLayout.WEST;
            }
            return BorderLayout.NORTH;
        }

        synchronized void startSpinner() {
            if (runningCount == 0) {
                icon.resume();
                icon.setVisible(true);
            }
            runningCount++;
        }

        synchronized void stopSpinner() {
            runningCount--;
            if (runningCount <= 0) {
                runningCount = 0;
                icon.suspend();
                icon.setVisible(false);
            }
        }

        @Override
        public JComponent createCustomComponent(Presentation presentation) {
            return icon;
        }

        @Override
        boolean checkEnabled(@NotNull PendingChangeItemSet items) {
            return true;
        }
    }


    private static abstract class BackgroundChangeTreeButton<T> extends ChangeTreeButton {
        private final String name;
        private final SpinnerChangeTreeButton spinner;
        private PendingChangesTreeList changeTree;

        BackgroundChangeTreeButton(@NotNull @NonNls String name, @NotNull @NonNls String description,
                @NotNull Icon icon, @NotNull SpinnerChangeTreeButton spinner) {
            super(name, description, icon);
            this.name = name;
            this.spinner = spinner;
            setEnabledInModalContext(true);
            setEnabled(true);
            getTemplatePresentation().setEnabled(true);
        }

        @Override
        public void actionPerformed(AnActionEvent anActionEvent) {
            LOG.info("Starting background action");
            if (changeTree != null) {
                spinner.startSpinner();
                BackgroundAwtActionRunner.runBackgroundAwtAction(
                        new BackgroundAwtActionRunner.BackgroundAwtAction<T>() {
                            @Override
                            public T runBackgroundProcess() {
                                return background(changeTree.getSelectedItems());
                            }

                            @Override
                            public void runAwtProcess(T value) {
                                spinner.stopSpinner();
                                after(value);
                            }
                        });
            }
        }

        @Nullable
        protected abstract T background(@NotNull PendingChangeItemSet selectedItems);

        protected abstract void after(@Nullable T value);


        void updateState(@NotNull PendingChangesTreeList tree, @NotNull PendingChangeItemSet items) {
            this.changeTree = tree;
            // FIXME DEBUG
            setEnabledInModalContext(true);
            setEnabled(true);
            // setEnabled(checkEnabled(items));
        }
    }


}
