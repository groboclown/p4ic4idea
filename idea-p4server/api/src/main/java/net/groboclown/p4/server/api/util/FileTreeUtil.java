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

package net.groboclown.p4.server.api.util;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FileTreeUtil {

    /**
     * Break apart the path so that it contains the complete directory chain.  The
     * First element returned is the path object passed in.
     *
     * @param path endpoint path
     * @return list of path directories.
     */
    @NotNull
    public static List<FilePath> getTree(@NotNull FilePath path) {
        List<FilePath> ret = new ArrayList<>();
        FilePath prev;
        FilePath next = path;
        do {
            ret.add(next);
            prev = next;
            next = next.getParentPath();
        } while (next != null && !next.equals(prev));
        return ret;
    }

    /**
     * Break apart the path into parent directories, up to and including the {@literal parent}.
     * If the {@literal parent} is never reached, then this returns null.
     *
     * @param path   child path
     * @param parent parent path
     * @return the paths in the tree, or null if the path is not in the parent.  The returned
     *      paths will always contain at least the path itself, if it is under the parent.
     */
    @Nullable
    public static List<FilePath> getTreeTo(@NotNull FilePath path, @Nullable FilePath parent) {
        if (parent == null) {
            return null;
        }
        List<FilePath> ret = new ArrayList<>();
        FilePath prev;
        FilePath next = path;
        do {
            ret.add(next);
            prev = next;
            next = next.getParentPath();
        } while (next != null && !next.equals(prev) && !parent.equals(prev));
        if (parent.equals(prev) || parent.equals(next)) {
            return ret;
        }
        return null;
    }

    /**
     * Break apart the path into parent directories, up to and including the {@literal parent}.
     * If the {@literal parent} is never reached, then this returns null.
     *
     * @param path   child path
     * @param parent parent path
     * @return the paths in the tree, or null if the path is not in the parent.  The returned
     *      paths will always contain at least the path itself, if it is under the parent.
     */
    @Nullable
    public static List<VirtualFile> getTreeTo(@NotNull VirtualFile path, @Nullable VirtualFile parent) {
        if (parent == null) {
            return null;
        }
        List<VirtualFile> ret = new ArrayList<>();
        VirtualFile prev;
        VirtualFile next = path;
        do {
            ret.add(next);
            prev = next;
            next = next.getParent();
        } while (next != null && !next.equals(prev) && !parent.equals(prev));
        if (parent.equals(prev) || parent.equals(next)) {
            return ret;
        }
        return null;
    }

    /**
     *
     * @param path   child path
     * @param parent parent path
     * @return < 0 if the path is not under the parent, 0 if the path is the parent, or the
     *      number of directories under the parent the path is.
     */
    public static int getPathDepth(@NotNull FilePath path, @Nullable FilePath parent) {
        if (parent == null) {
            return -1;
        }
        if (path.equals(parent)) {
            return 0;
        }
        List<FilePath> tree = getTreeTo(path, parent);
        if (tree == null) {
            return -1;
        }
        return tree.size() - 1;
    }


    /**
     *
     * @param path   child path
     * @param parent parent path
     * @return < 0 if the path is not under the parent, 0 if the path is the parent, or the
     *      number of directories under the parent the path is.
     */
    public static int getPathDepth(@NotNull VirtualFile path, @Nullable VirtualFile parent) {
        if (parent == null) {
            return -1;
        }
        if (path.equals(parent)) {
            return 0;
        }
        List<VirtualFile> tree = getTreeTo(path, parent);
        if (tree == null) {
            return -1;
        }
        return tree.size() - 1;
    }


    /**
     *
     * @param path   child path
     * @param parent parent path
     * @return < 0 if the path is not under the parent, 0 if the path is the parent, or the
     *      number of directories under the parent the path is.
     */
    public static int getPathDepth(@NotNull FilePath path, @Nullable VirtualFile parent) {
        if (parent == null) {
            return -1;
        }
        FilePath parentFile = VcsUtil.getFilePath(parent);
        return getPathDepth(path, parentFile);
    }

    /**
     * Tests if {@literal child} is a child (a sub-directory or sub-file) or the same directory as
     * {@literal parent}
     *
     * @param parent base directory for comparison.
     * @param child  file or directory to check against parent.
     * @return true if child is the same directory as parent, a sub-directory of parent, or a file in parent.
     */
    public static boolean isSameOrUnder(@Nullable FilePath parent, @NotNull FilePath child) {
        // "FilePath.isUnder" has been questionable in its implementation.  Some versions of
        // Idea have a bug in it.

        return getPathDepth(child, parent) >= 0;
    }

    /**
     * Tests if {@literal child} is a child (a sub-directory or sub-file) or the same directory as
     * {@literal parent}
     *
     * @param parent base directory for comparison.
     * @param child  file or directory to check against parent.
     * @return true if child is the same directory as parent, a sub-directory of parent, or a file in parent.
     */
    public static boolean isSameOrUnder(@Nullable VirtualFile parent, @NotNull FilePath child) {
        // "FilePath.isUnder" has been questionable in its implementation.  Some versions of
        // Idea have a bug in it.

        return getPathDepth(child, parent) >= 0;
    }

    @Nullable
    public static FilePath getCommonRoot(@NotNull Collection<FilePath> files) {
        Iterator<FilePath> iter = files.iterator();
        if (! iter.hasNext()) {
            return null;
        }
        FilePath match = iter.next();
        if (match == null || match.getIOFile() == null) {
            return null;
        }
        while (iter.hasNext()) {
            FilePath next = iter.next();
            while (! isSameOrUnder(match, next)) {
                FilePath prev = match;
                match = match.getParentPath();
                if (match == null || match.getIOFile() == null || prev.equals(match)) {
                    return null;
                }
            }
        }
        return match;
    }
}
