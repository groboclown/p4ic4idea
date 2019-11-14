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

package net.groboclown.p4plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.util.ProjectUtil;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import org.jetbrains.annotations.NotNull;

public class RootSettingsUtil {
    private static final Logger LOG = Logger.getInstance(RootSettingsUtil.class);

    @NotNull
    public static P4VcsRootSettings getFixedRootSettings(
            @NotNull Project project,
            @NotNull VcsDirectoryMapping mapping) {
        VcsRootSettings settings = mapping.getRootSettings();
        if (settings != null && ! (settings instanceof P4VcsRootSettings)) {
            LOG.warn("Encountered wrong root settings type in directory mapping: " + settings.getClass());
            settings = null;
        }
        if (settings == null) {
            // This shouldn't happen, but it does.  Instead, the mapping is supposed
            // to be created through the createEmptyVcsRootSettings() method.
            // That's reflected in the deprecation of setRootSettings.
            LOG.warn("Encountered empty root settings in directory mapping.");
            VirtualFile rootDir = VcsUtil.getVirtualFile(mapping.getDirectory());
            if (rootDir == null) {
                rootDir = ProjectUtil.guessProjectBaseDir(project);
            }
            settings = new P4VcsRootSettingsImpl(project, rootDir);
            mapping.setRootSettings(settings);
        }
        return (P4VcsRootSettings) settings;
    }
}
