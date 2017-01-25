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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.P4Config.ConnectionMethod;
import net.groboclown.idea.p4ic.config.P4ProjectConfigComponent;
import net.groboclown.idea.p4ic.ui.connection.AuthTicketConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.ClientPasswordConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.ConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.EnvConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.P4ConfigConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.RelP4ConfigConnectionPanel;
import net.groboclown.idea.p4ic.ui.connection.SSOConnectionPanel;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import net.groboclown.idea.p4ic.v2.ui.alerts.ConfigPanelErrorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class P4ConfigPanel {
    private static final Logger LOG = Logger.getInstance(P4ConfigPanel.class);

    private JPanel myMainPanel;
    private JComboBox/*<ConnectionPanel>*/ myConnectionChoice; // JDK 1.6 does not have generic combo box
    private JButton myRefreshClientList;
    private JComboBox/*<String>*/ myClientList; // JDK 1.6 does not have generic combo box
    private JCheckBox myReuseEnvValueCheckBox;
    private JButton myCheckConnectionButton;
    private JCheckBox mySilentlyGoOfflineOnCheckBox;
    private P4ConfigConnectionPanel myP4ConfigConnectionPanel;
    private ClientPasswordConnectionPanel myClientPasswordConnectionPanel;
    private AuthTicketConnectionPanel authTicketConnectionPanel;
    private SSOConnectionPanel mySSOConnectionPanel;
    private EnvConnectionPanel myEnvConnectionPanel;
    private JPanel myConnectionTypeContainerPanel;
    private JLabel myConnectionDescriptionLabel;
    private RelP4ConfigConnectionPanel myRelP4ConfigPanel;
    private JComboBox/*<String>*/ myResolvePath; // JDK 1.6 does not have generic combo box
    private JTextArea myResolvedValuesField;
    private JLabel myResolvePathLabel;
    private JButton myRefreshResolved;
    private AsyncProcessIcon myCheckConnectionSpinner;
    private AsyncProcessIcon myRefreshClientListSpinner;
    private AsyncProcessIcon myRefreshResolvedSpinner;

    private Project myProject;
    private AlertManager alertManager = AlertManager.getInstance();
    private final Set<String> activeProcesses = new HashSet<String>();
    private boolean initialized = false;

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
                refreshClientList();
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
        myCheckConnectionButton.addActionListener(new ActionListener() {
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

    public boolean isModified(@NotNull P4ProjectConfigComponent config) {
        /*
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

        //if (config.isPasswordStoredLocally() != mySavePasswordsCheckBox.isSelected()) {
        //    return true;
        //}

        Object selectedItem = myConnectionChoice.getSelectedItem();
        assert (selectedItem != null && (selectedItem instanceof ConnectionPanel));
        ConnectionPanel connection = (ConnectionPanel) selectedItem;
        if (config.getConnectionMethod() != connection.getConnectionMethod()) {
            return true;
        }
        return connection.isModified(config);
        */
        return false;
    }

    protected void loadSettingsIntoGUI(@NotNull P4ProjectConfigComponent config) {
        // ----------------------------------------------------------------
        // non-connection values
        /*
        if (!config.isConfigured()) {
            // Don't show a big nasty error message just because nothing is
            // configured right.

            LOG.info("Skipping setup because the configuration has nothing.");
            return;
        }

        initializeClientAndPathSelection(
                config.hasClientnameSet() ? config.getClientname() : null,
                config.getConnectionMethod());

        mySilentlyGoOfflineOnCheckBox.setSelected(config.isAutoOffline());
        //mySavePasswordsCheckBox.setSelected(config.isPasswordStoredLocally());


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

        resetResolvedProperties();
        initialized = true;
        refreshClientList();
        refreshConfigPaths();
        // refreshResolvedProperties();
        */
    }

    protected void saveSettingsToConfig(@NotNull P4ProjectConfigComponent config) {
        /*
        // Clear out the connection settings so old ones don't interfere
        // with the new ones
        config.setUsername(null);
        config.setPort(null);
        config.setProtocol(null);
        config.setPassword(null);
        config.setAuthTicketPath(null);
        config.setConfigFile(null);
        config.setTrustTicketPath(null);
        config.setServerFingerprint(null);
        config.setClientHostname(null);
        config.setClientname(null);
        config.setIgnoreFileName(null);

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
        //config.setPasswordStoredLocally(mySavePasswordsCheckBox.isSelected());
        */
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


    // ---------------------------------------------------------
    // Connection utilities


    /**
     * Reports a problem to the user if there is any error in a config,
     * and returns null.
     *
     * @return only the valid connections, or null if there are invalid ones.
     */
    // CalledInBackground
    @NotNull
    private ConfigSet getValidConfigs() {
        /*
        if (!initialized) {
            LOG.debug("called getValidConfigs before configs were loaded");
            return new ConfigSet();
        }

        final Collection<Builder> sources = createConnectionConfigs();
        final List<ProjectConfigSource> valid = new ArrayList<ProjectConfigSource>(sources.size());
        final List<Builder> invalid = new ArrayList<Builder>();
        for (Builder source : sources) {
            if (source.isInvalid()) {
                invalid.add(source);
            } else {
                try {
                    final ProjectConfigSource config = source.create();
                    valid.add(config);
                } catch (P4InvalidConfigException e) {
                    source.setError(e);
                    invalid.add(source);
                }
            }
        }
        return new ConfigSet(valid, invalid);
        */
        return new ConfigSet(Collections.<ProjectConfigSource>emptyList(), Collections.emptyList());
        //if (invalidConfigCount <= 0) {
        //    if (valid.isEmpty()) {
        //        alertManager.addCriticalError(new ConfigPanelErrorHandler(
        //                myProject,
        //                P4Bundle.message("configuration.error.title"),
        //                P4Bundle.message("configuration.error.no-config-defined")
        //        ), null);
        //        return null;
        //    }
        //    return valid;
        //}
        //alertManager.addCriticalError(new ConfigPanelErrorHandler(
        //        myProject,
        //        P4Bundle.message("configuration.error.title"),
        //        P4Bundle.message("configuration.error.problem-list",
        //                invalidConfigCount, invalidConfigs.toString())
        //), null);
        //return null;
        //reportProblems(Collections.<Exception>singleton(e));
    }


    // ---------------------------------------------------------
    // UI callbacks

    // CalledInAwt
    private void checkConnection() {
        /*
        runBackgroundAwtAction(myCheckConnectionSpinner,
                new BackgroundAwtAction<Collection<Builder>>() {
                    @Override
                    public Collection<Builder> runBackgroundProcess() {
                        final ConfigSet sources = getValidConfigs();
                        final List<Builder> problems = new ArrayList<Builder>();
                        problems.addAll(sources.invalid);
                        for (Entry<ProjectConfigSource, Exception> entry : ConnectionUIConfiguration
                                .findConnectionProblems(
                                        sources.valid, ServerConnectionManager.getInstance())
                                .entrySet()) {
                            final VcsException err;
                            //noinspection ThrowableResultOfMethodCallIgnored
                            if (entry.getValue() instanceof VcsException) {
                                err = (VcsException) entry.getValue();
                            } else {
                                err = new P4FileException(entry.getValue());
                            }
                            problems.add(entry.getKey().causedError(err));
                        }
                        return problems;
                    }

                    @Override
                    public void runAwtProcess(final Collection<Builder> problems) {
                        if (problems != null && problems.isEmpty()) {
                            Messages.showMessageDialog(myProject,
                                    P4Bundle.message("configuration.dialog.valid-connection.message"),
                                    P4Bundle.message("configuration.dialog.valid-connection.title"),
                                    Messages.getInformationIcon());
                        } else if (problems != null) {
                            reportConfigProblems(problems);
                        }
                    }
                });
        */
    }

    /**
     * Reloads the displayed list of clients.
     */
    // CalledInAwt
    private void refreshClientList() {
        final Object selected = myClientList.getSelectedItem();
        runBackgroundAwtAction(myRefreshClientListSpinner, new BackgroundAwtAction<List<String>>() {
            @Override
            public List<String> runBackgroundProcess() {
                return loadClientList(selected);
            }

            @Override
            public void runAwtProcess(final List<String> clientList) {
                if (clientList != null) {
                    myClientList.removeAllItems();
                    for (String client : clientList) {
                        myClientList.addItem(client);
                    }
                    if (!clientList.isEmpty()) {
                        myClientList.setSelectedIndex(0);
                    }
                }
                // else already handled the errors
            }
        });
    }

    // CalledInBackground
    private List<String> loadClientList(@Nullable Object selected) {
        /*
        final Map<ProjectConfigSource, ClientResult> clientResults =
                ConnectionUIConfiguration.getClients(getValidConfigs().valid,
                        ServerConnectionManager.getInstance());

        if (clientResults == null) {
            // Don't need a status update or any updates; the user should have
            // seen error dialogs.
            LOG.info("UserClientsLoader returned null");
            return null;
        }

        // Find errors and valid clients
        Set<String> toAdd = new HashSet<String>();
        List<Exception> problems = new ArrayList<Exception>();
        for (Entry<ProjectConfigSource, ClientResult> entry : clientResults.entrySet()) {
            if (entry.getValue().isInvalid()) {
                problems.add(entry.getValue().getConnectionProblem());
            } else {
                toAdd.addAll(entry.getValue().getClientNames());
            }
        }
        if (!problems.isEmpty()) {
            reportExceptions(problems);
            return null;
        }

        // Make sure to keep the currently selected item selected.
        // If it wasn't in the original list, it needs to be added
        // and have a custom renderer highlight it as ss invalid.
        // Also, move the currently selected one to the top.

        // TODO if the selected item isn't in the new client list,
        // then mark it as an error.

        List<String> orderedClients = new ArrayList<String>(toAdd);
        Collections.sort(orderedClients);
        if (selected != null) {
            if (orderedClients.remove(selected.toString()) && selected.toString().trim().length() > 0) {
                // in the new client list and it's a valid (not-empty) name.
                orderedClients.add(0, selected.toString());
            }
        }
        return orderedClients;
        */
        return null;
    }

    // CalledInAwt
    private void changeConnectionSelection() {
        int currentSelectedIndex = myConnectionChoice.getSelectedIndex();
        ConnectionPanel selected = (ConnectionPanel) myConnectionChoice.getItemAt(currentSelectedIndex);

        showConnectionPanel(selected);
    }

    // CalledInAwt
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

    // CalledInAwt
    private void showConnectionPanel(@NotNull ConnectionPanel panel) {
        myConnectionDescriptionLabel.setText("<html>" + panel.getDescription());
        ((CardLayout) myConnectionTypeContainerPanel.getLayout()).show(
                myConnectionTypeContainerPanel,
                panel.getConnectionMethod().name());

        initializeClientAndPathSelection(getSelectedClient(), panel.getConnectionMethod());
    }


    /**
     * Refresh the list of config file paths.  This will indirectly invoke
     * the refreshResolvedProperties
     */
    // CalledInAwt
    private void refreshConfigPaths() {
        runBackgroundAwtAction(myRefreshClientListSpinner, new BackgroundAwtAction<ConfigSet>() {
            @Override
            public ConfigSet runBackgroundProcess() {
                return getValidConfigs();
            }

            @Override
            public void runAwtProcess(final ConfigSet sources) {
                // load the drop-down list with the new values.
                // Make sure to maintain the existing selected item.
                Object current = myResolvePath.getSelectedItem();
                myResolvePath.removeAllItems();
                if (sources != null && !sources.valid.isEmpty()) {
                    boolean found = false;
                    for (ProjectConfigSource source : sources.valid) {
                        for (VirtualFile dir : source.getProjectSourceDirs()) {
                            if (dir.getPath().equals(current)) {
                                found = true;
                            }
                            myResolvePath.addItem(dir);
                        }
                    }
                    if (found) {
                        myResolvePath.setSelectedItem(current);
                    } else if (myResolvePath.getItemCount() > 0) {
                        myResolvePath.setSelectedIndex(0);
                    }
                }

                refreshResolvedProperties();
            }
        });
    }


    /**
     * Refresh just the list of resolved properties.
     */
    // CalledInAwt
    private void refreshResolvedProperties() {
        // This is actually invoked when the config directory drop-down is changed,
        // or when the connection is changed, but this spinner is fine.

        final Object selectedItem;
        if (myResolvePath.getItemCount() <= 0) {
            selectedItem = null;
        } else if (myResolvePath.getSelectedItem() == null) {
            selectedItem = myResolvePath.getItemAt(0);
        } else {
            selectedItem = myResolvePath.getSelectedItem();
        }

        runBackgroundAwtAction(myRefreshResolvedSpinner, new BackgroundAwtAction<StringBuilder>() {
            @Override
            public StringBuilder runBackgroundProcess() {
                // Find the source corresponding to the selected item
                final StringBuilder displayText = new StringBuilder();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("creating connection configs");
                }
                /*
                final Collection<Builder> sources = createConnectionConfigs();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" - found sources " + sources);
                }
                Builder selectedSource = null;
                if (sources.isEmpty()) {
                    selectedSource = null;
                } else if (sources.size() == 1 || selectedItem == null) {
                    selectedSource = sources.iterator().next();
                } else {
                    sourceLoop:
                    for (Builder source : sources) {
                        if (source.isInvalid()) {
                            // skip
                        } else {
                            for (VirtualFile file : source.getDirs()) {
                                if (file.equals(selectedItem)) {
                                    selectedSource = source;
                                    break sourceLoop;
                                }
                            }
                        }
                    }
                }


                if (selectedSource != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(" - selected source is " + selectedSource);
                    }
                    createSourceDisplay(displayText, selectedSource);
                } else {
                    // if there were config problems, report them.
                    sourceLoop:
                    for (Builder source : sources) {
                        if (source.isInvalid()) {
                            for (VirtualFile file : source.getDirs()) {
                                if (file.getPath().equals(selectedItem)) {
                                    selectedSource = source;
                                    break sourceLoop;
                                }
                            }
                        }
                    }
                    if (selectedSource != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(" - selected source is " + selectedSource);
                        }
                        createSourceDisplay(displayText, selectedSource);
                    } else {
                        LOG.info("No selected source, and no sources found");
                        displayText.append(P4Bundle.message("config.display.properties.no_path"));
                    }
                }
                */
                return displayText;
            }


            @Override
            public void runAwtProcess(final StringBuilder displayText) {
                myResolvedValuesField.setText(displayText.toString());
            }
        });
    }


    @NotNull
    private StringBuilder createSourceDisplay(@NotNull StringBuilder display,
                                              @Nullable Void builder) {
        return display;
        /*
        if (builder.isInvalid()) {
            if (builder.getError() != null) {
                display.append(P4Bundle.message("config.display.invalid.reason", builder.getError().getMessage())).
                        append("\n");
            } else {
                display.append(P4Bundle.message("config.display.invalid")).append("\n");
            }
        }
        final Map<String, String> props = P4ConfigUtil.getProperties(builder.getBaseConfig());
        List<String> keys = new ArrayList<String>(props.keySet());
        Collections.sort(keys);
        String sep = "";
        for (String key : keys) {
            display.append(sep);
            String val = props.get(key);
            if (val == null) {
                val = P4Bundle.getString("config.display.key.no-value");
            }
            display.append(P4Bundle.message("config.display.property-line", key, val));
            sep = "\n";
        }
        return display;
        */
    }


    private void resetResolvedProperties() {
        myResolvedValuesField.setText(P4Bundle.message("config.display.properties.refresh"));
    }


    private void reportConfigProblems(@NotNull Collection<?> problems) {
        // TODO use better UI element
        // FIXME localize
        /*
        String message = "";
        String sep = "";
        for (Builder builder : problems) {
            //noinspection ThrowableResultOfMethodCallIgnored
            message += sep + builder.getPresentableName() +
                    ": " + (builder.getError() == null
                    ? "(unknown)"
                    : builder.getError().getMessage());
            sep = "\n";
            LOG.info("Config problem: " + builder, builder.getError());
        }
        alertManager.addCriticalError(new ConfigPanelErrorHandler(
                        myProject,
                        P4Bundle.message("configuration.error.title"),
                        message),
                null);
        */
    }

    private void reportExceptions(@NotNull Collection<Exception> problems) {
        // TODO use better UI element
        String message = "";
        String sep = "";
        for (Exception ex : problems) {
            //noinspection ThrowableResultOfMethodCallIgnored
            message += sep + ex.getMessage();
            sep = "\n";
            LOG.info("Config problem", ex);
        }
        alertManager.addCriticalError(new ConfigPanelErrorHandler(
                        myProject,
                        P4Bundle.message("configuration.error.title"),
                        message),
                null);
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
        myMainPanel.setLayout(new GridLayoutManager(9, 3, new Insets(0, 0, 0, 0), -1, -1));
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
                new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        myMainPanel.add(panel3,
                new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myRefreshClientList = new JButton();
        myRefreshClientList.setHorizontalAlignment(0);
        this.$$$loadButtonText$$$(myRefreshClientList, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.choose-client-button"));
        panel3.add(myRefreshClientList);
        panel3.add(myRefreshClientListSpinner);
        myReuseEnvValueCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myReuseEnvValueCheckBox, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.clientname.inherit"));
        panel3.add(myReuseEnvValueCheckBox);
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
                new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
                new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConnectionChoice = new JComboBox();
        myConnectionChoice.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.connection-choice.picker.tooltip"));
        panel4.add(myConnectionChoice);
        myCheckConnectionButton = new JButton();
        myCheckConnectionButton.setHorizontalAlignment(10);
        this.$$$loadButtonText$$$(myCheckConnectionButton, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.check-connection.button"));
        panel4.add(myCheckConnectionButton);
        panel4.add(myCheckConnectionSpinner);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        myMainPanel.add(panel5,
                new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myConnectionDescriptionLabel = new JLabel();
        myConnectionDescriptionLabel.setText("");
        myConnectionDescriptionLabel.setVerticalAlignment(1);
        panel5.add(myConnectionDescriptionLabel, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel6,
                new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        mySilentlyGoOfflineOnCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(mySilentlyGoOfflineOnCheckBox,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.autoconnect"));
        mySilentlyGoOfflineOnCheckBox.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.panel.silent.tooltip"));
        panel6.add(mySilentlyGoOfflineOnCheckBox,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        myMainPanel.add(panel7,
                new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        myResolvePathLabel = new JLabel();
        this.$$$loadLabelText$$$(myResolvePathLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolved.path"));
        panel8.add(myResolvePathLabel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        myResolvePath = new JComboBox();
        myResolvePath.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolve.configfile.tooltip"));
        panel8.add(myResolvePath,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolved.title"));
        myMainPanel.add(label4, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        myMainPanel.add(scrollPane1,
                new GridConstraints(8, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        myMainPanel.add(panel9, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myRefreshResolved = new JButton();
        this.$$$loadButtonText$$$(myRefreshResolved, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolve.refresh"));
        panel9.add(myRefreshResolved);
        panel9.add(myRefreshResolvedSpinner);
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


    private static class ConfigSet {
        final Collection<ProjectConfigSource> valid;
        final Collection<?> invalid;

        ConfigSet() {
            this.valid = Collections.emptyList();
            this.invalid = Collections.emptyList();
        }

        ConfigSet(@NotNull Collection<ProjectConfigSource> valid, @NotNull Collection<?> invalid) {
            this.valid = Collections.unmodifiableCollection(valid);
            this.invalid = Collections.unmodifiableCollection(invalid);
        }
    }


    private interface BackgroundAwtAction<T> {
        T runBackgroundProcess();

        void runAwtProcess(T value);
    }


    // CalledInAwt
    private <T> void runBackgroundAwtAction(@NotNull final AsyncProcessIcon icon,
                                            @NotNull final BackgroundAwtAction<T> action) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested background action " + icon.getName());
        }
        synchronized (activeProcesses) {
            if (activeProcesses.contains(icon.getName())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" - process is already running in background (active: " + activeProcesses + ")");
                }
                return;
            }
            activeProcesses.add(icon.getName());
        }
        icon.resume();
        icon.setVisible(true);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Running " + icon.getName() + " in background ");
                }
                T tmpValue;
                Exception tmpFailure;
                try {
                    tmpValue = action.runBackgroundProcess();
                    tmpFailure = null;
                } catch (Exception e) {
                    LOG.error("Background processing for " + icon.getName() + " failed", e);
                    tmpValue = null;
                    tmpFailure = e;
                } finally {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Background processing for " + icon
                                .getName() + " completed.  Queueing AWT processing.");
                    }
                }
                final T value = tmpValue;
                final Exception failure = tmpFailure;

                // NOTE: ApplicationManager.getApplication().invokeLater
                // will not work, because it will wait until the UI dialog
                // goes away, which means we can't see the results until
                // the UI element goes away, which is just backwards.

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Running " + icon.getName() + " in AWT");
                        }
                        try {
                            if (failure == null) {
                                action.runAwtProcess(value);
                            }
                        } finally {
                            icon.suspend();
                            icon.setVisible(false);
                            synchronized (activeProcesses) {
                                activeProcesses.remove(icon.getName());
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Remaining background processes active: " + activeProcesses);
                                }
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("AWT processing for " + icon.getName() + " completed");
                            }
                        }
                    }
                });
            }
        });
    }


    // -----------------------------------------------------------------------
    // UI form stuff

    private void createUIComponents() {
        // Add custom component construction here.
        myP4ConfigConnectionPanel = new P4ConfigConnectionPanel();

        myCheckConnectionSpinner = new AsyncProcessIcon("Check Connection Progress");
        myCheckConnectionSpinner.setName("Check Connection Progress");
        myCheckConnectionSpinner.setVisible(false);

        myRefreshClientListSpinner = new AsyncProcessIcon("Refresh Client List Progress");
        myRefreshClientListSpinner.setName("Refresh Client List Progress");
        myRefreshClientListSpinner.setVisible(false);

        myRefreshResolvedSpinner = new AsyncProcessIcon("Refresh Resolved Progress");
        myRefreshResolvedSpinner.setName("Refresh Resolved Progress");
        myRefreshResolvedSpinner.setVisible(false);
    }


    private static class AuthenticationMethodRenderer
            extends ListCellRendererWrapper<ConnectionPanel> {
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
