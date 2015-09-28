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
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
        private final List<File> affectedButNotDirty = new ArrayList<File>();


        /**
         * Pushes all the cached changes into the builder.
         *
         * @param builder changelist builder
         */
        public void applyCache(@NotNull ChangelistBuilder builder) {
            for (ChangeToChangeList processedChange: processedChanges) {

                // ---------------------------------------------------------------------
                // This appears to be the source of the "refresh changelist forever"
                // code.  If this code registers the change AND registers it with
                // a changelist, it triggers another refresh, which makes
                // the refresh repeat forever.
                //
                // Note that if we don't mark the change as being processed, the
                // changelist view will not show the changed files.
                // ---------------------------------------------------------------------

                builder.processChangeInList(processedChange.change, processedChange.list, P4Vcs.getKey());
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

        public boolean hasChanged(@NotNull VcsDirtyScope dirtyScope, @NotNull ChangeListManagerGate addGate) {
            return hasChanged(dirtyScope.getDirtyFiles(), addGate);
        }


        /**
         * Checks if the cached changes contains all the dirty scoped files.
         *
         * @param dirtyScopeFiles dirty scope files
         * @return true if there were different files in the dirty scope than what is in the cache.
         */
        public boolean hasChanged(@NotNull Set<FilePath> dirtyScopeFiles, @NotNull ChangeListManagerGate addGate) {
            // See if any of the required changelists have been altered
            final Set<String> lastListIds = new HashSet<String>(changeListIds);
            List<LocalChangeList> listsCopy = addGate.getListsCopy();
            for (LocalChangeList list: listsCopy) {
                if (! lastListIds.remove(list.getId())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Changelists changed: new local changelist " + list.getId());
                    }
                    return true;
                }
            }
            if (! lastListIds.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Changelists changed: local changelists removed: " + lastListIds);
                }
                return true;
            }

            // The FilePath objects are not matching.  So, instead, we'll track the File objects.
            final Set<File> dirtyFiles = new HashSet<File>();
            for (FilePath file: dirtyScopeFiles) {
                dirtyFiles.add(file.getIOFile());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Dirty files: " + dirtyFiles);
            }

            for (VirtualFile unversionedFile : unversionedFiles) {
                File f = new File(unversionedFile.getPath());
                if (! dirtyFiles.remove(f)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(f)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Changelists changed: unversioned file no longer dirty: " + unversionedFile);
                        }
                        return true;
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Marked as previously unversioned: " + f);
                }
            }

            for (FilePath locallyDeletedFile : locallyDeletedFiles) {
                if (! dirtyFiles.remove(locallyDeletedFile.getIOFile())) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(locallyDeletedFile.getIOFile())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Changelists changed: locally deleted file no longer dirty: " +
                                    locallyDeletedFile);
                        }
                        return true;
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Marked as previously locally deleted: " + locallyDeletedFile);
                }
            }

            for (VirtualFile modified : modifiedWithoutCheckout) {
                File file = new File(modified.getPath());
                if (!dirtyFiles.remove(file)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(file)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Changelists changed: modified file no longer dirty: " + modified);
                        }
                        return true;
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Marked as previously modified without checkout: " + modified);
                }
            }

            for (VirtualFile ignoredFile : ignoredFiles) {
                File file = new File(ignoredFile.getPath());
                if (!dirtyFiles.remove(file)) {
                    // not in dirty list
                    if (! affectedButNotDirty.contains(file)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Changelists changed: ignored file no longer dirty: " + ignoredFile);
                        }
                        return true;
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Marked as previously ignored: " + file);
                }
            }

            // For the processed changes, it's very possible to have a file
            // be in the list multiple times.  Therefore, this won't remove
            // a file from the dirty list, but we'll keep track of what was
            // listed, so that we can do a final comparison.

            Set<File> changedFiles = new HashSet<File>();
            for (ChangeToChangeList processedChange : processedChanges) {
                final Change change = processedChange.change;
                boolean containsFirst = false;
                if (change.getBeforeRevision() != null) {
                    final File file = change.getBeforeRevision().getFile().getIOFile();
                    changedFiles.add(file);
                    containsFirst = dirtyFiles.contains(file) || affectedButNotDirty.contains(file);
                }

                boolean containsSecond = false;
                if (change.getAfterRevision() != null) {
                    final File file = change.getAfterRevision().getFile().getIOFile();
                    changedFiles.add(file);
                    containsSecond = dirtyFiles.contains(file) || affectedButNotDirty.contains(file);
                }
                if (! containsFirst && ! containsSecond) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Changelists changed: new files in change but not marked dirty: " + change);
                        LOG.debug(" -- before: " + (
                                change.getBeforeRevision() == null
                                        ? "null"
                                        : change.getBeforeRevision().getFile()));
                        LOG.debug(" -- after: " + (
                                change.getAfterRevision() == null
                                        ? "null"
                                        : change.getAfterRevision().getFile()));
                    }
                    return true;
                }

                // check to make sure the change hasn't moved to a different changelist
                for (LocalChangeList list : listsCopy) {
                    if (list.getChanges().contains(change)) {
                        if (! list.getName().equals(processedChange.list)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Changelists changed: change files (" + change + ") moved changelists from " +
                                        processedChange.list + " to " + list.getName());
                            }
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
            for (File dirtyFile : dirtyFiles) {
                if (! dirtyFile.isDirectory()) {
                    if (! changedFiles.remove(dirtyFile)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Changelists changed: new dirty file: " + dirtyFile);
                        }
                        return true;
                    }
                }
            }
            // We have now verified that all dirty files are in the list.
            // Need a final check on the "changed" files that are not in the dirty list.
            if (! changedFiles.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Changes included files that are no longer dirty: " + changedFiles);
                }
                return true;
            }

            return false;
        }
    }

    private final CachedChanges changes;
    private final Set<File> originallyDirty;
    private final ChangelistBuilder builder;


    public ChangeListBuilderCache(@NotNull Project project, @NotNull final ChangelistBuilder builder,
            @NotNull VcsDirtyScope dirtyScope) {
        this(project, builder, dirtyScope.getDirtyFiles());
    }

    public ChangeListBuilderCache(@NotNull Project project, @NotNull final ChangelistBuilder builder,
            @NotNull Set<FilePath> dirtyFiles){
        this.builder = builder;
        this.changes = new CachedChanges();
        ChangeListManager clm = ChangeListManager.getInstance(project);
        for (LocalChangeList list: clm.getChangeLists()) {
            changes.changeListIds.add(list.getId());
        }
        originallyDirty = new HashSet<File>();
        for (FilePath file: dirtyFiles) {
            originallyDirty.add(file.getIOFile());
        }
    }

    public CachedChanges getCache() {
        return changes;
    }

    public void processUnversionedFile(@NotNull final VirtualFile vf) {
        builder.processUnversionedFile(vf);
        changes.unversionedFiles.add(vf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("unversionedFile: " + vf);
        }

        File file = new File(vf.getPath());
        if (! originallyDirty.contains(file)) {
            LOG.debug(" -*- not originally dirty");
            changes.affectedButNotDirty.add(file);
        }
    }

    public void processLocallyDeletedFile(@NotNull final FilePath fp) {
        builder.processLocallyDeletedFile(fp);
        changes.locallyDeletedFiles.add(fp);

        if (LOG.isDebugEnabled()) {
            LOG.debug("locallyDeletedFile: " + fp);
        }

        if (!originallyDirty.contains(fp.getIOFile())) {
            LOG.debug(" -*- not originally dirty");
            changes.affectedButNotDirty.add(fp.getIOFile());
        }
    }

    public void processChange(@NotNull final Change change, @NotNull LocalChangeList list) {
        builder.processChange(change, P4Vcs.getKey());
        builder.processChangeInList(change, list, P4Vcs.getKey());

        if (LOG.isDebugEnabled()) {
            LOG.debug("processChange: " +
                    (change.getBeforeRevision() == null
                            ? "null"
                            : change.getBeforeRevision().getFile()) +
                    " ; " +
                    (change.getAfterRevision() == null
                            ? "null"
                            : change.getAfterRevision().getFile())
            );
        }

        changes.processedChanges.add(new ChangeToChangeList(change, list));
        if (change.getBeforeRevision() != null) {
            final FilePath fp = change.getBeforeRevision().getFile();
            if (!originallyDirty.contains(fp.getIOFile())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" -*- not originally dirty: " + fp);
                }
                changes.affectedButNotDirty.add(fp.getIOFile());
            }
        }
        if (change.getAfterRevision() != null) {
            final FilePath fp = change.getAfterRevision().getFile();
            if (!originallyDirty.contains(fp.getIOFile())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" -*- not originally dirty: " + fp);
                }
                changes.affectedButNotDirty.add(fp.getIOFile());
            }
        }
    }

    public void processModifiedWithoutCheckout(@NotNull final VirtualFile vf) {
        builder.processModifiedWithoutCheckout(vf);
        changes.modifiedWithoutCheckout.add(vf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("modifiedWithoutCheckout: " + vf);
        }

        File file = new File(vf.getPath());
        if (!originallyDirty.contains(file)) {
            LOG.debug(" -*- not originally dirty");
            changes.affectedButNotDirty.add(file);
        }
    }

    public void processIgnoredFile(@NotNull final VirtualFile vf) {
        builder.processIgnoredFile(vf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("processIgnoredFile: " + vf);
        }

        changes.ignoredFiles.add(vf);

        File file = new File(vf.getPath());
        if (!originallyDirty.contains(file)) {
            LOG.debug(" -*- not originally dirty");
            changes.affectedButNotDirty.add(file);
        }
    }

    private static class ChangeToChangeList {
        final Change change;
        final LocalChangeList list;


        private ChangeToChangeList(final Change change, final LocalChangeList list) {
            this.change = change;
            this.list = list;
        }
    }

}
