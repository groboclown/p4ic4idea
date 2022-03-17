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

package net.groboclown.p4plugin.modules.clientconfig.view.part;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.AsyncProcessIcon;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import net.groboclown.p4.server.impl.config.part.ClientNameConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.SwingUtil;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigConnectionController;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigPartUI;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class ClientNamePartUI extends ConfigPartUI<ClientNameConfigPart> {
    public static final ConfigPartUIFactory FACTORY = new Factory();
    private static final Icon REFRESH = AllIcons.Actions.Refresh;
    private static final Icon ERROR = AllIcons.General.Warning;

    private final Object sync = new Object();
    private JComponent rootPanel;
    private JComboBox<String> clientDropdownList;
    private JButton listRefreshButton;
    private JLabel errorLabel;
    private AsyncProcessIcon listRefreshSpinner;


    private static class Factory implements ConfigPartUIFactory {
        @Nls
        @NotNull
        @Override
        public String getName() {
            return P4Bundle.getString("configuration.connection-choice.picker.client-name");
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }

        @Nullable
        @Override
        public ConfigPartUI createForPart(ConfigPart part, ConfigConnectionController controller) {
            if (part instanceof ClientNameConfigPart) {
                return new ClientNamePartUI((ClientNameConfigPart) part, controller);
            }
            return null;
        }

        @NotNull
        @Override
        public ConfigPartUI createEmpty(@NotNull VirtualFile vcsRoot, ConfigConnectionController controller) {
            return new ClientNamePartUI(new ClientNameConfigPart(getName(), vcsRoot, Collections.emptyMap()), controller);
        }
    }


    private ClientNamePartUI(ClientNameConfigPart part, ConfigConnectionController controller) {
        super(part);
        setupUI();

        clientDropdownList.removeAllItems();
        if (part.hasClientnameSet()) {
            clientDropdownList.addItem(part.getClientname());
            clientDropdownList.setSelectedIndex(0);
        }

        controller.addConfigConnectionListener(this::refreshList);
        listRefreshButton.addActionListener(e -> {
            setRefreshState(true);
            controller.refreshConfigConnection();
        });
    }


    @Nls
    @NotNull
    @Override
    public String getPartTitle() {
        return P4Bundle.getString("configuration.connection-choice.picker.client-name");
    }

    @Nls
    @NotNull
    @Override
    public String getPartDescription() {
        return P4Bundle.getString("configuration.connection-choice.picker.client-name.description");
    }

    @NotNull
    @Override
    protected ClientNameConfigPart loadUIValuesIntoPart(@NotNull ClientNameConfigPart part) {
        part.setClientname(getCurrentClientname());
        return part;
    }

    @Override
    public JComponent getPanel() {
        return rootPanel;
    }


    private String getCurrentClientname() {
        synchronized (sync) {
            return (String) clientDropdownList.getSelectedItem();
        }
    }


    private void refreshList(Project project, ConfigPart part,
            ClientConfig clientConfig, ServerConfig serverConfig) {
        String current = getCurrentClientname();
        synchronized (sync) {
            clientDropdownList.removeAllItems();
            clientDropdownList.addItem(current);
            clientDropdownList.setSelectedIndex(0);
        }
        if (serverConfig != null) {
            // Load the list of clients from the server.
            // Similar to the P4VcsRootConfigurable class, this
            // needs to use a stand-alone connection without caching.


            // FIXME if there's a login issue, such as a bad password, the user is not notified.
            // Specifically, the user isn't given an opportunity to enter the correct password.

            P4ServerComponent
            .getClientsForUser(project, new OptionalClientServerConfig(serverConfig, clientConfig))
            .whenCompleted((clients) -> {
                synchronized (sync) {
                    for (P4WorkspaceSummary client : clients.getClients()) {
                        clientDropdownList.addItem(client.getClientName());
                    }
                }
                errorLabel.setVisible(false);
                setRefreshState(false);
            })
            .whenServerError((e) -> {
                // TODO on an error, mark the UI component has
                // having a problem.  Maybe even a tooltip with the error,
                // or a pop-up micro-dialog with the error list.
                errorLabel.setVisible(true);
                setRefreshState(false);
            });
        } else {
            errorLabel.setVisible(true);
            setRefreshState(false);
        }
    }


    private void setRefreshState(boolean spinnerOn) {
        listRefreshButton.setEnabled(!spinnerOn);
        listRefreshButton.setVisible(!spinnerOn);
        if (spinnerOn) {
            listRefreshSpinner.resume();
        } else {
            listRefreshSpinner.suspend();
        }
        listRefreshSpinner.setVisible(spinnerOn);
        rootPanel.doLayout();
        rootPanel.repaint();
    }


    private void setupUI() {
        rootPanel = new JPanel(new BorderLayout());

        clientDropdownList = new ComboBox<>();
        clientDropdownList.setEditable(true);
        clientDropdownList.setToolTipText(P4Bundle.getString("configuration.clientname.dropdown-list.tooltip"));


        JLabel label = SwingUtil.createLabelFor(P4Bundle.getString("configuration.clientname"), clientDropdownList);


        JPanel buttonPanel = new JPanel(new FlowLayout());
        errorLabel = new JLabel(ERROR);
        errorLabel.setVisible(false);
        listRefreshButton = new JButton(REFRESH);
        listRefreshButton.setPreferredSize(new Dimension(REFRESH.getIconWidth() + 2, REFRESH.getIconHeight() + 2));
        listRefreshSpinner = new AsyncProcessIcon("Refresh Client List Progress");
        listRefreshSpinner.setName("Refresh Client List Progress");
        listRefreshSpinner.setVisible(false);
        buttonPanel.add(errorLabel);
        buttonPanel.add(listRefreshSpinner);
        buttonPanel.add(listRefreshButton);


        rootPanel.add(label, BorderLayout.WEST);
        rootPanel.add(clientDropdownList, BorderLayout.CENTER);
        rootPanel.add(buttonPanel, BorderLayout.EAST);
    }
}
