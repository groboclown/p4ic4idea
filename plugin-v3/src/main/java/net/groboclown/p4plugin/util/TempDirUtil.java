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
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class TempDirUtil {
    private static final Logger LOG = Logger.getInstance(TempDirUtil.class);

    public static File getTempDir(@Nullable Project project) {
        if (project == null) {
            try {
                return File.createTempFile("p4tempfile", "y");
            } catch (IOException e) {
                LOG.info("Problem getting a temporary file", e);
                throw new RuntimeException(e);
            }
        }
        File tmpDir = P4Vcs.getInstance(project).getTempDir();
        if (! tmpDir.exists()) {
            if (! tmpDir.mkdirs()) {
                LOG.info("Problem creating directory " + tmpDir);
            }
        }
        return tmpDir;
    }
}