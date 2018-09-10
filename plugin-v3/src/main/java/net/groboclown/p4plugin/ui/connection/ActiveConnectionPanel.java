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
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.impl.util.IntervalPeriodExecution;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Collection;
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
            // There's a bit of code here to attempt to maintain the
            // expanded row set, but it doesn't work.
            connectionTree.setPaintBusy(true);
            Collection<TreeNode> needsRefresh = treeNode.refresh(project);
            Collection<Integer> expanded = connectionTree.getExpandableItemsHandler().getExpandedItems();
            ApplicationManager.getApplication().invokeAndWait(() -> {
                for (TreeNode toRefresh: needsRefresh) {
                    connectionTreeModel.reload(toRefresh);
                }
                // Is this accurate?  Probably not, but it at least attempts to keep the expansion.
                for (Integer nodeIndex : expanded) {
                    if (nodeIndex < connectionTree.getRowCount()) {
                        connectionTree.expandRow(nodeIndex);
                    }
                }
            });
            connectionTree.setPaintBusy(false);
        }, 5, TimeUnit.SECONDS);

        // Listeners must be registered after initializing the base data.
        final String cacheId = AbstractCacheMessage.createCacheId();
        MessageBusClient.ApplicationClient appBus = MessageBusClient.forApplication(parentDisposable);
        MessageBusClient.ProjectClient clientBus = MessageBusClient.forProject(project, parentDisposable);

        ClientConfigAddedMessage.addListener(clientBus, (root, clientConfig) -> refresh());
        ClientConfigRemovedMessage.addListener(clientBus, event -> refresh());
        ServerConnectedMessage.addListener(appBus, (serverConfig, loggedIn) -> refresh());
        UserSelectedOfflineMessage.addListener(clientBus, name -> refresh());
        ClientActionMessage.addListener(appBus, cacheId, event -> refresh());
        ServerActionCacheMessage.addListener(appBus, cacheId, event -> refresh());
        ConnectionErrorMessage.addListener(appBus, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public void onHostConnectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @Nullable Exception e) {
                // TODO add in a list of errors per server?
                refresh();
            }
        });
        LoginFailureMessage.addListener(appBus, new LoginFailureMessage.AllErrorListener() {
            @Override
            public void onLoginFailure(@NotNull ServerConfig serverConfig, @NotNull AuthenticationFailedException e) {
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
        }
    }


    private void setup() {
        root = new JPanel(new BorderLayout());
        JScrollPane scroll = new JBScrollPane();
        root.add(scroll, BorderLayout.CENTER);
        connectionTree = new Tree();
        scroll.setViewportView(connectionTree);

        connectionTree.getEmptyText().setText(P4Bundle.getString("connection.tree.empty"));
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

        // FIXME add context menu support for each selected node type.
        // FIXME add support for navigating to a file if a FilePath is selected.
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
                        AllIcons.Actions.Upload) { // Lightning?
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
                            UserSelectedOfflineMessage.requestOffline(project,
                                    sel.getClientConfig().getClientServerRef().getServerName());
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
                            UnnamedConfigurable configurable = P4Vcs.getInstance(project).getRootConfigurable(
                                    new VcsDirectoryMapping(dirName, P4VcsKey.VCS_NAME)
                            );
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

                // TODO add an action that allows retrying the pending actions.
                // (these cannot be done in isolation - they are strictly ordered).

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
