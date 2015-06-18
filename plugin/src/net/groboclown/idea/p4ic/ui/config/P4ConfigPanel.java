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
package net.groboclown.idea.p4ic.ui.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4Config.ConnectionMethod;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import net.groboclown.idea.p4ic.ui.ErrorDialog;
import net.groboclown.idea.p4ic.ui.connection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class P4ConfigPanel {
    private static final Logger LOG = Logger.getInstance(P4ConfigPanel.class);

    private JPanel myMainPanel;
    private JComboBox/*<ConnectionPanel>*/ myConnectionChoice; // JDK 1.6 does not have generic combo box
    private JButton myRefreshClientList;
    private JComboBox/*<String>*/ myClientList; // JDK 1.6 does not have generic combo box
    private JCheckBox myReuseEnvValueCheckBox;
    private JButton checkConnectionButton;
    private JCheckBox mySilentlyGoOfflineOnCheckBox;
    private P4ConfigConnectionPanel myP4ConfigConnectionPanel;
    private ClientPasswordConnectionPanel myClientPasswordConnectionPanel;
    private AuthTicketConnectionPanel authTicketConnectionPanel;
    private SSOConnectionPanel mySSOConnectionPanel;
    private EnvConnectionPanel myEnvConnectionPanel;
    private JPanel myConnectionTypeContainerPanel;
    private JLabel myConnectionDescriptionLabel;
    private JCheckBox mySavePasswordsCheckBox;
    private JLabel myPasswordWarning;
    private RelP4ConfigConnectionPanel myRelP4ConfigPanel;
    private JComboBox/*<String>*/ myResolvePath; // JDK 1.6 does not have generic combo box
    private JTextArea myResolvedValuesField;
    private JLabel myResolvePathLabel;
    private JButton myRefreshResolved;

    private final Object relativeConfigPathMapSync = new Object();
    private Map<String, P4Config> relativeConfigPathMap;

    private Project myProject;

    public P4ConfigPanel() {
        // Initialize GUI constant values
        $$$setupUI$$$();

        myConnectionChoice.setRenderer(new AuthenticationMethodRenderer());
        myConnectionChoice.setEditable(false);
        // Could add checks to ensure that there is 1 and only 1 connection panel for
        // each connection type
        myConnectionChoice.addItem(myP4ConfigConnectionPanel);
        myConnectionChoice.addItem(myRelP4ConfigPanel);
        myConnectionChoice.addItem(myClientPasswordConnectionPanel);
        myConnectionChoice.addItem(authTicketConnectionPanel);

        // Not supported yet
        //myConnectionChoice.addItem(mySSOConnectionPanel);

        myConnectionChoice.addItem(myEnvConnectionPanel);

        // an initial value for connection choice
        myConnectionChoice.setSelectedItem(myEnvConnectionPanel);


        // Initialize GUI listeners
        myConnectionChoice.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeConnectionSelection();
            }
        });
        myRefreshClientList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadClientList();
            }
        });
        myReuseEnvValueCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean allowSelection = !myReuseEnvValueCheckBox.isSelected();
                myRefreshClientList.setEnabled(allowSelection);
                myClientList.setEnabled(allowSelection);
            }
        });
        checkConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkConnection();
            }
        });

        myResolvePath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                refreshResolvedProperties();
            }
        });
        myRefreshResolved.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                refreshConfigPaths();
            }
        });
    }


    void initialize(@NotNull Project project) {
        this.myProject = project;
        myP4ConfigConnectionPanel.initialize(project);
    }


    public JPanel getPanel() {
        return myMainPanel;
    }

    public boolean isModified(@NotNull P4Config config) {
        if (config.hasClientnameSet()) {
            if (myReuseEnvValueCheckBox.isSelected()) {
                return true;
            }
            if (!Comparing.equal(config.getClientname(), myClientList.getSelectedItem())) {
                return true;
            }
        }

        if (config.isAutoOffline() != mySilentlyGoOfflineOnCheckBox.isSelected()) {
            return true;
        }

        if (config.isPasswordStoredLocally() != mySavePasswordsCheckBox.isSelected()) {
            return true;
        }

        Object selectedItem = myConnectionChoice.getSelectedItem();
        assert (selectedItem != null && (selectedItem instanceof ConnectionPanel));
        ConnectionPanel connection = (ConnectionPanel) selectedItem;
        if (config.getConnectionMethod() != connection.getConnectionMethod()) {
            return true;
        }
        return connection.isModified(config);
    }

    protected void loadSettingsIntoGUI(@NotNull P4Config config) {
        // ----------------------------------------------------------------
        // non-connection values


        initializeClientAndPathSelection(
                config.hasClientnameSet() ? config.getClientname() : null,
                config.getConnectionMethod());

        mySilentlyGoOfflineOnCheckBox.setSelected(config.isAutoOffline());
        mySavePasswordsCheckBox.setSelected(config.isPasswordStoredLocally());


        // ----------------------------------------------------------------
        // Dynamic setup for connection information
        for (int i = 0; i < myConnectionChoice.getItemCount(); i++) {
            ConnectionPanel conn = (ConnectionPanel) myConnectionChoice.getItemAt(i);
            if (conn.getConnectionMethod() == config.getConnectionMethod()) {
                showConnectionPanel(conn);
                conn.loadSettingsIntoGUI(config);
                myConnectionChoice.setSelectedIndex(i);
            }
        }

        //refreshResolvedProperties();
        resetResolvedProperties();
    }

    protected void saveSettingsToConfig(@NotNull ManualP4Config config) {
        // Clear out the connection settings so old ones don't interfere
        // with the new ones
        config.setUsername(null);
        config.setPort(null);
        config.setProtocol(null);
        config.setPassword(null);
        config.setAuthTicketPath(null);
        config.setConfigFile(null);
        config.setTrustTicketPath(null);

        ConnectionPanel conn = getSelectedConnection();
        conn.saveSettingsToConfig(config);
        config.setConnectionMethod(conn.getConnectionMethod());

        // ----------------------------------------------------------------
        // non-connection values - overwrite whatever the config panel set

        // Client name
        if (myReuseEnvValueCheckBox.isSelected()) {
            config.setClientname(null);
        } else {
            config.setClientname(getSelectedClient());
        }

        config.setAutoOffline(mySilentlyGoOfflineOnCheckBox.isSelected());
        config.setPasswordStoredLocally(mySavePasswordsCheckBox.isSelected());
    }


    @NotNull
    private ConnectionPanel getSelectedConnection() {
        Object val = myConnectionChoice.getSelectedItem();
        if (val != null && val instanceof ConnectionPanel) {
            return (ConnectionPanel) val;
        }
        throw new IllegalStateException(P4Bundle.message("error.config.invalid-selection"));
    }


    @Nullable
    private String getSelectedClient() {
        if (getSelectedConnection().getConnectionMethod().isRelativeToPath()) {
            // These connections can never declare a client
            return null;
        }
        Object selected = myClientList.getSelectedItem();
        String selectedClient = null;
        if (selected != null) {
            selectedClient = selected.toString();
            if (selectedClient.length() <= 0) {
                selectedClient = null;
            }
        }
        return selectedClient;
    }


    @Nullable
    private P4Config createConnectionConfig() throws IOException {
        ManualP4Config partial = new ManualP4Config();
        saveSettingsToConfig(partial);
        return P4ConfigUtil.loadCmdP4Config(partial);
    }


    // ---------------------------------------------------------
    // UI callbacks

    private void checkConnection() {
        try {
            final P4Config config = createConnectionConfig();
            List<String> clients = new UserClientsLoader(
                    myProject, config).loadClients();
            if (clients != null) {
                Messages.showMessageDialog(myProject,
                        P4Bundle.message("configuration.dialog.valid-connection.message"),
                        P4Bundle.message("configuration.dialog.valid-connection.title"),
                        Messages.getInformationIcon());
            }
        } catch (IOException e) {
            ErrorDialog.logError(myProject,
                    P4Bundle.message("configuration.check.io-error"),
                    e);
        }
    }

    private void loadClientList() {
        final List<String> clients;
        try {
            final P4Config config = createConnectionConfig();
            clients = new UserClientsLoader(
                    myProject, config).loadClients();
        } catch (IOException e) {
            ErrorDialog.logError(myProject,
                    P4Bundle.message("configuration.check.io-error"),
                    e);
            return;
        }
        if (clients == null) {
            // Don't need a status update or any updates; the user should have
            // seen error dialogs.
            return;
        }

        List<String> toAdd = new ArrayList<String>(clients.size() + 1);
        toAdd.addAll(clients);

        // Make sure to keep the currently selected item selected.
        // If it wasn't in the original list, it needs to be added
        // and have a custom renderer highlight it as ss invalid.
        // Also, move the currently selected one to the top.
        Object selected = myClientList.getSelectedItem();
        if (selected != null) {
            clients.remove(selected.toString());
            if (selected.toString().trim().length() > 0) {
                toAdd.add(0, selected.toString());
            }
        }

        myClientList.removeAllItems();
        for (String client : toAdd) {
            myClientList.addItem(client);
        }
        if (selected != null && selected.toString().trim().length() > 0) {
            myClientList.setSelectedItem(selected);
        }
    }

    private void changeConnectionSelection() {
        int currentSelectedIndex = myConnectionChoice.getSelectedIndex();
        ConnectionPanel selected = (ConnectionPanel) myConnectionChoice.getItemAt(currentSelectedIndex);
        showConnectionPanel(selected);
    }

    private void initializeClientAndPathSelection(@Nullable String currentClientName,
            @NotNull final ConnectionMethod connectionMethod) {
        if (connectionMethod.isRelativeToPath()) {
            myReuseEnvValueCheckBox.setSelected(true);
            myReuseEnvValueCheckBox.setEnabled(false);
            myClientList.setEnabled(false);
            myClientList.removeAllItems();
            myRefreshClientList.setEnabled(false);
            myResolvePathLabel.setEnabled(true);
            myResolvePath.setEnabled(true);

            refreshConfigPaths();
            resetResolvedProperties();
        } else {
            myReuseEnvValueCheckBox.setEnabled(true);
            myResolvePathLabel.setEnabled(false);
            myResolvePath.setEnabled(false);
            myClientList.removeAllItems();

            if (currentClientName != null) {
                myReuseEnvValueCheckBox.setSelected(false);
                myRefreshClientList.setEnabled(true);
                myClientList.setEnabled(true);

                // Currently selected client name needs to be added
                myClientList.addItem(currentClientName);
                // and select it
                myClientList.setSelectedItem(currentClientName);
            } else {
                myReuseEnvValueCheckBox.setSelected(true);
                myRefreshClientList.setEnabled(false);
                myClientList.setEnabled(false);
            }

            refreshResolvedProperties();
        }
    }


    private void showConnectionPanel(@NotNull ConnectionPanel panel) {
        myConnectionDescriptionLabel.setText("<html>" + panel.getDescription());
        ((CardLayout) myConnectionTypeContainerPanel.getLayout()).show(
                myConnectionTypeContainerPanel,
                panel.getConnectionMethod().name());

        initializeClientAndPathSelection(getSelectedClient(), panel.getConnectionMethod());
    }


    private void createUIComponents() {
        // Add custom component construction here.
        myP4ConfigConnectionPanel = new P4ConfigConnectionPanel();
    }


    /**
     * Refresh the list of config file paths.  This will indirectly invoke
     * the refreshResolvedProperties
     */
    private void refreshConfigPaths() {
        ProgressManager.getInstance()
                .run(new Backgroundable(myProject, P4Bundle.getString("configuration.configfiles.refresh"), true) {
                    @Override
                    public void run(@NotNull final ProgressIndicator indicator) {
                        final Map<String, P4Config> mapping = loadRelativeConfigPaths();
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                // load the drop-down list with the new values.
                                // Make sure to maintain the existing selected item.

                                Object current = myResolvePath.getSelectedItem();
                                myResolvePath.removeAllItems();
                                if (mapping != null && !mapping.isEmpty()) {
                                    boolean found = false;
                                    for (String s : mapping.keySet()) {
                                        if (s.equals(current)) {
                                            found = true;
                                        }
                                        myResolvePath.addItem(s);
                                    }
                                    if (found) {
                                        myResolvePath.setSelectedItem(current);
                                    } else {
                                        myResolvePath.setSelectedIndex(0);
                                    }
                                }

                                refreshResolvedProperties();
                            }
                        });
                    }
                });
    }


    /**
     * Refresh just the list of resolved properties.
     */
    private void refreshResolvedProperties() {
        final StringBuilder display = new StringBuilder();
        P4Config config = null;

        Object configPath = useRelativePathConfig()
                ? myResolvePath.getSelectedItem()
                : null;

        if (configPath != null) {
            LOG.info("using config path " + configPath);
            config = getConfigForRelativeConfigPath(configPath.toString());
        } else if (useRelativePathConfig()) {
            myResolvedValuesField.setText(P4Bundle.message("config.display.properties.no_path"));
            return;
        }
        if (config == null) {
            LOG.info("Using cmd style config loading");
            ManualP4Config manualConfig = new ManualP4Config();
            saveSettingsToConfig(manualConfig);
            try {
                config = P4ConfigUtil.loadCmdP4Config(manualConfig);
            } catch (IOException e) {
                display.append(P4Bundle.message("configuration.resolved.file-not-found",
                        manualConfig.getConfigFile()));
            }
        }


        final Map<String, String> props = P4ConfigUtil.getProperties(config);
        List<String> keys = new ArrayList<String>(props.keySet());
        Collections.sort(keys);
        boolean first = true;
        for (String key : keys) {
            if (first) {
                first = false;
            } else {
                display.append("\n");
            }
            String val = props.get(key);
            if (val == null) {
                val = P4Bundle.getString("config.display.key.no-value");
            }
            display.append(P4Bundle.message("config.display.property-line", key, val));
        }

        myResolvedValuesField.setText(display.toString());
    }


    private void resetResolvedProperties() {
        myResolvedValuesField.setText(P4Bundle.message("config.display.properties.refresh"));
    }

    // ----------------------------------------------------------------------------
    // Relative path methods


    private boolean useRelativePathConfig() {
        return useRelativePathConfig(getSelectedConnection());
    }


    private static boolean useRelativePathConfig(@NotNull ConnectionPanel conn) {
        return conn.getConnectionMethod().isRelativeToPath();
    }


    private void clearRelativeConfigPaths() {
        synchronized (relativeConfigPathMapSync) {
            relativeConfigPathMap = null;
        }
    }


    @Nullable
    private P4Config getConfigForRelativeConfigPath(String path) {
        synchronized (relativeConfigPathMapSync) {
            if (relativeConfigPathMap == null) {
                return null;
            }
            return relativeConfigPathMap.get(path);
        }
    }


    /**
     * Must be called in a background process
     */
    @Nullable
    private Map<String, P4Config> loadRelativeConfigPaths() {
        final ConnectionPanel connection = getSelectedConnection();
        // Should be connection.getConnectionMethod().isRelativeToPath(), but
        // we are reaching into the panel to grab its path.  This is at least a bit more
        // abstracted out.
        if (connection instanceof RelativeConfigConnectionPanel) {
            final String configFile = ((RelativeConfigConnectionPanel) connection).getConfigFileName();
            if (configFile != null && myProject != null) {
                final Map<VirtualFile, P4Config> allConfigs =
                        P4ConfigUtil.loadProjectP4Configs(myProject, configFile, true);

                Map<String, P4Config> configMap = new HashMap<String, P4Config>();
                for (Entry<VirtualFile, P4Config> entry : allConfigs.entrySet()) {
                    configMap.put(entry.getKey().getPath(), entry.getValue());
                }

                synchronized (relativeConfigPathMapSync) {
                    relativeConfigPathMap = new HashMap<String, P4Config>(configMap);
                }

                return configMap;
            }
        }
        LOG.info("No config file name set; not returning any configs");
        clearRelativeConfigPaths();
        return Collections.emptyMap();
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
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(11, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.connection-choice"));
        myMainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.clientname"));
        myMainPanel.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel1,
                new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myClientList = new JComboBox();
        myClientList.setEditable(true);
        panel2.add(myClientList,
                new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel3,
                new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myRefreshClientList = new JButton();
        myRefreshClientList.setHorizontalAlignment(0);
        this.$$$loadButtonText$$$(myRefreshClientList, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.choose-client-button"));
        panel3.add(myRefreshClientList,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myReuseEnvValueCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myReuseEnvValueCheckBox, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.clientname.inherit"));
        panel3.add(myReuseEnvValueCheckBox,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(10);
        label3.setHorizontalTextPosition(11);
        this.$$$loadLabelText$$$(label3,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.options"));
        myMainPanel.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myConnectionTypeContainerPanel = new JPanel();
        myConnectionTypeContainerPanel.setLayout(new CardLayout(0, 0));
        myMainPanel.add(myConnectionTypeContainerPanel,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConnectionTypeContainerPanel.add(myP4ConfigConnectionPanel.$$$getRootComponent$$$(), "P4CONFIG");
        myClientPasswordConnectionPanel = new ClientPasswordConnectionPanel();
        myConnectionTypeContainerPanel.add(myClientPasswordConnectionPanel.$$$getRootComponent$$$(), "CLIENT");
        authTicketConnectionPanel = new AuthTicketConnectionPanel();
        myConnectionTypeContainerPanel.add(authTicketConnectionPanel.$$$getRootComponent$$$(), "AUTH_TICKET");
        mySSOConnectionPanel = new SSOConnectionPanel();
        myConnectionTypeContainerPanel.add(mySSOConnectionPanel.$$$getRootComponent$$$(), "SSO");
        myEnvConnectionPanel = new EnvConnectionPanel();
        myConnectionTypeContainerPanel.add(myEnvConnectionPanel.$$$getRootComponent$$$(), "DEFAULT");
        myRelP4ConfigPanel = new RelP4ConfigConnectionPanel();
        myConnectionTypeContainerPanel.add(myRelP4ConfigPanel.$$$getRootComponent$$$(), "REL_P4CONFIG");
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        myMainPanel.add(panel4,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConnectionChoice = new JComboBox();
        myConnectionChoice.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.connection-choice.picker.tooltip"));
        panel4.add(myConnectionChoice);
        checkConnectionButton = new JButton();
        checkConnectionButton.setHorizontalAlignment(10);
        this.$$$loadButtonText$$$(checkConnectionButton, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.check-connection"));
        panel4.add(checkConnectionButton);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        myMainPanel.add(panel5,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConnectionDescriptionLabel = new JLabel();
        myConnectionDescriptionLabel.setText("");
        myConnectionDescriptionLabel.setVerticalAlignment(1);
        panel5.add(myConnectionDescriptionLabel, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel6,
                new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        mySavePasswordsCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(mySavePasswordsCheckBox,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("connection.password.save"));
        mySavePasswordsCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("config.panel.save_passwords.tooltip"));
        panel6.add(mySavePasswordsCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myPasswordWarning = new JLabel();
        myPasswordWarning.setForeground(new Color(-65536));
        this.$$$loadLabelText$$$(myPasswordWarning, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.password-warning"));
        panel6.add(myPasswordWarning,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        2, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel7,
                new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        mySilentlyGoOfflineOnCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(mySilentlyGoOfflineOnCheckBox,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.autoconnect"));
        mySilentlyGoOfflineOnCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.panel.silent.tooltip"));
        panel7.add(mySilentlyGoOfflineOnCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel8,
                new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myResolvePathLabel = new JLabel();
        this.$$$loadLabelText$$$(myResolvePathLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolved.path"));
        panel9.add(myResolvePathLabel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        myResolvePath = new JComboBox();
        myResolvePath.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolve.configfile.tooltip"));
        panel9.add(myResolvePath,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer3 = new Spacer();
        myMainPanel.add(spacer3,
                new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolved.title"));
        myMainPanel.add(label4, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        myMainPanel.add(scrollPane1,
                new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null));
        myResolvedValuesField = new JTextArea();
        myResolvedValuesField.setEditable(false);
        myResolvedValuesField.setFont(UIManager.getFont("TextArea.font"));
        myResolvedValuesField.setLineWrap(false);
        myResolvedValuesField.setRows(10);
        myResolvedValuesField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolved.tooltip"));
        myResolvedValuesField.setWrapStyleWord(false);
        scrollPane1.setViewportView(myResolvedValuesField);
        myRefreshResolved = new JButton();
        this.$$$loadButtonText$$$(myRefreshResolved, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolve.refresh"));
        myMainPanel.add(myRefreshResolved,
                new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(myConnectionChoice);
        label2.setLabelFor(myClientList);
        myResolvePathLabel.setLabelFor(myResolvedValuesField);
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
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
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
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }


    private class AuthenticationMethodRenderer extends ListCellRendererWrapper<ConnectionPanel> {
        @Override
        public void customize(JList list, ConnectionPanel value, int index, boolean isSelected, boolean hasFocus) {
            if (isSelected || hasFocus) {
                setBackground(UIUtil.getListSelectionBackground());
                final Color selectedForegroundColor = UIUtil.getListSelectionForeground();
                setForeground(selectedForegroundColor);
            } else {
                setBackground(UIUtil.getListBackground());
                final Color foregroundColor = UIUtil.getListForeground();
                setForeground(foregroundColor);
            }

            if (value == null) {
                setText("");
                setToolTipText("");
            } else {
                setText(value.getName());
                setToolTipText(P4Bundle.message("connection.choice"));
            }
        }
    }

}
