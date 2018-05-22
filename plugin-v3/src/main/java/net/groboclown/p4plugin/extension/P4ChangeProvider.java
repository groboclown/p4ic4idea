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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
        LOG.info("Cleanup called for  " + files);
        // FIXME clean up cache for the files.
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

        double lastFraction = 0.0;

        progress.setFraction(lastFraction);

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

        // For the purposes of this implementation, we'll always attempt to
        // refresh the cache from the server.  Then we'll update the requested file
        // status.

        Collection<ClientConfigRoot> allClientRoots =
                ProjectConfigRegistry.getInstance(project).getClientConfigRoots();

        // This request is performed by the IDE in a background thread.

        Pair<IdeChangelistMap, IdeFileMap> cachedMaps = CacheComponent.getInstance(project)
                .blockingRefreshServerOpenedCache(
                        allClientRoots.stream()
                            .map(ClientConfigRoot::getClientConfig)
                        .collect(Collectors.toList()),
                        // TODO maybe use a different timeout setting?
                        UserProjectPreferences.getLockWaitTimeoutMillis(project),
                        TimeUnit.MILLISECONDS
                );

        lastFraction = 0.6;
        progress.setFraction(lastFraction);

        updateChangelists(cachedMaps.first, cachedMaps.second, addGate);

        if (dirtyScope.wasEveryThingDirty()) {
            // Update all the files.

            double fractionRootIncr = (1.0 - lastFraction) / allClientRoots.size();
            for (ClientConfigRoot root : allClientRoots) {
                LOG.info("Processing changes in " + root.getProjectVcsRootDir());

                updateCache(root.getProjectVcsRootDir(), root.getClientConfig(),
                        cachedMaps.first, cachedMaps.second,
                        builder, addGate);

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
                double fractionRootIncr = (1.0 - lastFraction) / fileRoots.size();
                for (Map.Entry<VcsRoot, List<FilePath>> entry : fileRoots.entrySet()) {
                    if (entry.getKey().getVcs() != null &&
                            P4Vcs.getKey().equals(entry.getKey().getVcs().getKeyInstanceMethod())) {
                        LOG.info("Processing changes for " + entry.getValue());
                        VirtualFile root = entry.getKey().getPath();
                        ClientConfigRoot config = ProjectConfigRegistry.getInstance(project).getClientFor(root);
                        if (config != null) {
                            Set<FilePath> matched = new HashSet<>();
                            for (FilePath filePath : entry.getValue()) {
                                if (dirtyFiles.remove(filePath)) {
                                    matched.add(filePath);
                                }
                            }
                            if (!matched.isEmpty()) {
                                updateCache(root, config.getClientConfig(), matched, builder, addGate);
                            }
                        }
                    }

                    lastFraction += fractionRootIncr;
                    progress.setFraction(lastFraction);
                }
            }

            // All the remaining dirty files are not under our VCS, so mark them as ignored.
            markIgnored(dirtyFiles, builder);
        }

        progress.setFraction(1.0);
    }

    private void updateChangelists(IdeChangelistMap changelistMap, IdeFileMap fileMap, ChangeListManagerGate addGate) {
        List<LocalChangeList> existingLocalChangeLists = addGate.getListsCopy();
        Set<LocalChangeList> unvisitedLocalChangeLists = new HashSet<>(existingLocalChangeLists);
        for (ClientConfigRoot clientConfigRoot : ProjectConfigRegistry.getInstance(project).getClientConfigRoots()) {
            for (P4LocalChangelist changelist : CacheComponent.getInstance(project).getQueryHandler()
                    .getCachedOpenedChangelists(clientConfigRoot.getClientConfig())) {
                LocalChangeList ideChangeList =
                        changelistMap.getIdeChangeFor(changelist.getChangelistId());
                if (ideChangeList == null) {
                    ideChangeList = addGate.addChangeList(
                            createUniqueChangelistName(changelist, existingLocalChangeLists),
                            changelist.getComment());
                    changelistMap.setMapping(changelist.getChangelistId(), ideChangeList);
                } else {
                    unvisitedLocalChangeLists.remove(ideChangeList);
                }
            }
        }

        // Loop through the files, and see if any are associated with a local change list that isn't
        // mapped to a Perforce changelist.  If so, then create that changelist.
        // FIXME loop through the unvisited local changelists
        Iterator<LocalChangeList> iter = unvisitedLocalChangeLists.iterator();
        while (iter.hasNext()) {
            LocalChangeList ideChangeList = iter.next();
            for (Change change : ideChangeList.getChanges()) {
                if (change.getBeforeRevision() != null) {
                    // FIXME Check if the file is in a P4 VCS root.
                }
                if (change.getAfterRevision() != null) {
                    // FIXME Check if the file is in a P4 VCS root.
                }
            }
        }

        // FIXME any remaining unvisited local changelist but is associated to a
        // changelist in the map needs to be removed.
    }

    private String createUniqueChangelistName(P4LocalChangelist changelist,
            List<LocalChangeList> existingLocalChangeLists) {
        return null;
    }

    private void updateCache(VirtualFile root, ClientConfig config,
            IdeChangelistMap first, IdeFileMap second, ChangelistBuilder builder,
            ChangeListManagerGate addGate) {

        // FIXME update the cache

    }

    private void updateCache(VirtualFile root, ClientConfig config, Set<FilePath> files,
            ChangelistBuilder builder, ChangeListManagerGate addGate) {
        // FIXME update the cache
    }



    private void markIgnored(Set<FilePath> dirtyFiles, ChangelistBuilder builder) {
        for (FilePath dirtyFile : dirtyFiles) {
            builder.processIgnoredFile(dirtyFile.getVirtualFile());
        }
    }

}
