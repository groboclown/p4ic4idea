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

package net.groboclown.p4.server.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RootDiscoveryUtil {
    private static final Logger LOG = Logger.getInstance(RootDiscoveryUtil.class);


    /**
     * This does not perform link expansion (get absolute path).  We
     * assume that if you have a file under a path in a link, you want
     * it to be at that location, and not at its real location.
     *
     * @param file file to match against this client's root directories.
     * @return the directory depth at which this file is in the client.  This is the shallowest depth for all
     * the client roots.  It returns -1 if there is no match.
     */
    public static int getFilePathMatchDepth(@NotNull FilePath file,
            @NotNull final Collection<VirtualFile> projectSourceDirs,
            @NotNull final List<VirtualFile> projectClientRoots) {
        final List<File> inputParts = getPathParts(file);

        boolean hadMatch = false;
        int shallowest = Integer.MAX_VALUE;
        for (List<File> rootParts : getRoots(projectSourceDirs, projectClientRoots)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("- checking " + rootParts.get(rootParts.size() - 1));
            }

            if (inputParts.size() < rootParts.size()) {
                // input is at a higher ancestor level than the root parts,
                // so there's no way it could be in this root.

                LOG.debug("-- input is parent of root");

                continue;
            }

            // See if input is under the root.
            // We should be able to just call input.isUnder(configRoot), but
            // that seems to be buggy - it reported that "/a/b/c" was under "/a/b/d".

            final File sameRootDepth = inputParts.get(rootParts.size() - 1);
            if (FileUtil.filesEqual(sameRootDepth, rootParts.get(rootParts.size() - 1))) {
                LOG.debug("-- matched");

                // it's a match.  The input file ancestor path that is
                // at the same directory depth as the config root is the same
                // path.
                if (shallowest > rootParts.size()) {
                    shallowest = rootParts.size();
                    LOG.debug("--- shallowest");
                    hadMatch = true;
                }

                // Redundant - no code after this if block
                //continue;
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("-- not matched " + rootParts
                        .get(rootParts.size() - 1) + " vs " + file + " (" + sameRootDepth + ")");
            }

            // Not under the same path, so it's not a match.  Advance to next root.
        }
        return hadMatch ? shallowest : -1;
    }

    /**
     * The root directories that this perforce client covers in this project.
     * It starts with the client workspace directories, then those are stripped
     * down to just the files in the project, then those are limited by the
     * location of the perforce config directory.
     *
     * @return the actual client root directories used by the workspace,
     * split by parent directories.
     */
    @NotNull
    public static List<List<File>> getRoots(@NotNull final Collection<VirtualFile> projectSourceDirs,
            @NotNull final List<VirtualFile> projectClientRoots) {
        final Set<List<File>> ret = new HashSet<List<File>>();
        List<List<File>> projectRootsParts = new ArrayList<List<File>>(projectSourceDirs.size());
        for (VirtualFile projectRoot : projectSourceDirs) {
            projectRootsParts.add(getPathParts(FilePathUtil.getFilePath(projectRoot)));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("- project roots: " + projectSourceDirs);
            LOG.debug("- client roots: " + projectClientRoots);
        }

        // FIXME This code has a bug - it should not scan up all the parent directories.

        // VfsUtilCore.isAncestor seems to bug out at times.
        // Use the File, File version instead.

        for (VirtualFile root : projectClientRoots) {
            final List<File> rootParts = getPathParts(FilePathUtil.getFilePath(root));
            for (List<File> projectRootParts : projectRootsParts) {
                if (projectRootParts.size() >= rootParts.size()) {
                    // projectRoot could be a child of (or is) root
                    if (FileUtil.filesEqual(
                            projectRootParts.get(rootParts.size() - 1),
                            rootParts.get(rootParts.size() - 1))) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("-- projectRoot " + projectRootParts.get(projectRootParts.size() - 1) +
                                    " child of " + root + ", so using the project root");
                        }
                        ret.add(projectRootParts);
                    }
                } else if (rootParts.size() >= projectRootParts.size()) {
                    // root could be a child of (or is) projectRoot
                    if (FileUtil.filesEqual(
                            projectRootParts.get(projectRootParts.size() - 1),
                            rootParts.get(projectRootParts.size() - 1))) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("-- root " + root +
                                    " child of " + projectRootParts
                                    .get(projectRootParts.size() - 1) + ", so using the root");
                        }
                        ret.add(rootParts);
                    }
                }
            }

            // If it is not in any project root, then ignore it.
        }

        // The list could be further simplified, but this should
        // be sufficient.  (Simplification: remove directories that
        // are children of existing directories in the list)

        return new ArrayList<List<File>>(ret);
    }



    @NotNull
    private static List<File> getPathParts(@NotNull final FilePath child) {
        List<File> ret = new ArrayList<File>();
        FilePath next = child;
        while (next != null) {
            ret.add(next.getIOFile());
            next = next.getParentPath();
        }
        Collections.reverse(ret);
        return ret;
    }

}
