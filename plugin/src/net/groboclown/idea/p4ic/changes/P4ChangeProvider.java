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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4ContentRevision;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Pushes changes FROM Perforce INTO idea.  No Perforce jobs will be altered.
 *
 * If there was an IDEA changelist that referenced a Perforce changelist that
 * has since been submitted or deleted, the IDEA changelist will be removed,
 * and any contents will be moved into the default changelist.
 *
 * If there is a Perforce changelist that has no
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

        Map<Client, List<IChangelistSummary>> pendingChangelists = getPendingP4ChangeLists();
        Map<LocalChangeList, Map<Client, IChangelistSummary>> known = vcs.getChangeListMapping().cleanMappings(pendingChangelists);

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
            }
            sub.setFraction(1.0);
        }

        // Whatever is left over in the dirty bucket needs to be assigned to some changelist
        for (FilePath fp: filePaths) {
            VirtualFile vf = fp.getVirtualFile();
            if (vf != null) {
                if (! vf.isDirectory()) {
                    builder.processUnversionedFile(vf);
                }
            } else {
                builder.processLocallyDeletedFile(fp);
            }
        }
    }

    private Map<Client, List<IChangelistSummary>> getPendingP4ChangeLists() throws VcsException {
        Map<Client, List<IChangelistSummary>> ret = new HashMap<Client, List<IChangelistSummary>>();
        for (Client client: vcs.getClients()) {
            ServerExecutor exec = client.getServer();
            ret.put(client, exec.getPendingClientChangelists());
        }
        return ret;
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
                // ignore; the thrower properly makes the call
                // FIXME correct logging of the error
                e.printStackTrace();
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
            Map<Client, List<IChangelistSummary>> pendingChangelists,
            Map<LocalChangeList, Map<Client, IChangelistSummary>> known, SubProgressIndicator prog)
            throws VcsException {
        List<LocalChangeList> existingIdeaChangeLists = ChangeListManager.getInstance(vcs.getProject()).getChangeLists();

        // Loop over first the matchers.
        // This allows for best match first, over all the changelists, then
        // next best match, and so on.  That way, we don't
        // accidentally pull in too aggressively.

        prog.setFraction(0.1);

        Set<P4ChangeListMapping.P4ClId> foundClIds = new HashSet<P4ChangeListMapping.P4ClId>();
        for (Map<Client, IChangelistSummary> map: known.values()) {
            for (Map.Entry<Client, IChangelistSummary> e: map.entrySet()) {
                foundClIds.add(new P4ChangeListMapping.P4ClId(e.getKey().getConfig(), e.getValue()));
            }
        }

        prog.setFraction(0.2);

        // The goal is to use all the pending changelists.  So map those.
        double matcherIndex = 0.0;
        for (ClMatcher matcher : CL_MATCHERS) {
            prog.setFraction(0.2 + 0.7 * (matcherIndex / (double) CL_MATCHERS.length));
            matcherIndex += 1.0;
            for (Map.Entry<Client, List<IChangelistSummary>> pending : pendingChangelists.entrySet()) {
                List<LocalChangeList> unusedIdeaChangelists = filterOutUsedChangelists(existingIdeaChangeLists, known, pending.getKey());
                for (IChangelistSummary cls : pending.getValue()) {
                    P4ChangeListMapping.P4ClId p4ClId = new P4ChangeListMapping.P4ClId(pending.getKey().getConfig(), cls);
                    if (foundClIds.contains(p4ClId)) {
                        continue;
                    }
                    if (P4ChangeListMapping.isDefaultChangelist(cls)) {
                        // special default changelist handling.  The IDEA default
                        // named changelist should always exist to correspond to the
                        // Perforce default changelist.

                        addGate.findOrCreateList(P4ChangeListMapping.DEFAULT_CHANGE_NAME, "");
                        foundClIds.add(p4ClId);

                        // The mapping from default to default is implicit in the
                        // P4ChangeListMappingNew class.
                    } else if (!knownChangelist(pending.getKey(), cls, known)) {
                        LocalChangeList match = matcher.match(cls, splitNameComment(cls), unusedIdeaChangelists);

                        if (match != null) {
                            // Ensure the name matches the changelist, with a unique name
                            setUniqueName(match, cls, existingIdeaChangeLists);
                            foundClIds.add(p4ClId);

                            if (known.containsKey(match)) {
                                known.get(match).put(pending.getKey(), cls);
                            } else {
                                Map<Client, IChangelistSummary> km = new HashMap<Client, IChangelistSummary>();
                                km.put(pending.getKey(), cls);
                                known.put(match, km);
                            }
                            vcs.getChangeListMapping().createMapping(match, pending.getKey().getConfig(), cls);
                        }
                    } // else it's a changelist with an existing p4 mapping
                }
            }
        }

        prog.setFraction(0.9);

        // All the remaining changelists need to be mapped to a new changelist.
        for (Map.Entry<Client, List<IChangelistSummary>> pending : pendingChangelists.entrySet()) {
            for (IChangelistSummary cls : pending.getValue()) {
                P4ChangeListMapping.P4ClId p4ClId = new P4ChangeListMapping.P4ClId(pending.getKey().getConfig(), cls);
                if (foundClIds.contains(p4ClId)) {
                    continue;
                }
                LocalChangeList lcl = createUniqueChangeList(addGate, cls, existingIdeaChangeLists);
                vcs.getChangeListMapping().createMapping(lcl, pending.getKey().getConfig(), cls);
            }
        }

        prog.setFraction(1.0);
    }

    private void setUniqueName(LocalChangeList changeList, IChangelistSummary cls, List<LocalChangeList> existingIdeaChangeLists) {
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

    private LocalChangeList createUniqueChangeList(ChangeListManagerGate addGate, IChangelistSummary cls, List<LocalChangeList> existingIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        String name = desc[0];
        int count = -1;
        findLoop:
        while (true) {
            // Make sure we check against ALL change lists, not just the
            // filtered ones.
            for (LocalChangeList lcl : existingIdeaChangeLists) {
                if (lcl.getName().equals(name)) {
                    // the new name collides with an existing change (not the same change)
                    name = desc[0] + " (" + (++count) + ")";
                    continue findLoop;
                }
            }

            // it's a unique name!
            LOG.info("Mapped @" + cls.getId() + " to new IDEA change " + name);
            LocalChangeList ret = addGate.addChangeList(name, desc[1]);
            existingIdeaChangeLists.add(ret);
            return ret;
        }
    }

    private List<LocalChangeList> filterOutUsedChangelists(
            List<LocalChangeList> existingIdeaChangeLists,
            Map<LocalChangeList, Map<Client, IChangelistSummary>> known,
            Client client) {
        List<LocalChangeList> ret = new ArrayList<LocalChangeList>(existingIdeaChangeLists);
        for (Map.Entry<LocalChangeList, Map<Client, IChangelistSummary>> e: known.entrySet()) {
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

    private boolean knownChangelist(@NotNull Client client, @NotNull IChangelistSummary cls, @NotNull Map<LocalChangeList, Map<Client, IChangelistSummary>> known) {
        for (Map<Client, IChangelistSummary> map: known.values()) {
            IChangelistSummary c = map.get(client);
            if (c != null && c.getId() == cls.getId()) {
                return true;
            }
        }
        return false;
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

        for (P4FileInfo file : files) {
            VirtualFile vf = file.getPath().getVirtualFile();
            LOG.info("processing " + file);

            // VirtualFile can be null.  That means the file is deleted.

            if (file.isOpenInClient()) {
                LOG.info("already open in a changelist on the server");

                LocalChangeList changeList = vcs.getChangeListMapping().getLocalChangelist(client, file.getChangelist());
                if (changeList == null) {
                    LOG.error("Did not map an IntelliJ changelist for Perforce changelist " + file.getChangelist() +
                            " (file " + file + ")");
                    //LOG.info("Did not map an IntelliJ changelist for Perforce changelist " + file.getChangelist());
                    //changeList = addGate.findOrCreateList("(@" + file.getChangelist() + ")", "");
                    //vcs.getChangeListMapping().createMappingToP4Id(changeList, file.getChangelist());
                    //changeList = clm.getDefaultChangeList();
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
        List<P4FileInfo> opened = client.getServer().loadOpenFiles(client.getRoots().toArray(new VirtualFile[client.getRoots().size()]));
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
            //builder.reportChangesOutsideProject() ?
        }
    }

    private static String[] splitNameComment(IChangelistSummary p4cl) {
        String[] ret = new String[2];
        String desc = p4cl.getDescription();
        if (p4cl.getId() == IChangelist.DEFAULT) {
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


    private static interface ClMatcher {
        @Nullable
        LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists);
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
        public LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull IChangelistSummary cls, @NotNull String[] desc, @NotNull List<LocalChangeList> unusedIdeaChangelists) {
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
