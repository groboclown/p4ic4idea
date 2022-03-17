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
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens to VCS root chang events, and updates the corresponding configuration data.
 * Both VCS_CONFIGURATION_CHANGED and VCS_CONFIGURATION_CHANGED_IN_PLUGIN topics should be
 * covered by this.
 * <p>
 * This listener is registered via the plugin.xml file.
 */
public class VcsRootChangeListener implements VcsListener {
    private final Project project;

    public VcsRootChangeListener(Project project) {
        this.project = project;
    }

    @Override
    public void directoryMappingChanged() {
        if (project != null) {
            final Set<VirtualFile> currentRoots =
                    new HashSet<>(VcsRootConfigController.getInstance().getRegisteredRoots(project));
            for (VirtualFile root: ProjectLevelVcsManager.getInstance(project).getAllVersionedRoots()) {
                currentRoots.remove(root);
            }
            // whatever remains is extra.
            for (VirtualFile root: currentRoots) {
                VcsRootConfigController.getInstance().removeRoot(project, root);
            }
        }
    }
}
