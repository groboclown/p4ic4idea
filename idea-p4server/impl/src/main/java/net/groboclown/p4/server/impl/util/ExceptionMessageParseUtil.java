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

package net.groboclown.p4.server.impl.util;

import org.jetbrains.annotations.NotNull;

public class ExceptionMessageParseUtil {

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
}
