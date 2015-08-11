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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4ContentRevision;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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
public class ChangeListSync {
    private static final Logger LOG = Logger.getInstance(ChangeListSync.class);


    private final P4Vcs vcs;
    private final Project project;

    public ChangeListSync(@NotNull final P4Vcs vcs) {
        this.vcs = vcs;
        this.project = vcs.getProject();
    }


    /**
     * Refreshes the changes
     *
     * @param builder  data access and storage
     * @param progress progress bar
     * @throws VcsException
     */
    public void syncChanges(@NotNull VcsDirtyScope dirtyScope, @NotNull ChangeListBuilderCache builder,
            @NotNull final ChangeListManagerGate addGate, @NotNull ProgressIndicator progress) throws VcsException {
        LOG.info("start changelist refresh");

        progress.setIndeterminate(false);
        progress.setFraction(0.0);

        // 1. Find all the perforce files check-out state and changelist mappings.
        // 2. Create or remove IDEA changelists as needed.
        // 3. Put the perforce files into idea changes, while comparing them to the
        //    dirty list (to see if something should be marked as dirty that isn't).
        // 4. All remaining dirty files need to be correctly filed.


        // Step 1: find the perforce files and changelists

        // - the reload cache reloads the changelists and the files.
        final Map<Client, List<P4ChangeList>> pendingChangelists =
                P4ChangeListCache.getInstance().reloadCachesFor(vcs.getClients());
        LOG.debug("pending changelists: " + pendingChangelists);
        progress.setFraction(0.2);
        final Map<LocalChangeList, Map<Client, P4ChangeList>> known = vcs.getChangeListMapping().cleanMappings();
        LOG.debug("known changelist mappings: " + known);

        progress.setFraction(0.5);


        /*
        reports data to ChangelistBuilder using the following methods:

    processChange() is called for files which have been checked out (or modified if the VCS doesn't use an explicit checkout model), scheduled for addition or deletion, moved or renamed.
    processUnversionedFile() is called for files which exist on disk, but are not managed by the VCS, not scheduled for addition, and not ignored through .cvsignore or a similar mechanism.
    processLocallyDeletedFile() is called for files which exist in the VCS repository, but do not exist on disk and are not scheduled for deletion.
    processModifiedWithoutCheckout() is used only with VCSes which use an explicit checkout model. It is called for files which are writable on disk but not checked out through the VCS.
    processIgnoredFile() is called for files which are not managed by the VCS but are ignored through .cvsignore or a similar mechanism.
    processSwitchedFile() is called for files or directories for which the working copy corresponds to a different branch compared to the working copy of their parent directory. This can be called for the same files for which processSwitchedFile() has already been called.

         */


        // Step 2 & 3:
        progress.setFraction(0.55);
        SubProgressIndicator sub = new SubProgressIndicator(progress, 0.55, 0.9);
        Set<FilePath> remainingFiles = loadP4IntoIdeaChangelists(addGate, pendingChangelists, known, sub, dirtyScope,
                builder);

        // Step 4:
        // Pull in all the changes from Perforce that are within the dirty scope, into
        // the builder.
        progress.setFraction(0.9);

        // Process the remaining dirty files
        for (FilePath fp : remainingFiles) {
            VirtualFile vf = fp.getVirtualFile();
            if (vf != null) {
                if (!vf.isDirectory()) {
                    LOG.info("Found unversioned file " + vf);
                    builder.processUnversionedFile(vf);
                }
            } else {
                LOG.info("Found locally deleted file " + fp);
                builder.processLocallyDeletedFile(fp);
            }
        }
    }

    private Set<FilePath> getDirtyFiles(final VcsDirtyScope dirtyScope) {
        Set<FilePath> filePaths = dirtyScope.getDirtyFiles();

        // Strip out non-files from the dirty files
        Iterator<FilePath> iter = filePaths.iterator();
        while (iter.hasNext()) {
            FilePath next = iter.next();
            if (next.isDirectory()) {
                iter.remove();
            }
        }
        return filePaths;
    }

