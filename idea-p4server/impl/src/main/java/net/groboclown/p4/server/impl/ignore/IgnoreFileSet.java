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

package net.groboclown.p4.server.impl.ignore;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Checks if a file is ignored by scanning the {@link IgnoreFilePattern} list for each
 * line from an ignore file.
 */
public class IgnoreFileSet {
    private final VirtualFile ignoreDir;
    private final List<IgnoreFilePattern> ignorePatterns;

    static IgnoreFileSet create(@NotNull VirtualFile ignoreFile)
            throws IOException {
        try (InputStreamReader reader = new InputStreamReader(ignoreFile.getInputStream(), ignoreFile.getCharset())) {
            return new IgnoreFileSet(ignoreFile, IgnoreFilePattern.parseFile(reader));
        }
    }

    private IgnoreFileSet(@NotNull VirtualFile ignoreFile, @NotNull List<IgnoreFilePattern> ignorePatterns) {
        this.ignoreDir = ignoreFile.getParent();
        if (ignoreDir == null || ! ignoreDir.isDirectory()) {
            throw new IllegalArgumentException("ignore file has no parent (" + ignoreFile + ")");
        }
        this.ignorePatterns = Collections.unmodifiableList(new ArrayList<>(ignorePatterns));
    }

    /**
     *
     * @param tested file to check
     * @return true if the ignore file is in a parent directory of the argument.
     */
    public boolean isCoveredByIgnoreFile(@Nullable VirtualFile tested) {
        return tested != null && !relativeToIgnoreDir(tested).isEmpty();
    }

    public boolean isIgnored(@Nullable VirtualFile tested) {
        if (tested == null) {
            return true;
        }
        List<String> paths = relativeToIgnoreDir(tested);
        if (paths.isEmpty()) {
            // Not in the same directory as the ignoreDir, so skip it.
            return false;
        }

        // If the file matches a not-ignored pattern, then immediately return false.
        // If the file matches a is-ignored pattern, then immediately return true.
        // If the file does not match any pattern, then return false.
        for (IgnoreFilePattern pattern : ignorePatterns) {
            if (pattern.matches(paths)) {
                return pattern.isIgnoreMatchType();
            }
        }
        return false;
    }

    @NotNull
    private List<String> relativeToIgnoreDir(@NotNull final VirtualFile source) {
        List<String> ret = new ArrayList<>();
        VirtualFile vf = source;
        VirtualFile prev = null;
        while (vf != null && !vf.equals(prev) && !vf.equals(ignoreDir)) {
            prev = vf;
            ret.add(0, vf.getName());
            vf = vf.getParent();
        }
        if (vf == null || vf.equals(prev) || !vf.equals(ignoreDir)) {
            return Collections.emptyList();
        }
        return ret;
    }
}
