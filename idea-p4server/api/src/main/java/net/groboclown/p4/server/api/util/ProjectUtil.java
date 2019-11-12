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

package net.groboclown.p4.server.api.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ProjectUtil {
    /**
     * Some APIs need a root directory.  For the most part, they should be able to find it
     * relative to some directory, but after searching it may just need a project-relative
     * base directory to show.
     *
     * Replaces the deprecated call to Project.getBaseDir()
     *
     * @return
     */
    @NotNull
    public static VirtualFile guessProjectBaseDir(@NotNull Project project) {
        VirtualFile ret = findProjectBaseDir(project);
        if (ret == null) {
            throw new IllegalStateException("null project base directory");
        }
        return ret;
    }


    @Nullable
    public static VirtualFile findProjectBaseDir(@NotNull Project project) {
        String projectDir = project.getBasePath();
        if (projectDir == null) {
            projectDir = SystemProperties.getUserHome();
            if (projectDir == null) {
                projectDir = "/";
            }
        }
        return VfsUtil.findFileByIoFile(new File(projectDir), false);
    }
}
