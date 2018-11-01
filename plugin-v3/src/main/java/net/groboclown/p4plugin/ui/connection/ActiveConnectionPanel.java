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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.ServerErrorEvent;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import net.groboclown.p4.server.impl.util.IntervalPeriodExecution;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class ActiveConnectionPanel {
    private static final Logger LOG = Logger.getInstance(ActiveConnectionPanel.class);

    private final Project project;
    private final IntervalPeriodExecution runner;
    private JPanel root;
    private ConnectionTreeRootNode treeNode = new ConnectionTreeRootNode();
    private DefaultTreeModel connectionTreeModel;
    private Tree connectionTree;


    public ActiveConnectionPanel(@NotNull Project project, @Nullable Disposable parentDisposable) {
        this.project = project;
        if (parentDisposable == null) {
            parentDisposable = project;
        }
        runner = new IntervalPeriodExecution(() -> {
            // FIXME the tree is collapsed every time it's refreshed.
            // Attempts to store and restore the expanded list fails, and attempts to refresh by updating
            // the data without creating new data fail.
            connectionTree.setPaintBusy(true);
            treeNode.refresh(project);
            ApplicationManager.getApplication().invokeAndWait(() -> connectionTreeModel.reload());
            connectionTree.setPaintBusy(false);
        }, 5, TimeUnit.SECONDS);

        // Listeners must be registered after initializing the base data.
        final String cacheId = AbstractCacheMessage.createCacheId(project, ActiveConnectionPanel.class);
        MessageBusClient.ApplicationClient appBus = MessageBusClient.forApplication(parentDisposable);
        MessageBusClient.ProjectClient clientBus = MessageBusClient.forProject(project, parentDisposable);

        ClientConfigAddedMessage.addListener(clientBus, cacheId, e -> refresh());
        ClientConfigRemovedMessage.addListener(clientBus, cacheId, event -> refresh());
        ServerConnectedMessage.addListener(appBus, cacheId, (e) -> refresh());
        UserSelectedOfflineMessage.addListener(clientBus, cacheId, name -> refresh());
        ClientActionMessage.addListener(appBus, cacheId, event -> refresh());
        ServerActionCacheMessage.addListener(appBus, cacheId, event -> refresh());
        ConnectionErrorMessage.addListener(appBus, cacheId, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public <E extends Exception> void onHostConnectionError(@NotNull ServerErrorEvent<E> event) {
                // TODO add in a list of errors per server?
                refresh();
            }
        });
        LoginFailureMessage.addListener(appBus, cacheId, new LoginFailureMessage.AllErrorListener() {
            @Override
            protected void onLoginFailure(
                    @NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                // TODO add in a list of errors per server?
                refresh();
            }
        });

        // TODO if errors are displayed here, then add in listeners for
        // FileErrorMessage
        // InternalErrorMessage
        // P4WarningMessage
        // P4ServerErrorMessage
        // CancellationMessage

        // Don't listen to reconnect requests; instead, we listen for server connected.
        // ReconnectRequestMessage.addListener
    }

    public JComponent getRoot() {
        if (root == null) {
            setup();
        }
        return root;
    }


    public void refresh() {
        if (root != null) {
            runner.requestRun();
            connectionTree.getEmptyText().setText(P4Bundle.getString("connection.tree.empty"));
        }
    }


    private void setup() {
        root = new JPanel(new BorderLayout());
        JScrollPane scroll = new JBScrollPane();
        root.add(scroll, BorderLayout.CENTER);
        connectionTree = new Tree();
        scroll.setViewportView(connectionTree);

        connectionTree.getEmptyText().setText(P4Bundle.getString("connection.tree.initial"));
        connectionTree.setEditable(false);
        connectionTreeModel = new DefaultTreeModel(treeNode);
        connectionTree.setModel(connectionTreeModel);
        connectionTree.setCellRenderer(new ConnectionTreeCellRenderer());
        connectionTree.setRootVisible(false);
        DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        connectionTree.setSelectionModel(selectionModel);

        ActionGroup actionButtons = createActionGroup();
        ActionToolbar toolbar =
                ActionManager.getInstance().createActionToolbar("p4.active-connection",
                        actionButtons, false);
        root.add(toolbar.getComponent(), BorderLayout.WEST);

        // TODO add context menu support for each selected node type.
        // TODO add support for navigating to a file if a FilePath is selected.
    }

    private ActionGroup createActionGroup() {
        return new DefaultActionGroup(
                new DumbAwareAction(
                        P4Bundle.getString("active-connection.toolbar.refresh.name"),
                        P4Bundle.getString("active-connection.toolbar.refresh.tooltip"),
                        AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        refresh();
                    }
                },
                new DumbAwareAction(
                        P4Bundle.getString("active-connection.toolbar.expand.name"),
                        P4Bundle.getString("active-connection.toolbar.expand.tooltip"),
                        AllIcons.Actions.Expandall) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        for (int i = 0; i < connectionTree.getRowCount(); i++) {
                            connectionTree.expandRow(i);
                        }
                    }
                },
                new DumbAwareAction(
                        P4Bundle.getString("active-connection.toolbar.collapse.name"),
                        P4Bundle.getString("active-connection.toolbar.collapse.tooltip"),
                        AllIcons.Actions.Collapseall) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        for (int i = 0; i < connectionTree.getRowCount(); i++) {
                            connectionTree.collapseRow(i);
                        }
                    }
                },
                new ConnectionAction(
                        P4Bundle.getString("active-connection.toolbar.connect.name"),
                        P4Bundle.getString("active-connection.toolbar.connect.tooltip"),
                        AllIcons.Actions.Lightning) { // Upload?
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        final ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        if (sel != null && sel.isOffline()) {
                            ReconnectRequestMessage.requestReconnectToClient(project,
                                    sel.getClientConfig().getClientServerRef(), true);
                        }
                    }

                    @Override
                    boolean isEnabled() {
                        ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        return sel != null && sel.isOffline();
                    }
                },
                new ConnectionAction(
                        P4Bundle.getString("active-connection.toolbar.disconnect.name"),
                        P4Bundle.getString("active-connection.toolbar.disconnect.tooltip"),
                        // TODO choose a better icon
                        AllIcons.Actions.Pause) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        final ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        if (sel != null && sel.isOnline()) {
                            UserSelectedOfflineMessage.send(project).userSelectedServerOffline(
                                    new UserSelectedOfflineMessage.OfflineEvent(
                                            sel.getClientConfig().getClientServerRef().getServerName()));
                        }
                    }

                    @Override
                    boolean isEnabled() {
                        ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        return sel != null && sel.isOnline();
                    }
                },
                new ConnectionAction(
                        P4Bundle.getString("active-connection.toolbar.configure.name"),
                        P4Bundle.getString("active-connection.toolbar.configure.tooltip"),
                        AllIcons.General.GearPlain) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        final ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        boolean shown = false;
                        if (sel != null && sel.getClientRootDir() != null) {
                            String dirName = sel.getClientRootDir().getPath();
                            // TODO this is not accurate.  It does not supply the correct VcsDirectoryMapping instance.
                            VcsDirectoryMapping mapping = new VcsDirectoryMapping(dirName, P4VcsKey.VCS_NAME);
                            VirtualFile rootDir = VcsUtil.getVirtualFile(dirName);
                            if (rootDir == null) {
                                rootDir = project.getBaseDir();
                            }
                            mapping.setRootSettings(new P4VcsRootSettingsImpl(project, rootDir));
                            UnnamedConfigurable configurable = P4Vcs.getInstance(project).getRootConfigurable(mapping);
                            if (configurable != null) {
                                new ConfigDialog(project, dirName, configurable)
                                        .show();
                                shown = true;
                            }
                        }
                        if (!shown) {
                            LOG.error("No root directory information from " + sel);
                        }
                    }

                    @Override
                    boolean isEnabled() {
                        ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        return sel != null && sel.getClientRootDir() != null;
                    }
                },

                new ConnectionAction(
                        P4Bundle.getString("active-connection.toolbar.resend-action.name"),
                        P4Bundle.getString("active-connection.toolbar.resend-action.tooltip"),
                        AllIcons.Actions.Upload) {
                    @Override
                    boolean isEnabled() {
                        ClientConfigRoot sel = getSelected(ClientConfigRoot.class);
                        return sel != null && sel.isOnline();
                    }

                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        final ClientConfigRoot selRoot = getSelected(ClientConfigRoot.class);
                        if (selRoot != null) {
                            P4ServerComponent.sendCachedPendingRequests(project, selRoot.getClientConfig())
                                .whenCompleted(c -> LOG.info("Sent pending actions"));
                        }
                    }
                },

                new ConnectionAction(
                        P4Bundle.getString("active-connection.toolbar.remove-action.name"),
                        P4Bundle.getString("active-connection.toolbar.remove-action.tooltip"),
                        AllIcons.Actions.Clear) {
                    @Override
                    public void actionPerformed(AnActionEvent anActionEvent) {
                        final ClientConfigRoot selRoot = getSelected(ClientConfigRoot.class);
                        final ActionChoice sel = getSelected(ActionChoice.class);
                        final CacheComponent cache = CacheComponent.getInstance(project);
                        if (selRoot != null && sel != null && cache != null) {
                            try {
                                cache.getCachePending()
                                        .writeActions(selRoot.getClientConfig().getClientServerRef(),
                                                (c) -> c.removeActionById(sel.getActionId()));
                                // Updating the cache to remove actions doesn't send out events, so we must
                                // manually refresh the view.
                                // TODO This could be done more elegantly.
                                refresh();
                            } catch (InterruptedException e) {
                                LOG.warn(e);
                            }
                        }
                    }

                    @Override
                    boolean isEnabled() {
                        ActionChoice sel = getSelected(ActionChoice.class);
                        return sel != null;
                    }
                }
        );
    }

    private abstract class ConnectionAction extends DumbAwareAction {
        ConnectionAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
            super(text, description, icon);
        }

        @Nullable
        <T> T getSelected(@NotNull Class<T> type) {
            if (connectionTree != null) {
                TreePath treePath = connectionTree.getSelectionPath();
                if (treePath != null) {
                    // Should only be one of each type per path.  So return the first one found.
                    for (Object o: treePath.getPath()) {
                        if (o instanceof DefaultMutableTreeNode) {
                            o = ((DefaultMutableTreeNode) o).getUserObject();
                        }
                        if (type.isInstance(o)) {
                            return type.cast(o);
                        }
                    }
                }
            }
            return null;
        }

        abstract boolean isEnabled();

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            Presentation presentation = e.getPresentation();
            boolean enabled = isEnabled();
            presentation.setEnabled(enabled);
            if (ActionPlaces.isPopupPlace(e.getPlace())) {
                presentation.setVisible(enabled);
            } else {
                presentation.setVisible(true);
            }
        }
    }

    private static class ConfigDialog extends DialogWrapper {
        private final UnnamedConfigurable config;
        private JComponent ui;

        ConfigDialog(Project project, String dir, UnnamedConfigurable config) {
            super(project, false, IdeModalityType.MODELESS);
            this.config = config;
            init();
            setTitle(P4Bundle.message("vcsroot.dialog.title", dir));
            pack();
            centerRelativeToParent();
        }

        protected JComponent createCenterPanel() {
            if (ui == null) {
                ui = config.createComponent();
            }
            return ui;
        }

        @Override
        protected void dispose() {
            super.dispose();
            ui = null;
            config.disposeUIResources();
        }
    }
}
