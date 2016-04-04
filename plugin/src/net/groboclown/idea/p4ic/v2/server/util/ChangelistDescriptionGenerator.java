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
package net.groboclown.idea.p4ic.v2.server.util;

import com.intellij.openapi.vcs.changes.ChangeList;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;

public class ChangelistDescriptionGenerator {
    public static String getDescription(@NotNull ChangeList ideaChange) {
        // Bug #91: if both "name" and "comment" are specified, then the changelist will use both
        // the name and the description for the comment.  Instead, this should match the "submit"
        // functionality and only use the comment.

        if (ideaChange.getComment() != null && ideaChange.getComment().length() > 0) {
            return ideaChange.getComment();
        }
        if (ideaChange.getName().length() > 0) {
            return ideaChange.getName();
        }
        return P4Bundle.message("changelist.no-description");
    }
}
