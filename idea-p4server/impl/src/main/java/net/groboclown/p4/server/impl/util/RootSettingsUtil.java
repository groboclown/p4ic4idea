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

package net.groboclown.p4.server.impl.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RootSettingsUtil {
    private static final Logger LOG = Logger.getInstance(RootSettingsUtil.class);

    @NotNull
    public static P4VcsRootSettings getFixedRootSettings(
            @NotNull Project project,
            @NotNull VcsDirectoryMapping mapping,
            @NotNull VirtualFile rootDir) {
        // See #209 - This mapping, if done wrong, will cause the plugin
        // to not associate the roots to the configuration correctly.

        VcsRootSettings rawSettings = mapping.getRootSettings();
        P4VcsRootSettings retSettings = null;
        if (rawSettings instanceof P4VcsRootSettings) {
            retSettings = (P4VcsRootSettings) rawSettings;
        } else {
            LOG.warn("Encountered wrong root settings type in directory mapping: " +
                    (rawSettings == null ? null : rawSettings.getClass()));
        }
        List<ConfigPart> parts = null;
        if (retSettings != null) {
            if (! FileUtil.pathsEqual(rootDir.getPath(), retSettings.getRootDir().getPath())) {
                LOG.info("Mapping has directory " + mapping.getDirectory() +
                        " which does not match P4 VCS settings dir " +
                        retSettings.getRootDir().getPath() + "; root dir " + rootDir);
                if (! retSettings.usesDefaultConfigParts()) {
                    parts = retSettings.getConfigParts();
                }
                retSettings = null;
            }
        }
        if (retSettings == null) {
            // This shouldn't happen, but it does.  Instead, the mapping is supposed
            // to be created through the createEmptyVcsRootSettings() method.
            // That's reflected in the deprecation of setRootSettings.
            LOG.warn("Encountered empty root settings in directory mapping.");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using root path " + rootDir);
            }
            retSettings = new P4VcsRootSettingsImpl(project, rootDir);
            if (parts != null && retSettings.usesDefaultConfigParts()) {
                retSettings.setConfigParts(parts);
            }
            mapping.setRootSettings(retSettings);
        }
        return retSettings;
    }
}