    /**
     * For each perforce changelist that doesn't have a corresponding IDEA
     * changelist, map it to an existing IDEA changelist that isn't
     * mapped to a Perforce changelist.  There are a number of criteria
     * to pass here.
     * <p/>
     * When returned, the "known" mapping will be filled with all known p4 changelists.
     *
     * @param addGate            gate
     * @param pendingChangelists all pending changelists on all server configs.
     * @param known              the current mappings of IDEA changes to p4 changes (must be kept up to date)
     * @param prog               progress bar
     * @param dirtyScope         dirty files
     * @param builder            changelist builder
     * @throws VcsException
     */
    @NotNull
    private Set<FilePath> loadP4IntoIdeaChangelists(
            @NotNull final ChangeListManagerGate addGate,
            @NotNull Map<Client, List<P4ChangeList>> pendingChangelists,
            @NotNull Map<LocalChangeList, Map<Client, P4ChangeList>> known,
            @NotNull SubProgressIndicator prog, final VcsDirtyScope dirtyScope, final ChangeListBuilderCache builder)
            throws VcsException {
        Set<FilePath> dirtyFiles = getDirtyFiles(dirtyScope);
        prog.setFraction(0.1);
        Set<LocalChangeList> allIdeaChangeLists = new HashSet<LocalChangeList>(
                ChangeListManager.getInstance(vcs.getProject()).getChangeLists());
        prog.setFraction(0.2);


        // FIXME set the progress.

        Map<FilePath, P4FileInfo> fileInfoMap = new HashMap<FilePath, P4FileInfo>();
        Map<File, LocalChangeList> filesToChanges = new HashMap<File, LocalChangeList>();
        Map<LocalChangeList, Set<FilePath>> changesToFiles = new HashMap<LocalChangeList, Set<FilePath>>();
        for (Map.Entry<Client, List<P4ChangeList>> en : pendingChangelists.entrySet()) {
            final Client client = en.getKey();
            for (P4ChangeList p4Change : en.getValue()) {
                LocalChangeList local = findLocalChangeList(addGate, client, p4Change, known, allIdeaChangeLists);
                for (P4FileInfo fileInfo : p4Change.getFiles()) {
                    final FilePath path = fileInfo.getPath();
                    fileInfoMap.put(path, fileInfo);
                    if (dirtyFiles.contains(path)) {
                        dirtyFiles.remove(path);
                    } else {
                        VcsUtil.markFileAsDirty(project, path);
                    }
                    filesToChanges.put(path.getIOFile(), local);
                    if (!changesToFiles.containsKey(local)) {
                        changesToFiles.put(local, new HashSet<FilePath>());
                    }
                    changesToFiles.get(local).add(path);
                }
            }
        }

        for (Entry<LocalChangeList, Set<FilePath>> changeListSetEntry : changesToFiles.entrySet()) {
            final LocalChangeList local = changeListSetEntry.getKey();
            final Set<FilePath> fileSet = changeListSetEntry.getValue();

            // Ensure all the existing changes belong in the changelist.
            changeLoop:
            for (Change change : local.getChanges()) {
                final ContentRevision[] revs =
                        new ContentRevision[]{change.getBeforeRevision(), change.getAfterRevision()};
                final List<FilePath> revFileList = new ArrayList<FilePath>();
                for (ContentRevision rev : revs) {
                    if (rev != null) {
                        revFileList.add(rev.getFile());
                    }
                }
                for (ContentRevision revision : revs) {
                    if (revision != null) {
                        final FilePath revFile = revision.getFile();
                        final LocalChangeList revChangeList = filesToChanges.get(revFile.getIOFile());
                        if (revChangeList == null) {
                            // don't know what to do with this file.  It's not a Perforce managed file, but it's
                            // in an IDEA changelist.
                            final Client client = vcs.getClientFor(revFile);
                            if (client != null) {
                                // The file is under Perforce control, is in an IDEA changelist, but is
                                // not marked by Perforce as being open.
                                //openFile(change, revision, client);
                                //builder.processChange(change, local);
                                if (revFile.getVirtualFile() == null) {
                                    builder.processLocallyDeletedFile(revFile);
                                } else {
                                    builder.processModifiedWithoutCheckout(revFile.getVirtualFile());
                                }
                                fileSet.removeAll(revFileList);
                                continue changeLoop;
                            } else {
                                // Leave the file in the changelist
                                LOG.info("Non-perforce file " + revFile + " in perforce controlled IDEA change");
                            }
                        } else if (!local.equals(revChangeList)) {
                            // wrongly filed change.
                            builder.processChange(change, revChangeList);
                            final Set<FilePath> revChangeFiles = changesToFiles.get(revChangeList);
                            if (revChangeFiles != null) {
                                revChangeFiles.removeAll(revFileList);
                            } // else error; should always be non-null
                            continue changeLoop;
                        } else {
                            // mark the change as being in the right place.
                            builder.processChange(change, local);
                            fileSet.removeAll(revFileList);
                            continue changeLoop;
                        }
                    }
                }
            }
        }

        // All the remaining files in the lists are ones that don't have existing changes.
        // We can't have this as part of the above loop, because that must process all existing
        // changes first.
        for (Entry<LocalChangeList, Set<FilePath>> changeListSetEntry : changesToFiles.entrySet()) {
            final LocalChangeList local = changeListSetEntry.getKey();
            final Set<FilePath> fileSet = changeListSetEntry.getValue();

            for (FilePath path : fileSet) {
                builder.processChange(createChange(fileInfoMap.get(path)), local);
            }
        }

        return dirtyFiles;
    }

