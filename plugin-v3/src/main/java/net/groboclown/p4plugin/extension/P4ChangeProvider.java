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
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerGate;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.changes.ChangelistBuilder;
import com.intellij.openapi.vcs.changes.ContentRevision;
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
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.revision.P4LocalFileContentRevision;
import net.groboclown.p4plugin.revision.P4RemoteFileContentRevision;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final int CHANGELIST_NAME_LENGTH = 32;

    private final Project project;
    private final P4Vcs vcs;
    private final HistoryContentLoader loader;

    P4ChangeProvider(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
        this.vcs = vcs;
        this.loader = new HistoryContentLoaderImpl(project);

        final MessageBusClient.ApplicationClient mbClient = MessageBusClient.forApplication(project);
        final String cacheId = AbstractCacheMessage.createCacheId();
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
                LOG.info(e);
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

        // This request is performed by the IDE in a background thread.

        Pair<IdeChangelistMap, IdeFileMap> cachedMaps = CacheComponent.getInstance(project)
                .blockingRefreshServerOpenedCache(
                        allClientRoots.stream()
                            .map(ClientConfigRoot::getClientConfig)
                        .collect(Collectors.toList()),
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
            updateChangelists(cachedMaps.first, cachedMaps.second, addGate);
        } catch (InterruptedException e) {
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
                        LOG.info("Processing changes for " + entry.getValue() + " under " + root);
                        ClientConfigRoot config = getClientFor(root);
                        if (config != null) {
                            Set<FilePath> matched = new HashSet<>();
                            for (FilePath filePath : entry.getValue()) {
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
                                        entry.getValue() + " to root: " + matched);
                                LOG.debug("Remaining dirty files: " + dirtyFiles);
                            }
                            if (!matched.isEmpty()) {
                                updateFileCache(config.getClientConfig(),
                                        matched, cachedMaps.first, cachedMaps.second, builder);
                            }
                        } else if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping because not under a Perforce root: " + entry.getKey() + " @ " +
                                    root);
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

    private void updateChangelists(IdeChangelistMap changelistMap, IdeFileMap fileMap, ChangeListManagerGate addGate)
            throws InterruptedException {
        List<LocalChangeList> existingLocalChangeLists = addGate.getListsCopy();
        Set<LocalChangeList> unvisitedLocalChangeLists = new HashSet<>(existingLocalChangeLists);
        for (ClientConfigRoot clientConfigRoot : getClientConfigRoots()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating changelists for " + clientConfigRoot);
            }
            for (P4LocalChangelist changelist : CacheComponent.getInstance(project).getCacheQuery()
                    .getCachedOpenedChangelists(clientConfigRoot.getClientConfig())) {
                LocalChangeList ideChangeList =
                        changelistMap.getIdeChangeFor(changelist.getChangelistId());
                if (ideChangeList == null) {
                    if (changelist.getChangelistId().isDefaultChangelist()) {
                        // Link to the IDE default changelist.
                        LOG.debug("Attaching default changelist to IDE default changelist");
                        changelistMap.setMapping(changelist.getChangelistId(),
                            ChangeListManager.getInstance(project).getDefaultChangeList());
                    } else {
                        // Create a new IDE changelist and link to that.
                        ideChangeList = addGate.addChangeList(
                                createUniqueChangelistName(changelist, existingLocalChangeLists, null),
                                changelist.getComment());
                        // Mark the just-created changelist as added, so that we don't attempt to use the name a
                        // second time.
                        existingLocalChangeLists.add(ideChangeList);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Attaching " + changelist + " to IDE change " + ideChangeList);
                        }
                        changelistMap.setMapping(changelist.getChangelistId(), ideChangeList);
                    }
                } else if (!changelist.getChangelistId().isDefaultChangelist()) {
                    // Don't create a separate named changelist for the default changelist.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Already attached " + changelist + " to IDE change " + ideChangeList);
                    }
                    String newName = createUniqueChangelistName(changelist, existingLocalChangeLists, ideChangeList);
                    if (!newName.equals(ideChangeList.getName())) {
                        addGate.editName(ideChangeList.getName(), newName);
                    }
                    addGate.editComment(ideChangeList.getName(), changelist.getComment());
                    unvisitedLocalChangeLists.remove(ideChangeList);
                }
            }
        }

        // Loop through the files, and see if any are associated with a local change list that isn't
        // mapped to a Perforce changelist.  If so, then create that changelist.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing remaining " + unvisitedLocalChangeLists);
        }
        Iterator<LocalChangeList> iter = unvisitedLocalChangeLists.iterator();
        while (iter.hasNext()) {
            LocalChangeList ideChangeList = iter.next();
            for (FilePath file: getChangeListFiles(ideChangeList)) {
                if (P4Vcs.getInstance(project).fileIsUnderVcs(file)) {
                    P4LocalFile p4file = fileMap.forIdeFile(file);
                    if (p4file != null && (p4file.getChangelistId() == null ||
                            changelistMap.getIdeChangeFor(p4file.getChangelistId()) == null)) {

                        ClientConfig clientConfig = getClientConfigFor(p4file);
                        if (clientConfig != null) {
                            LOG.info("Creating P4 changelist due to IDE change refresh: " +
                                    ideChangeList);
                            CreateChangelistAction action =
                                    new CreateChangelistAction(clientConfig.getClientServerRef(),
                                            createP4ChangelistDescription(ideChangeList));
                            CacheComponent.getInstance(project).getServerOpenedCache().first.setMapping(
                                    action, ideChangeList);
                            P4ServerComponent.perform(project, clientConfig, action);
                        } else {
                            LOG.warn("IDE changelist " + ideChangeList +
                                    " file " + file + " -> " + p4file +
                                    " has no associated config");
                        }

                        iter.remove();
                        break;
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("IDE changelist " + ideChangeList +
                            " contains non-P4 file " + file);
                }
            }
        }

        if (!unvisitedLocalChangeLists.isEmpty()) {
            // This means there are local changelists that don't have a Perforce mapping.
            // TODO check a new user prefrence to see if the changelist should be removed.
            LOG.debug("TODO check a new user prefrence to see if the changelist should be removed: " +
                    unvisitedLocalChangeLists);
        }
    }

    @NotNull
    private String createUniqueChangelistName(P4LocalChangelist changelist,
            List<LocalChangeList> existingLocalChangeLists,
            @Nullable LocalChangeList currentChangeList) {
        String newName = getPrefix(changelist, CHANGELIST_NAME_LENGTH);
        int index = -1;

        match_outer_loop:
        while (true) {
            for (LocalChangeList lcl : existingLocalChangeLists) {
                if (!lcl.equals(currentChangeList) && newName.equals(lcl.getName())) {
                    index++;
                    String count = " (" + index + ')';
                    newName = getPrefix(changelist, CHANGELIST_NAME_LENGTH - count.length()) + count;
                    continue match_outer_loop;
                }
            }
            return newName;
        }
    }

    private String getPrefix(P4LocalChangelist changelist, int characterCount) {
        String ret = changelist.getComment();
        if (ret.length() > characterCount) {
            ret = ret.substring(0, characterCount - 3) + "...";
        }
        return ret;
    }

    private static final Pattern CL_INDEX_SUFFIX = Pattern.compile("^\\s*(.*?)\\s+\\(\\d+\\)\\s*$");

    @NotNull
    private String createP4ChangelistDescription(LocalChangeList ideChangeList) {
        String name = ideChangeList.getName();
        String desc = ideChangeList.getComment();
        if (desc != null) {
            desc = desc.trim();
            if (!UserProjectPreferences.getConcatenateChangelistNameComment(project) &&
                    !desc.isEmpty()) {
                return desc;
            }
        }
        Matcher m1 = CL_INDEX_SUFFIX.matcher(name);
        if (m1.matches()) {
            name = m1.group(1);
        }
        if (name.endsWith("...")) {
            name = name.substring(0, name.length() - 3);
        }
        name = name.trim();
        if (desc == null || desc.isEmpty()) {
            return name;
        }
        desc = desc.trim();
        if (desc.startsWith(name)) {
            return desc;
        }
        if (!name.endsWith(".")) {
            name += '.';
        }
        return name + "  " + desc;
    }

    @Nullable
    private ClientConfig getClientConfigFor(@NotNull P4LocalFile p4file) {
        ClientConfigRoot ret = getClientFor(p4file.getFilePath());
        if (ret == null) {
            LOG.info("File " + p4file.getDepotPath() + ", mapped to " +
                    p4file.getFilePath() + ", is not under a vcs root with a valid client configuration");
            return null;
        }
        return ret.getClientConfig();
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
        updateLocalFileCache(config, files.getLinkedFiles(), changes, builder);
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

        // FIXME find move-pairs, and join them into a single change.
        LOG.warn("FIXME find move-pairs, and join them into a single change.");

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
        updateLocalFileCache(config, localFiles.stream(), changes, builder);
    }


    private void updateLocalFileCache(ClientConfig config, Stream<P4LocalFile> files,
            IdeChangelistMap changes, ChangelistBuilder builder) {
        files.forEach((file) -> {
            // FIXME the file can have a null changelist here if "open for edit" didn't assign one.
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
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Adding change " + file + " to IDE changelist " + localChangeList);
                        }

                        Change change = createChange(file, config);
                        associateChangeWithList(builder, change, localChangeList);
                    }
                } catch (InterruptedException e) {
                    LOG.warn("Lock timed out for " + file);
                    builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
                }
            } else {
                LOG.warn("Encountered file " + file + " with no changelist.");
                builder.processModifiedWithoutCheckout(file.getFilePath().getVirtualFile());
            }
        });
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
                    LOG.debug("Marking " + dirtyFile + " as unversioned");
                }
                // TODO if these files aren't in P4, then they are locally edited but not
                // marked so by the server.  In that case, they are Unversioned.  If they
                // are in Perforce, then they are locally edited.

                // TODO unversioned files should be susceptible to the P4IGNORE settings.

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

        // Until the move operations are encapsulated into a single change, this can have an unexpected side-effect for
        // move operations!
        if (change.getBeforeRevision() != null) {
            VirtualFile vf = change.getBeforeRevision().getFile().getVirtualFile();
            if (vf != null) {
                LocalChangeList before = ChangeListManager.getInstance(project).getChangeList(vf);
                if (before != null) {
                    builder.removeRegisteredChangeFor(change.getBeforeRevision().getFile());
                }
            }
        }
        if (change.getAfterRevision() != null) {
            VirtualFile vf = change.getAfterRevision().getFile().getVirtualFile();
            if (vf != null) {
                LocalChangeList after = ChangeListManager.getInstance(project).getChangeList(vf);
                if (after != null) {
                    builder.removeRegisteredChangeFor(change.getAfterRevision().getFile());
                }
            }
        }
        builder.processChangeInList(change, localChangeList, P4Vcs.getKey());
    }


    private Change createChange(P4LocalFile file, ClientConfig config) {
        ContentRevision before = null;
        ContentRevision after = null;
        FileStatus status;
        switch (file.getFileAction()) {
            case ADD:
            case ADD_EDIT:
                after = new P4LocalFileContentRevision(file);
                status = FileStatus.ADDED;
                break;
            case EDIT:
            case REOPEN:
                assert file.getDepotPath() != null;
                // If we set the before to a different file location, then the IDE will
                // think we set the wrong status, and set it to a "move" operation.
                // before = new P4RemoteFileContentRevision(project,
                //        file.getDepotPath(), file.getHaveRevision(), config.getServerConfig());
                before = after = new P4LocalFileContentRevision(file);
                status = FileStatus.MODIFIED;
                break;
            case INTEGRATE:
                after = new P4LocalFileContentRevision(file);
                if (file.getIntegrateFrom() != null) {
                    // TODO find the right charset
                    before = new P4RemoteFileContentRevision(project,
                            file.getIntegrateFrom(), null, config.getServerConfig(), loader, null);
                } else if (file.getDepotPath() != null) {
                    // TODO find the right charset
                    before = new P4RemoteFileContentRevision(project,
                            file.getDepotPath(), file.getHaveRevision(), config.getServerConfig(), loader, null);
                } else {
                    before = after;
                }
                status = FileStatus.MERGE;
                break;
            case DELETE:
                before = new P4LocalFileContentRevision(file);
                status = FileStatus.DELETED;
                break;
            case REVERTED:
            case NONE:
                before = after = new P4LocalFileContentRevision(file);
                status = FileStatus.NOT_CHANGED;
                break;
            case EDIT_RESOLVED:
                assert file.getDepotPath() != null;
                // TODO find the right charset
                before = new P4RemoteFileContentRevision(project,
                        file.getDepotPath(), file.getHaveRevision(), config.getServerConfig(), loader, null);
                after = new P4LocalFileContentRevision(file);
                status = FileStatus.MERGE;
                break;
            case MOVE_DELETE:
                assert file.getDepotPath() != null;
                // TODO find the right charset
                before = new P4RemoteFileContentRevision(project,
                        file.getDepotPath(), file.getHaveRevision(), config.getServerConfig(), loader, null);
                status = FileStatus.DELETED;
                break;
            case MOVE_ADD:
            case MOVE_ADD_EDIT:
            case MOVE_EDIT:
                assert file.getDepotPath() != null;
                // TODO find the right charset
                before = new P4RemoteFileContentRevision(project,
                        file.getDepotPath(), file.getHaveRevision(), config.getServerConfig(), loader, null);
                after = new P4LocalFileContentRevision(file);
                // Even though the status is "ADD", the UI will notice the different before/after
                // and show the files as moved.
                status = FileStatus.ADDED;
                break;
            case UNKNOWN:
                before = after = new P4LocalFileContentRevision(file);
                status = FileStatus.UNKNOWN;
                break;
            default:
                throw new IllegalArgumentException("Unknown file action " + file.getFileAction());
        }
        return new Change(before, after, status);
    }


    private Collection<FilePath> getChangeListFiles(LocalChangeList changeList) {
        Set<FilePath> ret = new HashSet<>();
        for (Change change : changeList.getChanges()) {
            if (change != null && change.getBeforeRevision() != null) {
                ret.add(change.getBeforeRevision().getFile());
            }
            if (change != null && change.getAfterRevision() != null) {
                ret.add(change.getAfterRevision().getFile());
            }
        }
        return ret;
    }

    private void onChangelistCreated(CreateChangelistAction action, CreateChangelistResult result,
            ClientActionMessage.ActionState state)
            throws InterruptedException {
        IdeChangelistMap cache = CacheComponent.getInstance(project).getServerOpenedCache().first;
        switch (state) {
            // pending is ignored, because it's handled exclusively within this class
            // later.

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

    private ClientConfigRoot getClientFor(FilePath file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? null : reg.getClientFor(file);
    }

    @NotNull
    private Collection<ClientConfigRoot> getClientConfigRoots() {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? Collections.emptyList() : reg.getClientConfigRoots();
    }
}
