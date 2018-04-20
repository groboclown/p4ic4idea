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

package net.groboclown.p4.server.cache.local;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.config.ClientConfig;
import net.groboclown.p4.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A hybrid local cached file.  The ignore file is stored entirely on the client (it can be in Perforce,
 * but the storage will still be considered local), but we'll treat it like the local cache.
 */
public class IgnoreFiles {
    private static final Logger LOG = Logger.getInstance(IgnoreFiles.class);

    private final String ignoreFileName;


    public IgnoreFiles(@NotNull final ClientConfig clientConfig) {
        this.ignoreFileName = clientConfig.getIgnoreFileName();
    }

    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    public boolean isFileIgnored(@Nullable final FilePath file) {
        if (file == null) {
            return true;
        }
        final FilePath ignoreFile = findApplicableIgnoreFile(file);
        return isMatch(file, ignoreFile);
    }

    private boolean isMatch(@NotNull final FilePath file, @Nullable final FilePath ignoreFile) {
        LOG.debug("Checking ignore status on " + file + " against ignore file " + ignoreFile);
        if (ignoreFile == null || ignoreFile.getVirtualFile() == null || ignoreFile.getParentPath() == null) {
            LOG.debug("Some part of the ignore file is null (" + ignoreFile + "); not a match");
            return false;
        }
        if (! file.isUnder(ignoreFile.getParentPath(), false)) {
            throw new IllegalStateException("incorrect invocation: " + ignoreFile + " is not a parent of " + file);
        }

        String preparedPath = IgnoreFilePattern.preparePath(file, ignoreFile);
        if (preparedPath == null) {
            return false;
        }

        // TODO look at caching these ignore file results.
        // It would mean needing to be aware of file reload events, though.

        try {
            final List<IgnoreFilePattern> patterns = IgnoreFilePattern.parseFile(ignoreFile.getVirtualFile());
            for (IgnoreFilePattern pattern: patterns) {
                if (pattern.matches(preparedPath)) {
                    return pattern.isIgnoreMatchType();
                }
            }
        } catch (IOException e) {
            // problem reading; assume it's not ignored
            LOG.info(e);
            return false;
        }
        return false;
    }


    /**
     * Search up the directory tree for the most applicable p4ignore file.
     *
     * @param file source file
     * @return the ignore file, or {@code null} if it wasn't found
     */
    @Nullable
    private FilePath findApplicableIgnoreFile(@NotNull FilePath file) {
        String ignoreFileName = getIgnoreFileName();
        if (ignoreFileName != null) {
            File prevDir = file.getIOFile();
            File f = prevDir.getParentFile();
            while (f != null && f.isDirectory() && !FileUtil.filesEqual(f, prevDir)) {
                File c = new File(f, ignoreFileName);
                if (c.exists() && c.isFile() && c.canRead()) {
                    return FilePathUtil.getFilePath(c);
                }
                prevDir = f;
                f = f.getParentFile();
            }
        }
        return null;
    }
}