    @NotNull
    private LocalChangeList findLocalChangeList(@NotNull final ChangeListManagerGate addGate,
            @NotNull final Client client, @NotNull final P4ChangeList p4Change,
            @NotNull final Map<LocalChangeList, Map<Client, P4ChangeList>> knownChanges,
            @NotNull final Set<LocalChangeList> allChangeLists) {

        // allChangeLists is a set, so that we don't accidentally add duplicates, which would just
        // end up wasting time.

        final P4ChangeListId id = p4Change.getId();
        // Must reuse the existing all changelists, because one changelist can have multiple mappings
        List<LocalChangeList> unmatched = new ArrayList<LocalChangeList>(allChangeLists);
        for (Map.Entry<LocalChangeList, Map<Client, P4ChangeList>> entry : knownChanges.entrySet()) {
            if (entry.getKey() == null) {
                LOG.error("Internal error: null IDEA changelist mapped to perforce changes");
                continue;
            }

            final Map<Client, P4ChangeList> clientChangeMapping = entry.getValue();
            final P4ChangeList p4cl = clientChangeMapping.get(client);
            if (p4cl != null) {
                if (id.getChangeListId() == p4cl.getId().getChangeListId()) {
                    return entry.getKey();
                }

                // only one client can be associated with this changelist, and we just found our client's mapping
                unmatched.remove(entry.getKey());
                break;
            }
        }

        if (id.isDefaultChangelist()) {
            // default changelist for this client has not yet been mapped
            final LocalChangeList ret = addGate.findOrCreateList(P4ChangeListMapping.DEFAULT_CHANGE_NAME, "");
            if (ret == null) {
                LOG.error("Internal API error: findOrCreateList returned null");
            } else {
                if (!allChangeLists.contains(ret)) {
                    allChangeLists.add(ret);
                }
                if (!knownChanges.containsKey(ret)) {
                    knownChanges.put(ret, new HashMap<Client, P4ChangeList>());
                }
                knownChanges.get(ret).put(client, p4Change);
                return ret;
            }
        }

        // attempt to match the change against existing IDEA changelists
        for (ClMatcher matcher : CL_MATCHERS) {
            final LocalChangeList match = matcher.match(p4Change, splitNameComment(p4Change), unmatched);
            if (match != null) {
                LOG.debug("Associating " + id + " to IDEA changelist " + match);

                // Ensure the name matches the changelist, with a unique name
                setUniqueName(match, p4Change, allChangeLists);

                // Record this new association
                if (!knownChanges.containsKey(match)) {
                    knownChanges.put(match, new HashMap<Client, P4ChangeList>());
                }
                knownChanges.get(match).put(client, p4Change);
                vcs.getChangeListMapping().bindChangelists(match, id);
                return match;
            }
        }

        // Create a new changelist
        LocalChangeList lcl = createUniqueChangeList(addGate, p4Change, allChangeLists);
        if (!knownChanges.containsKey(lcl)) {
            knownChanges.put(lcl, new HashMap<Client, P4ChangeList>());
        }
        knownChanges.get(lcl).put(client, p4Change);
        vcs.getChangeListMapping().bindChangelists(lcl, id);
        return lcl;
    }

