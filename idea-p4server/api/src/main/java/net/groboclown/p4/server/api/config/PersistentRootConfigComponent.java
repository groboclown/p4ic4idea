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

package net.groboclown.p4.server.api.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Maintains storage of the VCS root directory configurations.  This is intentionally separate from the
 * root component so that the server configuration is maintained in the user's workspace file.
 */
@State(
        name = "PersistentRootConfigComponent",
        storages = {
                @Storage(
                        file = StoragePathMacros.WORKSPACE_FILE
                )
        }
)
public abstract class PersistentRootConfigComponent
        implements ProjectComponent, PersistentStateComponent<Element> {
    public static final String COMPONENT_NAME = PersistentRootConfigComponent.class.getName();


    @NotNull
    public static PersistentRootConfigComponent getInstance(@NotNull Project project) {
        PersistentRootConfigComponent ret = (PersistentRootConfigComponent) project.getComponent(COMPONENT_NAME);
        if (ret == null) {
            throw new IllegalStateException("PersistentRootConfigComponent not initialized");
        }
        return ret;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }


    @Override
    public void projectOpened() {
        // do nothing
    }

    @Override
    public void projectClosed() {
        disposeComponent();
    }

    @Override
    public void initComponent() {
        // do nothing explicit
    }

    @Override
    public void disposeComponent() {
    }

    public boolean hasConfigPartsForRoot(@NotNull VirtualFile root) {
        return getConfigPartsForRoot(root) != null;
    }

    @Nullable
    public abstract List<ConfigPart> getConfigPartsForRoot(@NotNull VirtualFile root);

    public abstract void setConfigPartsForRoot(@NotNull VirtualFile root, @NotNull List<ConfigPart> parts);
}
