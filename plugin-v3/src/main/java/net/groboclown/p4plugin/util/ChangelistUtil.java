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
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangelistDescriptionGenerator {
    /**
     * Turn an IDE changelist name + description into a Perforce changelist comment.
     *
     * @param project project
     * @param ideaChange IDE change list.
     * @return comment for Perforce changelist.
     */
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


    /**
     * Used to construct a simple IDE change list name from a Perforce changelist.  This does not guarantee uniqueness.
     *
     * @param changelist p4 changelist
     * @return a suitable "name" for the IDE's change list.
     */
    @NotNull
    public static String convertP4ChangelistCommentToIdeName(@NotNull P4LocalChangelist changelist, int maxLength) {
        return getPrefix(changelist, maxLength);
    }


    public static List<LocalChangeList>


    @NotNull
    public static String createUniqueIdeChangeListName(@NotNull P4LocalChangelist changelist,
            @NotNull List<LocalChangeList> existingLocalChangeLists) {
        // New way - just slap the cl # on the end.
        // Maybe use message properties.  However, this must match up with the REGEX below, so probably not.
        String suffix = " (" + changelist.getChangelistId().getChangelistId() + ")";
        String prefix = getPrefix(changelist, CHANGELIST_NAME_LENGTH - suffix.length());
        return prefix + suffix;

        /* Old way - caused issues.
        String newName = getPrefix(changelist, CHANGELIST_NAME_LENGTH);
        int index = -1;

        match_outer_loop:
        while (true) {
            for (LocalChangeList lcl : existingLocalChangeLists) {
                if (!lcl.equals(currentChangeList) && newName.equals(lcl.getName())) {
                    index++;
                    // Should use message properties
                    String count = " (" + index + ')';
                    newName = getPrefix(changelist, CHANGELIST_NAME_LENGTH - count.length()) + count;
                    continue match_outer_loop;
                }
            }
            return newName;
        }
        */
    }

    private static String getPrefix(@NotNull P4LocalChangelist changelist, int characterCount) {
        String ret = changelist.getComment();
        if (ret.length() > characterCount) {
            ret = ret.substring(0, characterCount - 3) + "...";
        }
        return ret;
    }

    private static final Pattern CL_INDEX_SUFFIX = Pattern.compile("^\\s*(.*?)\\s\\(\\d+)\\s*$");

    @NotNull
    private static String createP4ChangelistDescription(LocalChangeList ideChangeList) {
        String name = ideChangeList.getName();
        String desc = ideChangeList.getComment();
        if (desc != null) {
            desc = desc.trim();
            if (!UserProjectPreferences.getConcatenateChangelistNameComment(project) &&
                    !desc.isEmpty()) {
                return desc;
            }
        }
        Matcher m1 = CL_INDEX_SUFFIX.matcher(name);
        if (m1.matches()) {
            name = m1.group(1);
        }
        if (name.endsWith("...")) {
            name = name.substring(0, name.length() - 3);
        }
        name = name.trim();
        if (desc == null || desc.isEmpty()) {
            return name;
        }
        desc = desc.trim();
        if (desc.startsWith(name)) {
            return desc;
        }
        if (!name.endsWith(".")) {
            name += '.';
        }
        return name + "  " + desc;
    }

}