    // This matches with createUniqueChangeList
    private void setUniqueName(LocalChangeList changeList, P4ChangeList cls,
            Collection<LocalChangeList> existingIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        LOG.debug("Mapped @" + cls.getId() + " to " + changeList.getName());
        String name = desc[0];
        int count = -1;
        findLoop:
        while (true) {
            for (LocalChangeList lcl : existingIdeaChangeLists) {
                if (!Comparing.equal(lcl.getId(), changeList.getId()) &&
                        lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = desc[0] + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            changeList.setName(name);
            changeList.setComment(desc[1]);
            return;
        }
    }

    // This matches with setUniqueName
    @NotNull
    private LocalChangeList createUniqueChangeList(@NotNull final ChangeListManagerGate addGate,
            @NotNull P4ChangeList cls, @NotNull Collection<LocalChangeList> allIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        String baseDesc = desc[0];
        if (baseDesc == null || baseDesc.length() <= 0) {
            baseDesc = '@' + Integer.toString(cls.getId().getChangeListId());
        }
        String name = baseDesc;
        int count = -1;
        findLoop:
        while (true) {
            // Make sure we check against ALL change lists, not just the
            // filtered ones.
            for (LocalChangeList lcl : allIdeaChangeLists) {
                if (lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = baseDesc + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            LOG.debug("Mapped " + cls.getId() + " to new IDEA change " + name);
            // This can sometimes cause an assertion error, so don't let that
            // happen - use "findOrCreateList" instead of "addList".
            LocalChangeList ret = addGate.findOrCreateList(name, desc[1]);
            allIdeaChangeLists.add(ret);
            return ret;
        }
    }


    private static String[] splitNameComment(P4ChangeList p4cl) {
        String[] ret = new String[2];
        String desc = p4cl.getComment();
        if (p4cl.getId().isDefaultChangelist()) {
            ret[0] = P4ChangeListMapping.DEFAULT_CHANGE_NAME;
            ret[1] = "";
        } else if (desc == null) {
            ret[0] = "";
            ret[1] = "";
        } else if (desc.indexOf('\n') > 0) {
            ret[0] = desc.substring(0, desc.indexOf('\n')).trim();
            ret[1] = desc.substring(desc.indexOf('\n') + 1).trim();
        } else if (desc.length() > 20) {
            ret[0] = desc.substring(0, 20).trim();
            ret[1] = desc.trim();
        } else {
            ret[0] = desc.trim();
            ret[1] = "";
        }
        return ret;
    }

    private Change createChange(@NotNull P4FileInfo file) {
        ContentRevision beforeRev = new P4ContentRevision(project, file);
        ContentRevision afterRev = new CurrentContentRevision(file.getPath());
        return new Change(beforeRev, afterRev, file.getClientAction().getFileStatus());
    }


    private interface ClMatcher {
        @Nullable
        LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists);
    }

    private static ClMatcher[] CL_MATCHERS = new ClMatcher[]{
            new ExactMatcher(),
            new CasedNameMatcher(),
            new NameMatcher(),
            new NameNumberedMatcher(),
            new NumberedMatcher()
    };

    private static class ExactMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equals(lcl.getName().trim())) {
                    String comment = lcl.getComment();
                    if (comment == null && desc[1].length() <= 0) {
                        return lcl;
                    }
                    if (comment != null && desc[1].equals(comment.trim())) {
                        return lcl;
                    }
                }
            }
            return null;
        }
    }

    private static class CasedNameMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equals(lcl.getName().trim())) {
                    // IDEA has unique changelist names
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NameMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (desc[0].equalsIgnoreCase(lcl.getName().trim())) {
                    // IDEA has unique changelist names
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NameNumberedMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // match ignore case "[p4 name](\s+\(\d+\)\)"
            Pattern pattern = Pattern.compile(Pattern.quote(desc[0]) + "\\s+\\(\\d+\\)", Pattern.CASE_INSENSITIVE);
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (pattern.matcher(lcl.getName().trim()).matches()) {
                    return lcl;
                }
            }
            return null;
        }
    }

    private static class NumberedMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc,
                @NotNull List<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // match ignore case "[p4 name](\s+\(\d+\)\)"
            String match = "(@" + cls.getId() + ")";
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (match.equals(lcl.getName().trim())) {
                    return lcl;
                }
            }
            return null;
        }
    }
}
