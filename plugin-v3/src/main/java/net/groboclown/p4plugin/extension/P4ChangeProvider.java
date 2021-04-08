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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.cache.messagebus.AbstractCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.util.EqualUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.revision.P4DeletedLocalFileRevision;
import net.groboclown.p4plugin.revision.P4LocalFileContentRevision;
import net.groboclown.p4plugin.revision.P4RemoteFileContentRevision;
import net.groboclown.p4plugin.util.ChangelistUtil;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import net.groboclown.p4plugin.util.RemoteFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final HistoryContentLoader loader;

    private volatile boolean active = false;
    private volatile Exception activeThread = null;

    P4ChangeProvider(@NotNull P4Vcs vcs, @NotNull Disposable parentDisposable) {
        this.project = vcs.getProject();
        this.vcs = vcs;
        this.loader = new HistoryContentLoaderImpl(project);

        final MessageBusClient.ApplicationClient mbClient = MessageBusClient.forApplication(parentDisposable);
        final String cacheId = AbstractCacheMessage.createCacheId(project, P4ChangeProvider.class);
        ClientActionMessage.addListener(mbClient, cacheId, event -> {
            P4CommandRunner.ClientActionCmd cmd = (P4CommandRunner.ClientActionCmd) event.getAction().getCmd();
            try {
                switch (cmd) {
                    case CREATE_CHANGELIST:
                        onChangelistCreated(
                                (CreateChangelistAction) event.getAction(),
                                (CreateChangelistResult) event.getResult(),
                                event.getState());
                        break;
                    case DELETE_CHANGELIST:
                        if (event.getState() == ClientActionMessage.ActionState.PENDING) {
                            onChangelistDelete((DeleteChangelistAction) event.getAction());
                        }
                        break;
                }
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException(e)));
            }
        });
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
        LOG.warn("FIXME clean up cache for the files.");
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

        if (active) {
            LOG.warn("Second call into already active getChanges. Ignoring call.");
            LOG.warn("Original active thread", activeThread);
            LOG.warn("Current thread", new Exception());
            return;
        }
        try {
            active = true;
            activeThread = new Exception();

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

            Collection<ClientConfigRoot> allClientRoots = getClientConfigRoots();

            // This request is performed by the IDE in a background thread, so it can block.

            Pair<IdeChangelistMap, IdeFileMap> cachedMaps = CacheComponent.getInstance(project)
                    .blockingRefreshServerOpenedCache(
                            allClientRoots,
                            UserProjectPreferences.getLockWaitTimeoutMillis(project),
                            TimeUnit.MILLISECONDS
                    );
            if (cachedMaps.first == null || cachedMaps.second == null) {
                // This can happen if a non-P4Vcs project calls into here.
                progress.setFraction(1.0);
                return;
            }

            lastFraction = 0.6;
            progress.setFraction(lastFraction);


            try {
                // For now, just blocking get right here.
                updateChangelists(cachedMaps.first, addGate)
                        .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException("Update Changelists interrupted", e)));
                progress.setFraction(1.0);
                return;
            }

            if (dirtyScope.wasEveryThingDirty()) {
                // Update all the files.

                double fractionRootIncr = (1.0 - lastFraction) / allClientRoots.size();
                for (ClientConfigRoot root : allClientRoots) {
                    LOG.info("Processing changes in " + root.getProjectVcsRootDir());

                    updateFileCache(root.getClientConfig(),
                            cachedMaps.first, cachedMaps.second, builder);

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

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing dirty files " + dirtyFiles);
                }
                Map<VcsRoot, List<FilePath>> fileRoots = VcsUtil.groupByRoots(project, dirtyFiles, (f) -> f);
                if (!fileRoots.isEmpty()) {
                    double fractionRootIncr = (1.0 - lastFraction) / fileRoots.size();
                    for (Map.Entry<VcsRoot, List<FilePath>> entry : fileRoots.entrySet()) {
                        if (entry.getKey().getVcs() != null &&
                                P4Vcs.getKey().equals(entry.getKey().getVcs().getKeyInstanceMethod())) {
                            VirtualFile root = entry.getKey().getPath();
                            ClientConfigRoot config = getClientFor(root);
                            Set<FilePath> matched =
                                    getMatchedDirtyRootFiles(dirtyFiles, config, root, entry.getValue());
                            if (!matched.isEmpty() && config != null) {
                                updateFileCache(config.getClientConfig(),
                                        matched, cachedMaps.first, cachedMaps.second, builder);
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
        } finally {
            active = false;
            activeThread = null;
        }
    }

    private Set<FilePath> getMatchedDirtyRootFiles(Set<FilePath> dirtyFiles,
            @Nullable ClientConfigRoot config, VirtualFile root, List<FilePath> changedFiles) {
        LOG.info("Processing changes for " + changedFiles + " under " + root);
        if (config == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping because not under a Perforce root: " + root + "; expected key " + P4Vcs.getKey());
            }
            return Collections.emptySet();
        }
        Set<FilePath> matched = new HashSet<>();
        for (FilePath filePath : changedFiles) {
            if (dirtyFiles.remove(filePath)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Matched " + filePath + " in dirty file list");
                }
                matched.add(filePath);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Not in dirty file list (" + filePath + "); already processed?");
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing under " + root + ": matched dirty files " +
                    changedFiles + " to root: " + matched);
            LOG.debug("Remaining dirty files: " + dirtyFiles);
        }
        return matched;
    }

    private P4CommandRunner.ActionAnswer<Object> updateChangelists(IdeChangelistMap changelistMap, ChangeListManagerGate addGate)
            throws InterruptedException {
        // Need to make a copy of the changelists, because it can be immutable.
        // See bug #226
        List<LocalChangeList> existingLocalChangeLists = new ArrayList<>(addGate.getListsCopy());
        P4CommandRunner.ActionAnswer<Object> actions = new DoneActionAnswer<>(null);

        Collection<ClientConfigRoot> roots = getClientConfigRoots();

        // #177 - prevent old clients from keeping their changelist mappings around.
        changelistMap.clearChangesNotIn(
                roots.stream()
                    .map(ClientConfigRoot::getClientConfig)
                    .map(ClientConfig::getClientServerRef)
                    .collect(Collectors.toList())
        );

        for (ClientConfigRoot clientConfigRoot : roots) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating changelists for " + clientConfigRoot);
            }
            Set<LocalChangeList> unvisitedLocalChangeLists = new HashSet<>(existingLocalChangeLists);
            for (P4LocalChangelist changelist : CacheComponent.getInstance(project).getCacheQuery()
                    .getCachedOpenedChangelists(clientConfigRoot.getClientConfig())) {
                LocalChangeList ideChangeList =
                        changelistMap.getIdeChangeFor(changelist.getChangelistId());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("P4 Changelist " + changelist.getChangelistId() +
                            " has cached map to IDE changelist " + ideChangeList);
                }
                if (ideChangeList == null) {
                    if (changelist.getChangelistId().isDefaultChangelist()) {
                        // Link to the IDE default changelist.
                        LOG.debug("Attaching default changelist to IDE default changelist");
                        LocalChangeList defaultIdeCl = ChangeListManager.getInstance(project).getDefaultChangeList();
                        unvisitedLocalChangeLists.remove(defaultIdeCl);
                        changelistMap.setMapping(changelist.getChangelistId(), defaultIdeCl);
                    } else {
                        // Create a new IDE changelist and link to that.
                        ideChangeList = addGate.addChangeList(
                                ChangelistUtil.createUniqueIdeChangeListName(changelist, null, existingLocalChangeLists,
                                        UserProjectPreferences.getMaxChangelistNameLength(project)),
                                changelist.getComment());
                        // Mark the just-created changelist as added, so that we don't attempt to use the name a
                        // second time.
                        existingLocalChangeLists.add(ideChangeList);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Attaching " + changelist + " to IDE change " + ideChangeList);
                        }
                        changelistMap.setMapping(changelist.getChangelistId(), ideChangeList);
                    }
                } else {
                    unvisitedLocalChangeLists.remove(ideChangeList);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Already attached " + changelist + " to IDE change " + ideChangeList);
                    }
                    // Don't change the name of the IDE's default changelist.
                    if (!changelist.getChangelistId().isDefaultChangelist()) {
                        // Only change things up if the p4 changelist comment changed.
                        if (!EqualUtil.isEqual(changelist.getComment(), ideChangeList.getComment())) {
                            addGate.editComment(ideChangeList.getName(), changelist.getComment());
                            String newName = ChangelistUtil.createUniqueIdeChangeListName(changelist, ideChangeList,
                                    existingLocalChangeLists,
                                    UserProjectPreferences.getMaxChangelistNameLength(project));
                            if (!EqualUtil.isEqual(ideChangeList.getName(), newName)) {
                                addGate.editName(ideChangeList.getName(), newName);
                            }
                        }
                    }
                }
            }

            // If there are still links to changelists that are no longer pending, then the link MUST be removed.
            // The other direction - local IDE change list removed which has a link to a P4 changelist - is handled
            // by the P4ChangelistListener class.
            for (LocalChangeList unvisited : unvisitedLocalChangeLists) {
                P4ChangelistId attached = changelistMap.getP4ChangeFor(
                            clientConfigRoot.getClientConfig().getClientServerRef(),
                            unvisited);
                if (attached != null) {
                    changelistMap.changelistDeleted(attached);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Removing association of removed changelist " + attached + " to " + unvisited);
                    }
                }
            }
        }

        // Files that are in IDE change lists but not in mapped Perforce changelists should not be fixed here.
        // That operation should be handled by the P4ChangelistListener class.

        return actions;
    }

    /**
     * Everything is marked as dirty.  Just look at the state of files on the server,
     * and move files into the right changelist.
     *
     * @param config config for the root.
     * @param changes changelist mapping
     * @param files file mapping, which has already been loaded from the server.
     * @param builder changelist builder
     */
    private void updateFileCache(ClientConfig config,
            IdeChangelistMap changes, IdeFileMap files, ChangelistBuilder builder) {
        updateLocalFileCache(config, files.getLinkedFiles().collect(Collectors.toList()), changes, builder);
    }

    /**
     * Just the list of files are dirty.  If additional files are checked out
     * on the server, but they aren't marked as dirty, then make a call to mark
     * them as dirty.
     *  @param config config
     * @param files cached files
     * @param builder builds changelists
     */
    private void updateFileCache(ClientConfig config, Set<FilePath> dirty,
            IdeChangelistMap changes, IdeFileMap files, ChangelistBuilder builder) {
        List<P4LocalFile> localFiles = new ArrayList<>(dirty.size());
        for (FilePath filePath : dirty) {
            P4LocalFile local = files.forIdeFile(filePath);
            if (local == null) {
                if (filePath.getVirtualFile() == null || !filePath.getVirtualFile().exists()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Marking " + filePath + " as locally deleted");
                    }
                    builder.processLocallyDeletedFile(filePath);
                } else if (UserProjectPreferences.getAutoCheckoutModifiedFiles(project)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Scheduling " + filePath + " for edit and marking it as modified without checkout");
                    }
                    vcs.getCheckinEnvironment().scheduleUnversionedFilesForAddition(
                            Collections.singletonList(filePath.getVirtualFile()));
                    builder.processModifiedWithoutCheckout(filePath.getVirtualFile());
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Marking " + filePath + " as modified without checkout");
                    }
                    builder.processModifiedWithoutCheckout(filePath.getVirtualFile());
                }
            } else {
                localFiles.add(local);
            }
        }
        updateLocalFileCache(config, localFiles, changes, builder);
    }


    // #213: the files passed in can be in multiple clients; however, this needs to filter out the files
    // to instead be only in the requested client.  Without this, all kinds of weird things happen with the
    // changelist mapping.
    private void updateLocalFileCache(ClientConfig config, List<P4LocalFile> files,
            IdeChangelistMap changes, ChangelistBuilder builder) {
        // Collect only the files that are in the client.
        updateLocalFileCacheForClient(
                config,
                changes,
                builder,
                files.stream().filter((f) ->
                    f != null &&
                    f.getChangelistId() != null &&
                    Objects.equals(config.getClientname(), f.getChangelistId().getClientname())
                ).collect(Collectors.toList())
        );
    }

    private void updateLocalFileCacheForClient(ClientConfig config,
            IdeChangelistMap changes, ChangelistBuilder builder,
            List<P4LocalFile> files) {
        final Map<P4LocalFile, P4LocalFile> moved = findMovedFiles(files);
        final Set<P4LocalFile> unprocessedDeletedMovedFiles = new HashSet<>(moved.values());

        files.forEach((file) -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating cache for " + file);
            }
            if (unprocessedDeletedMovedFiles.contains(file)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping delete file because it is part of a move", file);
                }
            } else {
                final P4LocalFile movedFrom = moved.get(file);
                if (processModifiedFileChange(config, changes, builder, file, movedFrom)) {
                    unprocessedDeletedMovedFiles.remove(movedFrom);
                }
            }
        });

        unprocessedDeletedMovedFiles.forEach((file) -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating cache for " + file);
            }
            processModifiedFileChange(config, changes, builder, file, null);
        });
    }

    private boolean processModifiedFileChange(ClientConfig config, IdeChangelistMap changes, ChangelistBuilder builder,
            @NotNull P4LocalFile file, @Nullable P4LocalFile movedFrom) {
        // The file can have a null changelist here if "open for edit" didn't assign one.
        if (file.getChangelistId() != null) {
            try {
                LocalChangeList localChangeList = changes.getIdeChangeFor(file.getChangelistId());
                if (localChangeList == null) {
                    // Should have already been created.
                    LOG.warn("Encountered changelist " + file.getChangelistId() +
                            " that wasn't mapped to an IDE changelist.");
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Mapped changelists: " + changes.getLinkedIdeChanges());
                    }
                    builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
                    return false;
                }
                // See #206
                // There's a weird situation where the getChangeList(id) succeeds, but
                // the underlying call is to an equivalent of findChangeList(name), which
                // fails.  This might even be a race condition.  From what I can tell, the
                // ChangeListManagerImpl has a ChangeListWorker, which knows about this
                // changelist, but the UpdatingChangeListBuilder has a worker which does not.
                //
                LocalChangeList ideChangelist =
                        ChangeListManager.getInstance(project).findChangeList(localChangeList.getName());
                if (ideChangelist == null) {
                    // This can happen after submit, and is a sign that the cache is out of date.
                    LOG.info("Encountered deleted changelist " + localChangeList +
                            "; cache is probably out of date and needs a refresh.");
                    builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
                    return false;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding change " + file + " to IDE changelist " + localChangeList);
                    }

                    Pair<Change, Boolean> change = createChange(file, movedFrom, config);
                    associateChangeWithList(builder, change.first, localChangeList);
                    return change.second;
                }
            } catch (InterruptedException e) {
                InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                        new VcsInterruptedException("Lock timed out for " + file, e)));
                builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
                return false;
            }
        } else {
            LOG.warn("Encountered file " + file + " with no changelist.");
            builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
            return false;
        }
    }

    private Map<P4LocalFile, P4LocalFile> findMovedFiles(List<P4LocalFile> files) {
        Map<P4LocalFile, P4RemoteFile> movedFrom = new HashMap<>();
        Map<P4RemoteFile, P4LocalFile> deleted = new HashMap<>();

        files.forEach((f) -> {
            if (f.getIntegrateFrom() != null) {
                movedFrom.put(f, f.getIntegrateFrom());
            }
            if (f.getFileAction() == P4FileAction.DELETE || f.getFileAction() == P4FileAction.MOVE_DELETE) {
                deleted.put(f.getDepotPath(), f);
            }
        });

        Map<P4LocalFile, P4LocalFile> ret = new HashMap<>();
        movedFrom.forEach((key, value) -> {
            P4LocalFile f = deleted.get(value);
            if (f != null) {
                ret.put(key, f);
            }
        });

        return ret;
    }


    private void markIgnored(Set<FilePath> dirtyFiles, ChangelistBuilder builder) {
        for (FilePath dirtyFile : dirtyFiles) {
            if (dirtyFile.getVirtualFile() == null || !dirtyFile.getVirtualFile().exists()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Marking " + dirtyFile + " as locally deleted");
                }
                builder.processLocallyDeletedFile(dirtyFile);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Marking " + dirtyFile + " as not versioned");
                }

                // TODO include the IgnoreFileSet

                // TODO if these files aren't in P4, then they are locally edited but not
                // marked so by the server.  In that case, they are Unversioned.  If they
                // are in Perforce, then they are locally edited.

                // TODO unversioned files should be susceptible to the P4IGNORE settings.

                // This is deprecated in >v193, which introduced the new method
                // that takes a FilePath.  Earlier versions don't have that
                // method.
                builder.processUnversionedFile(dirtyFile.getVirtualFile());
            }
        }
    }


    private void associateChangeWithList(ChangelistBuilder builder, Change change, LocalChangeList localChangeList) {
        // The IDE reports a warning from ChangeListWorker when this change is just straight-up added.
        // This happens when multiple change objects are added into the builder.
        // The check for "equals" happens on the Change object, which checks the
        // before and after FilePath objects, if both are equal.  This warning
        // indicates that this class should ensure that the FilePath combo is only in
        // this one changelist: if it's already in the changelist, then skip it; if it's
        // in another changelist, then move it; if it's not in a changelist, then add it.

        if (change.getBeforeRevision() != null) {
            VirtualFile vf = change.getBeforeRevision().getFile().getVirtualFile();
            if (vf != null) {
                LocalChangeList before = ChangeListManager.getInstance(project).getChangeList(vf);
                // Only perform the "remove" if the change is different and exists
                if (before != null && !before.equals(localChangeList)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Moving file " + vf + " off of 'before' IDE change list " + before);
                    }
                    builder.removeRegisteredChangeFor(change.getBeforeRevision().getFile());
                }
            }
        }
        if (change.getAfterRevision() != null) {
            VirtualFile vf = change.getAfterRevision().getFile().getVirtualFile();
            if (vf != null) {
                LocalChangeList after = ChangeListManager.getInstance(project).getChangeList(vf);
                // Only perform the "remove" if the change is different and exists
                if (after != null && !after.equals(localChangeList)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Moving file " + vf + " off of 'after' IDE change list " + after);
                    }
                    builder.removeRegisteredChangeFor(change.getAfterRevision().getFile());
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding change " + change + " to IDE change list " + localChangeList);
        }

        // #206 can't figure out the reason why the ChangeListManagerImpl would know about the change,
        // while the builder's worker wouldn't know it.
        try {
            builder.processChangeInList(change, localChangeList, P4Vcs.getKey());
        } catch (Throwable err) {
            if (err.getClass().equals(Throwable.class)) {
                // Chances are this is related to #206.
                InternalErrorMessage.send(project).unexpectedError(new ErrorEvent<>(
                        err,
                        P4Bundle.message("error.changelist.bad-cache", localChangeList.getName())
                ));
                // fallback...
                builder.processChange(change, P4Vcs.getKey());
            } else {
                throw err;
            }
        }
    }


    private Pair<Change, Boolean> createChange(@NotNull P4LocalFile file, @Nullable P4LocalFile movedFrom,
            ClientConfig config) {
        boolean usedMovedFrom = false;
        ContentRevision before = null;
        ContentRevision after = null;
        FileStatus status;
        switch (file.getFileAction()) {
            case ADD:
            case ADD_EDIT:
                if (movedFrom != null) {
                    before = new P4LocalFileContentRevision(config, movedFrom, loader);
                    usedMovedFrom = true;
                }
                after = new CurrentContentRevision(file.getFilePath());
                status = FileStatus.ADDED;
                break;
            case EDIT:
            case REOPEN:
                assert file.getDepotPath() != null;
                // If we set the before to a different file location, then the IDE will
                // think we set the wrong status, and set it to a "move" operation.
                // before = new P4RemoteFileContentRevision(project,
                //        file.getDepotPath(), file.getHaveRevision(), config.getServerConfig());
                before = new P4LocalFileContentRevision(config, file, loader);
                after = new CurrentContentRevision(file.getFilePath());
                status = FileStatus.MODIFIED;
                break;
            case INTEGRATE:
                after = new CurrentContentRevision(file.getFilePath());
                if (file.getIntegrateFrom() != null) {
                    // TODO find the right charset
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("INTEGRATE: Finding relative path from " + file.getFilePath() + " ; " +
                                file.getDepotPath() + " ; " + file.getClientDepotPath() + " -> " +
                                file.getIntegrateFrom());
                    }
                    before = P4RemoteFileContentRevision.create(file.getIntegrateFrom(),
                            RemoteFileUtil.findRelativeRemotePath(file, file.getIntegrateFrom()),
                            config, loader, null);
                } else if (file.getDepotPath() != null) {
                    before = P4RemoteFileContentRevision.create(
                            file.getDepotPath(), file.getFilePath(), file.getHaveRevision(), config,
                            loader, file.getCharset());
                } else {
                    before = new P4LocalFileContentRevision(config, file, loader);
                }
                status = FileStatus.MERGE;
                break;
            case DELETE:
                before = new P4LocalFileContentRevision(config, file, loader);
                status = FileStatus.DELETED;
                break;
            case REVERTED:
            case NONE:
                before = after = new P4LocalFileContentRevision(config, file, loader);
                status = FileStatus.NOT_CHANGED;
                break;
            case EDIT_RESOLVED:
                if (file.getDepotPath() == null) {
                    before = null;
                    if (movedFrom != null) {
                        before = new P4LocalFileContentRevision(config, movedFrom, loader);
                        usedMovedFrom = true;
                    }
                } else {
                    before = P4RemoteFileContentRevision.create(
                            file.getDepotPath(), file.getFilePath(),
                            file.getHaveRevision(), config, loader, file.getCharset());
                }
                after = new CurrentContentRevision(file.getFilePath());
                status = FileStatus.MERGE;
                break;
            case MOVE_DELETE:
                if (file.getDepotPath() == null) {
                    // TODO needs to reference the source file.
                    before = new P4DeletedLocalFileRevision(file);
                } else {
                    before = P4RemoteFileContentRevision.create(
                            file.getDepotPath(), file.getFilePath(),
                            file.getHaveRevision(), config, loader, file.getCharset());
                }
                status = FileStatus.DELETED;
                break;
            case MOVE_ADD:
            case MOVE_ADD_EDIT:
            case MOVE_EDIT: {
                if (movedFrom != null) {
                    before = new P4LocalFileContentRevision(config, movedFrom, loader);
                    usedMovedFrom = true;
                } else if (file.getIntegrateFrom() != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("MOVE_ADD: Finding relative path from " + file.getFilePath() + " ; " +
                                file.getDepotPath() + " ; " + file.getClientDepotPath() + " -> " +
                                file.getIntegrateFrom());
                    }
                    FilePath relativeFrom = RemoteFileUtil.findRelativeRemotePath(file, file.getIntegrateFrom());
                    before = P4RemoteFileContentRevision.create(
                            file.getIntegrateFrom(), relativeFrom,
                            file.getHaveRevision(), config, loader, file.getCharset());
                }
                after = new CurrentContentRevision(file.getFilePath());
                // Even though the status is "ADD", the UI will notice the different before/after
                // and show the files as moved.
                status = FileStatus.ADDED;
                break;
            }
            case UNKNOWN:
                if (movedFrom != null) {
                    before = new P4LocalFileContentRevision(config, movedFrom, loader);
                    usedMovedFrom = true;
                } else {
                    before = new P4LocalFileContentRevision(config, file, loader);
                }
                after = new CurrentContentRevision(file.getFilePath());
                status = FileStatus.UNKNOWN;
                break;
            default:
                throw new IllegalArgumentException("Unknown file action " + file.getFileAction());
        }
        return Pair.create(new Change(before, after, status), usedMovedFrom);
    }

    private void onChangelistCreated(CreateChangelistAction action, CreateChangelistResult result,
            ClientActionMessage.ActionState state)
            throws InterruptedException {
        IdeChangelistMap cache = CacheComponent.getInstance(project).getServerOpenedCache().first;
        switch (state) {
            // pending is ignored, because it's handled within this class later, and by IdeChangelistCacheStore.

            case COMPLETED:
                cache.setMapping(new P4ChangelistIdImpl(
                        result.getChangelistId(), result.getClientConfig().getClientServerRef()), action);
                break;

            case FAILED:
                cache.actionFailed(action);
                break;
        }
    }

    private void onChangelistDelete(DeleteChangelistAction action)
            throws InterruptedException {
        // Only care about pending changelists that haven't been created yet.
        IdeChangelistMap cache = CacheComponent.getInstance(project).getServerOpenedCache().first;
        cache.changelistDeleted(action.getChangelistId());
    }

    private ClientConfigRoot getClientFor(VirtualFile file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? null : reg.getClientFor(file);
    }

    @NotNull
    private Collection<ClientConfigRoot> getClientConfigRoots() {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? Collections.emptyList() : reg.getClientConfigRoots();
    }
}
