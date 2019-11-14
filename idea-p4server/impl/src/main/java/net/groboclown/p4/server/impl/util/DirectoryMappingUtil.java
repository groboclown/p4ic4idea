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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;

public class DirectoryMappingUtil {

    @NotNull
    public static VirtualFile getDirectory(@NotNull Project project, @NotNull VcsDirectoryMapping directoryMapping) {
        String dir = directoryMapping.getDirectory();
        if (dir == null || dir.length() <= 0) {
            return ProjectUtil.guessProjectBaseDir(project);
        }
        VirtualFile ret = VcsUtil.getVirtualFile(dir);
        if (ret == null) {
            return ProjectUtil.guessProjectBaseDir(project);
        }
        return ret;
    }
}
