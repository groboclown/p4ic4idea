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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class FilePathUtil {
    private static final Logger LOG = Logger.getInstance(FilePathUtil.class);

    private FilePathUtil() {
        // utility class
    }

    @Nullable
    public static FilePath getFilePath(@Nullable String path) {
        if (path == null) {
            return null;
        }
        return getFilePath(new File(path));
    }

    @NotNull
    public static FilePath getFilePath(@NotNull final File f) {
        try {
            return VcsUtil.getFilePath(f);
        } catch (Exception ex) {
            // This can happen when in unit test mode
            LOG.debug("VcsUtil.getFilePath raised an exception for " + f, ex);
            return new FilePathImpl(f, f.isDirectory());
        }
    }

    @NotNull
    public static FilePath getFilePath(@NotNull final VirtualFile f) {
        try {
            return VcsUtil.getFilePath(f);
        } catch (Exception ex) {
            // This can happen when in unit test mode
            LOG.debug("VcsUtil.getFilePath raised an exception for " + f, ex);
            return new FilePathImpl(f);
        }
    }
}
