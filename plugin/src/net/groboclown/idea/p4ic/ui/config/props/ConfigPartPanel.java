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
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public abstract class ConfigPartPanel<T extends ConfigPart> {
    private final Project project;
    private final String id;
    private final T part;
    private ConfigPartUpdatedListener listener;

    protected ConfigPartPanel(@NotNull Project project, @NotNull String id, @NotNull T part) {
        this.project = project;
        this.id = id;
        this.part = part;
        this.listener = listener;
    }

    void setConfigPartUpdatedListener(@NotNull ConfigPartUpdatedListener listener) {
        this.listener = listener;
    }

    public final String getId() {
        return id;
    }

    public final T getConfigPart() {
        return part;
    }

    public abstract boolean isModified(T originalPart);

    abstract JPanel getRootPanel();

    void onPropertyChange() {
        listener.onConfigPartUpdated();
    }
}
