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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

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
        //return myPanel.getPanel(loadConfig().getConfig(), new EnvP4Config());
        return myPanel.getPanel(loadConfig().getBaseConfig());
    }

    @Override
    public boolean isModified() {
       return myPanel.isModified(loadConfig().getBaseConfig());
    }

    @Override
    public void apply() throws ConfigurationException {
        ManualP4Config saved = new ManualP4Config();
        myPanel.saveSettings(saved);
        loadConfig().loadState(saved);
    }

    @Override
    public void reset() {
        P4ConfigProject config = P4ConfigProject.getInstance(myProject);
        myPanel.loadSettings(config.getBaseConfig());
    }

    @Override
    public void disposeUIResources() {
        myPanel.dispose();
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

    private P4ConfigProject loadConfig() {
        P4ConfigProject project = P4ConfigProject.getInstance(myProject);
        assert project != null;
        return project;
    }
}
