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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Compatibility with the old way the configurations were stored.
 */
@State(
    name = "P4ConfigProject",
    reloadable = true,
    storages = {
        @Storage(
                id = "other",
                file = StoragePathMacros.WORKSPACE_FILE
        )
    }
)
@Deprecated
public class P4ConfigProject implements ProjectComponent, PersistentStateComponent<ManualP4Config> {
    private static final Logger LOG = Logger.getInstance(P4ConfigProject.class);

    private final Project project;

    @NotNull
    private ManualP4Config config = new ManualP4Config();

    public P4ConfigProject(@NotNull Project project) {
        this.project = project;
    }

    public static P4ConfigProject getInstance(final Project project) {
        return ServiceManager.getService(project, P4ConfigProject.class);
    }

    /**
     * 
     * @return a copy of the base config.  The only way to actually update the value is
     *      through a {@link #loadState(ManualP4Config)} call.
     */
    public ManualP4Config getBaseConfig() {
        return new ManualP4Config(config);
    }


    @Override
    public ManualP4Config getState() {
        return getBaseConfig();
    }

    @Override
    public void loadState(@NotNull ManualP4Config state) {
        LOG.debug("Loading config state");

        // save a copy of the config
        this.config = new ManualP4Config(state);
    }

    @Override
    public void projectOpened() {
        // intentionally empty
    }

    @Override
    public void projectClosed() {
        // intentionally empty
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4ConfigProject";
    }

}
