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
package net.groboclown.p4plugin.ui.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.ui.WrapperPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class P4ConfigurationProjectPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(P4ConfigurationProjectPanel.class);

    private final Project project;
    private UserPreferencesPanel myMainPanel;
    private WrapperPanel wrappedPanel;
    private volatile boolean isInitialized = false;

    P4ConfigurationProjectPanel(@NotNull Project project) {
        this.project = project;
    }

    synchronized boolean isModified(@NotNull UserProjectPreferences preferences) {
        return isInitialized && myMainPanel.isModified(preferences);
    }

    synchronized void saveSettings(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            // nothing to do
            return;
        }
        myMainPanel.saveSettingsToConfig(preferences);
    }

    synchronized void loadSettings(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            getPanel(preferences);
            return;
        }

        LOG.debug("Loading settings into the main panel");
        myMainPanel.loadSettingsIntoGUI(preferences);
    }

    synchronized JPanel getPanel(@NotNull UserProjectPreferences preferences) {
        if (!isInitialized) {
            LOG.debug("Creating settings panel");
            myMainPanel = new UserPreferencesPanel();
            isInitialized = true;
            wrappedPanel = new WrapperPanel(myMainPanel.getRootPanel());
        }
        loadSettings(preferences);
        return wrappedPanel;
    }

    public synchronized void dispose() {
        // TODO is there a dispose to call on this panel?
        //if (myMainPanel != null) {
        //    Disposer.dispose(myMainPanel);
        //}
        myMainPanel = null;
        wrappedPanel = null;
        isInitialized = false;
    }
}
