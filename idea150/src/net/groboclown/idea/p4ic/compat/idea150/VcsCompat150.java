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

package net.groboclown.idea.p4ic.compat.idea150;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.VcsUserRegistry;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

public class VcsCompat150 extends VcsCompat {
    @Override
    public void setupPlugin(@NotNull Project project) {
        ServiceManager.getService(project, VcsUserRegistry.class); // make sure to read the registry before opening commit dialog
    }

    @Override
    public void refreshFiles(@NotNull final Project project, final Collection<VirtualFile> affectedFiles) {
        // do nothing - not supported in Idea 15
    }

    @Override
    public FilePath getLowLevelFilePath(final File file) {
        return new LocalFilePath(file.getAbsolutePath(), file.isDirectory());
    }
}
