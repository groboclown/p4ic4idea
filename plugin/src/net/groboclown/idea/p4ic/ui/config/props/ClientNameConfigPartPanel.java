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

package net.groboclown.idea.p4ic.ui.config.props;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.BackgroundAwtActionRunner;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.part.ClientNameDataPart;
import net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class ClientNameConfigPartPanel
        extends ConfigPartPanel<ClientNameDataPart> {
    private static final Logger LOG = Logger.getInstance(ClientNameConfigPartPanel.class);


    private JPanel rootPanel;
    private JComboBox/*<String>*/ clientDropdownList;
    private JButton listRefreshButton;
    private AsyncProcessIcon listRefreshSpinner;
    private JLabel listLabel;
    private JPanel listPanel;
    private JPanel refreshPanel;

    ClientNameConfigPartPanel(@NotNull Project project, @NotNull ClientNameDataPart part) {
        super(project, part);

        $$$setupUI$$$();

        listRefreshButton.setIcon(AllIcons.Actions.Refresh);
        listRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshClientList();
            }
        });
    }

    @NotNull
    @Override
    ClientNameDataPart copyPart() {
        ClientNameDataPart ret = new ClientNameDataPart();
        ret.setClientname(getConfigPart().getClientname());
        return ret;
    }

    private void createUIComponents() {
        listRefreshSpinner = new AsyncProcessIcon("Refresh Client List Progress");
        listRefreshSpinner.setName("Refresh Client List Progress");
        listRefreshSpinner.setVisible(false);
    }

    @Override
    public boolean isModified(@NotNull ClientNameDataPart originalPart) {
        if (originalPart.getClientname() == null) {
            return getSelectedClientName() == null;
        }
        return getSelectedClientName() != null && originalPart.getClientname().equals(getSelectedClientName());
    }

    @Nls
    @NotNull
    @Override
    public String getTitle() {
        return P4Bundle.getString("configuration.stack.clientname.title");
    }

    @NotNull
    @Override
    public JPanel getRootPanel() {
        return rootPanel;
    }

    @Override
    public void updateConfigPartFromUI() {
        getConfigPart().setClientname(getSelectedClientName());
        LOG.info("Set the client to " + getConfigPart().getClientname());
    }

    private String getSelectedClientName() {
        Object obj = clientDropdownList.getSelectedItem();
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    private void refreshClientList() {
        final String selected = getSelectedClientName();
        BackgroundAwtActionRunner.runBackgroundAwtAction(listRefreshSpinner,
                new BackgroundAwtActionRunner.BackgroundAwtAction<List<String>>() {
                    @Override
                    public List<String> runBackgroundProcess() {
                        return loadClientList(selected);
                    }

                    @Override
                    public void runAwtProcess(@Nullable final List<String> clientList) {
                        if (clientList != null) {
                            clientDropdownList.removeAllItems();
                            for (String client : clientList) {
                                clientDropdownList.addItem(client);
                            }
                            if (selected != null) {
                                for (int i = 0; i < clientDropdownList.getItemCount(); i++) {
                                    if (selected.equals(clientDropdownList.getItemAt(i))) {
                                        clientDropdownList.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            } else if (!clientList.isEmpty()) {
                                clientDropdownList.setSelectedIndex(0);
                            }
                        }
                        // else already handled the errors; leave the list as it was.
                    }
                });
    }

    private List<String> loadClientList(String selected) {
        final Collection<ClientConfig> configs = getLatestConfig() == null
                ? Collections.<ClientConfig>emptyList()
                : getLatestConfig().getClientConfigs();
        if (configs.size() != 1) {
            getConfigPart().addAdditionalProblem(new ConfigProblem(
                    getConfigPart(), "configuration.client.error.no-single-server"));
            if (selected == null || selected.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(selected);
            }
        } else {
            ConnectionUIConfiguration.ClientResult clients =
                    ClientNameDataPart.loadClientNames(configs.iterator().next());
            List<String> ret = new ArrayList<String>(clients.getClientNames());
            if (!ret.contains(selected)) {
                ret.add(selected);
            }
            return ret;
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(4, 0));
        listLabel = new JLabel();
        this.$$$loadLabelText$$$(listLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.clientname"));
        listLabel.setVerticalAlignment(0);
        rootPanel.add(listLabel, BorderLayout.WEST);
        listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.add(listPanel, BorderLayout.CENTER);
        clientDropdownList = new JComboBox();
        clientDropdownList.setEditable(true);
        clientDropdownList.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.clientname.dropdown-list.tooltip"));
        listPanel.add(clientDropdownList, BorderLayout.CENTER);
        refreshPanel = new JPanel();
        refreshPanel.setLayout(new BorderLayout(0, 0));
        listPanel.add(refreshPanel, BorderLayout.EAST);
        listRefreshButton = new JButton();
        listRefreshButton.setText("");
        listRefreshButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.refresh-client-list"));
        refreshPanel.add(listRefreshButton, BorderLayout.CENTER);
        refreshPanel.add(listRefreshSpinner, BorderLayout.EAST);
        listLabel.setLabelFor(clientDropdownList);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
