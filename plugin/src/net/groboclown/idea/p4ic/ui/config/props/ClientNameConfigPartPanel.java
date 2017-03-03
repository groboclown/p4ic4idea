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
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.part.ClientNameDataPart;
import net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

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

        clientDropdownList.addItem(part.getClientname());
        clientDropdownList.setSelectedIndex(0);
        listRefreshButton.setIcon(AllIcons.Actions.Refresh);
        listRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshClientList();
            }
        });

        // FIXME this is a temporary fix to prevent users from pressing the refresh button.
        // We'll remove this line once the refresh works right.
        // The underlying issue is with the authentication; the way we're doing it now
        // does not correctly load the user's password from the PasswordManager.
        listRefreshButton.setEnabled(false);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Set the client to " + getConfigPart().getClientname());
        }
    }

    private String getSelectedClientName() {
        Object obj = clientDropdownList.getSelectedItem();
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    private void refreshClientList() {
        LOG.debug("Refreshing client list...");
        final String selected = getSelectedClientName();
        BackgroundAwtActionRunner.runBackgroundAwtAction(
                listRefreshSpinner,
                listRefreshButton,
                new BackgroundAwtActionRunner.BackgroundAwtAction<Collection<String>>() {
                    @Override
                    public Collection<String> runBackgroundProcess() {
                        Collection<String> list = loadClientList(selected);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("client list loaded: " + list);
                        }
                        return list;
                    }

                    @Override
                    public void runAwtProcess(@Nullable final Collection<String> clientList) {
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

    private Collection<String> loadClientList(String selected) {
        final P4ProjectConfig config = loadProjectConfigFromUI();
        final Collection<ClientConfig> configs = config == null
                ? Collections.<ClientConfig>emptyList()
                : config.getClientConfigs();
        if (configs.isEmpty()) {
            LOG.debug("No client configs in project");
            getConfigPart().addAdditionalProblem(new ConfigProblem(
                    getConfigPart(), false, "configuration.client.error.no-server"));
        } else if (configs.size() != 1) {
            getConfigPart().addAdditionalProblem(new ConfigProblem(
                    getConfigPart(), false, "configuration.client.error.no-single-server"));
            // Still load up the client names, though.
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading clients with configs " + configs + " from " + config);
        }
        Set<String> ret = new HashSet<String>();
        for (ConnectionUIConfiguration.ClientResult result : ClientNameDataPart.loadClientNames(configs).values()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded clients " + result.getClientNames() + "; problem " + result.getConnectionProblem());
            }
            ret.addAll(result.getClientNames());
            if (result.getConnectionProblem() != null) {
                getConfigPart().addAdditionalProblem(new ConfigProblem(getConfigPart(), result.getConnectionProblem()));
            }
        }
        ret.add(selected);
        return ret;
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
        refreshPanel.add(listRefreshSpinner, BorderLayout.WEST);
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
