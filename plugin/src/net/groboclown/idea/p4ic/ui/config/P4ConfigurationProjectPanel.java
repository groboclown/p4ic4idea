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

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.P4ProjectConfigComponent;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class P4ConfigurationProjectPanel {

    private final Project project;
    private P4SettingsPanel myMainPanel;
    private volatile boolean isInitialized = false;

    P4ConfigurationProjectPanel(@NotNull Project project) {
        this.project = project;
    }

    public synchronized boolean isModified(@NotNull P4ProjectConfigComponent myConfig, @NotNull UserProjectPreferences preferences) {
        return isInitialized && myMainPanel.isModified(myConfig, preferences);
    }

    synchronized void saveSettings(@NotNull P4ProjectConfigComponent config, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            // nothing to do
            return;
        }
        myMainPanel.saveSettingsToConfig(config, preferences);
    }

    synchronized void loadSettings(@NotNull P4ProjectConfigComponent config, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            getPanel(config, preferences);
            return;
        }

        myMainPanel.loadSettingsIntoGUI(config, preferences);
    }

    synchronized JPanel getPanel(@NotNull P4ProjectConfigComponent config, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            myMainPanel = new P4SettingsPanel();
            myMainPanel.initialize(project);
            isInitialized = true;
        }
        loadSettings(config, preferences);
        return myMainPanel.getPanel();
    }

    public synchronized void dispose() {
        //myMainPanel.dispose();
        myMainPanel = null;
        isInitialized = false;
    }
}
