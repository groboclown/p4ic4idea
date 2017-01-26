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

package net.groboclown.idea.p4ic.ui.config.props;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import net.groboclown.idea.p4ic.ui.ComponentListPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class ConfigPartPanel<T extends ConfigPart>
        implements ConfigurationUpdatedListener,
            ComponentListPanel.SelectableComponent,
            ComponentListPanel.WithRootPanel {
    private final Project project;
    private final String id;
    private final T part;
    private ConfigPartUpdatedListener listener;
    private P4ProjectConfig latestConfig;

    ConfigPartPanel(@NotNull Project project, @NotNull String id, @NotNull T part) {
        this.project = project;
        this.id = id;
        this.part = part;
        // getRootPanel().setMaximumSize(new Dimension(-1, getRootPanel().getMinimumSize().height));
    }

    void setConfigPartUpdatedListener(@NotNull ConfigPartUpdatedListener listener) {
        this.listener = listener;
    }

    public final String getId() {
        return id;
    }

    /**
     *
     * @return a copy of the part, for exporting.
     */
    abstract @NotNull T copyPart();

    final T getConfigPart() {
        return part;
    }

    public abstract boolean isModified(@NotNull T originalPart);

    /**
     * Completely copy the panel's configuration <i>into</i> the parameter.
     *
     * @param userPart destination for the panel's configuration.
     */
    public abstract void applySettingsTo(@NotNull T userPart);

    /**
     * Call whenever a config part setting changes.
     */
    void firePropertyChange() {
        listener.onConfigPartUpdated();
    }

    @Override
    public void onConfigurationUpdated(@NotNull P4ProjectConfig config) {
        this.latestConfig = config;
    }

    @Nullable
    P4ProjectConfig getLatestConfig() {
        return latestConfig;
    }

    @NotNull
    Project getProject() {
        return project;
    }
}