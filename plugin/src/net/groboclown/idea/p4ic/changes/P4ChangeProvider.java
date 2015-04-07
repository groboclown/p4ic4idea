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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4ContentRevision;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
public class P4ChangeProvider implements ChangeProvider {
    private static final Logger LOG = Logger.getInstance(P4ChangeProvider.class);

    private final P4Vcs vcs;

    public P4ChangeProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public void getChanges(VcsDirtyScope dirtyScope, ChangelistBuilder builder, ProgressIndicator progress,
                           ChangeListManagerGate addGate) throws VcsException {
        LOG.debug("enter");
        if (vcs.getProject().isDisposed()) {
            return;
        }
        if (dirtyScope.getVcs() != vcs) {
            throw new VcsException("invalid dirty scope VCS");
        }

        // In the current thread, pull in all the changes from Perforce that are within the dirty scope, into
        // the addGate.

        progress.setIndeterminate(false);
        progress.setFraction(0.0);

        Set<FilePath> filePaths = dirtyScope.getDirtyFiles();

        // Strip out non-files from the dirty files
        Iterator<FilePath> iter = filePaths.iterator();
        while (iter.hasNext()) {
            FilePath next = iter.next();
            if (next.isDirectory()) {
                iter.remove();
            }
        }

        progress.setFraction(0.1);

        /*
        reports data to ChangelistBuilder using the following methods:

    processChange() is called for files which have been checked out (or modified if the VCS doesn't use an explicit checkout model), scheduled for addition or deletion, moved or renamed.
    processUnversionedFile() is called for files which exist on disk, but are not managed by the VCS, not scheduled for addition, and not ignored through .cvsignore or a similar mechanism.
    processLocallyDeletedFile() is called for files which exist in the VCS repository, but do not exist on disk and are not scheduled for deletion.
    processModifiedWithoutCheckout() is used only with VCSes which use an explicit checkout model. It is called for files which are writable on disk but not checked out through the VCS.
    processIgnoredFile() is called for files which are not managed by the VCS but are ignored through .cvsignore or a similar mechanism.
    processSwitchedFile() is called for files or directories for which the working copy corresponds to a different branch compared to the working copy of their parent directory. This can be called for the same files for which processSwitchedFile() has already been called.

         */

        P4ChangeListCache.getInstance().reloadCachesFor(vcs.getClients());
        final Map<Client, List<P4ChangeList>> pendingChangelists =
                P4ChangeListCache.getInstance().getChangeListsForAll(vcs.getClients());

        final Map<LocalChangeList, Map<Client, P4ChangeList>> known = vcs.getChangeListMapping().cleanMappings();

        progress.setFraction(0.2);

        SubProgressIndicator sub = new SubProgressIndicator(progress, 0.2, 0.5);
        loadUnmappedP4ChangeListsToExitingIdea(addGate, pendingChangelists, known, sub);

        progress.setFraction(0.5);

        // Update the files
        List<Client> clients = vcs.getClients();
        double clientIndex = 0.0;
        for (Client client: vcs.getClients()) {
            sub = new SubProgressIndicator(progress,
                    0.5 + 0.5 * (clientIndex / (double) clients.size()),
                    0.5 + 0.5 * ((clientIndex + 1.0) / (double) clients.size()));
            clientIndex += 1.0;
            List<P4FileInfo> files = moveDirtyFilesIntoIdeaChangeLists(client, builder,
                    sub, getFilesUnderClient(client, filePaths));
            sub.setFraction(0.9);
            moveP4FilesIntoIdeaChangeLists(client, builder, files);
            for (P4FileInfo f: files) {
                filePaths.remove(f.getPath());
                ensureOnlyIn(builder, client, f);
            }
            sub.setFraction(1.0);
        }

        // Process the remaining dirty files
        for (FilePath fp: filePaths) {
            VirtualFile vf = fp.getVirtualFile();
            if (vf != null) {
                if (! vf.isDirectory()) {
                    LOG.info("Found unversioned file " + vf);
                    builder.processUnversionedFile(vf);
                }
            } else {
                LOG.info("Found locally deleted file " + fp);
                builder.processLocallyDeletedFile(fp);
            }
        }
    }

