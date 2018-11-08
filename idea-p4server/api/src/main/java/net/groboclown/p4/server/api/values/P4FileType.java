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

package net.groboclown.p4.server.api.values;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Immutable
public class P4FileType {
    private static final P4FileType UNKNOWN = new P4FileType(BaseType.UNKNOWN, -1, Collections.emptyList(), KeywordExpansion.NONE);

    public BaseType getBaseType() {
        return baseType;
    }

    public enum BaseType {
        TEXT,
        BINARY,
        APPLE,
        RESOURCE,
        SYMLINK,
        UNICODE,
        UTF16,
        UNKNOWN
    }

    public enum Modifier {
        STORED_COMPRESSED('C'),
        STORED_DELTAS('D'),
        STORED_FULL('F'),
        EXCLUSIVE_LOCKING('l'),
        PRESERVE_ORIGINAL_TIME('m'),
        ALWAYS_WRITABLE('w'),
        EXECUTABLE('x');

        private final char key;

        Modifier(char key) {
            this.key = key;
        }
    }

    public enum KeywordExpansion {
        NONE(""),
        FULL("k"),
        LIMITED("ko");

        private final String key;

        KeywordExpansion(String key) {
            this.key = key;
        }
    }

    private final BaseType baseType;
    private final int recentRevisionsStored;
    private final Collection<Modifier> modifiers;
    private final KeywordExpansion keywordExpansion;
    private final String display;

    private P4FileType(BaseType baseType, int recentRevisionsStored,
            List<Modifier> modifiers, KeywordExpansion keywordExpansion) {
        this.baseType = baseType;
        this.recentRevisionsStored = recentRevisionsStored;
        this.modifiers = Collections.unmodifiableCollection(new ArrayList<>(modifiers));
        this.keywordExpansion = keywordExpansion;

        StringBuilder sb = new StringBuilder(baseType.toString().toLowerCase());
        if (recentRevisionsStored > 0 || !modifiers.isEmpty() || keywordExpansion != KeywordExpansion.NONE) {
            sb.append('+')
                .append(keywordExpansion.key);
            for (Modifier modifier : modifiers) {
                sb.append(modifier.key);
            }
            if (recentRevisionsStored == 1) {
                sb.append('S');
            } else if (recentRevisionsStored > 1) {
                sb.append('S').append(recentRevisionsStored);
            }
        }
        this.display = new String(sb);
    }

    private P4FileType(String unknown, int recentRevisionsStored,
            List<Modifier> modifiers, KeywordExpansion keywordExpansion) {
        this.baseType = BaseType.UNKNOWN;
        this.recentRevisionsStored = recentRevisionsStored;
        this.modifiers = Collections.unmodifiableCollection(new ArrayList<>(modifiers));
        this.keywordExpansion = keywordExpansion;
        this.display = unknown;
    }

    public String toString() {
        return display;
    }

    @Nullable
    public static P4FileType convertNullable(@Nullable String fileType) {
        if (fileType == null) {
            return null;
        }
        return convert(fileType);
    }

    @NotNull
    public static P4FileType convert(@Nullable final String fileType) {
        if (fileType == null) {
            return UNKNOWN;
        }
        List<Modifier> modifiers = new ArrayList<>();
        KeywordExpansion keywordExpansion = KeywordExpansion.NONE;
        int revsStored = -1;

        final String ft;
        final int plusPos = fileType.indexOf('+');
        if (plusPos > 0) {
            final String modStr = fileType.substring(plusPos + 1);
            for (Modifier mod : Modifier.values()) {
                if (modStr.indexOf(mod.key) >= 0) {
                    modifiers.add(mod);
                }
            }
            ft = fileType.substring(0, plusPos).toLowerCase();
        } else {
            ft = fileType.toLowerCase();
        }

        if ("apple".equals(ft)) {
            return new P4FileType(BaseType.APPLE, revsStored, modifiers, keywordExpansion);
        }
        if ("binary".equals(ft)) {
            return new P4FileType(BaseType.BINARY, revsStored, modifiers, keywordExpansion);
        }
        if ("resource".equals(ft)) {
            return new P4FileType(BaseType.RESOURCE, revsStored, modifiers, keywordExpansion);
        }
        if ("symlink".equals(ft)) {
            return new P4FileType(BaseType.SYMLINK, revsStored, modifiers, keywordExpansion);
        }
        if ("text".equals(ft)) {
            return new P4FileType(BaseType.TEXT, revsStored, modifiers, keywordExpansion);
        }
        if ("unicode".equals(ft)) {
            return new P4FileType(BaseType.UNICODE, revsStored, modifiers, keywordExpansion);
        }
        if ("utf16".equals(ft)) {
            return new P4FileType(BaseType.UTF16, revsStored, modifiers, keywordExpansion);
        }
        return new P4FileType(fileType, revsStored, modifiers, keywordExpansion);
    }
}
