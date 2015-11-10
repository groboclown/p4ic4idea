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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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

    private final Project project;
    private final P4Vcs vcs;
    private final AlertManager alerts;
    private final ChangeListMatcher changeListMatcher;

    public P4ChangeProvider(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
        this.vcs = vcs;
        this.alerts = AlertManager.getInstance();
        this.changeListMatcher = new ChangeListMatcher(vcs, alerts);
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
    public void getChanges(VcsDirtyScope dirtyScope, ChangelistBuilder builder,
            ProgressIndicator progress,
            ChangeListManagerGate addGate) throws VcsException {
        progress.setFraction(0.0);
        if (project.isDisposed()) {
            progress.setFraction(1.0);
            return;
        }

        // This check is kind of necessary.  It's ensuring that IntelliJ is doing the right thing.
        // Note the identity equals, not .equals().
        if (dirtyScope.getVcs() != vcs) {
            throw new VcsException(P4Bundle.message("error.vcs.dirty-scope.wrong"));
        }

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
        // files into their IDEA changelists, but sometimes, there can be an
        // existing change in another changelist.

        final Set<FilePath> dirtyFiles;
        if (dirtyScope.wasEveryThingDirty()) {
            dirtyFiles = null;
        } else {
            dirtyFiles = dirtyScope.getDirtyFiles();
            if (dirtyFiles == null || dirtyFiles.isEmpty()) {
                LOG.info("No dirty files.");
                progress.setFraction(1.0);
                return;
            }
        }

        try {
            // As part of the execution, we'll include an integrity check, to ensure the
            // local cache matches up with the remaining actions.
            for (P4Server server : vcs.getP4Servers()) {
                server.checkLocalIntegrity();
            }

            syncChanges(dirtyFiles, builder, addGate, progress);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    private void syncChanges(@Nullable final Set<FilePath> dirtyFiles,
            @NotNull final ChangelistBuilder builder,
            @NotNull final ChangeListManagerGate addGate,
            final ProgressIndicator progress) throws InterruptedException {
        MappedOpenFiles mapped = getOpenedFiles(dirtyFiles, progress);
        progress.setFraction(0.80);

        /* marking dirty may cause infinite loops.
        This is why we have the special case for "everything is dirty",
        as it is usually called at startup or at critical times, which allows
        the change provider to mark anything as dirty.
        List<VirtualFile> dirty = new ArrayList<VirtualFile>();
        for (Entry<P4Server, Set<P4FileAction>> entry : mapped.notDirtyOpenedFiles.entrySet()) {
            for (P4FileAction action : entry.getValue()) {
                if (action.getFile() != null && action.getFile().getVirtualFile() != null) {
                    dirty.add(action.getFile().getVirtualFile());
                }
            }
        }
        VfsUtil.markDirty(false, false, dirty.toArray(new VirtualFile[dirty.size()]));
        */
        if (LOG.isDebugEnabled()) {
            LOG.debug("ignoring files that should already be considered dirty: " +
                    mapped.notDirtyOpenedFiles);
        }

        for (FilePath file : mapped.noServerDirtyFiles) {
            if (file.getVirtualFile() == null) {
                builder.processLocallyDeletedFile(file);
            } else {
                builder.processIgnoredFile(file.getVirtualFile());
            }
        }
        progress.setFraction(0.82);

        for (Entry<FilePath, P4Server> entry : mapped.notAddedDirtyFiles.entrySet()) {
            FilePath file = entry.getKey();
            VirtualFile virt = file.getVirtualFile();
            if (virt == null) {
                builder.processLocallyDeletedFile(file);
            }
            if (entry.getValue().isIgnored(file)) {
                builder.processIgnoredFile(virt);
            } else {
                builder.processUnversionedFile(virt);
            }
        }
        progress.setFraction(0.84);

        for (FilePath file : mapped.notEditedDirtyFiles.keySet()) {
            VirtualFile virt = file.getVirtualFile();
            if (virt == null) {
                builder.processLocallyDeletedFile(file);
            } else {
                // In this situation, there are times where a file is
                // marked as dirty, it hasn't been checked out, and the
                // file isn't different than the server version.

                // This needs to be verified by comparing against the
                // server version, but that can only happen if we're
                // online.

                final P4Server server = mapped.notEditedDirtyFiles.get(file);
                if (server.isWorkingOnline()) {
                    if (isDifferentThanServerCopy(server, file)) {
                        builder.processModifiedWithoutCheckout(virt);
                    } else {
                        // do nothing with the file.
                        LOG.info("Incorrectly tagged " + file + " as dirty.");
                    }
                } else {
                    // TODO this needs a special case, in that we can't
                    // tell if it's different or not.
                    builder.processModifiedWithoutCheckout(virt);
                }
            }
        }

        progress.setFraction(0.85);
        final Map<P4Server, Map<P4ChangeListValue, LocalChangeList>> changeListMappings =
                changeListMatcher.getLocalChangelistMapping(mapped.affectedServers, addGate);

        progress.setFraction(0.98);
        for (Entry<FilePath, ServerAction> entry : mapped.dirtyP4Files.entrySet()) {
            LocalChangeList changeList = changeListMatcher.getChangeList(
                    entry.getValue().action, entry.getValue().server, changeListMappings);
            ensureOnlyIn(entry.getValue().action, changeList, builder);
        }

        progress.setFraction(1.0);
    }

    // There are situations where a change for a file is already registered in a changelist.
    // We must not create a new change associated with a different changelist, because
    // that will cause duplicate entries.
    private void ensureOnlyIn(@NotNull P4FileAction action, @NotNull LocalChangeList changeList,
            @NotNull ChangelistBuilder builder) {
        if (action.getFile() == null) {
            throw new IllegalStateException("File action " + action + " has no local file setting");
        }

        /* We must explicitly add all changes into the changelists, otherwise
        it doesn't show up in the changes.
        If a file ends up being in two changes at once, then there's a bug in this
        code.  This particular loop ends up just putting files into the wrong
        state if the state changed (say, moved from edit to delete).

        // FIXME remove this dead code, and rename the method
        boolean movedChange = false;
        for (LocalChangeList cl : ChangeListManager.getInstance(project).getChangeLists()) {
            for (Change change : cl.getChanges()) {
                if (change.affectsFile(action.getFile().getIOFile())) {
                    if (changeList.equals(cl)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(" --- Keeping " + action.getFile() + " in " + changeList);
                        }
                        // null changelist: setting the changelist will cause infinite
                        // reloads, because it means that the changelist has changed.
                        builder.processChange(change, P4Vcs.getKey());
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(" --- Moving " + action.getFile() + " out of " + cl + " into " + changeList);
                        }
                        builder.processChangeInList(change, changeList, P4Vcs.getKey());
                    }
                    movedChange = true;
                }
            }
        }
        */
        //if (!movedChange) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(" --- Put " + action.getFile() + " into " + changeList);
            }
            builder.processChangeInList(
                    changeListMatcher.createChange(action),
                    changeList,
                    P4Vcs.getKey());
        //}
    }


    private boolean isDifferentThanServerCopy(@NotNull P4Server server, @NotNull FilePath file) {
        try {
            assert file.getVirtualFile() != null;
            final byte[] serverCopy = server.loadFileAsBytesOnline(file, P4ChangeListId.P4_UNKNOWN);
            final byte[] localCopy = file.getVirtualFile().contentsToByteArray();
            return Arrays.equals(serverCopy, localCopy);
        } catch (P4DisconnectedException e) {
            LOG.info("Could not check online state, as the server just went offline", e);
            return true;
        } catch (InterruptedException e) {
            LOG.info("Could not get server bytes from " + file, e);
            return true;
        } catch (IOException e) {
            LOG.info("Could not get local bytes from " + file, e);
            return true;
        }
    }


    @NotNull
    private MappedOpenFiles getOpenedFiles(@Nullable final Set<FilePath> dirtyFiles,
            @NotNull final ProgressIndicator progress)
            throws InterruptedException {
        if (dirtyFiles != null) {
            return new MappedOpenFiles(vcs, alerts, dirtyFiles, progress);
        } else {
            return new MappedOpenFiles(vcs, alerts, progress);
        }
    }



    static class ServerAction {
        final P4Server server;
        final P4FileAction action;

        ServerAction(final P4Server server, final P4FileAction action) {
            this.server = server;
            this.action = action;
        }
    }

    static class MappedOpenFiles {
        // for reference
        final Set<FilePath> scopedDirtyFiles;

        final Set<P4Server> affectedServers;
        final Set<FilePath> noServerDirtyFiles;
        final Map<FilePath, P4Server> notAddedDirtyFiles;
        final Map<FilePath, P4Server> notEditedDirtyFiles;
        final Map<FilePath, ServerAction> dirtyP4Files;
        final Map<P4Server, Set<P4FileAction>> notDirtyOpenedFiles;

        MappedOpenFiles(@NotNull P4Vcs vcs, @NotNull AlertManager alerts,
                @NotNull Set<FilePath> scopedDirtyFiles, @NotNull final ProgressIndicator progress)
                throws InterruptedException {
            this.scopedDirtyFiles = scopedDirtyFiles;

            // We could just discover the open state for the dirty files,
            // which is the optimal way to handle this method, but for now,
            // just find everything that's dirty and organize it.
            // TODO optimize this to only check dirty files.
            final Set<FilePath> unknownDirties = new HashSet<FilePath>(scopedDirtyFiles);
            this.notDirtyOpenedFiles = new HashMap<P4Server, Set<P4FileAction>>();
            this.dirtyP4Files = new HashMap<FilePath, ServerAction>();

            this.affectedServers = new HashSet<P4Server>(vcs.getP4Servers());

            for (P4Server server: affectedServers) {
                final Collection<P4FileAction> opened = server.getOpenFiles();
                if (! opened.isEmpty()) {
                    affectedServers.add(server);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Opened files for " + server + ": " + opened);
                }
                for (P4FileAction file: opened) {
                    final FilePath fp = file.getFile();
                    if (fp == null) {
                        alerts.addNotice(vcs.getProject(),
                                P4Bundle.message("unknown.opened.file.path", file.getDepotPath()),
                                null);
                        Set<P4FileAction> fileSet = notDirtyOpenedFiles.get(server);
                        if (fileSet == null) {
                            fileSet = new HashSet<P4FileAction>();
                            notDirtyOpenedFiles.put(server, fileSet);
                        }
                        fileSet.add(file);
                        continue;
                    }

                    if (unknownDirties.remove(fp)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Marking dirty file for " + server.getClientServerId() + ": " + fp);
                        }
                        dirtyP4Files.put(fp, new ServerAction(server, file));
                    } else {
                        Set<P4FileAction> fileSet = notDirtyOpenedFiles.get(server);
                        if (fileSet == null) {
                            fileSet = new HashSet<P4FileAction>();
                            notDirtyOpenedFiles.put(server, fileSet);
                        }
                        fileSet.add(file);
                    }
                }
            }

            // Setup the unknown files that are marked as dirty

            // ensure that directories aren't marked as unknown and dirty.
            final Iterator<FilePath> iter = unknownDirties.iterator();
            while (iter.hasNext()) {
                if (iter.next().isDirectory()) {
                    iter.remove();
                }
            }
            this.noServerDirtyFiles = new HashSet<FilePath>();
            this.notEditedDirtyFiles = new HashMap<FilePath, P4Server>();
            this.notAddedDirtyFiles = new HashMap<FilePath, P4Server>();
            final Map<P4Server, List<FilePath>> unknownMap = vcs.mapFilePathsToP4Server(unknownDirties);
            for (Entry<P4Server, List<FilePath>> serverListEntry : unknownMap.entrySet()) {
                P4Server server = serverListEntry.getKey();
                if (server == null) {
                    for (FilePath filePath : serverListEntry.getValue()) {
                        noServerDirtyFiles.add(filePath);
                    }
                } else if (! serverListEntry.getValue().isEmpty()) {
                    affectedServers.add(server);
                    final Map<FilePath, IExtendedFileSpec> status =
                            server.getFileStatus(serverListEntry.getValue());
                    if (status == null) {
                        // Mapped to the server, but we're disconnected, so we can't tell
                        // if they've actually been added or not.
                        // Just assume that they're on the server.
                        for (FilePath filePath : serverListEntry.getValue()) {
                            notEditedDirtyFiles.put(filePath, server);
                        }
                    } else {
                        // Mapped to the server, and we can tell if they've been
                        // added or not.
                        for (Entry<FilePath, IExtendedFileSpec> entry: status.entrySet()) {
                            if (isStoredOnServer(entry.getValue())) {
                                notEditedDirtyFiles.put(entry.getKey(), server);
                            } else {
                                notAddedDirtyFiles.put(entry.getKey(), server);
                            }
                        }
                    }
                }
            }
        }

        MappedOpenFiles(@NotNull P4Vcs vcs, @NotNull AlertManager alerts,
                @NotNull final ProgressIndicator progress)
                throws InterruptedException {
            // Discover everything that is dirty as known by the server.
            // Nothing is not-dirty, and nothing is locally changed.

            this.noServerDirtyFiles = Collections.emptySet();
            this.notAddedDirtyFiles = Collections.emptyMap();
            this.notEditedDirtyFiles = Collections.emptyMap();
            this.notDirtyOpenedFiles = Collections.emptyMap();

            this.scopedDirtyFiles = new HashSet<FilePath>();
            this.dirtyP4Files = new HashMap<FilePath, ServerAction>();

            this.affectedServers = new HashSet<P4Server>(vcs.getP4Servers());

            LOG.debug("Performing 'all dirty' refresh");

            for (P4Server server : affectedServers) {
                final Collection<P4FileAction> opened = server.getOpenFiles();
                for (P4FileAction file : opened) {
                    final FilePath fp = file.getFile();
                    if (fp == null) {
                        alerts.addNotice(vcs.getProject(),
                                P4Bundle.message("unknown.opened.file.path", file.getDepotPath()),
                                null);
                        continue;
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Marking dirty: " + fp);
                    }
                    dirtyP4Files.put(fp, new ServerAction(server, file));
                    scopedDirtyFiles.add(fp);
                }
            }
        }

        private boolean isStoredOnServer(@Nullable final IExtendedFileSpec spec) {
            if (spec == null) {
                return false;
            }
            if (spec.getOpStatus() != FileSpecOpStatus.VALID) {
                return false;
            }
            return (spec.getHeadRev() > 0);
        }
    }
}