    /**
     * Ensure every file in an idea changelist is in the correct
     * associated Perforce changelist. (bug #22 comment 3)
     * There's a situation that can occur where a file has 2 different
     * changes, each split across multiple change lists.  IDEA supports
     * this, but Perforce doesn't.
     *
     * @param client client
     * @param f file
     */
    private void ensureOnlyIn(@NotNull ChangelistBuilder builder, @NotNull final Client client, @NotNull final P4FileInfo f) {
        File file = f.getPath().getIOFile();
        LocalChangeList actualLocalChange =
                vcs.getChangeListMapping().getLocalChangelist(client, f.getChangelist());
        List<LocalChangeList> allIdeaChangeLists = ChangeListManager.getInstance(vcs.getProject()).getChangeLists();
        for (LocalChangeList lcl: allIdeaChangeLists) {
            if (! lcl.equals(actualLocalChange)) {
                for (Change change : lcl.getChanges()) {
                    if (change.affectsFile(file)) {
                        LOG.info("moving to correct changelist (was double-filed): " + f);
                        builder.processChangeInList(change, actualLocalChange, P4Vcs.getKey());
                    }
                }
            }
        }
    }


    private List<FilePath> getFilesUnderClient(Client client, Collection<FilePath> files) {
        List<FilePath> ret = new ArrayList<FilePath>(files.size());
        for (FilePath file: files) {
            try {
                for (FilePath root : client.getFilePathRoots()) {
                    if (file.isUnder(root, false)) {
                        ret.add(file);
                    }
                }
            } catch (P4InvalidConfigException e) {
                // the thrower properly makes the call to the
                // config error listener
                LOG.info(e);
            }
        }
        return ret;
    }

    /**
     * For each perforce changelist that doesn't have a corresponding IDEA
     * changelist, map it to an existing IDEA changelist that isn't
     * mapped to a Perforce changelist.  There are a number of criteria
     * to pass here.
     *
     * @param addGate gate
     * @param pendingChangelists all pending changelists on all server configs.
     * @param known  the current mappings of IDEA changes to p4 changes (must be kept up to date)
     * @param prog progress bar
     * @throws VcsException
     */
    private void loadUnmappedP4ChangeListsToExitingIdea(
            ChangeListManagerGate addGate,
            Map<Client, List<P4ChangeList>> pendingChangelists,
            Map<LocalChangeList, Map<Client, P4ChangeList>> known, SubProgressIndicator prog)
            throws VcsException {
        List<LocalChangeList> allIdeaChangeLists = ChangeListManager.getInstance(vcs.getProject()).getChangeLists();

        prog.setFraction(0.1);

        Set<P4ChangeListId> associatedP4clIds = new HashSet<P4ChangeListId>();
        for (Map<Client, P4ChangeList> map: known.values()) {
            for (Map.Entry<Client, P4ChangeList> en: map.entrySet()) {
                // It's possible to have a client map to a null changelist.
                P4ChangeList cl = en.getValue();
                if (cl != null) {
                    associatedP4clIds.add(cl.getId());
                //} else {
                //    LOG.info("mapped client " + en.getKey() + " to null changelist");
                }
            }
        }

        prog.setFraction(0.2);

        // For each p4 pending changelist that isn't already associated with an
        // IDEA changelist, find a match to any IDEA changelist that doesn't
        // already have a p4 changelist for that specific client.

        // However, the outer loop is the matchers.  This prevents aggressive
        // consumption of changes - allow the more exact matchers to match the
        // changelists before a looser one tries to match.

        double matcherIndex = 0.0;
        for (ClMatcher matcher : CL_MATCHERS) {
            prog.setFraction(0.2 + 0.7 * (matcherIndex / (double) CL_MATCHERS.length));
            matcherIndex += 1.0;

            for (Map.Entry<Client, List<P4ChangeList>> pending : pendingChangelists.entrySet()) {
                final Client client = pending.getKey();
                final List<LocalChangeList> unusedIdeaChangeLists =
                        filterOutUsedChangelists(allIdeaChangeLists, known, client);
                for (P4ChangeList p4cl : pending.getValue()) {
                    if (associatedP4clIds.contains(p4cl.getId())) {
                        continue;
                    }

                    if (p4cl.getId().isDefaultChangelist()) {
                        // special default changelist handling.  The IDEA default
                        // named changelist should always exist to correspond to the
                        // Perforce default changelist.  Note that this is
                        // independent of the matchers.

                        LOG.info("Associating " + client.getClientName() + " default changelist to IDEA changelist " +
                                P4ChangeListMapping.DEFAULT_CHANGE_NAME);
                        addGate.findOrCreateList(P4ChangeListMapping.DEFAULT_CHANGE_NAME, "");
                        associatedP4clIds.add(p4cl.getId());

                        // The mapping from default to default is implicit in the
                        // P4ChangeListMappingNew class.
                    } else {
                        // it's not associated with any IDEA changelist.
                        LocalChangeList match = matcher.match(p4cl, splitNameComment(p4cl), unusedIdeaChangeLists);

                        if (match != null) {
                            LOG.info("Associating " + p4cl.getId() + " to IDEA changelist " + match);

                            // Ensure the name matches the changelist, with a unique name
                            setUniqueName(match, p4cl, allIdeaChangeLists);

                            // Record this new association
                            associatedP4clIds.add(p4cl.getId());
                            if (known.containsKey(match)) {
                                known.get(match).put(client, p4cl);
                            } else {
                                Map<Client, P4ChangeList> km = new HashMap<Client, P4ChangeList>();
                                km.put(client, p4cl);
                                known.put(match, km);
                            }
                            vcs.getChangeListMapping().bindChangelists(match, p4cl.getId());
                        }
                    }
                }
            }
        }

        prog.setFraction(0.9);

        // All the remaining changelists need to be mapped to a new changelist.
        for (List<P4ChangeList> pendingChanges : pendingChangelists.values()) {
            for (P4ChangeList p4cl : pendingChanges) {
                if (! associatedP4clIds.contains(p4cl.getId())) {
                    LocalChangeList lcl = createUniqueChangeList(addGate, p4cl, allIdeaChangeLists);
                    vcs.getChangeListMapping().bindChangelists(lcl, p4cl.getId());
                }
            }
        }

        prog.setFraction(1.0);
    }

