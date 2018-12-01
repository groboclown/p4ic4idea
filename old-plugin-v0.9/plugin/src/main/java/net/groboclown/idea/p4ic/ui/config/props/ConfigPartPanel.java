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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import net.groboclown.idea.p4ic.ui.ComponentListPanel;
import net.groboclown.idea.p4ic.ui.config.RequestConfigurationLoadListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ConfigPartPanel<T extends ConfigPart>
        implements RequestConfigurationUpdateListener,
            ComponentListPanel.WithRootPanel {
    private static final Logger LOG = Logger.getInstance(ConfigPartPanel.class);


    private final Project project;
    private final T part;
    private final Class<T> type;
    private RequestConfigurationLoadListener requestConfigurationLoadListener;

    @SuppressWarnings("unchecked")
    ConfigPartPanel(@NotNull Project project, @NotNull T part) {
        this.project = project;
        this.part = part;
        this.type = (Class<T>) part.getClass();
    }

    @Nullable
    T castAs(@NotNull ConfigPart part) {
        if (type.isInstance(part)) {
            return type.cast(part);
        }
        return null;
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

    void setRequestConfigurationLoadListener(@NotNull RequestConfigurationLoadListener listener) {
        this.requestConfigurationLoadListener = listener;
    }

    @Nullable
    P4ProjectConfig loadProjectConfigFromUI() {
        if (requestConfigurationLoadListener != null) {
            LOG.debug("Loading config from the current ui settings.");
            return requestConfigurationLoadListener.updateConfigPartFromUI();
        }
        LOG.warn("Invalid state: No load listener set yet.");
        return null;
    }


    @NotNull
    Project getProject() {
        return project;
    }
}
