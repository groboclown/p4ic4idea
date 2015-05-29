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

package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Stores all the updates to the {@link ChangelistBuilder}, so that it can be
 * replayed at a later date.
 */
public class ChangeListBuilderCache {
    private static final Logger LOG = Logger.getInstance(ChangeListBuilderCache.class);

    public static class CachedChanges {
        private final List<String> changeListIds = new ArrayList<String>();
        private final List<VirtualFile> unversionedFiles = new ArrayList<VirtualFile>();
        private final List<FilePath> locallyDeletedFiles = new ArrayList<FilePath>();
        private final List<VirtualFile> modifiedWithoutCheckout = new ArrayList<VirtualFile>();
        private final List<VirtualFile> ignoredFiles = new ArrayList<VirtualFile>();
        private final List<ChangeToChangeList> processedChanges = new ArrayList<ChangeToChangeList>();
        private final List<FilePath> affectedButNotDirty = new ArrayList<FilePath>();


        /**
         * Pushes all the cached changes into the builder.
         *
         * @param builder changelist builder
         */
        public void applyCache(@NotNull ChangelistBuilder builder) {
            for (ChangeToChangeList processedChange: processedChanges) {
                builder.processChange(processedChange.change, P4Vcs.getKey());
                builder.processChangeInList(processedChange.change, processedChange.listName, P4Vcs.getKey());
            }

            for (VirtualFile unversionedFile : unversionedFiles) {
                builder.processUnversionedFile(unversionedFile);
            }

            for (FilePath locallyDeletedFile : locallyDeletedFiles) {
                builder.processLocallyDeletedFile(locallyDeletedFile);
            }

            for (VirtualFile modified : modifiedWithoutCheckout) {
                builder.processModifiedWithoutCheckout(modified);
            }

            for (VirtualFile ignoredFile : ignoredFiles) {
                builder.processIgnoredFile(ignoredFile);
            }
        }


        /**
         * Checks if the cached changes contains all the dirty scoped files.
         *
         * @param dirtyScope dirty scope
         * @return true if there were different files in the dirty scope than what is in the cache.
         */
        public boolean hasChanged(@NotNull VcsDirtyScope dirtyScope, @NotNull ChangeListManagerGate addGate) {
            // See if any of the required changelists have been altered
            final Set<String> lastListIds = new HashSet<String>(changeListIds);
            List<LocalChangeList> listsCopy = addGate.getListsCopy();
            for (LocalChangeList list: listsCopy) {
                if (! lastListIds.remove(list.getId())) {
                    LOG.info("Changelists changed: new local changelist " + list.getId());
                    return true;
                }
            }
            if (! lastListIds.isEmpty()) {
                LOG.info("Changelists changed: local changelists removed: " + lastListIds);
                return true;
            }

            final Set<FilePath> dirtyFiles = new HashSet<FilePath>(dirtyScope.getDirtyFiles());

            for (VirtualFile unversionedFile : unversionedFiles) {
                final FilePath fp = VcsUtil.getFilePath(unversionedFile);
                if (! dirtyFiles.remove(fp)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(fp)) {
                        LOG.info("Changelists changed: unversioned file no longer dirty: " + unversionedFile);
                        return true;
                    }
                }
            }

            for (FilePath locallyDeletedFile : locallyDeletedFiles) {
                if (! dirtyFiles.remove(locallyDeletedFile)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(locallyDeletedFile)) {
                        LOG.info("Changelists changed: locally deleted file no longer dirty: " + locallyDeletedFile);
                        return true;
                    }
                }
            }

