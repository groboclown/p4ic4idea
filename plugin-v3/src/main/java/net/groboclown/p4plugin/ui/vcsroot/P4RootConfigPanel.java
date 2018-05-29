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

package net.groboclown.p4plugin.ui.vcsroot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.ConfigPropertiesUtil;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4plugin.util.PartValidation;
import net.groboclown.p4plugin.P4Bundle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class P4RootConfigPanel {
    private static final Icon ERROR_NOTICE = AllIcons.General.Error;
    private static final Icon WARNING_NOTICE = AllIcons.General.Warning;

    private final VirtualFile vcsRoot;
    private final ConfigConnectionController configConnectionController;
    private JPanel rootPanel;
    private JButton myCheckConnectionButton;
    private JPanel myProblemsPanel;
    private JTextPane myResolvedProperties;
    private JList<ConfigProblem> myProblemsList;
    private DefaultListModel<ConfigProblem> problemListModel;
    private ConfigPartStack myConfigPartStack;
    private AsyncProcessIcon myCheckConnectionSpinner;
    private JLabel myDetailsTitle;
    private JTabbedPane myTabbedPane;
    private JPanel myResolvedPropertyPanel;
    private JPanel myConfigPanel;


    P4RootConfigPanel(VirtualFile vcsRoot,
            ConfigConnectionController configConnectionController) {
        this.vcsRoot = vcsRoot;
        this.configConnectionController = configConnectionController;

        $$$setupUI$$$();

        myDetailsTitle.setText("");
        myCheckConnectionSpinner.suspend();
        myCheckConnectionSpinner.setVisible(false);
        configConnectionController.addConfigConnectionListener(this::updateConfigPanel);
        myCheckConnectionButton.addActionListener((e) -> {
            myCheckConnectionSpinner.resume();
            myCheckConnectionSpinner.setVisible(true);
            myCheckConnectionButton.setEnabled(false);
            myCheckConnectionSpinner.getParent().revalidate();
            myCheckConnectionSpinner.getParent().doLayout();
            myCheckConnectionSpinner.getParent().repaint();
            configConnectionController.refreshConfigConnection();
        });
    }

    JPanel getRootPane() {
        return rootPanel;
    }

    List<ConfigPart> getConfigParts() {
        return myConfigPartStack.getParts();
    }

    void setConfigParts(List<ConfigPart> parts) {
        myConfigPartStack.setParts(parts);
    }

    boolean isModified(List<ConfigPart> configParts) {
        return myConfigPartStack.isModified(configParts);
    }

    private void updateConfigPanel(Project project, ConfigPart part,
            ClientConfig clientConfig, ServerConfig serverConfig) {
        myCheckConnectionSpinner.suspend();
        myCheckConnectionSpinner.setVisible(false);
        myCheckConnectionButton.setEnabled(true);

        myResolvedProperties.setText(getResolvedProperties(part));
        myTabbedPane.setEnabledAt(2, true);

        int errorCount = 0;
        int warningCount = 0;
        problemListModel.clear();
        Set<String> problemText = new HashSet<>();
        for (ConfigProblem configProblem : PartValidation.findAllProblems(part)) {
            if (!problemText.contains(configProblem.getMessage())) {
                problemText.add(configProblem.getMessage());
                problemListModel.addElement(configProblem);
                if (configProblem.isError()) {
                    errorCount++;
                } else {
                    warningCount++;
                }
            }
        }

        myProblemsList.revalidate();
        myProblemsList.doLayout();

        if (problemListModel.isEmpty()) {
            if (myTabbedPane.getSelectedIndex() == 1) {
                myTabbedPane.setSelectedIndex(0);
            }
            myTabbedPane.setEnabledAt(1, false);
            myDetailsTitle.setVisible(false);
        } else {
            myTabbedPane.setEnabledAt(1, true);
            myTabbedPane.setSelectedIndex(1);
            myDetailsTitle.setVisible(true);
            myDetailsTitle.setText(P4Bundle.message("configuration.stack.wrapper.toggle.title",
                    errorCount, warningCount));
            if (errorCount > 0) {
                myDetailsTitle.setIcon(ERROR_NOTICE);
            } else {
                myDetailsTitle.setIcon(WARNING_NOTICE);
            }
        }

        rootPanel.revalidate();
        rootPanel.doLayout();
        rootPanel.repaint();
    }

    private String getResolvedProperties(ConfigPart part) {
        Map<String, String> props = ConfigPropertiesUtil.toProperties(part,
                P4Bundle.getString("configuration.resolve.value.unset"),
                P4Bundle.getString("configuration.resolve.password.unset"),
                P4Bundle.getString("configuration.resolve.password.set"));
        List<String> keys = new ArrayList<>(props.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append('=').append(props.get(key)).append('\n');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void createUIComponents() {
        // custom component creation code
        myConfigPartStack = new ConfigPartStack(vcsRoot, configConnectionController);

        problemListModel = new DefaultListModel<>();
        myProblemsList = new JBList<>(problemListModel);
        myProblemsList.setCellRenderer(new ProblemRenderer());

        myCheckConnectionSpinner = new AsyncProcessIcon("Checking Connection");
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
        panel2.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:grow"));
        panel1.add(panel2, BorderLayout.EAST);
        myCheckConnectionButton = new JButton();
        this.$$$loadButtonText$$$(myCheckConnectionButton, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configuration.check-connection.button"));
        CellConstraints cc = new CellConstraints();
        panel2.add(myCheckConnectionButton, cc.xy(3, 1));
        panel2.add(myCheckConnectionSpinner, cc.xy(1, 1));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.WEST);
        myDetailsTitle = new JLabel();
        myDetailsTitle.setText("");
        panel3.add(myDetailsTitle, BorderLayout.CENTER);
        myTabbedPane = new JTabbedPane();
        rootPanel.add(myTabbedPane, BorderLayout.CENTER);
        myConfigPanel = new JPanel();
        myConfigPanel.setLayout(new BorderLayout(0, 0));
        myTabbedPane.addTab(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                        .getString("configuration.tab.properties"), null, myConfigPanel,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                        .getString("configuration.tab.properties.tooltip"));
        myConfigPanel.add(myConfigPartStack.$$$getRootComponent$$$(), BorderLayout.CENTER);
        myProblemsPanel = new JPanel();
        myProblemsPanel.setLayout(new BorderLayout(0, 0));
        myTabbedPane.addTab(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configuration.problems-list.tab"), myProblemsPanel);
        myTabbedPane.setEnabledAt(1, false);
        final JScrollPane scrollPane1 = new JScrollPane();
        myProblemsPanel.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setViewportView(myProblemsList);
        myResolvedPropertyPanel = new JPanel();
        myResolvedPropertyPanel.setLayout(new BorderLayout(0, 0));
        myTabbedPane.addTab(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configurations.resolved-values.tab"), myResolvedPropertyPanel);
        myTabbedPane.setEnabledAt(2, false);
        final JScrollPane scrollPane2 = new JScrollPane();
        myResolvedPropertyPanel.add(scrollPane2, BorderLayout.CENTER);
        myResolvedProperties = new JTextPane();
        myResolvedProperties.setEditable(false);
        Font myResolvedPropertiesFont = this.$$$getFont$$$("DialogInput", -1, -1, myResolvedProperties.getFont());
        if (myResolvedPropertiesFont != null) {
            myResolvedProperties.setFont(myResolvedPropertiesFont);
        }
        myResolvedProperties.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configuration.resolved.tooltip"));
        scrollPane2.setViewportView(myResolvedProperties);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
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
        return rootPanel;
    }

    private static class ProblemRenderer
            implements ListCellRenderer<ConfigProblem> {
        private final JTextArea cell;

        ProblemRenderer() {
            this.cell = new JTextArea();
            cell.setLineWrap(true);
            cell.setWrapStyleWord(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ConfigProblem> list, ConfigProblem problem,
                int index, boolean isSelected, boolean cellHasFocus) {
            // TODO allow selection, and show selection differently.

            cell.setForeground(problem.isError()
                    ? JBColor.RED
                    : UIUtil.getTextAreaForeground());
            cell.setBackground(UIUtil.getListBackground(isSelected));
            cell.setText(problem.getMessage());
            return cell;
        }
    }

}
