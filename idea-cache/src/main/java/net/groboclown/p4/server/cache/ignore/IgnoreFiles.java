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

package net.groboclown.p4.server.cache.ignore;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A hybrid local cached file.  The ignore file is stored entirely on the client (it can be in Perforce,
 * but the storage will still be considered local), but we'll treat it like the local cache.
 */
public class IgnoreFiles {
    private static final Logger LOG = Logger.getInstance(IgnoreFiles.class);

    private final String ignoreFileName;

    public IgnoreFiles(@Nullable final String ignoreFileName) {
        this.ignoreFileName = ignoreFileName;
    }

    @Nullable
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    @Nullable
    public VirtualFile getIgnoreFileForPath(@Nullable final VirtualFile path) {
        if (path == null) {
            return null;
        }
        String ignoreFileName = getIgnoreFileName();
        if (ignoreFileName != null) {
            VirtualFile prevDir = path;
            VirtualFile f = prevDir.getParent();
            while (f != null && f.isDirectory() && !f.equals(prevDir)) {
                VirtualFile ignoreFile = f.findChild(ignoreFileName);
                if (ignoreFile != null && ignoreFile.exists() && !ignoreFile.isDirectory()) {
                    return ignoreFile;
                }
                prevDir = f;
                f = f.getParent();
            }
        }
        return null;
    }

    public boolean isFileIgnored(@Nullable final FilePath file) {
        if (file == null || file.getVirtualFile() == null) {
            return true;
        }
        final VirtualFile ignoreFile = getIgnoreFileForPath(file.getVirtualFile());
        return isMatch(file, ignoreFile);
    }

    private boolean isMatch(@NotNull final FilePath file, @Nullable final VirtualFile ignoreFile) {
        LOG.debug("Checking ignore status on " + file + " against ignore file " + ignoreFile);
        if (ignoreFile == null || ignoreFile.isDirectory()) {
            return false;
        }
        VirtualFile vf = file.getVirtualFile();
        if (vf == null) {
            return false;
        }

        // TODO look at caching these ignore file results.
        // It would mean needing to be aware of file reload events, though.

        try {
            final IgnoreFileSet patterns = IgnoreFileSet.create(ignoreFile);
            return patterns.isCoveredByIgnoreFile(vf) && patterns.isIgnored(vf);
        } catch (IOException e) {
            // problem reading; assume it's not ignored
            LOG.info(e);
            return false;
        }
    }
}
