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
package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.ChangeListBuilderCache;
import net.groboclown.idea.p4ic.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.history.P4ContentRevision;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
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

    private final Project project;
    private final P4Vcs vcs;
    private final AlertManager alerts;

    public ChangeListSync(@NotNull final P4Vcs vcs, @NotNull AlertManager alerts) {
        this.project = vcs.getProject();
        this.vcs = vcs;
        this.alerts = alerts;
    }


    /**
     * Refreshes the changes.  It only acts upon the dirty files.
     *
     * @param builder data access and storage
     * @param progress progress bar
     * @throws VcsException
     */
    public void syncChanges(@NotNull Set<FilePath> dirtyFiles, @NotNull ChangeListBuilderCache builder,
            @NotNull final ChangeListManagerGate addGate, @NotNull ProgressIndicator progress)
            throws VcsException, InterruptedException {
        // TODO add in progress indicator

        // FIXME change all these info to debug
        LOG.info("start changelist refresh", new Throwable("stack capture"));


        { // Strip out directories from the search.
            final Iterator<FilePath> iter = dirtyFiles.iterator();
            while (iter.hasNext()) {
                if (iter.next().isDirectory()) {
                    iter.remove();
                }
            }
        }

        // Load all the dirty files into the correct changelist, based on the
        // server state of the file.

        final Map<P4Server, List<FilePath>> mappedFiles =
                vcs.mapFilePathsToP4Server(dirtyFiles);

        final Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> mappedChanges = getLocalChangelistMapping(
                mappedFiles.keySet(), addGate);


        for (Entry<P4Server, List<FilePath>> serverFileEntry: mappedFiles.entrySet()) {
            P4Server server = serverFileEntry.getKey();
            if (server == null) {
                // files that do not map to a server, so they are not covered by VCS.
                for (FilePath fp : serverFileEntry.getValue()) {
                    LOG.info(" - server-less file: " + fp);
                    final VirtualFile vf = fp.getVirtualFile();
                    if (vf != null) {
                        LOG.info(" --- marked as unversioned");
                        builder.processUnversionedFile(vf);
                        if (! dirtyFiles.remove(fp)) {
                            LOG.warn("Duplicate file processed (1): " + vf);
                        }
                    } else {
                        LOG.info(" --- no virtual file");
                        builder.processLocallyDeletedFile(fp);
                        if (! dirtyFiles.remove(fp)) {
                            LOG.warn("Duplicate file processed (2): " + fp);
                        }
                    }
                }
                // don't need to put the "else" in an indention...
                continue;
            }

            // Files on the server.  This allows us to know whether the
            // file is currently tracked by the server or not (e.g. whether
            // a change is a delete, add, or edit).

            // Always use the server.getOpenedFiles first.  The files that
            // aren't matched after that are not already open; we pull their
            // server status separately.

            Set<FilePath> unopenedFiles = new HashSet<FilePath>(serverFileEntry.getValue());

            // Files which we know their current state; note that we
            // only track the files that the current client has open
            // (delete, edit, add, integrate, move).
            final Collection<P4FileAction> opened = server.getOpenFiles();
            final Iterator<FilePath> iter = unopenedFiles.iterator();
            while (iter.hasNext()) {
                final FilePath fp = iter.next();
                P4FileAction action = findActionFor(fp, opened);
                if (action != null) {
                    // Matched this file state
                    iter.remove();
                    LocalChangeList changeList = getChangeList(action, server, mappedChanges);
                    LOG.info(" --- mapped file " + fp + " to known change, in local changelist " + changeList);
                    ensureOnlyIn(action, changeList, builder);
                    if (!dirtyFiles.remove(fp)) {
                        LOG.warn("Duplicate file processed (3): " + fp);
                    }
                }
            }

            LOG.info(" --- handling unknown p4 action for " + unopenedFiles);
            final Map<FilePath, IExtendedFileSpec> status =
                    server.getFileStatus(unopenedFiles);
            if (status == null) {
                // working offline - nothing can be discovered about the state.
                for (FilePath unopenedFile: unopenedFiles) {
                    VirtualFile vf = unopenedFile.getVirtualFile();
                    if (vf == null) {
                        LOG.info(" --- locally deleted: " + unopenedFile);
                        builder.processLocallyDeletedFile(unopenedFile);
                        if (!dirtyFiles.remove(unopenedFile)) {
                            LOG.warn("Duplicate file processed (4): " + unopenedFile);
                        }
                    } else {
                        LOG.info(" --- modified without checkout: " + vf);
                        builder.processModifiedWithoutCheckout(vf);
                        if (!dirtyFiles.remove(unopenedFile)) {
                            LOG.warn("Duplicate file processed (5): " + unopenedFile);
                        }
                    }
                }
            } else {
                for (Entry<FilePath, IExtendedFileSpec> specEntry : status.entrySet()) {
                    FilePath fp = specEntry.getKey();
                    VirtualFile vf = fp.getVirtualFile();
                    final IExtendedFileSpec spec = specEntry.getValue();
                    if (spec.getOpStatus() != FileSpecOpStatus.VALID ||
                            spec.getOpenAction() == null) {
                        LOG.info(" -- spec status: " + spec.getOpStatus() + " [" + spec.getStatusMessage() + "]");
                        LOG.info(" -- spec open action: " + spec.getOpenAction());
                        // assume it's a file that isn't added yet.
                        if (vf == null) {
                            builder.processLocallyDeletedFile(fp);
                            LOG.info(" --- invalid p4 status, locally deleted: " + fp);
                            if (!dirtyFiles.remove(fp)) {
                                LOG.warn("Duplicate file processed (6): " + fp);
                            }
                        } else if (server.isIgnored(specEntry.getKey())) {
                            LOG.info(" --- in ignore file: " + vf);
                            builder.processIgnoredFile(vf);
                            if (!dirtyFiles.remove(fp)) {
                                LOG.warn("Duplicate file processed (7): " + fp);
                            }
                        } else {
                            // not on server, and user did not indicate that they
                            // wanted to edit it.
                            LOG.info(" --- user didn't want to edit: " + vf);
                            builder.processUnversionedFile(vf);
                            if (!dirtyFiles.remove(fp)) {
                                LOG.warn("Duplicate file processed (8): " + fp);
                            }
                        }
                    } else {
                        // FIXME do something smart
                        alerts.addWarning(P4Bundle.message("errors.changelist.mapping",
                                fp, spec.getDepotPath()), null);
                        if (vf == null) {
                            LOG.info(" --- no changelist mapping, marking as locally deleted: " + fp);
                            builder.processLocallyDeletedFile(fp);
                            if (!dirtyFiles.remove(fp)) {
                                LOG.warn("Duplicate file processed (9): " + fp);
                            }
                        } else {
                            LOG.info(" --- no changelist mapping, marking as modified without checkout; " + vf);
                            builder.processModifiedWithoutCheckout(vf);
                            if (!dirtyFiles.remove(fp)) {
                                LOG.warn("Duplicate file processed (10): " + fp);
                            }
                        }
                    }
                }
            }
        }


        LOG.info("Remaining dirty files: " + new ArrayList<FilePath>(dirtyFiles));
        if (! dirtyFiles.isEmpty()) {
            throw new VcsException("Did not process all the dirty files: " + new ArrayList<FilePath>(dirtyFiles));
        }
    }

    private void ensureOnlyIn(@NotNull P4FileAction action, @NotNull LocalChangeList changeList,
            @NotNull ChangeListBuilderCache builder) {
        boolean movedChange = false;
        if (action.getFile() == null) {
            throw new IllegalStateException("File action " + action + " has no local file setting");
        }
        for (LocalChangeList cl: ChangeListManager.getInstance(project).getChangeLists()) {
            for (Change change: cl.getChanges()) {
                if (change.affectsFile(action.getFile().getIOFile())) {
                    if (changeList.equals(cl)) {
                        LOG.info(" --- Keeping " + action.getFile() + " in " + changeList);
                        // null changelist: setting the changelist will cause infinite
                        // reloads, because it means that the changelist has changed.
                        builder.processChange(change, null);
                    } else {
                        LOG.info(" --- Moving " + action.getFile() + " out of " + cl + " into " + changeList);
                        builder.processChange(change, changeList);
                    }
                    movedChange = true;
                }
            }
        }
        if (! movedChange) {
            LOG.info(" --- Put " + action.getFile() + " into " + changeList);
            builder.processChange(createChange(action), changeList);
        }
    }

    @NotNull
    private LocalChangeList getChangeList(@NotNull P4FileAction action, @NotNull P4Server server,
            @NotNull Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> mappedChanges) {
        final Map<P4ChangeListValue, LocalChangeList> changes = mappedChanges.get(server);
        if (changes == null) {
            throw new IllegalStateException("Bad setup: no changes set for server " + server.getClientServerId());
        }

        for (Entry<P4ChangeListValue, LocalChangeList> entry: changes.entrySet()) {
            final P4ChangeListValue p4cl = entry.getKey();
            // only need to check for the changelist #.  The changes are going to be
            // in the same server.
            if (p4cl.getChangeListId() == action.getChangeList()) {
                return entry.getValue();
            }
        }

        throw new IllegalStateException("Bad setup: no matching changelist for server " + server.getClientServerId() +
                " changelist " + action.getChangeList() + " (known changes for server: " + changes.keySet() + ")");
    }

    @Nullable
    private P4FileAction findActionFor(@NotNull FilePath fp, @NotNull Collection<P4FileAction> opened) {
        for (P4FileAction action : opened) {
            if (action.affects(fp)) {
                return action;
            }
        }
        return null;
    }

    private Change createChange(@NotNull P4FileAction file) {
        // FIXME looks like we need 2 kinds of revision classes: one for this method, one for the history methods.
        ContentRevision beforeRev = new P4ContentRevision(project, file);
        ContentRevision afterRev = new CurrentContentRevision(file.getFile());
        return new Change(beforeRev, afterRev, file.getClientFileStatus());
    }


    @NotNull
    private Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> getLocalChangelistMapping(
            @NotNull Set<P4Server> p4Servers, @NotNull ChangeListManagerGate addGate)
            throws InterruptedException {
        Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> ret = new HashMap<P4Server, Map<P4ChangeListValue, LocalChangeList>>();
        for (P4Server server : p4Servers) {
            if (server != null) {
                final Collection<P4ChangeListValue> changes = server.getOpenChangeLists();
                ret.put(server, mapIdeaChanges(addGate, changes));
            }
        }
        return ret;
    }

    @NotNull
    private Map<P4ChangeListValue, LocalChangeList> mapIdeaChanges(@NotNull ChangeListManagerGate addGate,
            @NotNull Collection<P4ChangeListValue> changes) {
        // Note: the "unused local changes" is based on a per-server basis.
        // This means an idea changelist could potentially be shared across servers
        // when it was constructed by this mapping, rather than by the user.
        // However, that should be fine.

        final List<LocalChangeList> allLocalChanges = ChangeListManager.getInstance(project).getChangeLists();
        final List<LocalChangeList> unusedLocalChanges = new ArrayList<LocalChangeList>(allLocalChanges);
        final Map<P4ChangeListValue, LocalChangeList> ret = new HashMap<P4ChangeListValue, LocalChangeList>();
        final List<P4ChangeListValue> unmapped = new ArrayList<P4ChangeListValue>();
        for (P4ChangeListValue change: changes) {
            LocalChangeList local = vcs.getChangeListMapping().getIdeaChangelistFor(change);
            if (local == null) {
                unmapped.add(change);
            } else {
                ret.put(change, local);
                unusedLocalChanges.remove(local);
            }
        }

        if (! unmapped.isEmpty()) {
            loadUnmappedChangelists(ret, unmapped, unusedLocalChanges, allLocalChanges, addGate);
        }

        return ret;
    }

    private void loadUnmappedChangelists(
            @NotNull Map<P4ChangeListValue, LocalChangeList> mapping,
            @NotNull List<P4ChangeListValue> unmappedP4,
            @NotNull Collection<LocalChangeList> unusedIdeaChangeLists,
            @NotNull Collection<LocalChangeList> allIdeaChangeLists,
            @NotNull ChangeListManagerGate addGate) {
        // For each p4 pending changelist that isn't already associated with an
        // IDEA changelist, find a match to any IDEA changelist that doesn't
        // already have a p4 changelist for that specific client.

        // However, the outer loop is the matchers.  This prevents aggressive
        // consumption of changes - allow the more exact matchers to match the
        // changelists before a looser one tries to match.

        for (ClMatcher matcher : CL_MATCHERS) {
            final Iterator<P4ChangeListValue> iter = unmappedP4.iterator();
            while (iter.hasNext()) {
                final P4ChangeListValue p4cl = iter.next();
                if (mapping.containsKey(p4cl)) {
                    // Already been mapped in this loop
                    iter.remove();
                    continue;
                }

                if (p4cl.isDefaultChangelist()) {
                    // special default changelist handling.  The IDEA default
                    // named changelist should always exist to correspond to the
                    // Perforce default changelist.  Note that this is
                    // independent of the matchers.

                    LOG.info("Associating " + p4cl.getClientServerId() + " default changelist to IDEA changelist " +
                            P4ChangeListMapping.DEFAULT_CHANGE_NAME);
                    LocalChangeList local = addGate.findOrCreateList(P4ChangeListMapping.DEFAULT_CHANGE_NAME, "");
                    mapping.put(p4cl, local);
                    iter.remove();
                } else {
                    // it's not associated with any IDEA changelist.
                    LocalChangeList match = matcher.match(p4cl, splitNameComment(p4cl), unusedIdeaChangeLists);

                    if (match != null) {
                        LOG.info("Associating " + p4cl.getChangeListId() + " to IDEA changelist " + match);

                        // Ensure the name matches the changelist, with a unique name
                        setUniqueName(match, p4cl, allIdeaChangeLists);

                        // Record this new association
                        mapping.put(p4cl, match);
                        iter.remove();

                        // TODO this line needs to be better handled to remove all the public cruft.
                        vcs.getChangeListMapping().bindChangelists(match, p4cl.getIdObject());
                    }
                }
            }
        }

        for (P4ChangeListValue p4cl : unmappedP4) {
            LocalChangeList lcl = createUniqueChangeList(addGate, p4cl, allIdeaChangeLists);
            mapping.put(p4cl, lcl);

            // TODO this line needs to be better handled to remove all the public cruft.
            LOG.info("Binding " + p4cl.getChangeListId() + " to IDEA changelist " + lcl);
            vcs.getChangeListMapping().bindChangelists(lcl, p4cl.getIdObject());
        }
    }

    private static String[] splitNameComment(P4ChangeListValue p4cl) {
        String[] ret = new String[2];
        String desc = p4cl.getComment();
        if (p4cl.isDefaultChangelist()) {
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

    // This matches with createUniqueChangeList
    private void setUniqueName(@NotNull LocalChangeList changeList,
            @NotNull P4ChangeListValue cls,
            @NotNull Collection<LocalChangeList> existingIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        LOG.debug("Mapped @" + cls.getChangeListId() + " to " + changeList.getName());
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
            @NotNull P4ChangeListValue cls, @NotNull Collection<LocalChangeList> allIdeaChangeLists) {
        String[] desc = splitNameComment(cls);
        String baseDesc = desc[0];
        if (baseDesc == null || baseDesc.length() <= 0) {
            baseDesc = '@' + Integer.toString(cls.getChangeListId());
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
            LOG.info("Mapped " + cls.getChangeListId() + " to new IDEA change " + name);
            // This can sometimes cause an assertion error, so don't let that
            // happen - use "findOrCreateList" instead of "addList".
            LocalChangeList ret = addGate.findOrCreateList(name, desc[1]);
            allIdeaChangeLists.add(ret);
            return ret;
        }
    }


    private interface ClMatcher {
        @Nullable
        LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists);
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
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
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
        public LocalChangeList match(@NotNull P4ChangeListValue cls, @NotNull String[] desc,
                @NotNull Collection<LocalChangeList> unusedIdeaChangelists) {
            // check exact match on name & comment
            // match ignore case "[p4 name](\s+\(\d+\)\)"
            String match = "(@" + cls.getChangeListId() + ")";
            for (LocalChangeList lcl : unusedIdeaChangelists) {
                if (match.equals(lcl.getName().trim())) {
                    return lcl;
                }
            }
            return null;
        }
    }
}
