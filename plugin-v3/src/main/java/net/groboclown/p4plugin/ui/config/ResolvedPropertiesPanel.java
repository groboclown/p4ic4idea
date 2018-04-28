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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.BackgroundAwtActionRunner;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ClientConfigSetup;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.part.DataPart;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.config.props.ConfigurationUpdatedListener;
import net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectionManager;
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
import java.util.Map;
import java.util.ResourceBundle;

public class ResolvedPropertiesPanel {
    private static final Logger LOG = Logger.getInstance(ResolvedPropertiesPanel.class);

    private static final ConfigProblem PROBLEM_ELSEWHERE = new ConfigProblem(null, true, "");

    @Nullable
    private Project project;

    private JPanel rootPanel;

    private JComboBox rootDirDropdownBox;
    // FIXME 2017.1
    private DefaultComboBoxModel/*<ConfigPath>*/ rootDirDropdownBoxModel; // JDK 1.6 doesn't have generic models

    private JButton refreshResolvedPropertiesButton;
    private JTextArea resolvedValuesText;

    private AsyncProcessIcon refreshResolvedPropertiesSpinner;
    private JPanel myProblemsPanel;
    private JList/*<RootedConfigProblem>*/ selectedProblemsList;
    private CollectionListModel/*<RootedConfigProblem>*/ selectedProblemsListModel;
    // JDK 1.6 doesn't have generic models

    private P4ProjectConfig lastConfig;

    private RequestConfigurationLoadListener requestConfigurationLoadListener;

    private final ConfigurationUpdatedListener configurationUpdatedListener = new ConfigurationUpdatedListener() {
        @Override
        public void onConfigurationUpdated(@NotNull P4ProjectConfig config) {
            // The user may not want to immediately refresh the connection information
            // upon an update.  If the state is bad, this will make the user bombarded
            // with "cannot connect" errors at every change.
            LOG.debug("Skipping refresh of the resolved properties panel.");
            // refresh(config);
            // Rather than calling refresh, we'll just cache the passed-in config.
            lastConfig = config;
        }
    };

