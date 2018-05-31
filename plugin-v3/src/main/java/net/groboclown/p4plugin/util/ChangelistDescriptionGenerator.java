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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeList;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangelistDescriptionGenerator {
    public static String getDescription(@Nullable Project project, @NotNull ChangeList ideaChange) {
        // Old behavior which isn't really that good.
        if (UserProjectPreferences.getConcatenateChangelistNameComment(project)) {
            StringBuilder sb = new StringBuilder();
            if (ideaChange.getName().length() > 0) {
                sb.append(ideaChange.getName());
            }
            if (ideaChange.getComment() != null && ideaChange.getComment().length() > 0) {
                String comment = ideaChange.getComment();
                if (sb.length() > 0) {


                    String head = sb.toString();
                    if (head.endsWith("...")) {
                        head = head.substring(0, head.length() - 3);
                    }
                    if (head.trim().equalsIgnoreCase(comment)) {
                        // Just use the comment, not the name + comment.
                        sb.setLength(0);
                    } else {
                        sb.append("\n\n");
                    }
                }
                sb.append(ideaChange.getComment());
            }
            return sb.toString();
        }

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