            for (VirtualFile modified : modifiedWithoutCheckout) {
                final FilePath fp = VcsUtil.getFilePath(modified);
                if (!dirtyFiles.remove(fp)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(fp)) {
                        LOG.info("Changelists changed: modified file no longer dirty: " + modified);
                        return true;
                    }
                }
            }

            for (VirtualFile ignoredFile : ignoredFiles) {
                final FilePath fp = VcsUtil.getFilePath(ignoredFile);
                if (!dirtyFiles.remove(fp)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(fp)) {
                        LOG.info("Changelists changed: ignored file no longer dirty: " + ignoredFile);
                        return true;
                    }
                }
            }

            for (ChangeToChangeList processedChange : processedChanges) {
                final Change change = processedChange.change;
                boolean containsFirst = false;
                if (change.getBeforeRevision() != null) {
                    if (! dirtyFiles.remove(change.getBeforeRevision().getFile())) {
                        containsFirst = affectedButNotDirty.contains(change.getBeforeRevision().getFile());
                    }
                }
                boolean containsSecond = false;
                if (change.getAfterRevision() != null) {
                    if (! dirtyFiles.remove(change.getAfterRevision().getFile())) {
                        containsSecond = affectedButNotDirty.contains(change.getAfterRevision().getFile());
                    }
                }
                if (! containsFirst && ! containsSecond) {
                    LOG.info("Changelists changed: change files no longer dirty: " + change);
                    return true;
                }

                // check to make sure the change hasn't moved to a different changelist
                for (LocalChangeList list : listsCopy) {
                    if (list.getChanges().contains(change)) {
                        if (! list.getName().equals(processedChange.listName)) {
                            LOG.info("Changelists changed: change files (" + change + ") moved changelists from " +
                                processedChange.listName + " to " + list.getName());
                            return true;
                        }
                        break;
                    }
                }
                // This is actually an acceptable state - we'll swap which changelist it's
                // associated to in the update.  What matters is that we've already
                // marked it as being changed.
                //if (! found) {
                //    LOG.info("Changelists changed: change files (" + change + ") not in any active changelist");
                //    return true;
                //}
            }

            // For Perforce, directories are ignored in terms of marking things as dirty.
            for (FilePath dirtyFile : dirtyFiles) {
                if (! dirtyFile.isDirectory()) {
                    LOG.info("Changelists changed: new dirty file: " + dirtyFile);
                    return true;
                }
            }

            return false;
        }
    }

    private final CachedChanges changes = new CachedChanges();
    private final Set<FilePath> originallyDirty;
    private final ChangelistBuilder builder;


    public ChangeListBuilderCache(@NotNull Project project, @NotNull final ChangelistBuilder builder,
            @NotNull VcsDirtyScope dirtyScope) {
        this.builder = builder;
        ChangeListManager clm = ChangeListManager.getInstance(project);
        for (LocalChangeList list: clm.getChangeLists()) {
            changes.changeListIds.add(list.getId());
        }
        originallyDirty = new HashSet<FilePath>(dirtyScope.getDirtyFiles());
    }

    public CachedChanges getCache() {
        return changes;
    }

    public void processUnversionedFile(@NotNull final VirtualFile vf) {
        builder.processUnversionedFile(vf);
        changes.unversionedFiles.add(vf);
        FilePath fp = VcsUtil.getFilePath(vf);
        if (! originallyDirty.contains(fp)) {
            changes.affectedButNotDirty.add(fp);
        }
    }

    public void processLocallyDeletedFile(@NotNull final FilePath fp) {
        builder.processLocallyDeletedFile(fp);
        changes.locallyDeletedFiles.add(fp);
        if (!originallyDirty.contains(fp)) {
            changes.affectedButNotDirty.add(fp);
        }
    }

    public void processChange(@NotNull final Change change, @NotNull LocalChangeList list) {
        builder.processChange(change, P4Vcs.getKey());
        builder.processChangeInList(change, list, P4Vcs.getKey());
        changes.processedChanges.add(new ChangeToChangeList(change, list));
        if (change.getBeforeRevision() != null) {
            final FilePath fp = change.getBeforeRevision().getFile();
            if (!originallyDirty.contains(fp)) {
                changes.affectedButNotDirty.add(fp);
            }
        }
        if (change.getAfterRevision() != null) {
            final FilePath fp = change.getAfterRevision().getFile();
            if (!originallyDirty.contains(fp)) {
                changes.affectedButNotDirty.add(fp);
            }
        }
    }

    public void processModifiedWithoutCheckout(@NotNull final VirtualFile vf) {
        builder.processModifiedWithoutCheckout(vf);
        changes.modifiedWithoutCheckout.add(vf);
    }

    public void processIgnoredFile(@NotNull final VirtualFile vf) {
        builder.processIgnoredFile(vf);
        changes.ignoredFiles.add(vf);
    }

    private static class ChangeToChangeList {
        final Change change;
        final String listName;


        private ChangeToChangeList(final Change change, final LocalChangeList list) {
            this.change = change;
            this.listName = list.getName();
        }
    }

}
