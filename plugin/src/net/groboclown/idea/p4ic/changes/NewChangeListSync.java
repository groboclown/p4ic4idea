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
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4ContentRevision;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class NewChangeListSync {
    private static final Logger LOG = Logger.getInstance(NewChangeListSync.class);


    private final P4Vcs vcs;
    private final Project project;

    public NewChangeListSync(@NotNull final P4Vcs vcs) {
        this.vcs = vcs;
        this.project = vcs.getProject();
    }


    /**
     * Refreshes the changes
     *
     * @param builder data access and storage
     * @param progress progress bar
     * @throws VcsException
     */
    public void syncChanges(@NotNull VcsDirtyScope dirtyScope, @NotNull ChangeListBuilderCache builder,
            @NotNull final ChangeListManagerGate addGate, @NotNull ProgressIndicator progress) throws VcsException {
        LOG.info("start changelist refresh");

        // Pull in all the changes from Perforce that are within the dirty scope, into
        // the builder.

        progress.setIndeterminate(false);
        progress.setFraction(0.0);

        /*
        reports data to ChangelistBuilder using the following methods:

    processChange() is called for files which have been checked out (or modified if the VCS doesn't use an explicit checkout model), scheduled for addition or deletion, moved or renamed.
    processUnversionedFile() is called for files which exist on disk, but are not managed by the VCS, not scheduled for addition, and not ignored through .cvsignore or a similar mechanism.
    processLocallyDeletedFile() is called for files which exist in the VCS repository, but do not exist on disk and are not scheduled for deletion.
    processModifiedWithoutCheckout() is used only with VCSes which use an explicit checkout model. It is called for files which are writable on disk but not checked out through the VCS.
    processIgnoredFile() is called for files which are not managed by the VCS but are ignored through .cvsignore or a similar mechanism.
    processSwitchedFile() is called for files or directories for which the working copy corresponds to a different branch compared to the working copy of their parent directory. This can be called for the same files for which processSwitchedFile() has already been called.

         */

        SubProgressIndicator sub = new SubProgressIndicator(progress, 0.0, 0.5);
        Map<P4FileInfo, LocalChangeList> assignedIdeaChangelist = loadP4Changes(sub, addGate);

        progress.setFraction(0.5);
        assignToIdeaChangelist(builder, assignedIdeaChangelist);

        progress.setFraction(0.9);
        Set<FilePath> ideaFilePaths = getDirtyFiles(dirtyScope, assignedIdeaChangelist.keySet());

        progress.setFraction(0.95);
        processLocalChanges(ideaFilePaths, builder);
        progress.setFraction(1.0);
    }

    /**
     * Associate Perforce changelist-controlled files with IDEA changelists.
     *
     * @param progress
     * @param addGate
     * @return file associations
     * @throws VcsException
     */
    private Map<P4FileInfo, LocalChangeList> loadP4Changes(final SubProgressIndicator progress,
            final ChangeListManagerGate addGate) throws VcsException {
        progress.setFraction(0.0);

        final Map<Client, List<P4ChangeList>> pendingChangelists =
                P4ChangeListCache.getInstance().reloadCachesFor(vcs.getClients());

        final Map<LocalChangeList, Map<Client, P4ChangeList>> known = vcs.getChangeListMapping().cleanMappings();
        LOG.debug("pending changelists: " + pendingChangelists);
        LOG.debug("known changelists: " + known);

        progress.setFraction(0.2);

        // Find new Perforce changelists that should be associated with an existing IDEA changelist.
        SubProgressIndicator sub = new SubProgressIndicator(progress, 0.2, 0.5);
        loadUnmappedP4ChangeListsToIdeaChangeList(addGate, pendingChangelists, known, sub);
        progress.setFraction(0.5);

        // Associate all Perforce files with IDEA changelists.
        List<Client> clients = vcs.getClients();
        double clientIndex = 0.0;
        double clientIncr = 0.5 / (double) clients.size();
        SubProgressIndicator clientProgress = new SubProgressIndicator(progress, 0.5, 1.0);
        Map<P4FileInfo, LocalChangeList> associatedChanges = new HashMap<P4FileInfo, LocalChangeList>();
        for (Client client : clients) {
            //sub = new SubProgressIndicator(clientProgress, clientIndex, clientIndex + clientIncr);
            clientIndex += clientIncr;
            associateOpenedFilesToChanges(associatedChanges, client, known);
            //if (client.isWorkingOnline()) {
            //} else {
            //    LOG.info("not refreshing changelists for " + client + ": working offline");
            //}
            clientProgress.setFraction(clientIndex);
        }
        return associatedChanges;
    }


    private void associateOpenedFilesToChanges(final Map<P4FileInfo, LocalChangeList> associatedChanges,
            final Client client, final Map<LocalChangeList, Map<Client, P4ChangeList>> known) {
        for (Map.Entry<LocalChangeList, Map<Client, P4ChangeList>> en: known.entrySet()) {
            final LocalChangeList lcl = en.getKey();
            final P4ChangeList p4cl = en.getValue().get(client);
            if (p4cl != null) {
                for (P4FileInfo file: p4cl.getFiles()) {
                    if (associatedChanges.containsKey(file)) {
                        if (! associatedChanges.get(file).equals(lcl)) {
                            LOG.warn("Already associated " + file + " with local changelist " +
                                    associatedChanges.get(file) + ", but also found it with " + lcl);
                        }
                        // else it's fine.
                    } else {
                        associatedChanges.put(file, lcl);
                    }
                }
            }
        }
    }

    /**
     * Move the files into the assigned IDEA changelists, and ensure that they are only
     * in that changelist.
     *
     * @param builder
     * @param assignedIdeaChangelist
     */
    private void assignToIdeaChangelist(final ChangeListBuilderCache builder,
            final Map<P4FileInfo, LocalChangeList> assignedIdeaChangelist) {
        // First, collect all the changes for the files.
        final Set<Change> allUsedChanges = new HashSet<Change>();
        final Map<P4FileInfo, Set<Change>> associatedChanges = new HashMap<P4FileInfo, Set<Change>>();
        List<LocalChangeList> allIdeaChangeLists = ChangeListManager.getInstance(vcs.getProject()).getChangeLists();
        for (P4FileInfo fileInfo: assignedIdeaChangelist.keySet()) {
            FilePath file = fileInfo.getPath();
            Set<Change> changes = new HashSet<Change>();
            associatedChanges.put(fileInfo, changes);
            for (LocalChangeList lcl: allIdeaChangeLists) {
                for (Change ch : lcl.getChanges()) {
                    final ContentRevision before = ch.getBeforeRevision();
                    if (before != null && file.equals(before.getFile())) {
                        if (allUsedChanges.contains(ch)) {
                            LOG.warn("one change " + ch + " used by multiple files");
                        }
                        allUsedChanges.add(ch);
                        changes.add(ch);
                    } else {
                        final ContentRevision after = ch.getAfterRevision();
                        if (after != null && file.equals(after.getFile())) {
                            if (allUsedChanges.contains(ch)) {
                                LOG.warn("one change " + ch + " used by multiple files");
                            }
                            allUsedChanges.add(ch);
                            changes.add(ch);
                        }
                    }
                }
            }
        }

        // We have a set of existing changes.  Force these into the correct changelist.
        for (Map.Entry<P4FileInfo, LocalChangeList> en: assignedIdeaChangelist.entrySet()) {
            Set<Change> changes = associatedChanges.get(en.getKey());
            if (changes == null) {
                // need to create a new change
                builder.processChange(createChange(en.getKey()), en.getValue());
            } else {
                for (Change ch: changes) {
                    builder.processChange(ch, en.getValue());
                }
            }
        }

    }

    /**
     * Move the files marked by IDEA as dirty, but aren't covered by a Perforce changelist.
     *
     * @param ideaFilePaths
     * @param builder
     */
    private void processLocalChanges(final Set<FilePath> ideaFilePaths, final ChangeListBuilderCache builder)
            throws VcsException {
        // Separate these into client and non-client files.
        final Map<Client, List<FilePath>> clientFiles = vcs.mapFilePathToClient(ideaFilePaths);
        for (Map.Entry<Client, List<FilePath>> en: clientFiles.entrySet()) {
            final Client client = en.getKey();
            for (P4FileInfo file : client.getServer().getFilePathInfo(en.getValue())) {
                VirtualFile vf = file.getPath().getVirtualFile();
                LOG.debug("processing " + file);

                // VirtualFile can be null.  That means the file is deleted.

                if (file.isOpenInClient()) {
                    // This should never happen
                    LOG.warn("already open in a changelist on the server, but not discovered yet");

                    LocalChangeList changeList =
                            vcs.getChangeListMapping().getLocalChangelist(client, file.getChangelist());
                    if (changeList == null) {
                        // This can happen if the changelist was submitted,
                        // and the file status hasn't been updated to be
                        // marked as not open.

                        LOG.warn("Did not map an IDEA changelist for Perforce changelist " + file.getChangelist() +
                                " (file " + file + ")");
                        continue;
                    }

                    // Create a new change for the file, to replace any
                    // default changes created by the CLM.
                    // (that is, don't call clm.getChange(file.getPath())
                    LOG.debug("added to local changelist " + changeList + ": " + file);
                    Change change = createChange(file);
                    builder.processChange(change, changeList);
                    ideaFilePaths.remove(file.getPath());
                } else if (file.isInDepot()) {
                    if (vf == null || !vf.exists()) {
                        LOG.info("marked as locally deleted: " + file);
                        builder.processLocallyDeletedFile(file.getPath());
                        ideaFilePaths.remove(file.getPath());
                    } else {
                        // See bug #49
                        //  Changelists show files that are unchanged as locally
                        //  modified without checkout.

                        if (isServerAndLocalEqual(client, file, vf)) {
                            LOG.info("marked as locally modified without edit: " + vf);
                            builder.processModifiedWithoutCheckout(vf);
                            ideaFilePaths.remove(file.getPath());
                        } else {
                            LOG.info("idea thinks is locally modified without checkout, but it's the same: " + vf);
                            file.getPath().hardRefresh();
                            ideaFilePaths.remove(file.getPath());
                        }
                    }
                } else if (file.isInClientView() && vf != null) {
                    LOG.info("marked as locally added: " + vf);
                    builder.processUnversionedFile(vf);
                    ideaFilePaths.remove(file.getPath());
                } else if (vf != null) {
                    LOG.debug("marked as ignored: " + vf);
                    builder.processIgnoredFile(vf);
                    ideaFilePaths.remove(file.getPath());
                } else {
                    LOG.debug("not in depot but deleted: " + vf);
                    // NOTE not removed from the file paths
                }
            }
        }

        for (FilePath fp : ideaFilePaths) {
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

    private Set<FilePath> getDirtyFiles(final VcsDirtyScope dirtyScope, final Set<P4FileInfo> p4FileInfos) {
        Set<FilePath> filePaths = dirtyScope.getDirtyFiles();

        // Strip out non-files from the dirty files
        Iterator<FilePath> iter = filePaths.iterator();
        while (iter.hasNext()) {
            FilePath next = iter.next();
            if (next.isDirectory()) {
                iter.remove();
            }
        }
        for (P4FileInfo info: p4FileInfos) {
            filePaths.remove(info.getPath());
            //if (filePaths.contains(info.getPath())) {
            //    filePaths.remove(info.getPath());
            //}
        }
        return filePaths;
    }

    /**
     * For each perforce changelist that doesn't have a corresponding IDEA
     * changelist, map it to an IDEA changelist.  There are a number of criteria
     * to pass here.
     *
     * @param addGate            gate
     * @param pendingChangelists all pending changelists on all server configs.
     * @param known              the current mappings of IDEA changes to p4 changes (must be kept up to date)
     * @param prog               progress bar
     * @throws VcsException
     */
    private void loadUnmappedP4ChangeListsToIdeaChangeList(
            @NotNull final ChangeListManagerGate addGate,
            @NotNull Map<Client, List<P4ChangeList>> pendingChangelists,
            @NotNull Map<LocalChangeList, Map<Client, P4ChangeList>> known,
            @NotNull SubProgressIndicator prog)
            throws VcsException {
        List<LocalChangeList> allIdeaChangeLists = ChangeListManager.getInstance(vcs.getProject()).getChangeLists();

        prog.setFraction(0.1);

        Set<P4ChangeListId> associatedP4clIds = new HashSet<P4ChangeListId>();
        for (Map<Client, P4ChangeList> map : known.values()) {
            for (Map.Entry<Client, P4ChangeList> en : map.entrySet()) {
                // It's possible to have a client map to a null changelist.
                P4ChangeList cl = en.getValue();
                if (cl != null) {
                    associatedP4clIds.add(cl.getId());
                    LOG.debug("mapped client " + en.getKey() + " to changelist " + cl.getId());
                } else {
                    LOG.debug("mapped client " + en.getKey() + " to null changelist");
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

                        LOG.debug("Associating " + client.getClientName() + " default changelist to IDEA changelist " +
                                P4ChangeListMapping.DEFAULT_CHANGE_NAME);
                        addGate.findOrCreateList(P4ChangeListMapping.DEFAULT_CHANGE_NAME, "");
                        associatedP4clIds.add(p4cl.getId());

                        // The mapping from default to default is implicit in the
                        // P4ChangeListMappingNew class.
                    } else {
                        // it's not associated with any IDEA changelist.
                        LocalChangeList match = matcher.match(p4cl, splitNameComment(p4cl), unusedIdeaChangeLists);

                        if (match != null) {
                            LOG.debug("Associating " + p4cl.getId() + " to IDEA changelist " + match);

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
        for (Map.Entry<Client, List<P4ChangeList>> en: pendingChangelists.entrySet()) {
            for (List<P4ChangeList> pendingChanges : pendingChangelists.values()) {
                for (P4ChangeList p4cl : pendingChanges) {
                    if (!associatedP4clIds.contains(p4cl.getId())) {
                        LocalChangeList lcl = createUniqueChangeList(addGate, p4cl, allIdeaChangeLists);
                        vcs.getChangeListMapping().bindChangelists(lcl, p4cl.getId());
                        if (known.containsKey(lcl)) {
                            // shouldn't happen, but just for completion
                            known.get(lcl).put(en.getKey(), p4cl);
                        } else {
                            Map<Client, P4ChangeList> km = new HashMap<Client, P4ChangeList>();
                            km.put(en.getKey(), p4cl);
                            known.put(lcl, km);
                        }
                    }
                }
            }
        }

        prog.setFraction(1.0);
    }

    // This matches with createUniqueChangeList
    private void setUniqueName(LocalChangeList changeList, P4ChangeList cls,
            List<LocalChangeList> existingIdeaChangeLists) {
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
    private LocalChangeList createUniqueChangeList(@NotNull final ChangeListManagerGate addGate,
            @NotNull P4ChangeList cls, @NotNull List<LocalChangeList> allIdeaChangeLists) {
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

    private List<LocalChangeList> filterOutUsedChangelists(
            List<LocalChangeList> allIdeaChangeLists,
            Map<LocalChangeList, Map<Client, P4ChangeList>> known,
            Client client) {
        List<LocalChangeList> ret = new ArrayList<LocalChangeList>(allIdeaChangeLists);
        for (Map.Entry<LocalChangeList, Map<Client, P4ChangeList>> e : known.entrySet()) {
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

    private boolean isServerAndLocalEqual(@NotNull final Client client, @NotNull final P4FileInfo file,
            @NotNull VirtualFile vf)
            throws VcsException {
        byte[] src = client.getServer().loadFileAsBytes(file.getPath(), file.getHaveRev());
        if (src == null) {
            return false;
        }
        try {
            byte[] tgt = vf.contentsToByteArray();
            if (src.length != tgt.length) {
                return false;
            }
            for (int pos = 0; pos < src.length; ++pos) {
                if (src[pos] != tgt[pos]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new P4FileException(e);
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
            // check exact match on name but not comment
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
            // check match case-insensitive name, ignore comment
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
            // check exact match on changelist number
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
