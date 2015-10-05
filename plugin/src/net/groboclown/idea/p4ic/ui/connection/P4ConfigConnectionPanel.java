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
package net.groboclown.idea.p4ic.ui.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.FileP4Config;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4Config.ConnectionMethod;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class P4ConfigConnectionPanel implements ConnectionPanel {
    private static final Logger LOG = Logger.getInstance(P4ConfigConnectionPanel.class);
    private static final String DEFAULT_P4CONFIG_NAME = ".p4config";

    private Project project;
    private TextFieldWithBrowseButton myP4ConfigFile;
    private JLabel myFileMessage;
    private JPanel myRootPanel;

    public P4ConfigConnectionPanel() {
        setConfigFileText(null);
        myP4ConfigFile.addBrowseFolderListener(
                P4Bundle.message("configuration.connection-choice.picker.p4config"),
                P4Bundle.message("configuration.p4config.chooser"),
                null, new FileChooserDescriptor(true, false, false, false, false, false));

        myP4ConfigFile.addActionListener(new FileActionListener());
        myP4ConfigFile.getTextField().addActionListener(new FileActionListener());
    }


    public void initialize(@NotNull final Project project) {
        this.project = project;
    }


    private void createUIComponents() {
        // place custom component creation code here
    }

    @Override
    public boolean isModified(@NotNull P4Config config) {
        String configFile = config.getConfigFile();
        String myFile = myP4ConfigFile.getText();
        if (configFile == null || configFile.length() <= 0) {
            return myFile == null || myFile.length() <= 0;
        }
        if (myFile == null) {
            return false;
        }

        return FileUtil.filesEqual(
                new File(configFile),
                new File(myFile));
    }

    @Override
    public String getName() {
        return P4Bundle.message("configuration.connection-choice.picker.p4config");
    }

    @Override
    public String getDescription() {
        return P4Bundle.message("connection.p4config.description");
    }

    @Override
    public ConnectionMethod getConnectionMethod() {
        return ConnectionMethod.P4CONFIG;
    }

    @Override
    public void loadSettingsIntoGUI(@NotNull P4Config config) {
        myFileMessage.setText("");
        myFileMessage.setVisible(false);
        setConfigFileText(config.getConfigFile());
        validateConfigFile();
    }

    @Override
    public void saveSettingsToConfig(@NotNull ManualP4Config config) {
        String myFile = myP4ConfigFile.getText();
        if (myFile != null && myFile.length() > 0) {
            config.setConfigFile(myFile);
        } else {
            config.setConfigFile(null);
        }
    }


    void setConfigFileText(@Nullable String text) {
        if (text == null || text.length() <= 0) {
            if (project == null || project.getBaseDir() == null) {
                // This can happen when the user edits the default settings.
                // It can also happen if there are multiple project roots.
                // See bug #21.
                //text = new File(".", DEFAULT_P4CONFIG_NAME).getAbsolutePath();
                LOG.info("Using default config file name (null project? " +
                        (project == null) + ")");
                text = DEFAULT_P4CONFIG_NAME;
            } else {
                text = (new File(project.getBaseDir().getPath(), DEFAULT_P4CONFIG_NAME)).getAbsolutePath();
            }
            LOG.info("Config file set to [" + text + "]");
        }
        myP4ConfigFile.setText(text);
    }

    private void validateConfigFile() {
        ManualP4Config config = new ManualP4Config();
        saveSettingsToConfig(config);

        String filePath = config.getConfigFile();
        if (filePath == null) {
            P4InvalidConfigException e = new P4InvalidConfigException(
                    P4Bundle.message("configuration.p4config.no-file"));
            setTextMessage(P4Bundle.message("configuration.p4config.bad-file", e.getMessage()));
            return;
        }
        try {
            new FileP4Config(new File(config.getConfigFile()));
            setTextMessage("");
        } catch (IOException e) {
            setTextMessage(
                    P4Bundle.message("configuration.p4config.file-read-problem", config.getConfigFile(),
                            e.getMessage()));
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                        .getString("configuration.p4config.panel")));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("configuration.p4config"));
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myP4ConfigFile = new TextFieldWithBrowseButton();
        myP4ConfigFile.setText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("config.file.location.tooltip"));
        myP4ConfigFile.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.p4config.chooser"));
        panel1.add(myP4ConfigFile,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myFileMessage = new JLabel();
        myFileMessage.setText("");
        panel1.add(myFileMessage,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        label1.setLabelFor(myP4ConfigFile);
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
        return myRootPanel;
    }

    private class FileActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            validateConfigFile();
        }
    }


    private void setTextMessage(String message) {
        boolean wasVisible = myFileMessage.isVisible();
        boolean willBeVisible = message != null && message.length() > 0;
        myFileMessage.setText(message);
        if (wasVisible != willBeVisible) {
            myFileMessage.setVisible(willBeVisible);
            myRootPanel.validate();
        }
    }
}
