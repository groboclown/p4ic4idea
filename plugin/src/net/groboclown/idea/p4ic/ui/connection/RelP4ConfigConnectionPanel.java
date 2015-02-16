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

import com.intellij.openapi.util.Comparing;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class RelP4ConfigConnectionPanel implements ConnectionPanel {
    private JTextField myConfigFileName;
    private JPanel myRootPane;

    @Override
    public boolean isModified(@NotNull P4Config config) {
        return Comparing.equal(config.getConfigFile(), myConfigFileName.getText());
    }

    @Override
    public String getName() {
        return P4Bundle.message("configuration.connection-choice.picker.relp4config");
    }

    @Override
    public String getDescription() {
        return P4Bundle.message("connection.relp4config.description");
    }

    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return P4Config.ConnectionMethod.REL_P4CONFIG;
    }

    @Override
    public void loadSettingsIntoGUI(@NotNull P4Config config) {
        myConfigFileName.setText(config.getConfigFile());
    }

    @Override
    public void saveSettingsToConfig(@NotNull ManualP4Config config) {
        config.setConfigFile(myConfigFileName.getText());
    }

}
