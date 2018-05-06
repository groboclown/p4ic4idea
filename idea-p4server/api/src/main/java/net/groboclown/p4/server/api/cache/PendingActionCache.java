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

package net.groboclown.p4.server.api.cache;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;

public interface PendingActionCache {
    void addPendingAction(@NotNull FilePath sourceFile, @NotNull P4FileAction action);
    void addPendingAction(@NotNull FilePath sourceFile, @NotNull P4FileAction action, @NotNull P4FileType newFileType);
    void addPendingChangelistAction(@NotNull P4ChangelistId changelistId, @NotNull PendingChangelistAction action);
}
