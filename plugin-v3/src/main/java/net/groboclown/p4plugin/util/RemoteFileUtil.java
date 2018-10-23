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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RemoteFilePath;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handle the issue where a depot path can show up in the change provider as a relative path to the local
 * file (../../..//depot)
 */
public class RemoteFileUtil {
    private static final Logger LOG = Logger.getInstance(RemoteFileUtil.class);


    @NotNull
    public static FilePath findRelativeRemotePath(@NotNull P4LocalFile local, @Nullable P4RemoteFile remote) {
        if (remote == null) {
            LOG.info("No remote file, so just assuming local file.");
            return local.getFilePath();
        }
        FilePath ret = null;
        if (remote.equals(local.getDepotPath())) {
            ret = local.getFilePath();
        }
        if (ret == null && remote.getLocalPath().isPresent()) {
            ret = VcsUtil.getFilePath(remote.getLocalPath().get());
        }

        // Attempt to construct a relative path.  This is error prone, and just a guess, but should
        // make the UI show up a bit better.
        if (ret == null && local.getClientDepotPath().isPresent()) {
            ret = createRelativePath(local.getFilePath(), local.getClientDepotPath().get(), remote);
        }
        if (ret == null && local.getDepotPath() != null) {
            ret = createRelativePath(local.getFilePath(), local.getDepotPath(), remote);
        }

        if (ret == null) {
            // FIXME This causes a source file location bug.  The UI shows a relative path to the depot path.
            LOG.warn("FIXME returning a remote path, which causes a bad UI relative path");
            ret = new RemoteFilePath(remote.getDisplayName(), false);
        }
        return ret;
    }

    // TODO this seems like duplicate code from elsewhere.  Try to reuse it.
    static List<String> splitDepotPaths(String depot) {
        if (depot.startsWith("//")) {
            depot = depot.substring(2);
        }
        List<String> ret = new ArrayList<>();
        int prev = 0;
        int pos = depot.indexOf('/');
        while (pos < depot.length() && pos >= 0) {
            if (pos - prev > 0) {
                ret.add(depot.substring(prev, pos));
            }
            prev = pos + 1;
            pos = depot.indexOf('/', prev);
        }
        if (prev >= 0 && prev < depot.length()) {
            ret.add(depot.substring(prev));
        }
        return ret;
    }

    private static List<String> splitFilePaths(final FilePath filePath) {
        File[] roots = File.listRoots();
        List<String> revRet = new ArrayList<>();
        FilePath next = filePath;
        while (next != null) {
            String name = next.getName();
            if (name.length() == 0) {
                name = next.getIOFile().getAbsolutePath();
                while (name.endsWith(File.separator)) {
                    name = name.substring(0, name.length() - File.separator.length());
                }
            }
            revRet.add(name);
            if (isRoot(next, roots)) {
                next = null;
            } else {
                next = next.getParentPath();
            }
        }
        Collections.reverse(revRet);
        return revRet;
    }

    static FilePath createRelativePath(@NotNull FilePath localFile, @NotNull P4RemoteFile localDepot,
            P4RemoteFile remote) {
        List<String> localDepotPaths = splitDepotPaths(localDepot.getDisplayName());
        List<String> remoteDepotPaths = splitDepotPaths(remote.getDisplayName());

        // Count depot path parts forward to see up to what part do they differ.
        // Then, the distance from that point to the end of the local depot paths is the
        // stuff to cut out of the local file paths, and add on after that the parts after the
        // shared remote depot paths.

        int diffPos = 0;
        while (diffPos < localDepotPaths.size() && diffPos < remoteDepotPaths.size()
                && localDepotPaths.get(diffPos).equals(remoteDepotPaths.get(diffPos))) {
            diffPos++;
        }
        if (diffPos <= 0) {
            // Everything is different
            if (LOG.isDebugEnabled()) {
                LOG.debug("No similarities between " + localDepot + " and " + remote.getDepotPath());
            }
            return null;
        }
        // We now have the number of shared path parts between local and remote.
        // We then count backwards from the end of the local depot path to find
        // what is the base path.
        List<String> localFilePaths = splitFilePaths(localFile);
        int localRelativeSize =
                // The source we're trying to find the final length of
                localFilePaths.size() - (
                    // The relative difference path, think of this as a ".."
                    localDepotPaths.size() - diffPos
                );
        // and pop those off of the paths...
        while (localFilePaths.size() > localRelativeSize) {
            localFilePaths.remove(localFilePaths.size() - 1);
        }

        // Now we stick on the different paths for the remote.
        for (int i = diffPos; i < remoteDepotPaths.size(); i++) {
            localFilePaths.add(remoteDepotPaths.get(i));
        }

        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (String part : localFilePaths) {
            if (first) {
                first = false;
            } else {
                ret.append(File.separator);
            }
            ret.append(part);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Transformed local " + localDepot + " + remote " + remote + " -> " + ret);
        }
        return VcsUtil.getFilePath(ret.toString());
    }

    private static boolean isRoot(FilePath file, File[] roots) {
        if (file == null) {
            return true;
        }
        File f = file.getIOFile();
        for (File root : roots) {
            if (FileUtil.filesEqual(root, f)) {
                return true;
            }
        }
        return false;
    }
}
