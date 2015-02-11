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
package net.groboclown.idea.p4ic.ui;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class P4ConfigurationProjectPanel {

    private final Project project;
    private P4ConfigPanel myMainPanel;
    private volatile boolean isInitialized = false;

    public P4ConfigurationProjectPanel(@NotNull Project project) {
        this.project = project;
    }

    public boolean isModified(@NotNull ManualP4Config myConfig) {
        if (!isInitialized) {
            return false;
        }

        return myMainPanel.isModified(myConfig);
    }

    public void saveSettings(@NotNull ManualP4Config myConfig) {
        if (!isInitialized) {
            // nothing to do
            return;
        }

        myMainPanel.saveSettingsToConfig(myConfig);
    }

    public void loadSettings(@NotNull ManualP4Config myConfig) {
        if (!isInitialized) {
            getPanel(myConfig);
            return;
        }

        myMainPanel.loadSettingsIntoGUI(myConfig);
    }

    public synchronized JPanel getPanel(@NotNull ManualP4Config config) {
        if (!isInitialized) {
            myMainPanel = new P4ConfigPanel(project);
            isInitialized = true;
        }
        loadSettings(config);
        return myMainPanel.getPanel();
    }

    public void dispose() {
        //myMainPanel.dispose();
        myMainPanel = null;
        isInitialized = false;
    }
}
