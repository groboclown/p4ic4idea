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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import net.groboclown.idea.p4ic.config.P4ProjectConfigComponent;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.ui.config.props.ConfigStackPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class P4SettingsPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(P4SettingsPanel.class);


    private UserPreferencesPanel myUserPreferencesPanel;
    private JPanel myRootPanel;
    private ConfigStackPanel configStackPanel;
    private ResolvedPropertiesPanel resolvePropertiesPanel;

    P4SettingsPanel() {
        // FIXME the creation of these components causes a whole bunch of
        // AWT events to be added.  This needs to be delayed as much as
        // possible until after creation.  This is probably due to listeners
        // added at the wrong time.  The big hang-up is the DefaultCaret sending
        // out a signal.
        LOG.debug("Panel GUI constructed.  Now registering listeners for settings panel.");
        Disposer.register(this, configStackPanel);

        configStackPanel.addConfigurationUpdatedListener(resolvePropertiesPanel.getConfigurationUpdatedListener());
        resolvePropertiesPanel.setRequestConfigurationLoadListener(configStackPanel);
        LOG.debug("Completed listener initialization");
    }

    boolean isModified(@NotNull final P4ProjectConfigComponent myConfig,
            @NotNull final UserProjectPreferences preferences) {
        return configStackPanel.isModified(myConfig)
                || myUserPreferencesPanel.isModified(preferences);
    }

    void saveSettingsToConfig(@NotNull final P4ProjectConfigComponent config,
            @NotNull final UserProjectPreferences preferences) {
        configStackPanel.loadFromUI(config);
        myUserPreferencesPanel.saveSettingsToConfig(preferences);
    }

    void loadSettingsIntoGUI(@NotNull final P4ProjectConfigComponent config,
            @NotNull final UserProjectPreferences preferences) {
        configStackPanel.updateUI(config);
        resolvePropertiesPanel.refresh(config.getP4ProjectConfig());
        myUserPreferencesPanel.loadSettingsIntoGUI(preferences);
    }

    JPanel getPanel() {
        return myRootPanel;
    }

    void initialize(final Project project) {
        configStackPanel.initialize(project);
        resolvePropertiesPanel.initialize(project);
        if (project != null) {
            final P4ProjectConfigComponent configComponent = P4ProjectConfigComponent.getInstance(project);
            resolvePropertiesPanel.getConfigurationUpdatedListener().onConfigurationUpdated(
                    configComponent.getP4ProjectConfig());
        }
    }

    @Override
    public void dispose() {
        if (configStackPanel != null) {
            Disposer.dispose(configStackPanel);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new BorderLayout(0, 0));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        myRootPanel.add(tabbedPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("configuration.tab.properties"), panel1);
        configStackPanel = new ConfigStackPanel();
        panel1.add(configStackPanel.$$$getRootComponent$$$(), BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle")
                .getString("user.settings.connection"), panel2);
        resolvePropertiesPanel = new ResolvedPropertiesPanel();
        panel2.add(resolvePropertiesPanel.$$$getRootComponent$$$(), BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        tabbedPane1
                .addTab(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("user.settings.prefs"),
                        panel3);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, BorderLayout.CENTER);
        myUserPreferencesPanel = new UserPreferencesPanel();
        scrollPane1.setViewportView(myUserPreferencesPanel.$$$getRootComponent$$$());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myRootPanel;
    }
}
