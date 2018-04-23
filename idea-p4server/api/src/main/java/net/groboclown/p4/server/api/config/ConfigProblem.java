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

package net.groboclown.p4.server.api.config;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigProblem implements Comparable<ConfigProblem> {
    @Nullable
    public abstract VirtualFile getRootPath();

    @Nullable
    public abstract ConfigPart getSource();

    @NonNls
    public abstract String getMessage();

    /**
     *
     * @return true if an error, false if a warning.
     */
    public abstract boolean isError();

    @Override
    public String toString() {
        return "problem(" + getSource() + ": " + getMessage() + ")";
    }

    @NotNull
    protected static String cleanMessage(@NotNull Exception ex) {
        String baseMsg = ex.getLocalizedMessage();
        if (baseMsg == null) {
            baseMsg = ex.getMessage();
        }
        if (baseMsg == null) {
            return reduceCamelCase(ex.getClass().getSimpleName());
        }
        if (baseMsg.startsWith(ex.getClass().getName())) {
            baseMsg = baseMsg.substring(ex.getClass().getName().length());
            if (baseMsg.startsWith(":")) {
                baseMsg = baseMsg.substring(1);
            }
            baseMsg = baseMsg.trim();
        }
        if (baseMsg.isEmpty() || "null".equals(baseMsg)) {
            return reduceCamelCase(ex.getClass().getSimpleName());
        }

        // Strip off the %' and '% from the Perforce message.
        int p0 = 0;
        do {
            int p1 = baseMsg.indexOf("%'", p0);
            int p2 = baseMsg.indexOf("'%", p0);
            if (p1 > p0 && p2 > p1) {
                // Strip out the % from the escaping.
                baseMsg = baseMsg.substring(0, p1)
                        + baseMsg.substring(p1 + 1, p2 + 1)
                        + baseMsg.substring(p2 + 2);
                p0 = p2 + 2;
            } else {
                p0 = -1;
            }
        } while (p0 > 0);
        return baseMsg;
    }

    protected static String reduceCamelCase(@NotNull String text) {
        StringBuilder sb = new StringBuilder();
        boolean inCaps = false;
        boolean whitespace = false;
        for (char c : text.toCharArray()) {
            boolean nextCaps = Character.isUpperCase(c);
            if (! inCaps && nextCaps && sb.length() > 0 && ! whitespace) {
                // switched from lowercase to uppercase
                sb.append(' ');
            }
            sb.append(Character.toLowerCase(c));
            inCaps = nextCaps;
            whitespace = Character.isWhitespace(c);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(@Nonnull ConfigProblem that) {
        if (that.isError() && ! isError()) {
            return -1;
        }
        if (isError() && !that.isError()) {
            return 1;
        }

        // Don't use equals here, because we are performing
        // a null check.
        //noinspection UseVirtualFileEquals
        if (that.getRootPath() == getRootPath()) {
            return 0;
        }
        if (getRootPath() == null) {
            return 1;
        }
        if (that.getRootPath() == null) {
            return -1;
        }
        return getRootPath().getPath().compareTo(that.getRootPath().getPath());
    }

    /**
     * All that matters for message equality is what the end user sees.  The end user
     * cares about whether the error message is the same, not the internal details.
     *
     * @param problems the list of problems to collate.
     * @return problems sorted by the message.  For each message text, the problems themselves are
     *      sorted.  The natural sort order means that error problems come before non-errors.
     */
    public static Map<String, List<ConfigProblem>> collate(Collection<ConfigProblem> problems) {
        Map<String, List<ConfigProblem>> ret = new HashMap<>();
        for (ConfigProblem problem : problems) {
            String msg = problem.getMessage();
            List<ConfigProblem> msgProblems = ret.computeIfAbsent(msg, k -> new ArrayList<>());
            msgProblems.add(problem);
        }
        for (List<ConfigProblem> configProblems : ret.values()) {
            Collections.sort(configProblems);
        }
        return ret;
    }
}
