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
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class P4ConfigConnectionPanel implements ConnectionPanel {
    private static final String DEFAULT_P4CONFIG_NAME = ".p4config";

    private final Project project;
    private TextFieldWithBrowseButton myP4ConfigFile;
    private JLabel myFileMessage;
    private JPanel myRootPanel;

    public P4ConfigConnectionPanel(Project project) {
        this.project = project;
        setConfigFileText(null);
        myP4ConfigFile.addBrowseFolderListener(
                P4Bundle.message("configuration.connection-choice.picker.p4config"),
                P4Bundle.message("configuration.p4config.chooser"),
                project, new FileChooserDescriptor(true, false, false, false, false, false));

        // This component doesn't respond as normal
        //myP4ConfigFile.getTextField().addKeyListener(new KeyAdapter() {
        //    @Override
        //    public void keyTyped(KeyEvent e) {
        //        if (e.getKeyChar() == '\n' || e.getKeyChar() == '\r' || e.getKeyChar() == '\t') {
        //            validateConfigFile();
        //        }
        //    }
        //});
        //myP4ConfigFile.getTextField().addFocusListener(new FocusAdapter() {
        //    @Override
        //    public void focusLost(FocusEvent e) {
        //        validateConfigFile();
        //    }
        //});
        myP4ConfigFile.addActionListener(new FileActionListener());
        myP4ConfigFile.getTextField().addActionListener(new FileActionListener());
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
    public P4Config.ConnectionMethod getConnectionMethod() {
        return P4Config.ConnectionMethod.P4CONFIG;
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

    /*
    @Nullable
    @Override
    public P4Config loadChildConfig(@NotNull P4Config config) throws P4DisconnectedException {
        String filePath = config.getConfigFile();
        if (filePath == null) {
            P4InvalidConfigException e = new P4InvalidConfigException(
                    P4Bundle.message("configuration.p4config.no-file"));
            setTextMessage(P4Bundle.message("configuration.p4config.bad-file", e.getMessage()));
            throw e;
        }
        try {
            P4Config ret = P4ConfigUtil.loadP4ConfigFile(new File(config.getConfigFile()));
            setTextMessage("");
            return ret;
        } catch (IOException e) {
            setTextMessage(
                    P4Bundle.message("configuration.p4config.bad-file", e.getMessage()));
            throw new P4InvalidConfigException(
                    P4Bundle.message("configuration.p4config.file-load-error",
                            config.getConfigFile(), e.getMessage()));
        }
    }
    */

    void setConfigFileText(String text) {
        if (text == null || text.length() <= 0) {
            text = (new File(project.getBaseDir().getPath(), DEFAULT_P4CONFIG_NAME)).getAbsolutePath();
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
            //throw e;
            return;
        }
        try {
            new FileP4Config(new File(config.getConfigFile()));
            setTextMessage("");
        } catch (IOException e) {
            setTextMessage(
                    P4Bundle.message("configuration.p4config.bad-file", e.getMessage()));
            //throw new P4InvalidConfigException(
            //        P4Bundle.message("configuration.p4config.file-load-error",
            //                config.getConfigFile(), e.getMessage()));
        }
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
