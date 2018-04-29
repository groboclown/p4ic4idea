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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.preferences.UserProjectPreferences;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class P4ProjectConfigurable implements SearchableConfigurable {

    @NotNull
    private final P4ConfigurationProjectPanel myPanel;
    @NotNull
    private final Project myProject;

    public P4ProjectConfigurable(@NotNull Project project) {
        myProject = project;
        myPanel = new P4ConfigurationProjectPanel(project);
    }

    @Override
    @Nls
    public String getDisplayName() {
        return P4Bundle.message("p4ic.name");
    }

    @Override
    public String getHelpTopic() {
        return "project.propVCSSupport.VCSs.Perforce";
    }

    @Override
    public JComponent createComponent() {
        UserProjectPreferences prefs = loadPreferences();
        if (prefs == null) {
            return null;
        }
        return myPanel.getPanel(prefs);
    }

    @Override
    public boolean isModified() {
        UserProjectPreferences prefs = loadPreferences();
        return prefs != null && myPanel.isModified(prefs);
    }

    @Override
    public void apply() throws ConfigurationException {
        UserProjectPreferences prefs = loadPreferences();
        if (prefs != null) {
            myPanel.saveSettings(prefs);
            // Note: the "save settings" call will call P4ProjectConfigComponent.setUserConfigParts(new config)
        }
    }

    @Override
    public void reset() {
        UserProjectPreferences prefs = loadPreferences();
        if (prefs != null) {
            myPanel.loadSettings(prefs);
        }
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(myPanel);
    }

    @Override
    @NotNull
    public String getId() {
        return "Perforce.Project";
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nullable
    private UserProjectPreferences loadPreferences() {
        return UserProjectPreferences.getInstance(myProject);
    }
}
