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
import com.intellij.uiDesigner.core.Spacer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class SSOConnectionPanel implements ConnectionPanel {
    private JPanel myRootPanel;

    @Override
    public boolean isModified(@NotNull P4Config config) {
        return false;
    }

    @Override
    public String getName() {
        return P4Bundle.message("configuration.connection-choice.picker.sso");
    }

    @Override
    public String getDescription() {
        return P4Bundle.message("connection.sso.description");
    }

    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return P4Config.ConnectionMethod.SSO;
    }

    @Override
    public void loadSettingsIntoGUI(@NotNull P4Config config) {
        // Nothing to do right now
    }

    @Override
    public void saveSettingsToConfig(@NotNull ManualP4Config config) {
        // Nothing to do right now
    }

    /*
    @Nullable
    @Override
    public P4Config loadChildConfig(@NotNull P4Config config) throws P4DisconnectedException {
        return null;
    }
    */

}
