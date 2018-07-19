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

package net.groboclown.p4plugin.ui.vcsroot;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ConfigConnectionController {
    private final Project project;
    private final List<ConfigConnectionListener> listeners = new ArrayList<>();

    protected ConfigConnectionController(Project project) {
        this.project = project;
    }

    public void addConfigConnectionListener(@NotNull ConfigConnectionListener listener) {
        synchronized (listeners) {
            this.listeners.add(listener);
        }
    }

    public final void refreshConfigConnection() {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(this::performRefresh);
        } else {
            performRefresh();
        }
    }

    protected abstract void performRefresh();

    final void fireConfigConnectionRefreshed(
            @NotNull final ConfigPart parentPart,
            @Nullable final ClientConfig clientConfig,
            @Nullable final ServerConfig serverConfig) {
        synchronized (listeners) {
            for (ConfigConnectionListener listener : listeners) {
                // Because this fire happens from inside a modal dialog, invoke
                // commands on the Application waits until the dialog completes.
                EventQueue.invokeLater(() ->
                        listener.onConfigRefresh(project, parentPart, clientConfig, serverConfig));
            }
        }
    }
}
