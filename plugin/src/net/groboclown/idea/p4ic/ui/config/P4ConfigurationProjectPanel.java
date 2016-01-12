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
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class P4ConfigurationProjectPanel {

    private final Project project;
    private P4SettingsPanel myMainPanel;
    private volatile boolean isInitialized = false;

    public P4ConfigurationProjectPanel(@NotNull Project project) {
        this.project = project;
    }

    public synchronized boolean isModified(@NotNull ManualP4Config myConfig, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            return false;
        }

        return myMainPanel.isModified(myConfig, preferences);
    }

    public synchronized void saveSettings(@NotNull P4ConfigProject config, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            // nothing to do
            return;
        }
        ManualP4Config saved = new ManualP4Config();
        myMainPanel.saveSettingsToConfig(saved, preferences);

        config.loadState(saved);

        try {
            // ensure the sources are loaded
            config.loadProjectConfigSources();

            // Announce the change to state.
            config.announceBaseConfigUpdated();
        } catch (P4InvalidConfigException e) {
            // TODO ensure that this is the correct kind of error to show.
            AlertManager.getInstance().addWarning(project,
                    P4Bundle.message("error.config.load-sources"),
                    P4Bundle.message("error.config.load-sources"),
                    e, new FilePath[0]);
        }

    }

    public synchronized void loadSettings(@NotNull ManualP4Config config, @NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            getPanel(config, preferences);
            return;
        }

        myMainPanel.loadSettingsIntoGUI(config, preferences);
    }

    public synchronized JPanel getPanel(@NotNull ManualP4Config config, @NotNull UserProjectPreferences preferences) {
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
