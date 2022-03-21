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
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangelistUtil {
    private static final Logger LOG = Logger.getInstance(ChangelistUtil.class);
    private static final Pattern CL_INDEX_SUFFIX = Pattern.compile("^\\s*(.*?)\\s\\(\\d+\\)\\s*$");

    /**
     * Turn an IDE changelist name + description into a Perforce changelist comment.
     *
     * @param project project
     * @param ideChangeList IDE change list.
     * @return comment for Perforce changelist.
     */
    @NotNull
    public static String createP4ChangelistDescription(@Nullable Project project, @NotNull ChangeList ideChangeList) {
        String name = ideChangeList.getName();
        String desc = ideChangeList.getComment();
        if (desc != null) {
            desc = desc.trim();
            if (!UserProjectPreferences.getConcatenateChangelistNameComment(project) &&
                    !desc.isEmpty()) {
                return desc;
            }
        }

        // Strip off any mechanically generated suffix.
        Matcher m1 = CL_INDEX_SUFFIX.matcher(name);
        if (m1.matches()) {
            name = m1.group(1);
        }
        if (name.endsWith("...")) {
            name = name.substring(0, name.length() - 3);
        }
        name = name.trim();

        if (desc == null || desc.isEmpty()) {
            if (name.isEmpty()) {
                return P4Bundle.message("changelist.no-description");
            }
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

    @NotNull
    public static String createUniqueIdeChangeListName(@NotNull P4LocalChangelist changelist,
            @Nullable LocalChangeList mappedChangeList,
            @NotNull List<LocalChangeList> existingLocalChangeLists, int maxLength) {
        String newName = getPrefix(changelist, maxLength);
        int index = -1;

        match_outer_loop:
        while (true) {
            for (LocalChangeList lcl : existingLocalChangeLists) {
                if (!lcl.equals(mappedChangeList) && newName.equals(lcl.getName())) {
                    index++;
                    // Should use message properties
                    String count = " (" + index + ')';
                    newName = getPrefix(changelist, maxLength - count.length()) + count;
                    continue match_outer_loop;
                }
            }
            return newName;
        }
    }

    private static String getPrefix(@NotNull P4LocalChangelist changelist, int characterCount) {
        String ret = changelist.getComment();
        if (ret.length() > characterCount) {
            ret = ret.substring(0, characterCount - 3) + "...";
        }
        return ret;
    }

    @NotNull
    public static Map<ClientServerRef, P4ChangelistId> getActiveChangelistIds(@NotNull Project project) {
        LocalChangeList defaultIdeChangeList =
                ChangeListManager.getInstance(project).getDefaultChangeList();
        Map<ClientServerRef, P4ChangelistId> ret = new HashMap<>();
        try {
            CacheComponent.getInstance(project).getServerOpenedCache().first
                    .getP4ChangesFor(defaultIdeChangeList)
                    .forEach((id) -> ret.put(id.getClientServerRef(), id));
        } catch (InterruptedException e) {
            InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(e)));
        }
        return ret;
    }

    @NotNull
    public static P4ChangelistId getActiveChangelistFor(@NotNull RootedClientConfig root,
            @NotNull Map<ClientServerRef, P4ChangelistId> ids) {
        ClientServerRef ref = root.getClientConfig().getClientServerRef();
        P4ChangelistId ret = ids.get(ref);
        if (ret == null) {
            ret = P4ChangelistIdImpl.createDefaultChangelistId(ref);
            ids.put(ref, ret);
        }
        return ret;
    }
}
