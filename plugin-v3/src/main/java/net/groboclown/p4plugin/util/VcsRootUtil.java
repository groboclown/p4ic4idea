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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VcsRootUtil {
    private static final Logger LOG = Logger.getInstance(VcsRootUtil.class);


    private VcsRootUtil() {
        // util class
    }

    public static List<VcsRoot> getP4Roots(@NotNull Project project) {
        return ServiceManager.getService(project, VcsRootDetector.class).detect()
                .stream().filter((root) ->
                        root.getVcs() != null
                                && root.getPath() != null
                                && P4Vcs.getKey().equals(root.getVcs().getKeyInstanceMethod()))
                .collect(Collectors.toList());
    }

    // FIXME this should instead be in a cached object.

}
