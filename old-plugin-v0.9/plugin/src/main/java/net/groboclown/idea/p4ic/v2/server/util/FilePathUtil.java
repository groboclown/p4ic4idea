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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    @Nullable
    public static FilePath getFilePath(@Nullable IFileSpec spec) {
        if (spec == null) {
            return null;
        }
        if (spec.getClientPathString() != null) {
            return FilePathUtil.getFilePath(spec.getClientPathString());
        }
        // TODO this is kind of a hack
        if (spec.getOriginalPathString() != null && ! spec.getOriginalPathString().startsWith("//")) {
            return FilePathUtil.getFilePath(spec.getClientPathString());
        }
        return null;
    }

    @NotNull
    public static FilePath getFilePath(@NotNull final File f) {
        try {
            return VcsUtil.getFilePath(f);
        } catch (Exception ex) {
            // This can happen when in unit test mode
            LOG.debug("VcsUtil.getFilePath raised an exception for " + f, ex);
            return VcsCompat.getInstance().getLowLevelFilePath(f);
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

    @Nullable
    public static FilePath getNullableFilePath(@Nullable final VirtualFile f) {
        if (f == null) {
            return null;
        }
        return getFilePath(f);
    }

    @NotNull
    public static Collection<FilePath> getFilePathsForVirtualFiles(@NotNull Collection<VirtualFile> virtualFiles) {
        List<FilePath> ret = new ArrayList<FilePath>(virtualFiles.size());
        for (VirtualFile virtualFile : virtualFiles) {
            if (virtualFile != null) {
                ret.add(getFilePath(virtualFile));
            }
        }
        return ret;
    }

    @NotNull
    public static Collection<FilePath> getFilePathsForStrings(@NotNull List<String> paths) {
        List<FilePath> ret = new ArrayList<FilePath>(paths.size());
        for (String path: paths) {
            if (path != null) {
                ret.add(getFilePath(path));
            }
        }
        return ret;
    }

    @NotNull
    public static List<String> toStringList(@Nullable Collection<FilePath> files) {
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<String>(files.size());
        for (FilePath file : files) {
            if (file == null) {
                ret.add(null);
            } else {
                ret.add(file.getIOFile().getAbsolutePath());
            }
        }
        return ret;
    }

    /**
     * Break apart the path so that it contains the complete directory chain.  The
     * First element returned is the path object passed in.
     *
     * @param path endpoint path
     * @return list of path directories.
     */
    @NotNull
    public static List<FilePath> getTree(@NotNull FilePath path) {
        List<FilePath> ret = new ArrayList<FilePath>();
        FilePath prev;
        FilePath next = path;
        do {
            ret.add(next);
            prev = next;
            next = next.getParentPath();
        } while (next != null && ! next.equals(prev));
        return ret;
    }

    /**
     * Break apart the path into parent directories, up to and including the {@literal parent}.
     * If the {@literal parent} is never reached, then the complete tree is returned.
     *
     * @param path child path
     * @param parent parent path
     * @return
     */
    @NotNull
    public static List<FilePath> getTreeTo(@NotNull FilePath path, @Nullable FilePath parent) {
        List<FilePath> ret = new ArrayList<FilePath>();
        FilePath prev;
        FilePath next = path;
        do {
            ret.add(next);
            prev = next;
            next = next.getParentPath();
        } while (next != null && ! next.equals(prev) && ! prev.equals(parent));
        return ret;
    }

    public static List<FilePath> getTreeTo(@NotNull VirtualFile path, @Nullable VirtualFile parent) {
        return getTreeTo(getFilePath(path), getNullableFilePath(parent));
    }

    /**
     * Tests if {@literal child} is a child (a sub-directory or sub-file) or the same directory as
     * {@literal parent}
     *
     * @param parent base directory for comparison.
     * @param child file or directory to check against parent.
     * @return true if child is the same directory as parent, a sub-directory of parent, or a file in parent.
     */
    public static boolean isSameOrUnder(@NotNull FilePath parent, @NotNull FilePath child) {
        // "FilePath.isUnder" has been questionable in its implementation.  Some versions of
        // Idea have a bug in it.

        List<FilePath> paths = getTreeTo(child, parent);
        return ! paths.isEmpty() && parent.equals(paths.get(paths.size() - 1));
    }

    /**
     * Tests if {@literal child} is a child (a sub-directory or sub-file) or the same directory as
     * {@literal parent}
     *
     * @param parent base directory for comparison.
     * @param child file or directory to check against parent.
     * @return true if child is the same directory as parent, a sub-directory of parent, or a file in parent.
     */
    public static boolean isSameOrUnder(@NotNull VirtualFile parent, @NotNull VirtualFile child) {
        return isSameOrUnder(getFilePath(parent), getFilePath(child));
    }


    /**
     *
     * @param project project invoking
     * @param files files to make writable
     */
    public static void makeWritable(@NotNull final Project project, @NotNull final VirtualFile[] files) {
        // write actions can only be in the dispatch thread.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    for (VirtualFile file : files) {
                        innerMakeWritable(project, file);
                    }
                }
            });
        } else {
            for (VirtualFile file : files) {
                innerMakeWritable(project, file);
            }
        }
    }

    /**
     * @param project project invoking
     * @param file file to make writable
     * @return true if there was no failure on changing the state, or false if there was a failure.  Only local files
     * that are not writable will have this action be run; all other virtual files will cause this to return true.
     */
    public static void makeWritable(@NotNull final Project project, @NotNull final FilePath file) {
        // write actions can only be in the dispatch thread.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    innerMakeWritable(project, file);
                }
            });
        } else {
            innerMakeWritable(project, file);
        }
    }


    private static boolean innerMakeWritable(@NotNull final Project project, @NotNull final FilePath file) {
        VirtualFile vf = file.getVirtualFile();
        if (vf != null) {
            if (innerMakeWritable(project, vf)) {
                return true;
            }
        }
        File f = file.getIOFile();
        if (f.isFile() && f.exists() && !f.canWrite()) {
            if (!f.setWritable(false)) {
                LOG.info("Problem making writable: " + file);
                AlertManager.getInstance().addWarning(project,
                        P4Bundle.message("error.writable.failed.title"),
                        P4Bundle.message("error.writable.failed", file),
                        null, file);
                return false;
            }
        }
        return true;
    }

    /**
     * @param project project invoking
     * @param file file to make writable
     * @return true if there was no failure on changing the state, or false if there was a failure.  Only local files
     * that are not writable will have this action be run; all other virtual files will cause this to return true.
     */
    private static boolean innerMakeWritable(@NotNull final Project project, @NotNull final VirtualFile file) {
        if (file.exists() && !file.isDirectory() && file.isInLocalFileSystem() &&
                !file.isWritable()) {
            try {
                file.setWritable(true);
                return true;
            } catch (IOException e) {
                LOG.info("Problem making writable: " + file, e);
                AlertManager.getInstance().addWarning(project,
                        P4Bundle.message("error.writable.failed.title"),
                        P4Bundle.message("error.writable.failed", file),
                        null, new VirtualFile[]{file});

                // Keep going with the action, even though we couldn't set it to
                // writable...
                return false;
            }
        }
        // Return true here, to indicate that there was no failure.
        return true;
    }
}
