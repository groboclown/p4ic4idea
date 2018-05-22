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
import net.groboclown.p4.server.impl.config.part.PartValidation;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class P4RootConfigPanel {
    private static final Icon EXPAND_DESCRIPTION = AllIcons.Actions.Right;
    private static final Icon COLLAPSE_DESCRIPTION = AllIcons.Actions.Down;

    private final VirtualFile vcsRoot;
    private final ConfigConnectionController configConnectionController;
    private JPanel rootPanel;
    private JPanel myConfigRefreshDetailsPanel;
    private JButton myCheckConnectionButton;
    private JPanel myProblemsPanel;
    private JTextPane myResolvedProperties;
    private JList<ConfigProblem> myProblemsList;
    private DefaultListModel<ConfigProblem> problemListModel;
    private ConfigPartStack myConfigPartStack;
    private JButton myDetailsToggle;
    private AsyncProcessIcon myCheckConnectionSpinner;
    private JLabel myDetailsTitle;

    // FIXME better manage the toggled panel size.  Or, somehow setup
    // a better interface.  The old tab display may work well, but it
    // was finicky.

    P4RootConfigPanel(VirtualFile vcsRoot,
            ConfigConnectionController configConnectionController) {
        this.vcsRoot = vcsRoot;
        this.configConnectionController = configConnectionController;

        $$$setupUI$$$();

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

        myDetailsToggle.setVisible(false);
        SwingUtil.iconOnlyButton(myDetailsToggle, EXPAND_DESCRIPTION, SwingUtil.ButtonType.ACCENT);
        myDetailsTitle.setText("");
        myDetailsToggle.addActionListener((e) -> {
            if (myConfigRefreshDetailsPanel.isVisible()) {
                SwingUtil.iconOnlyButton(myDetailsToggle, EXPAND_DESCRIPTION, SwingUtil.ButtonType.ACCENT);
                myConfigRefreshDetailsPanel.setVisible(false);
            } else {
                SwingUtil.iconOnlyButton(myDetailsToggle, COLLAPSE_DESCRIPTION, SwingUtil.ButtonType.ACCENT);
                myConfigRefreshDetailsPanel.setVisible(true);
            }
            rootPanel.revalidate();
            rootPanel.doLayout();
            rootPanel.repaint();
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

        // Only show the details panel when explicitly clicked.
        // TODO probably put a high-level text describing the number of problems found.
        // myConfigRefreshDetailsPanel.setVisible(true);
        SwingUtil.iconOnlyButton(myDetailsToggle, EXPAND_DESCRIPTION, SwingUtil.ButtonType.ACCENT);
        myDetailsToggle.setVisible(true);
        myResolvedProperties.setText(getResolvedProperties(part));

        int errorCount = 0;
        int warningCount = 0;
        problemListModel.clear();
        for (ConfigProblem configProblem : PartValidation.findAllProblems(part)) {
            problemListModel.addElement(configProblem);
            if (configProblem.isError()) {
                errorCount++;
            } else {
                warningCount++;
            }
        }

        myProblemsList.revalidate();
        myProblemsList.doLayout();

        if (problemListModel.isEmpty()) {
            myProblemsPanel.setVisible(false);
            myDetailsTitle.setVisible(false);
        } else {
            myProblemsPanel.setVisible(true);
            myDetailsTitle.setVisible(true);
            myDetailsTitle.setText(P4Bundle.message("configuration.stack.wrapper.toggle.title",
                    errorCount, warningCount));
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
        myConfigRefreshDetailsPanel = new JPanel(new VerticalFlowLayout());
        myConfigRefreshDetailsPanel.setVisible(false);
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
        rootPanel.add(panel1, BorderLayout.CENTER);
        panel1.add(myConfigPartStack.$$$getRootComponent$$$(), BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        rootPanel.add(panel2, BorderLayout.NORTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.NORTH);
        myDetailsToggle = new JButton();
        myDetailsToggle.setText("");
        myDetailsToggle.setVerticalAlignment(3);
        panel3.add(myDetailsToggle, BorderLayout.WEST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:grow"));
        panel3.add(panel4, BorderLayout.EAST);
        myCheckConnectionButton = new JButton();
        this.$$$loadButtonText$$$(myCheckConnectionButton, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("configuration.check-connection.button"));
        CellConstraints cc = new CellConstraints();
        panel4.add(myCheckConnectionButton, cc.xy(3, 1));
        panel4.add(myCheckConnectionSpinner, cc.xy(1, 1));
        myConfigRefreshDetailsPanel.setLayout(new BorderLayout(0, 0));
        panel2.add(myConfigRefreshDetailsPanel, BorderLayout.CENTER);
        myProblemsPanel = new JPanel();
        myProblemsPanel.setLayout(new FormLayout("fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:d:grow"));
        myConfigRefreshDetailsPanel.add(myProblemsPanel, BorderLayout.NORTH);
        final JScrollPane scrollPane1 = new JScrollPane();
        myProblemsPanel.add(scrollPane1, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
        scrollPane1.setViewportView(myProblemsList);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        myConfigRefreshDetailsPanel.add(panel5, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(scrollPane2, BorderLayout.NORTH);
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
            cell.setText(problem.getMessage());
            return cell;
        }
    }

}
