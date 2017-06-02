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

package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.Nullable;

public class ShelveChange extends Change {
    public ShelveChange(@Nullable ContentRevision beforeRevision,
            @Nullable ContentRevision afterRevision,
            @Nullable FileStatus fileStatus) {
        super(beforeRevision, afterRevision, fileStatus);
    }


    public String getOriginText(final Project project) {
        return P4Bundle.getString("change.shelve.origin");
    }
}