    // This matches with createUniqueChangeList
    private void setUniqueName(LocalChangeList changeList, P4ChangeList cls, List<LocalChangeList> existingIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        LOG.info("Mapped @" + cls.getId() + " to " + changeList.getName());
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
    private LocalChangeList createUniqueChangeList(ChangeListManagerGate addGate, P4ChangeList cls,
            List<LocalChangeList> allIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        String name = desc[0];
        int count = -1;
        findLoop:
        while (true) {
            // Make sure we check against ALL change lists, not just the
            // filtered ones.
            for (LocalChangeList lcl : allIdeaChangeLists) {
                if (lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = desc[0] + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            LOG.info("Mapped " + cls.getId() + " to new IDEA change " + name);
            LocalChangeList ret = addGate.addChangeList(name, desc[1]);
            allIdeaChangeLists.add(ret);
            return ret;
        }
    }

    private List<LocalChangeList> filterOutUsedChangelists(
            List<LocalChangeList> allIdeaChangeLists,
            Map<LocalChangeList, Map<Client, P4ChangeList>> known,
            Client client) {
        List<LocalChangeList> ret = new ArrayList<LocalChangeList>(allIdeaChangeLists);
        for (Map.Entry<LocalChangeList, Map<Client, P4ChangeList>> e: known.entrySet()) {
            if (e.getValue().containsKey(client)) {
                ret.remove(e.getKey());
            }
        }
        Iterator<LocalChangeList> iter = ret.iterator();
        while (iter.hasNext()) {
            if (P4ChangeListMapping.isDefaultChangelist(iter.next())) {
                iter.remove();
            }
        }
        return ret;
    }


    /**
     * Put dirty IDEA files into correct IDEA changelists, as per the dirty file's associated Perforce changelist.
     *
     * @param client server client owning the files
     * @param builder changelist builder
     * @param progress progress bar
     * @param filePaths files to move
     * @return files processed
     * @throws VcsException
     */
    private List<P4FileInfo> moveDirtyFilesIntoIdeaChangeLists(Client client, ChangelistBuilder builder,
            ProgressIndicator progress, Collection<FilePath> filePaths) throws VcsException {
        LOG.info("processing incoming files " + new ArrayList<FilePath>(filePaths));
        final List<P4FileInfo> files = client.getServer().getFilePathInfo(filePaths);
        progress.setFraction(0.1);


        // This code looks to be too aggressive.  It is probably the
        // root cause behind #22 (2nd comment).


        for (P4FileInfo file : files) {
            VirtualFile vf = file.getPath().getVirtualFile();
            LOG.info("processing " + file);

            // VirtualFile can be null.  That means the file is deleted.

            if (file.isOpenInClient()) {
                LOG.info("already open in a changelist on the server");

                LocalChangeList changeList = vcs.getChangeListMapping().getLocalChangelist(client, file.getChangelist());
                if (changeList == null) {
                    // This can happen if the changelist was submitted,
                    // and the file status hasn't been updated to be
                    // marked as not open.

                    LOG.warn("Did not map an IntelliJ changelist for Perforce changelist " + file.getChangelist() +
                            " (file " + file + ")");
                    continue;
                }

                // Create a new change for the file, to replace any
                // default changes created by the CLM.
                // (that is, don't call clm.getChange(file.getPath())
                Change change = createChange(file);
                builder.processChange(change, P4Vcs.getKey());

                LOG.info("added to local changelist " + changeList + ": " + file);
                builder.processChangeInList(change, changeList, P4Vcs.getKey());
            } else if (file.isInDepot()) {
                if (vf == null) {
                    LOG.info("marked as locally deleted");
                    builder.processLocallyDeletedFile(file.getPath());
                } else {
                    LOG.info("marked as locally modified without edit");
                    builder.processModifiedWithoutCheckout(vf);
                }
            } else if (file.isInClientView()) {
                LOG.info("marked as locally added");
                builder.processUnversionedFile(vf);
            } else {
                LOG.info("marked as ignored");
                builder.processIgnoredFile(vf);
            }
        }
        return files;
    }

    private void moveP4FilesIntoIdeaChangeLists(Client client, ChangelistBuilder builder, List<P4FileInfo> files) throws VcsException {
        // go through the changelist cache, because it should be fresh.
        Collection<P4FileInfo> opened = P4ChangeListCache.getInstance().getOpenedFiles(client);
        LOG.info("opened files: " + opened);
        // remove files not already handled
        opened.removeAll(files);
        LOG.info("opened but not passed into this method: " + opened);

        for (P4FileInfo file : opened) {
            LOG.info("looks like " + file + " is in changelist " + file.getChangelist());
            Change change = createChange(file);
            builder.processChange(change, P4Vcs.getKey());

            LocalChangeList changeList = vcs.getChangeListMapping().getLocalChangelist(client, file.getChangelist());
            LOG.info("Putting " + file + " into local change " + changeList);
            builder.processChangeInList(change,
                    changeList,
                    P4Vcs.getKey());

            // Any way to use this call?
            //builder.reportChangesOutsideProject() ?
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
            ret[0] = desc.substring(0,20).trim();
            ret[1] = desc.trim();
        } else {
            ret[0] = desc.trim();
            ret[1] = "";
        }
        return ret;
    }

    private Change createChange(P4FileInfo file) {
        ContentRevision beforeRev = new P4ContentRevision(vcs.getProject(), file);
        ContentRevision afterRev = new CurrentContentRevision(file.getPath());
        return new Change(beforeRev, afterRev, file.getClientAction().getFileStatus());
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
        System.out.println("Cleanup called for  " + files);
    }


    private interface ClMatcher {
        @Nullable
        LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists);
    }

    private static ClMatcher[] CL_MATCHERS = new ClMatcher[] {
            new ExactMatcher(),
            new CasedNameMatcher(),
            new NameMatcher(),
            new NameNumberedMatcher(),
            new NumberedMatcher()
    };

    private static class ExactMatcher implements ClMatcher {
        @Nullable
        @Override
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeList cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
