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
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.CollectionListModel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.ui.util.BackgroundAwtActionRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class ResolvedPropertiesPanel {
    private JPanel rootPanel;

    private JComboBox rootDirDropdownBox;
    private CollectionComboBoxModel/*<ConfigPath>*/ rootDirDropdownBoxModel; // JDK 1.6 doesn't have generic models

    private JButton refreshResolvedPropertiesButton;
    private JTextArea resolvedValuesText;
    private JScrollPane configProblemsPanel;

    private JList configProblemsList;
    private AsyncProcessIcon refreshResolvedPropertiesSpinner;
    private CollectionListModel/*<String>*/ configProblemsListModel; // JDK 1.6 doesn't have generic models

    private boolean problemsVisible = true;

    private P4ProjectConfig lastConfig;

    private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            refresh((P4ProjectConfig) evt.getNewValue());
        }
    };

    public ResolvedPropertiesPanel() {
        // Initialize GUI constant values
        $$$setupUI$$$();

        configProblemsListModel = new CollectionListModel/*<String>*/();
        configProblemsList.setModel(configProblemsListModel);

        rootDirDropdownBoxModel = new CollectionComboBoxModel/*<Object>*/();
        rootDirDropdownBox.setModel(rootDirDropdownBoxModel);
        rootDirDropdownBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSelectedConfig();
            }
        });

        refreshResolvedPropertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    @NotNull
    public PropertyChangeListener getPropertyChangeListener() {
        return propertyChangeListener;
    }

    public void refresh(@Nullable P4ProjectConfig config) {
        lastConfig = config;
        refresh();
    }


    // FIXME this needs to be in a background process.

    private void refresh() {
        BackgroundAwtActionRunner.runBackgroundAwtAction(refreshResolvedPropertiesSpinner,
                new BackgroundAwtActionRunner.BackgroundAwtAction<ComputedConfigResults>() {
                    @Override
                    public ComputedConfigResults runBackgroundProcess() {
                        lastConfig.refresh();

                        ComputedConfigResults results = new ComputedConfigResults();
                        Collection<ConfigProblem> problems = lastConfig.getConfigProblems();
                        results.problemMessages = new ArrayList<String>(problems.size());
                        for (ConfigProblem problem : problems) {
                            results.problemMessages.add(problem.getMessage());
                        }

                        Collection<ClientConfig> configs = lastConfig.getClientConfigs();
                        for (ClientConfig config : configs) {
                            results.configs.add(new ConfigPath(config));
                        }
                        return results;
                    }

                    @Override
                    public void runAwtProcess(ComputedConfigResults results) {
                        if (results.problemMessages.isEmpty()) {
                            configProblemsListModel.removeAll();
                            if (problemsVisible) {
                                problemsVisible = false;
                                configProblemsPanel.setVisible(false);
                                rootPanel.doLayout();
                            }
                        } else {
                            configProblemsListModel.replaceAll(results.problemMessages);
                            if (!problemsVisible) {
                                problemsVisible = true;
                                configProblemsPanel.setVisible(true);
                                rootPanel.doLayout();
                            }
                        }
                        if (results.configs.isEmpty()) {
                            rootDirDropdownBoxModel.removeAll();
                            rootDirDropdownBox.setEnabled(false);
                        } else {
                            rootDirDropdownBox.setEnabled(true);
                            rootDirDropdownBoxModel.replaceAll(results.configs);
                            rootDirDropdownBox.setSelectedIndex(0);
                        }

                        refreshSelectedConfig();
                    }
                });
    }


    // calld in Awt
    private void refreshSelectedConfig() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        final String text;
        if (rootDirDropdownBoxModel.isEmpty()) {
            text = P4Bundle.message("config.display.properties.no_path");
        } else {
            final Object selected = rootDirDropdownBoxModel.getSelected();
            if (selected == null || !(selected instanceof ConfigPath)) {
                text = P4Bundle.message("config.display.properties.no_path");
            } else {
                Map<String, String> props = ((ConfigPath) selected).config.toProperties();
                ArrayList<String> keys = new ArrayList<String>(props.keySet());
                Collections.sort(keys);
                StringBuilder sb = new StringBuilder();
                for (String key : keys) {
                    sb.append(key).append('=').append(props.get(key)).append('\n');
                }
                text = sb.toString();
            }
        }
        resolvedValuesText.setText(text);
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
        rootPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        rootDirDropdownBox = new JComboBox();
        panel1.add(rootDirDropdownBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolved.path"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        rootPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        refreshResolvedPropertiesButton = new JButton();
        this.$$$loadButtonText$$$(refreshResolvedPropertiesButton, ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolve.refresh"));
        refreshResolvedPropertiesButton.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.resolve.refresh.tooltip"));
        panel2.add(refreshResolvedPropertiesButton);
        panel2.add(refreshResolvedPropertiesSpinner);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setVerticalScrollBarPolicy(22);
        rootPanel.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        resolvedValuesText = new JTextArea();
        resolvedValuesText.setFont(UIManager.getFont("TextArea.font"));
        scrollPane1.setViewportView(resolvedValuesText);
        configProblemsPanel = new JScrollPane();
        configProblemsPanel.setVisible(false);
        rootPanel.add(configProblemsPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        configProblemsList = new JList();
        configProblemsPanel.setViewportView(configProblemsList);
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
                if (i == text.length()) break;
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
                if (i == text.length()) break;
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
        return rootPanel;
    }


    private static class ComputedConfigResults {
        ArrayList<String> problemMessages;
        ArrayList<ConfigPath> configs;
    }


    private static class ConfigPath {
        final ClientConfig config;

        private ConfigPath(ClientConfig config) {
            this.config = config;
        }

        @Override
        public String toString() {
            return config.getRootDir().getPath();
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


}
