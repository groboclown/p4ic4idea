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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pushes changes FROM Perforce INTO idea.  No Perforce jobs will be altered.
 * <p/>
 * If there was an IDEA changelist that referenced a Perforce changelist that
 * has since been submitted or deleted, the IDEA changelist will be removed,
 * and any contents will be moved into the default changelist.
 * <p/>
 * If there is a Perforce changelist that has no mapping to IDEA, an IDEA
 * change list is created.
 */
public class P4ChangeProvider
        implements ChangeProvider {
    private static final Logger LOG = Logger.getInstance(P4ChangeProvider.class);

    private final Project project;
    private final P4Vcs vcs;

    public P4ChangeProvider(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
        this.vcs = vcs;
    }

    @Override
    public boolean isModifiedDocumentTrackingRequired() {
        // editing a file requires opening the file for edit or add, and thus changing its dirty state.
        return true;
    }

    @Override
    public void doCleanup(List<VirtualFile> files) {
        // clean up the working copy.
        // Nothing to do?
        LOG.info("Cleanup called for  " + files);
    }

    @Override
    public void getChanges(@NotNull VcsDirtyScope dirtyScope,
            @NotNull ChangelistBuilder builder,
            @NotNull ProgressIndicator progress,
            @NotNull ChangeListManagerGate addGate) throws VcsException {
        if (project.isDisposed()) {
            return;
        }

        // This check is kind of necessary.  It's ensuring that IntelliJ is doing the right thing.
        // Note the identity equals, not .equals().
        if (dirtyScope.getVcs() != vcs) {
            throw new VcsException(P4Bundle.message("error.vcs.dirty-scope.wrong"));
        }

        progress.setFraction(0.0);

        // How this is called by IntelliJ:
        // IntelliJ calls this method on updates to the files or changes;
        // not for all the files or changes, but just the ones that are
        // in the dirty scope.  The method is expected to only categorize
        // the dirty scoped files, nothing more.

        // The biggest issue that this method presents to us (the plugin makers)
        // is that incorrect usage will cause infinite refresh of the change lists.
        // If any dirty file is not handled, this will be called again.  If any
        // file is marked as dirty by calling this method, the method will be
        // called again.

        // Unfortunately, there are circumstances where a follow-up call can't be
        // avoided.  An example of this is where a file is mis-marked to be in
        // a different changelist.  It's up to this method to correctly sort
        // files into their IDEA changelists.

        if (dirtyScope.wasEveryThingDirty()) {
            // Update all the files.
            Collection<VcsRoot> roots = getRoots();
            if (roots.isEmpty()) {
                LOG.info("No project VCS roots");
                progress.setFraction(1.0);
                return;
            }

            double lastFraction = 0.0;
            double fractionRootIncr = 1.0 / roots.size();
            for (VcsRoot root : roots) {
                LOG.info("Processing changes in " + root.getPath());

                updateCache(root, builder, addGate);

                lastFraction += fractionRootIncr;
                progress.setFraction(lastFraction);
            }
        } else {
            // Update just the dirty files.
            final Set<FilePath> dirtyFiles = dirtyScope.getDirtyFiles();
            if (dirtyFiles == null || dirtyFiles.isEmpty()) {
                LOG.info("No dirty files.");
                progress.setFraction(1.0);
                return;
            }

            Map<VcsRoot, List<FilePath>> fileRoots = VcsUtil.groupByRoots(project, dirtyFiles, (f) -> f);
            if (!fileRoots.isEmpty()) {
                double lastFraction = 0.0;
                double fractionRootIncr = 1.0 / fileRoots.size();
                for (Map.Entry<VcsRoot, List<FilePath>> entry : fileRoots.entrySet()) {
                    LOG.info("Processing changes for " + entry.getValue());

                    Set<FilePath> matched = new HashSet<>();
                    for (FilePath filePath : entry.getValue()) {
                        if (dirtyFiles.remove(filePath)) {
                            matched.add(filePath);
                        }
                    }
                    updateCache(entry.getKey(), matched, builder, addGate);

                    lastFraction += fractionRootIncr;
                    progress.setFraction(lastFraction);
                }
            }

            // All the remaining dirty files are not under our VCS, so mark them as ignored.
            markIgnored(dirtyFiles, builder);
        }

        progress.setFraction(1.0);
    }

    private void updateCache(VcsRoot root, ChangelistBuilder builder, ChangeListManagerGate addGate) {
        // FIXME update the cache

    }

    private void updateCache(VcsRoot root, Set<FilePath> files,
            ChangelistBuilder builder, ChangeListManagerGate addGate) {
        // FIXME update the cache
    }



    private void markIgnored(Set<FilePath> dirtyFiles, ChangelistBuilder builder) {
        for (FilePath dirtyFile : dirtyFiles) {
            builder.processIgnoredFile(dirtyFile.getVirtualFile());
        }
    }


    private List<VcsRoot> getRoots() {
        return ServiceManager.getService(project, VcsRootDetector.class).detect()
                .stream().filter((root) ->
                        root.getVcs() != null
                        && root.getPath() != null
                        && P4Vcs.getKey().equals(root.getVcs().getKeyInstanceMethod()))
                .collect(Collectors.toList());
    }
}
