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

package net.groboclown.idea.p4ic.v2.state;

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;

public class P4ClientFile {
    private String depotPath;
    private FilePath localPath;

    void refreshInternalState(@NotNull final String depotPath, @NotNull final FilePath localPath) {
        this.depotPath = depotPath;
        this.localPath = localPath;
    }
}
