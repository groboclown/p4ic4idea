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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RemoteFilePath;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handle the issue where a depot path can show up in the change provider as a relative path to the local
 * file (../../..//depot)
 */
public class RemoteFileUtil {
    @Nullable
    public static FilePath findRelativeRemotePath(@NotNull P4LocalFile local, @Nullable P4RemoteFile remote) {
        if (remote == null) {
            return null;
        }
        if (remote.equals(local.getDepotPath())) {
            return local.getFilePath();
        }

        // FIXME this causes a source file location bug.  The UI shows a relative path to the depot path.
        return new RemoteFilePath(remote.getDisplayName(), false);
    }
}
