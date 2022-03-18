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

package net.groboclown.p4plugin.modules.clientconfig;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.messagebus.VcsRootClientPartsMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Manages access to the model.  Does not monitor the VCS roots status - that is handled in the
 *
 */
public class VcsRootConfigController {
    private static final VcsRootConfigController INSTANCE = new VcsRootConfigController();

    public static VcsRootConfigController getInstance() {
        return INSTANCE;
    }

    private VcsRootConfigController() {
        // utility class
    }


    /** Does the root have parts?  If false, then that means the root is not registered. */
    public boolean hasConfigPartsForRoot(@NotNull final Project project, @NotNull final VirtualFile root) {
        return withModel(project, false,
                (m) -> m.getConfigPartsForRoot(root) != null);
    }

    /**
     * Gets the list of config parts for the given VCS root.  If the root is not registered,
     * then null is returned.
     */
    @Nullable
    public List<ConfigPart> getConfigPartsForRoot(@NotNull final Project project, @NotNull final VirtualFile root) {
        return withModel(project, null,
                (m) -> m.getConfigPartsForRoot(root));
    }

    /**
     * Return a collection of all the VCS roots with registered configurations.
     */
    @NotNull
    public Collection<VirtualFile> getRegisteredRoots(@NotNull final Project project) {
        return withModel(project, Collections.emptyList(), PersistentRootConfigModel::getRegisteredRoots);
    }

    /**
     * Register or update a VCS root with the configuration parts.
     */
    public void setRootConfigParts(@NotNull final Project project, @NotNull final VirtualFile root,
            @NotNull final List<ConfigPart> parts) {
        withModel(project, null, (m) -> {
            m.setConfigPartsForRoot(root, parts);
            VcsRootClientPartsMessage.sendVcsRootClientPartsUpdated(project, root, parts);
            return null;
        });
    }

    /**
     * Remove the VCS root from the registration store.
     */
    public void removeRoot(@NotNull final Project project, @NotNull final VirtualFile root) {
        withModel(project, null, (m) -> {
            m.removeRoot(root);
            VcsRootClientPartsMessage.sendVcsRootClientPartsRemoved(project, root);
            return null;
        });
    }


    private <T> T withModel(@NotNull Project project, T defaultValue,
            @NotNull Function<PersistentRootConfigModel, T> func) {
        PersistentRootConfigModel model = PersistentRootConfigModel.getInstance(project);
        if (model == null) {
            return defaultValue;
        }
        return func.apply(model);
    }
}