    ResolvedPropertiesPanel() {
        // Initialize GUI constant values
        $$$setupUI$$$();

        LOG.debug("Completed UI setup, now setting listeners and models.");

        selectedProblemsListModel = new CollectionListModel/*<String>*/();
        selectedProblemsList.setModel(selectedProblemsListModel);
        selectedProblemsList.setCellRenderer(new ProblemListRenderer());

        rootDirDropdownBoxModel = new DefaultComboBoxModel/*<ConfigPath>*/();
        rootDirDropdownBox.setModel(rootDirDropdownBoxModel);
        rootDirDropdownBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSelectedConfig();
            }
        });

        refreshResolvedPropertiesButton.setIcon(AllIcons.Actions.Refresh);
        refreshResolvedPropertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh(null);
            }
        });
        LOG.debug("Completed setup");
    }

    void initialize(@Nullable Project project) {
        if (project != null) {
            this.project = project;
        }
    }

    @NotNull
    ConfigurationUpdatedListener getConfigurationUpdatedListener() {
        return configurationUpdatedListener;
    }

    public void refresh(@Nullable final P4ProjectConfig config) {
        BackgroundAwtActionRunner.runBackgroundAwtAction(
                refreshResolvedPropertiesSpinner,
                refreshResolvedPropertiesButton,
                new BackgroundAwtActionRunner.BackgroundAwtAction<ComputedConfigResults>() {
                    @Override
                    public ComputedConfigResults runBackgroundProcess() {
                        if (requestConfigurationLoadListener != null && config == null) {
                            lastConfig = requestConfigurationLoadListener.updateConfigPartFromUI();
                        } else {
                            lastConfig = config;
                        }
                        if (config != null) {
                            initialize(config.getProject());
                        }
                        if (lastConfig == null) {
                            final ComputedConfigResults results = new ComputedConfigResults();
                            if (project != null) {
                                results.configs.add(new ConfigPath(null, project.getBaseDir(),
                                        Collections.singletonList(createNoClientConfigProblem())));
                            }
                            return results;
                        }

                        LOG.debug("Refreshing last configuration");

                        lastConfig.refresh();

                        final ComputedConfigResults results = loadConfigResults(lastConfig);

                        final Object selected = rootDirDropdownBox.getSelectedItem();
                        boolean found = false;
                        for (int i = 0; selected != null && i < results.configs.size(); i++) {
                            if (selected == results.configs.get(i)) {
                                results.selectedConfigIndex = i;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            if (results.configs.isEmpty()) {
                                results.selectedConfigIndex = -1;
                            } else {
                                results.selectedConfigIndex = 0;
                                found = true;
                            }
                        }
                        if (found) {
                            ConfigPath selectedPath = results.configs.get(results.selectedConfigIndex);
                            results.selectedConfigText = createResolvedPropertiesText(
                                    selectedPath).toString();
                            results.selectedProblemText = toProblemMessages(selectedPath.file,
                                    selectedPath.allProblems);
                        } else {
                            results.selectedConfigText = null;
                        }

                        return results;
                    }

                    @Override
                    public void runAwtProcess(ComputedConfigResults results) {
                        if (results.configs == null || results.configs.isEmpty()) {
                            rootDirDropdownBoxModel.removeAllElements();
                            rootDirDropdownBox.setEnabled(false);
                            LOG.debug("No configurations; showing no resolved properties.");
                            showResolvedProperties(null, results.selectedProblemText);
                        } else {
                            rootDirDropdownBox.setEnabled(true);
                            rootDirDropdownBoxModel.removeAllElements();
                            for (ConfigPath config : results.configs) {
                                rootDirDropdownBoxModel.addElement(config);
                            }
                            if (results.selectedConfigIndex < 0) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Selected dropdown path in model is null or invalid: "
                                            + results.selectedConfigIndex);
                                }
                                results.selectedConfigText = null;
                            } else {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Showing dropdown path on reload for " + results.selectedConfigIndex);
                                }
                                rootDirDropdownBox.setSelectedIndex(results.selectedConfigIndex);
                            }
                            showResolvedProperties(results.selectedConfigText,
                                    results.selectedProblemText);
                        }
                    }
                });
    }

    // called in Awt
    private void refreshSelectedConfig() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (rootDirDropdownBoxModel.getSize() <= 0) {
            LOG.debug("No items in dropdown path box; showing no properties.");
            showResolvedProperties(null, null);
        } else {
            Object selected = rootDirDropdownBoxModel.getSelectedItem();
            if (selected == null || !(selected instanceof ConfigPath)) {
                rootDirDropdownBox.setSelectedIndex(0);
                selected = rootDirDropdownBoxModel.getElementAt(0);
                if (selected == null || !(selected instanceof ConfigPath)) {
                    throw new IllegalStateException("Resolved properties selected item is not ConfigPath: " + selected);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Showing dropdown path for selected " + selected);
            }
            showResolvedPropertiesText((ConfigPath) selected);
        }
    }

    private void showResolvedProperties(String configText, List<ConfigProblem> problemText) {
        LOG.debug("Showing resolved properties");
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (configText == null || configText.isEmpty()) {
            configText = P4Bundle.message("config.display.properties.no_path");
        }
        resolvedValuesText.setText(configText);

        if (problemText == null || problemText.isEmpty()) {
            selectedProblemsListModel.removeAll();
            if (myProblemsPanel.isVisible()) {
                myProblemsPanel.setVisible(false);
                rootPanel.doLayout();
            }
        } else {
            selectedProblemsListModel.replaceAll(problemText);
            if (!myProblemsPanel.isVisible()) {
                myProblemsPanel.setVisible(true);
                rootPanel.doLayout();
            }
        }
    }

    private void showResolvedPropertiesText(@NotNull final ConfigPath selected) {
        // This can load values from a file, so put in the background.
        BackgroundAwtActionRunner.runBackgroundAwtAction(
                refreshResolvedPropertiesSpinner,
                refreshResolvedPropertiesButton,
                new BackgroundAwtActionRunner.BackgroundAwtAction<ComputedConfigResults>() {
                    @Override
                    public ComputedConfigResults runBackgroundProcess() {
                        ComputedConfigResults ret = new ComputedConfigResults();
                        ret.selectedConfigText = createResolvedPropertiesText(selected).toString();
                        ret.selectedProblemText = toProblemMessages(selected.file, selected.allProblems);
                        return ret;
                    }

                    @Override
                    public void runAwtProcess(ComputedConfigResults value) {
                        showResolvedProperties(value.selectedConfigText, value.selectedProblemText);
                    }
                });
    }

    private StringBuilder createResolvedPropertiesText(@NotNull final ConfigPath selected) {
        if (selected.config == null) {
            return new StringBuilder();
        }
        Map<String, String> props = ConfigPropertiesUtil.toProperties(selected.config);
        List<String> keys = new ArrayList<String>(props.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append('=').append(props.get(key)).append('\n');
        }
        return sb;
    }


    void setRequestConfigurationLoadListener(
            @NotNull RequestConfigurationLoadListener requestConfigurationLoadListener) {
        this.requestConfigurationLoadListener = requestConfigurationLoadListener;
    }

    private ComputedConfigResults loadConfigResults(@NotNull final P4ProjectConfig projectConfig) {
        ComputedConfigResults results = new ComputedConfigResults();
        List<ConfigProblem> problemMessages = new ArrayList<ConfigProblem>(projectConfig.getConfigProblems());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Base project config problems: " + problemMessages);
        }

        Collection<ClientConfigSetup> configs = projectConfig.getClientConfigSetups();
        if (configs.isEmpty()) {
            LOG.debug("No client configs in setup.");
            problemMessages.add(createNoClientConfigProblem());
            results.configs.add(new ConfigPath(null, projectConfig.getProject().getBaseDir(), problemMessages));
        }
        for (ClientConfigSetup configSetup : configs) {
            final ClientConfig config = configSetup.getClientConfig();
            // Don't try a connection if there are flagrant errors in the setup.
            if (!configSetup.hasErrors() && config != null) {
                ConfigProblem problem = ConnectionUIConfiguration.checkConnection(config,
                        ServerConnectionManager.getInstance(), false);
                if (problem != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Config setup " + configSetup.getSource() + " has problem "
                                + problem);
                    }
                    problemMessages.add(problem);
                }
                // We can have a connection without a client, for testing purposes,
                // but to actually use it, we need a client.
                if (!config.isWorkspaceCapable()) {
                    problemMessages.add(new ConfigProblem(configSetup.getSource(), false,
                            "error.config.no-client"));
                }
            }
            if (config != null && config.getProjectSourceDirs().isEmpty()) {
                problemMessages.add(new ConfigProblem(configSetup.getSource(), false,
                        "client.root.non-existent", config.getProject().getBaseDir()));
                if (config.getProject().getBaseDir() != null) {
                    results.configs.add(new ConfigPath(
                            configSetup.getSource(), config.getProject().getBaseDir(), problemMessages));
                }
            }
            Collection<VirtualFile> sourceDirs = config == null
                    ? Collections.singleton(configSetup.getSource().getRootPath())
                    : config.getProjectSourceDirs();
            for (VirtualFile virtualFile : sourceDirs) {
                if (virtualFile != null) {
                    results.configs.add(new ConfigPath(configSetup.getSource(), virtualFile, problemMessages));
                }
            }
        }
        return results;
    }

    private static ConfigProblem createNoClientConfigProblem() {
        return new ConfigProblem(null,
                new P4InvalidConfigException(P4Bundle.getString("configuration.error.no-config-list")));
    }

    private static List<ConfigProblem> toProblemMessages(
            @Nullable VirtualFile root, @NotNull List<ConfigProblem> problemMessages) {
        List<ConfigProblem> ret = new ArrayList<ConfigProblem>(problemMessages.size());
        for (ConfigProblem problem : problemMessages) {
            if ((problem.getRootPath() == null || problem.getRootPath().equals(root))
                    && !containsDuplicate(problem, ret)) {
                ret.add(problem);
            }
        }
        if (ret.isEmpty() && !problemMessages.isEmpty()) {
            // Tell the user that there are problems on other root directories.
            ret.add(PROBLEM_ELSEWHERE);
        }
        return ret;
    }

    private static boolean containsDuplicate(ConfigProblem c, List<ConfigProblem> problems) {
        for (ConfigProblem problem : problems) {
            if (problem.isSameMessage(c)) {
                return true;
            }
        }
        return false;
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
        rootPanel.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        rootPanel.add(panel1, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.EAST);
        panel2.add(refreshResolvedPropertiesSpinner);
        refreshResolvedPropertiesButton = new JButton();
        refreshResolvedPropertiesButton.setText("");
        refreshResolvedPropertiesButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.resolve.refresh.tooltip"));
        panel2.add(refreshResolvedPropertiesButton);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, BorderLayout.CENTER);
        rootDirDropdownBox = new JComboBox();
        panel3.add(rootDirDropdownBox,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolved.path"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FormLayout("fill:d:grow",
                "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow"));
        rootPanel.add(panel4, BorderLayout.CENTER);
        myProblemsPanel = new JPanel();
        myProblemsPanel.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        CellConstraints cc = new CellConstraints();
        panel4.add(myProblemsPanel, cc.xy(1, 1));
        myProblemsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("config.resolved.problem-panel")));
        selectedProblemsList = new JList();
        myProblemsPanel.add(selectedProblemsList, cc.xy(1, 1));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel4.add(panel5, cc.xy(1, 3));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("config.resolved.config-panel")));
        resolvedValuesText = new JTextArea();
        panel5.add(resolvedValuesText, cc.xy(1, 1));
        final Spacer spacer1 = new Spacer();
        panel4.add(spacer1, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.FILL));
        label1.setLabelFor(rootDirDropdownBox);
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


    private static class ComputedConfigResults {
        List<ConfigPath> configs = new ArrayList<ConfigPath>();
        int selectedConfigIndex;
        String selectedConfigText;
        List<ConfigProblem> selectedProblemText;
    }


    private static class ConfigPath {
        @Nullable final DataPart config;
        private final List<ConfigProblem> allProblems;
        final VirtualFile file;

        private ConfigPath(@Nullable DataPart config, @NotNull VirtualFile virtualFile,
                @NotNull List<ConfigProblem> problems) {
            this.config = config;
            this.file = virtualFile;
            this.allProblems = problems;
        }

        @Override
        public String toString() {
            return file.getPath();
        }
    }


    private static class ProblemListRenderer
            implements ListCellRenderer/*<ConfigProblem>*/ {
        private final JTextArea cell;
        private final Font normal;
        private final Font italic;

        private ProblemListRenderer() {
            this.cell = new JTextArea();
            cell.setLineWrap(true);
            cell.setWrapStyleWord(true);
            this.normal = cell.getFont();
            this.italic = normal.deriveFont(Font.ITALIC);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (value == PROBLEM_ELSEWHERE) {
                cell.setFont(italic);
                cell.setForeground(UIUtil.getTextAreaForeground());
                cell.setText(P4Bundle.getString("config.resolve.other-roots-have-errors"));
            } else if (value instanceof ConfigProblem) {
                ConfigProblem problem = (ConfigProblem) value;
                cell.setFont(normal);
                cell.setForeground(problem.isError()
                        ? JBColor.RED
                        : UIUtil.getTextAreaForeground());
                cell.setText(problem.getMessage());
            } else {
                cell.setFont(normal);
                cell.setForeground(UIUtil.getTextAreaForeground());
            }
            return cell;
        }
    }

    // -----------------------------------------------------------------------
    // UI form stuff

    private void createUIComponents() {
        // Add custom component construction here.
        refreshResolvedPropertiesSpinner = new AsyncProcessIcon("Refresh Resolved Progress");
        refreshResolvedPropertiesSpinner.setName("Refresh Resolved Progress");
        refreshResolvedPropertiesSpinner.setVisible(false);
    }


    /**
     * @noinspection ALL
     */
    private Font getFont1494608681498(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) {
            return null;
        }
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(),
                size >= 0 ? size : currentFont.getSize());
    }

}
