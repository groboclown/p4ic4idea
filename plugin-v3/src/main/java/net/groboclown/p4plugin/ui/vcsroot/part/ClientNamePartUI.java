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

package net.groboclown.p4plugin.ui.vcsroot.part;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.AsyncProcessIcon;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.ClientNameConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.vcsroot.ConfigConnectionController;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUI;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class ClientNamePartUI extends ConfigPartUI<ClientNameConfigPart> {
    private JComponent rootPanel;
    private JComboBox<String> clientDropdownList;
    private JButton listRefreshButton;
    private AsyncProcessIcon listRefreshSpinner;
    public static final ConfigPartUIFactory FACTORY = new Factory();


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
            listRefreshButton.setEnabled(false);
            listRefreshButton.setVisible(false);
            listRefreshSpinner.resume();
            listRefreshSpinner.setVisible(true);
            rootPanel.doLayout();
            rootPanel.repaint();
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

    @Override
    protected void loadUIValuesIntoPart(@NotNull ClientNameConfigPart part) {
        part.setClientname(clientDropdownList.getItemAt(clientDropdownList.getSelectedIndex()));
    }

    @Override
    public JComponent getPanel() {
        return rootPanel;
    }


    private void refreshList(Project project, ClientConfig config) {
        // FIXME refresh the list of clients.
    }


    private void setupUI() {
        rootPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(P4Bundle.getString("configuration.clientname"));


        clientDropdownList = new ComboBox<>();
        clientDropdownList.setEditable(true);
        clientDropdownList.setToolTipText(P4Bundle.getString("configuration.clientname.dropdown-list.tooltip"));


        JPanel buttonPanel = new JPanel(new FlowLayout());
        listRefreshButton = new JButton(AllIcons.Actions.Refresh);
        listRefreshSpinner = new AsyncProcessIcon("Refresh Client List Progress");
        listRefreshSpinner.setName("Refresh Client List Progress");
        listRefreshSpinner.setVisible(false);
        buttonPanel.add(listRefreshSpinner);
        buttonPanel.add(listRefreshButton);


        rootPanel.add(label, BorderLayout.WEST);
        rootPanel.add(clientDropdownList, BorderLayout.CENTER);
        rootPanel.add(buttonPanel, BorderLayout.EAST);
    }
}
