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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.perforce.p4java.exception.AuthenticationFailedException;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class ActiveConnectionPanel {
    private final Project project;
    private final Object refreshSync = new Object();
    private JPanel root;
    private ConnectionTreeRootNode treeNode = new ConnectionTreeRootNode();
    private DefaultTreeModel connectionTreeModel;


    public ActiveConnectionPanel(@NotNull Project project, @Nullable Disposable parentDisposable) {
        this.project = project;
        if (parentDisposable == null) {
            parentDisposable = project;
        }
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
    }

    public JComponent getRoot() {
        if (root == null) {
            setup();
        }
        return root;
    }


    public void refresh() {
        if (root != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                // TODO make this more fine-tuned refreshing.
                synchronized (refreshSync) {
                    treeNode.refresh(project);
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    synchronized (refreshSync) {
                        connectionTreeModel.reload();
                    }
                });
            });
        }
    }


    private void setup() {
        root = new JPanel(new BorderLayout());
        JScrollPane scroll = new JBScrollPane();
        root.add(scroll, BorderLayout.CENTER);
        Tree connectionTree = new Tree();
        scroll.setViewportView(connectionTree);

        connectionTree.getEmptyText().setText(P4Bundle.getString("connection.tree.empty"));
        connectionTree.setEditable(false);
        connectionTreeModel = new DefaultTreeModel(treeNode);
        connectionTree.setModel(connectionTreeModel);
        connectionTree.setCellRenderer(new ConnectionTreeCellRenderer());
        connectionTree.setRootVisible(false);

        // TODO add a proper side-bar toolbar.
        //      ToolbarPanel ?
        //      ActionToolbarImpl ?
        //      ButtonToolbarImpl ? <- called from ActionManager.getInstance().createButtonToolbar()

        // TODO add connect / disconnect buttons.
        //      These need to be context-sensitive to the selected node in the tree.

        // TODO add button to edit node configuration.

        JPanel sidebar = new JPanel(new FlowLayout());
        root.add(sidebar, BorderLayout.WEST);
        JButton refreshButton = SwingUtil.iconOnlyButton(new JButton(), AllIcons.Actions.Refresh, SwingUtil.ButtonType.MAJOR);
        sidebar.add(refreshButton);
        refreshButton.addActionListener((e) -> refresh());
    }
}
