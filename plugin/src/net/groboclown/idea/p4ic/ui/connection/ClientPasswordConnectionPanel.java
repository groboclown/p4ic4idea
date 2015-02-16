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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class ClientPasswordConnectionPanel implements ConnectionPanel {
    private JTextField myPort;
    private JTextField myUsername;
    private JPanel myRootPanel;



    @Override
    public boolean isModified(@NotNull P4Config config) {
        if (P4ConfigUtil.isPortModified(config, myPort.getText())) {
            return true;
        }
        if (config.getUsername() == null || config.getUsername().length() <= 0) {
            if (getUsername() != null) {
                return true;
            }
        } else {
            if (!config.getUsername().equals(getUsername())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return P4Bundle.message("configuration.connection-choice.picker.client-password");
    }

    @Override
    public String getDescription() {
        return P4Bundle.message("connection.client.description");
    }

    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return P4Config.ConnectionMethod.CLIENT;
    }

    @Override
    public void loadSettingsIntoGUI(@NotNull P4Config config) {
        myUsername.setText(config.getUsername());
        myPort.setText(P4ConfigUtil.toFullPort(config.getProtocol(), config.getPort()));
    }

    @Override
    public void saveSettingsToConfig(@NotNull ManualP4Config config) {
        config.setUsername(getUsername());
        config.setProtocol(P4ConfigUtil.getProtocolFromPort(myPort.getText()));
        config.setPort(P4ConfigUtil.getSimplePortFromPort(myPort.getText()));
    }

    /*
    @Nullable
    @Override
    public P4Config loadChildConfig(@NotNull P4Config config) throws P4DisconnectedException {
        return null;
    }
    */


    private String getUsername() {
        String ret = myUsername.getText();
        if (ret == null || ret.length() <= 0) {
            return null;
        }
        return ret.trim();
    }

}
