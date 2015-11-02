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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.MessageGenericCode;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.server.FileSyncResult;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server.IntegrateFile;
import net.groboclown.idea.p4ic.v2.server.cache.AbstractServerUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.AbstractServerUpdateAction.ExecutionStatus;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.ServerUpdateActionFactory;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction.UpdateParameterNames;
import net.groboclown.idea.p4ic.v2.server.cache.state.*;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Keeps track of all the file actions (files open for edit, move, etc).
 * All actions must be performed from within a
 * {@link net.groboclown.idea.p4ic.v2.server.connection.ServerConnection}
 * in order to ensure correct asynchronous behavior.
 */
public class FileActionsServerCacheSync extends CacheFrontEnd {
    private static final Logger LOG = Logger.getInstance(FileActionsServerCacheSync.class);

    private final Cache cache;
    private final FileUpdateStateList localClientUpdatedFiles;
    private final FileUpdateStateList cachedServerUpdatedFiles;
    private final Set<FilePath> committed = new HashSet<FilePath>();
    private Date lastRefreshed;


    public FileActionsServerCacheSync(@NotNull final Cache cache,
            @NotNull final FileUpdateStateList localClientUpdatedFiles,
            @NotNull final FileUpdateStateList cachedServerUpdatedFiles) {
        this.cache = cache;

        // these variables must be directly referenced, because they're a link into
        // the persisted state.
        this.localClientUpdatedFiles = localClientUpdatedFiles;
        this.cachedServerUpdatedFiles = cachedServerUpdatedFiles;

        lastRefreshed = CachedState.NEVER_LOADED;
        for (P4FileUpdateState state: cachedServerUpdatedFiles) {
            if (state.getLastUpdated().after(lastRefreshed)) {
                lastRefreshed = state.getLastUpdated();
            }
        }
    }


    public Collection<P4FileAction> getOpenFiles() {
        // Get all the cached files that we know about from the server
        final Set<P4FileUpdateState> files = new HashSet<P4FileUpdateState>(cachedServerUpdatedFiles.copy());
        // Overwrite them with the locally updated versions of them.
        files.addAll(localClientUpdatedFiles.copy());
        final List<P4FileAction> ret = new ArrayList<P4FileAction>(files.size());
        for (P4FileUpdateState file : files) {
            ret.add(new P4FileAction(file, file.getFileUpdateAction().getUpdateAction()));
        }
        return ret;
    }


    @Override
    protected void innerLoadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
        ServerConnection.assertInServerConnection();

        // Load our server cache.  Note that we only load specs that we consider to be in a
        // "valid" file action state.

        try {
            // This is okay to run with the "-s" argument.
            final MessageResult<List<IExtendedFileSpec>> results =
                exec.loadOpenedFiles(getClientRootSpecs(exec.getProject(), alerts), false);
            if (!alerts.addWarnings(exec.getProject(),
                    P4Bundle.message("error.load-opened", cache.getClientName()), results, true)) {
                lastRefreshed = new Date();

                // Only clear the cache once we know that we have valid results.

                final List<IExtendedFileSpec> validSpecs = new ArrayList<IExtendedFileSpec>(results.getResult());
                final List<IExtendedFileSpec> invalidSpecs = sortInvalidActions(validSpecs);
                addInvalidActionAlerts(exec.getProject(), alerts, invalidSpecs);

                cachedServerUpdatedFiles.replaceWith(cache.fromOpenedToAction(exec.getProject(), validSpecs, alerts));

                // Flush out the local changes that have been updated.
                final Iterator<P4FileUpdateState> iter = localClientUpdatedFiles.iterator();
                while (iter.hasNext()) {
                    final P4FileUpdateState next = iter.next();
                    if (committed.remove(next.getLocalFilePath())) {
                        iter.remove();
                    }
                }
            }
        } catch (VcsException e) {
            alerts.addWarning(
                    exec.getProject(),
                    P4Bundle.message("error.load-opened.title", cache.getClientName()),
                    P4Bundle.message("error.load-opened", cache.getClientName()),
                    e, FilePathUtil.getFilePath(exec.getProject().getBaseDir()));
        }
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshed;
    }


    /**
     * Create the edit file update state.  Called whether the file needs to be edited or added.
     *
     * @param file file to add or edit
     * @param changeListId P4 changelist to add the file into.
     * @return the update state
     */
    @Nullable
    public PendingUpdateState addOrEditFile(@NotNull Project project, @NotNull final FilePath file, int changeListId) {
        // Edit operations will need to have the file be writable.  The Perforce
        // command may delay when the edit action actually occurs, so ensure
        // the file is set to writable first.
        makeWritable(project, file);

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(file, FileUpdateAction.ADD_EDIT_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), file.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(file.getIOFile().getAbsolutePath()),
                params);
    }


    /**
     * Create the edit file update state.  Called whether the file needs to be edited or added.
     *
     * @param file         file to edit
     * @param changeListId P4 changelist to add the file into.
     * @return the update state
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull Project project, @NotNull final VirtualFile file, int changeListId) {
        // Edit operations will need to have the file be writable.  The Perforce
        // command may delay when the edit action actually occurs, so ensure
        // the file is set to writable first.
        FilePath fp = FilePathUtil.getFilePath(file);
        makeWritable(project, fp);

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(fp, FileUpdateAction.EDIT_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), fp.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(fp.getIOFile().getAbsolutePath()),
                params);
    }


    @Nullable
    public PendingUpdateState deleteFile(@NotNull Project project, @NotNull FilePath file, int changeListId) {
        // Let the IDE deal with the actual removal of the file.
        // But just to be sure, make it writable first.
        if (file.getIOFile().exists()) {
            makeWritable(project, file);
        }

        // TODO put the file in the local cache, or mark it in the IDEA built-in vcs.

        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(file, FileUpdateAction.DELETE_FILE, changeListId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(), file.getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changeListId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                Collections.singleton(file.getIOFile().getAbsolutePath()),
                params);
    }

    @Nullable
    public PendingUpdateState moveFile(@NotNull final Project project,
            @NotNull final IntegrateFile file, final int changelistId) {
        // Let the IDE deal with the actual removal of the file.
        // But just to be sure, make it writable first.
        if (file.getSourceFile().getIOFile().exists()) {
            makeWritable(project, file.getSourceFile());
        }


        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState tgtAction = createFileUpdateState(file.getTargetFile(),
                FileUpdateAction.MOVE_FILE, changelistId);
        // Make a source action, too
        createFileUpdateState(file.getSourceFile(),
                FileUpdateAction.MOVE_DELETE_FILE, changelistId);
        if (tgtAction == null) {
            // nothing to do
            return null;
        }
        // doesn't matter too much if the delete file is not null

        // TODO if the source file is on the same server but different
        // client, use a depot path as the source.  This requires being
        // online to perform this action,  This logic may be improved
        // to be delayed as long as possible, through.

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(),
                file.getTargetFile().getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.FILE_SOURCE.getKeyName(),
                file.getSourceFile().getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);

        return new PendingUpdateState(
                tgtAction.getFileUpdateAction().getUpdateAction(),
                new HashSet<String>(Arrays.asList(
                        file.getTargetFile().getIOFile().getAbsolutePath(),
                        file.getSourceFile().getIOFile().getAbsolutePath())),
                params);
    }


    @Nullable
    public PendingUpdateState integrateFile(@NotNull final IntegrateFile file, final int changelistId) {
        // The destination file does not need to be writable


        // Check if it is already in an action state, and create it if necessary
        final P4FileUpdateState action = createFileUpdateState(file.getTargetFile(),
                FileUpdateAction.INTEGRATE_FILE, changelistId);
        if (action == null) {
            // nothing to do
            return null;
        }

        // Create the action.
        final HashMap<String, Object> params = new HashMap<String, Object>();
        // We don't know for sure the depot path, so use the file path.
        params.put(UpdateParameterNames.FILE.getKeyName(),
                file.getTargetFile().getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.FILE_SOURCE.getKeyName(),
                file.getSourceFile().getIOFile().getAbsolutePath());
        params.put(UpdateParameterNames.CHANGELIST.getKeyName(), changelistId);

        return new PendingUpdateState(
                action.getFileUpdateAction().getUpdateAction(),
                // Note that the source is not in the ID.
                Collections.singleton(file.getTargetFile().getIOFile().getAbsolutePath()),
                params);
    }


    @Nullable
    public ServerUpdateAction revertFileOnline(@NotNull final List<FilePath> files,
            @NotNull final Ref<MessageResult<Collection<FilePath>>> ret) {
        return new ImmediateServerUpdateAction() {
            @Override
            public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                try {
                    final List<IFileSpec> results = exec.revertFiles(FileSpecUtil.getFromFilePaths(files));
                    ret.set(MessageResult.createForFilePath(files, results, false));
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert-file.unchanged.title"),
                            P4Bundle.message("error.revert-file.unchanged",
                                    FilePathUtil.toStringList(files)),
                            e,
                            files);
                }
            }
        };
    }

    @Nullable
    public ServerUpdateAction revertFileIfUnchangedOnline(@NotNull Collection<FilePath> files,
            final int changelistId,
            @NotNull final Ref<MessageResult<Collection<FilePath>>> ret) {
        final List<FilePath> orderedFiles = new ArrayList<FilePath>(files);
        return new ImmediateServerUpdateAction() {
            @Override
            public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                try {
                    final List<IFileSpec> results =
                            exec.revertUnchangedFiles(FileSpecUtil.getFromFilePaths(orderedFiles), changelistId);
                    ret.set(MessageResult.createForFilePath(orderedFiles, results, false));
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert-file.unchanged.title"),
                            P4Bundle.message("error.revert-file.unchanged",
                                    FilePathUtil.toStringList(orderedFiles)),
                            e,
                            orderedFiles);
                }
            }
        };
    }

    @Nullable
    public ServerUpdateAction synchronizeFilesOnline(@NotNull Collection<FilePath> files,
            final int revisionNumber,
            @Nullable final String syncSpec, final boolean force,
            final Ref<MessageResult<Collection<FileSyncResult>>> ref) {
        final List<FilePath> fileList = new ArrayList<FilePath>(files);
        return new ImmediateServerUpdateAction() {
            @Override
            public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                try {
                    List<IFileSpec> specs;
                    if (revisionNumber >= 0) {
                        specs = FileSpecUtil.getFromFilePathsAt(fileList,
                                "#" + revisionNumber, false);
                    } else if (syncSpec != null) {
                        specs = FileSpecUtil.getFromFilePathsAt(fileList, syncSpec, false);
                    } else {
                        specs = FileSpecUtil.getFromFilePaths(fileList);
                    }
                    final List<IFileSpec> results = exec.synchronizeFiles(specs, force);
                    Iterator<FilePath> srcIter = fileList.iterator();
                    Iterator<IFileSpec> resultIter = results.iterator();
                    List<FileSyncResult> ret = new ArrayList<FileSyncResult>(fileList.size());
                    List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                    while (resultIter.hasNext()) {
                        final IFileSpec spec = resultIter.next();
                        if (spec.getOpStatus() == FileSpecOpStatus.VALID) {
                            FilePath file = srcIter.next();
                            final P4ClientFileMapping mapping =
                                    cache.getClientMappingFor(file);
                            if (spec.getDepotPathString() != null) {
                                cache.updateDepotPathFor(mapping, spec.getDepotPathString());
                            }
                            ret.add(new FileSyncResult(mapping, spec.getAction(),
                                    // "rev" is turned into end revision
                                    spec.getEndRevision()));
                        } else if (P4StatusMessage.isErrorStatus(spec)) {
                            final P4StatusMessage msg = new P4StatusMessage(spec);

                            // 17 = "file(s) up-to-date"
                            if (msg.getErrorCode() != 17) {
                                LOG.info(msg + ": error code " + msg.getErrorCode());
                                messages.add(msg);
                            } else {
                                LOG.info(msg + ": ignored");
                            }
                        } else if (spec.getOpStatus() == FileSpecOpStatus.INFO) {
                            // INFO messages don't have a source, unfortunately.
                            // So we need to extract the path information.
                            LOG.info("info message: " + spec.getStatusMessage());
                            messages.add(new P4StatusMessage(spec));
                        }
                    }

                    ref.set(new MessageResult<Collection<FileSyncResult>>(ret, messages));
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.sync.title"),
                            P4Bundle.message("error.sync"),
                            e, fileList);
                } catch (NoSuchElementException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.sync.title"),
                            P4Bundle.message("error.sync"),
                            e, fileList);
                }
            }
        };
    }

    @Nullable
    public ServerUpdateAction submitChangelist(@NotNull final List<FilePath> files,
            @NotNull final List<P4ChangeListJob> jobs,
            @Nullable final String submitStatus, final int changelistId,
            @NotNull final Ref<VcsException> problem) {
        final List<String> jobIds = new ArrayList<String>(jobs.size());
        for (P4ChangeListJob job : jobs) {
            jobIds.add(job.getJobId());
        }

        return new ImmediateServerUpdateAction() {
            @Override
            public void perform(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager clientCacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                try {
                    exec.submit(changelistId, jobIds, submitStatus);
                } catch (VcsException e) {
                    problem.set(e);
                }
            }
        };
    }


    void markLocalFileStateAsCommitted(@NotNull FilePath file) {
        committed.add(file);
    }



    @Nullable
    private P4FileUpdateState getCachedUpdateState(@NotNull final FilePath file) {
        P4FileUpdateState action = localClientUpdatedFiles.getUpdateStateFor(file);
        if (action != null) {
            return action;
        }
        return cachedServerUpdatedFiles.getUpdateStateFor(file);
    }


    private void addInvalidActionAlerts(@NotNull Project project, @NotNull final AlertManager alerts, @NotNull final List<IExtendedFileSpec> invalidSpecs) {
        if (invalidSpecs.isEmpty()) {
            return;
        }
        List<FilePath> files = new ArrayList<FilePath>(invalidSpecs.size());
        StringBuilder sb = new StringBuilder(P4Bundle.message("error.opened-action-status.invalid"));
        String sep = "";
        for (IFileSpec spec: invalidSpecs) {
            sb.append(sep);
            if (spec.getStatusMessage() != null) {
                P4Bundle.message("file.invalid.action.spec.message",
                        spec.getDepotPathString(),
                        spec.getClientPathString(),
                        spec.getAction(),
                        spec.getStatusMessage());
            } else {
                P4Bundle.message("file.invalid.action.spec.no-message",
                        spec.getDepotPathString(),
                        spec.getClientPathString(),
                        spec.getAction(),
                        spec.getStatusMessage());
            }
            files.add(FilePathUtil.getFilePath(spec.getClientPathString()));
            sep = P4Bundle.message("file.invalid.action.spec.seperator");
        }
        alerts.addNotice(project, sb.toString(), null,
                files.toArray(new FilePath[files.size()]));
    }

    @NotNull
    private List<IExtendedFileSpec> sortInvalidActions(@NotNull List<IExtendedFileSpec> validSpecs) {
        List<IExtendedFileSpec> ret = new ArrayList<IExtendedFileSpec>(validSpecs.size());
        Iterator<IExtendedFileSpec> iter = validSpecs.iterator();
        while (iter.hasNext()) {
            final IExtendedFileSpec next = iter.next();
            if (next != null) {
                if (! isValidUpdateAction(next)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.info("invalid spec: " + next.getDepotPathString() + "; " + next
                                .getOpStatus() + ": action: " +
                                next.getAction() + "/" + next.getOtherAction() + "/" +
                                next.getOpenAction() + "/" + next.getHeadAction() +
                                "; client path string: " + next.getClientPathString());
                    }
                    ret.add(next);
                    iter.remove();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.info("valid spec: " + next.getDepotPathString() + "; " + next.getOpStatus() + ": action: " +
                                next.getAction() + "/" + next.getOtherAction() + "/" +
                                next.getOpenAction() + "/" + next.getHeadAction() +
                                "; client path string: " + next.getClientPathString());
                    }
                }
            } else {
                iter.remove();
            }
        }
        return ret;
    }

    private boolean isValidUpdateAction(IFileSpec spec) {
        final FileAction action = spec.getAction();
        return action != null && UpdateAction.getUpdateActionForOpened(action) != null;
    }

    /**
     *
     * @return all the "..." directory specs for the roots of this client.
     * @param project project
     * @param alerts alerts manager
     */
    private List<IFileSpec> getClientRootSpecs(@NotNull Project project, @NotNull AlertManager alerts) throws VcsException {
        final List<VirtualFile> roots = cache.getClientRoots(project, alerts);
        return FileSpecUtil.makeRootFileSpecs(roots.toArray(new VirtualFile[roots.size()]));
    }

    private void makeWritable(@NotNull final Project project, @NotNull final FilePath file) {
        // write actions can only be in the dispatch thread.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (file.getVirtualFile() != null) {
                            file.getVirtualFile().setWritable(true);
                        }
                    } catch (IOException e) {
                        // ignore for now
                        LOG.info(e);
                    }
                }
            });
        }


        File f = file.getIOFile();
        if (f.exists() && f.isFile() && !f.canWrite()) {
            if (!f.setWritable(true)) {
                // TODO get access to the alerts in a better way
                AlertManager.getInstance().addWarning(project,
                        P4Bundle.message("error.writable.failed.title"),
                        P4Bundle.message("error.writable.failed", file),
                        null, file);

                // Keep going with the action, even though we couldn't set it to
                // writable...
            }
        }
    }


    @Nullable
    private P4FileUpdateState createFileUpdateState(@NotNull FilePath file,
            @NotNull FileUpdateAction fileUpdateAction, int changeListId) {
        // Check if it is already in an action state.
        P4FileUpdateState action = getCachedUpdateState(file);
        if (action != null && fileUpdateAction.equals(action.getFileUpdateAction())) {
            LOG.info("Already opened for " + fileUpdateAction + " " + file);

            // FIXME this might be wrong - the file may need to move to a different changelist.

            return null;
        }
        // If the action already exists but isn't the right update action,
        // we still need to create a new one to put in our local cache.
        P4FileUpdateState newAction = new P4FileUpdateState(
                cache.getClientMappingFor(file), changeListId, fileUpdateAction);
        if (action != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("removing action from local file cache: " + action);
            }
            localClientUpdatedFiles.remove(action);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("adding action into local file cache: " + newAction);
        }
        localClientUpdatedFiles.add(newAction);

        return newAction;
    }


    private static boolean isNotKnownToServer(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                (spec.getGenericCode() == MessageGenericCode.EV_EMPTY ||
                spec.getStatusMessage().hasMessageFragment(" - no such file(s)."));
    }

    private static boolean isNotInClientView(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.ERROR &&
                spec.getStatusMessage().hasMessageFragment(" is not under client's root ");
    }

    private static boolean isStatusMessage(@NotNull final IExtendedFileSpec spec) {
        return spec.getOpStatus() == FileSpecOpStatus.INFO ||
                spec.getOpStatus() == FileSpecOpStatus.CLIENT_ERROR ||
                spec.getOpStatus() == FileSpecOpStatus.UNKNOWN;
    }

    private static void markUpdated(final ClientCacheManager clientCacheManager, @NotNull Set<FilePath> files,
            @NotNull List<P4StatusMessage> msgs) {
        Set<FilePath> updated = new HashSet<FilePath>(files);
        for (P4StatusMessage msg: msgs) {
            updated.remove(msg.getFilePath());
        }
        for (FilePath fp: updated) {
            clientCacheManager.markLocalFileStateCommitted(fp);
        }
    }

    @NotNull
    private static List<FilePath> getFilePaths(@NotNull final List<PendingUpdateState> updateList) {
        List<FilePath> filenames = new ArrayList<FilePath>(updateList.size());
        final Iterator<PendingUpdateState> iter = updateList.iterator();
        while (iter.hasNext()) {
            PendingUpdateState update = iter.next();
            String val = UpdateParameterNames.FILE.getParameterValue(update);
            if (val != null) {
                filenames.add(FilePathUtil.getFilePath(val));
            } else {
                iter.remove();
            }
        }
        return filenames;
    }

    @NotNull
    private static List<String> toStringList(@NotNull Collection<? extends IFileSpec> specs) {
        List<String> ret = new ArrayList<String>(specs.size());
        for (IFileSpec spec : specs) {
            if (spec == null) {
                ret.add(null);
            } else if (spec.getDepotPathString() != null) {
                ret.add(spec.getDepotPathString());
            } else if (spec.getClientPathString() != null) {
                ret.add(spec.getClientPathString());
            } else if (spec.getOriginalPathString() != null) {
                ret.add(spec.getOriginalPathString());
            } else {
                ret.add(spec.toString());
            }
        }
        return ret;
    }

    static class ActionSplit {
        ExecutionStatus status;
        final Map<Integer, Set<FilePath>> notInPerforce = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> notOpened = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> edited = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> deleted = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> integrated = new HashMap<Integer, Set<FilePath>>();
        final Map<Integer, Set<FilePath>> move_deleted = new HashMap<Integer, Set<FilePath>>();

        ActionSplit(@NotNull P4Exec2 exec,
                @NotNull Collection<PendingUpdateState> pendingUpdateStates,
                @NotNull AlertManager alerts,
                boolean ignoreAddsIfEditOnly) {
            List<PendingUpdateState> updateList = new ArrayList<PendingUpdateState>(pendingUpdateStates);
            List<FilePath> filenames = getFilePaths(updateList);
            final List<IFileSpec> srcSpecs;
            try {
                srcSpecs = FileSpecUtil.getFromFilePaths(filenames);
            } catch (P4Exception e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.file-spec.create.title"),
                        P4Bundle.message("error.file-spec.create", filenames),
                        e, filenames);
                status = ExecutionStatus.FAIL;
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("File specs: " + toStringList(srcSpecs));
            }
            final List<IExtendedFileSpec> fullSpecs;
            try {
                fullSpecs = exec.getFileStatus(srcSpecs);
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.file-status.fetch.title"),
                        P4Bundle.message("error.file-status.fetch", srcSpecs),
                        e, srcSpecs);
                status = ExecutionStatus.FAIL;
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Full specs: " + fullSpecs);
            }

            Iterator<PendingUpdateState> updatesIter = updateList.iterator();
            for (IExtendedFileSpec spec : fullSpecs) {

                if (isStatusMessage(spec)) {
                    // no corresponding file, so don't increment the update
                    // however, it could mean that the file isn't on the server.
                    // there's the chance that it means that the file isn't
                    // in the client view.  Generally, that's an ERROR status.
                    LOG.info("fstat non-valid state: " + spec.getOpStatus() + ": " + spec.getStatusMessage());
                    continue;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("fstat state: " + spec.getOpStatus() + ": [" + spec.getStatusMessage() + "];" +
                                spec.getClientPathString() + ";" + spec.getOriginalPathString() +
                                ";" + spec.getUniqueCode() + ":" + spec.getGenericCode() + ":" + spec.getSubCode());
                    }
                }

                // Valid response.  Advance iterators
                final PendingUpdateState update = updatesIter.next();
                Integer changelist = UpdateParameterNames.CHANGELIST.getParameterValue(update);
                if (changelist == null) {
                    changelist = -1;
                }
                String filename = UpdateParameterNames.FILE.getParameterValue(update);
                // is edit only mode is only valid for the EDIT_FILE action.
                boolean isEditOnly = update.getUpdateAction() == UpdateAction.EDIT_FILE;
                FilePath filePath = FilePathUtil.getFilePath(filename);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" - Checking category for " + filePath + " (from [" + spec
                            .getDepotPathString() + "];[" + spec.getOriginalPathString() + "])");
                }
                if (isNotInClientView(spec)) {
                    LOG.debug(" -+- not in client view");
                    alerts.addNotice(exec.getProject(),
                            P4Bundle.message("error.client.not-in-view", spec.getClientPathString()), null,
                            filePath);
                    // don't handle
                    continue;
                }
                Map<Integer, Set<FilePath>> container = null;
                if (isNotKnownToServer(spec)) {
                    if (isEditOnly && ignoreAddsIfEditOnly) {
                        LOG.debug(" -+- Ignoring because Perforce doesn't know it, and the change is edit-only.");
                    } else {
                        LOG.debug(" -+- not known to server");
                        container = notInPerforce;
                    }
                } else if (spec.getOpStatus() != FileSpecOpStatus.VALID) {
                    LOG.debug(" -+- unknown error");
                    P4StatusMessage msg = new P4StatusMessage(spec);
                    FilePath[] files;
                    if (filePath != null) {
                        files = new FilePath[] { filePath };
                    } else {
                        files = new FilePath[0];
                    }
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.client.unknown.p4.title"),
                            msg.toString(),
                            P4StatusMessage.messageAsError(msg),
                            files);
                    // don't handle
                    continue;
                } else {
                    FileAction action = spec.getOpenAction();
                    if (action == null) {
                        // In perforce, not already opened.
                        container = notOpened;
                    } else {
                        switch (action) {
                            case ADD:
                            case ADD_EDIT:
                            case ADDED:
                            case EDIT:
                            case EDIT_FROM:
                            case MOVE_ADD:
                                // open for add or edit.
                                container = edited;
                                break;
                            case INTEGRATE:
                            case BRANCH:
                                // Integrated, but not open for edit.  Open for edit.
                                // TODO branch may need "add" in some cases.
                                container = integrated;
                                break;

                            case MOVE_DELETE:
                                // On the delete side of a move.  Mark as open for edit.
                                container = move_deleted;
                                break;
                            case DELETE:
                            case DELETED:
                                // Already open for delete; need to revert the file and edit.
                                // Because it's open for delete, that means the server knows
                                // about it.

                                LOG.info("Reverting delete and opening for edit: " + filename);
                                container = deleted;
                                break;
                            default:
                                LOG.error("Unexpected file action " + action + " from fstat");
                                // do nothing
                        }
                    }
                }
                if (container != null) {
                    Set<FilePath> set = container.get(changelist);
                    if (set == null) {
                        set = new HashSet<FilePath>();
                        container.put(changelist, set);
                    }
                    set.add(filePath);
                }
            }
        }
    }

    private static Map<Integer, Set<FilePath>> joinChangelistFiles(final Map<Integer, Set<FilePath>>... mList) {
        Map<Integer, Set<FilePath>> ret = new HashMap<Integer, Set<FilePath>>();
        for (Map<Integer, Set<FilePath>> m : mList) {
            for (Entry<Integer, Set<FilePath>> entry : m.entrySet()) {
                final Set<FilePath> fpSet = ret.get(entry.getKey());
                if (fpSet == null) {
                    ret.put(entry.getKey(), entry.getValue());
                } else {
                    fpSet.addAll(entry.getValue());
                }
            }
        }
        return ret;
    }

    private static Map<FilePath, FilePath> matchFileMoves(final Map<Integer, Set<FilePath>> moves,
            final Collection<PendingUpdateState> pendingUpdateStates) {
        Map<FilePath, FilePath> ret = new HashMap<FilePath, FilePath>();
        List<PendingUpdateState> remaining = new ArrayList<PendingUpdateState>(pendingUpdateStates);
        for (Set<FilePath> filePaths : moves.values()) {
            for (FilePath filePath : filePaths) {
                final Iterator<PendingUpdateState> iter = remaining.iterator();
                boolean found = false;
                while (iter.hasNext()) {
                    final PendingUpdateState next = iter.next();
                    final String dest = UpdateParameterNames.FILE.getParameterValue(next);
                    final String source = UpdateParameterNames.FILE_SOURCE.getParameterValue(next);
                    if (dest != null) {
                        FilePath destFile = FilePathUtil.getFilePath(dest);
                        if (filePath.equals(destFile)) {
                            found = true;
                            ret.put(filePath, FilePathUtil.getFilePath(source));
                            iter.remove();
                            break;
                        }
                    }
                }
                if (! found) {
                    // TODO report real problem
                    LOG.error("No matched source file for " + filePath);
                }
            }
        }
        return ret;
    }

    // =======================================================================
    // ACTIONS

    // Action classes must be static so that they can be correctly referenced
    // from the UpdateAction class.  They also must be fully executable
    // from the passed-in arguments and the pending state value.

    // The actions are also placed in here, rather than as stand-alone classes,
    // so that they have increased access to the cache objects.


    // FIXME all this logic below is really complicated.  Look at ways to
    // simplify it.


    static abstract class AbstractFileAction extends AbstractServerUpdateAction {
        protected AbstractFileAction(@NotNull final Collection<PendingUpdateState> pendingUpdateStates) {
            super(pendingUpdateStates);
        }

        public void abort(@NotNull ClientCacheManager clientCacheManager) {
            for (PendingUpdateState state: getPendingUpdateStates()) {
                String file = UpdateParameterNames.FILE.getParameterValue(state);
                if (file != null) {
                    FilePath fp = FilePathUtil.getFilePath(file);
                    clientCacheManager.markLocalFileStateCommitted(fp);
                }
            }
        }
    }



    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Add/Edit Action

    public static class AddEditFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new AddEditAction(states);
        }
    }

    static class AddEditAction extends AbstractFileAction {
        AddEditAction(@NotNull Collection<PendingUpdateState> pendingUpdateState) {
            super(pendingUpdateState);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull ClientCacheManager clientCacheManager, @NotNull final AlertManager alerts) {
            LOG.debug("Running edit");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, true);
            if (split.status != null) {
                return split.status;
            }

            // open for add, only if allowsAdd is true:
            //     split.notInPerforce
            // open for edit:
            //     split.notOpened
            // already open for edit or add; check for correct changelist:
            //     split.edited
            // open for edit (integrated, but not open for edit)
            //     split.integrated
            // on the delete side of a move; open for edit
            //     split.move_deleted
            // already open for delete; revert and edit
            // (because it is open for delete, the server knows about it).
            //     split.deleted

            // Perform the reverts first
            boolean hasUpdate = false;
            if (! split.deleted.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<IFileSpec> results = exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    final List<P4StatusMessage> msgs = P4StatusMessage.getErrors(results);
                    alerts.addNotices(exec.getProject(),
                            P4Bundle.message("warning.edit.file.revert",
                                    FilePathUtil.toStringList(reverts)), msgs, false);
                    // No need to mark updated, because these updates
                    // are happening later.
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert.title"),
                            P4Bundle.message("error.revert", FilePathUtil.toStringList(reverts)),
                            e, reverts);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }

            ExecutionStatus returnCode = ExecutionStatus.NO_OP;
            if (! split.notInPerforce.isEmpty()) {
                for (Entry<Integer, Set<FilePath>> entry : split.notInPerforce.entrySet()) {
                    try {
                        final List<P4StatusMessage> msgs =
                                exec.addFiles(FileSpecUtil.getFromFilePaths(entry.getValue()), entry.getKey());
                        alerts.addNotices(exec.getProject(),
                                P4Bundle.message("warning.edit.file.add",
                                        FilePathUtil.toStringList(entry.getValue())),
                                msgs, false);
                        markUpdated(clientCacheManager, entry.getValue(), msgs);
                        hasUpdate = true;
                    } catch (P4DisconnectedException e) {
                        // error already handled as critical
                        return ExecutionStatus.RETRY;
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("error.add.title"),
                                P4Bundle.message("error.add",
                                        FilePathUtil.toStringList(entry.getValue())),
                                e, entry.getValue());
                        returnCode = ExecutionStatus.FAIL;
                    }
                }
            } else if (! split.notInPerforce.isEmpty()) {
                LOG.info("Skipping add (because command is edit-only: " + split.notInPerforce);
            }

            // reopen:
            //     split.edited
            for (Entry<Integer, Set<FilePath>> entry: split.edited.entrySet()) {
                try {
                    final List<P4StatusMessage> msgs =
                            exec.reopenFiles(FileSpecUtil.getFromFilePaths(entry.getValue()),
                                    entry.getKey(), null);
                    markUpdated(clientCacheManager, entry.getValue(), msgs);
                    // Note: any errors here will be displayed to the
                    // user, but they do not indicate that the actual
                    // actions were errors.  Instead, they are notifications
                    // to the user that they must do something again
                    // with a correction.
                    alerts.addWarnings(exec.getProject(),
                            P4Bundle.message("warning.edit.file.reopen",
                                    FilePathUtil.toStringList(entry.getValue())),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.edit.title"),
                            P4Bundle.message("error.edit",
                                    FilePathUtil.toStringList(entry.getValue())),
                            e, entry.getValue());
                    returnCode = ExecutionStatus.FAIL;
                }
            }

            // open for edit:
            //     split.notOpened
            // open for edit (integrated, but not open for edit)
            //     split.integrated
            // on the delete side of a move; open for edit
            //     split.move_deleted
            // already open for delete; revert and edit
            //     split.deleted
            @SuppressWarnings("unchecked") Map<Integer, Set<FilePath>> edits = joinChangelistFiles(
                    split.notOpened, split.integrated, split.move_deleted, split.deleted);


            for (Entry<Integer, Set<FilePath>> entry : edits.entrySet()) {
                try {
                    final List<P4StatusMessage> msgs =
                            exec.editFiles(FileSpecUtil.getFromFilePaths(entry.getValue()),
                                    entry.getKey());
                    markUpdated(clientCacheManager, entry.getValue(), msgs);
                    // Note: any errors here will be displayed to the
                    // user, but they do not indicate that the actual
                    // actions were errors.  Instead, they are notifications
                    // to the user that they must do something again
                    // with a correction.
                    alerts.addWarnings(exec.getProject(),
                            P4Bundle.message("warning.edit.file.edit",
                                    FilePathUtil.toStringList(entry.getValue())),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.edit.title"),
                            P4Bundle.message("error.edit",
                                    FilePathUtil.toStringList(entry.getValue())),
                            e, entry.getValue());
                    returnCode = ExecutionStatus.FAIL;
                }
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Override
        @Nullable
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Delete Action

    public static class DeleteFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new DeleteAction(states);
        }
    }


    static class DeleteAction extends AbstractFileAction {

        DeleteAction(final Collection<PendingUpdateState> states) {
            super(states);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            LOG.debug("Running delete");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, false);
            if (split.status != null) {
                return split.status;
            }

            // ignore, because perforce doesn't know about it
            //     split.notInPerforce
            // open for delete:
            //     split.notOpened
            // revert and delete
            //     split.edited
            // revert and delete
            //     split.integrated
            // on the delete side of a move; ignore
            //     split.move_deleted
            // already open for delete; ignore
            //     split.deleted

            boolean hasUpdate = false;
            ExecutionStatus returnCode = ExecutionStatus.NO_OP;

            // Perform the reverts first
            @SuppressWarnings("unchecked") final Map<Integer, Set<FilePath>> revertSets =
                    joinChangelistFiles(split.edited, split.integrated);
            if (!revertSets.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<IFileSpec> results = exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    final List<P4StatusMessage> msgs = P4StatusMessage.getErrors(results);
                    alerts.addNotices(exec.getProject(),
                            P4Bundle.message("warning.edit.file.revert",
                                    FilePathUtil.toStringList(reverts)),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert.title"),
                            P4Bundle.message("error.revert",
                                    FilePathUtil.toStringList(reverts)),
                            e, reverts);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }


            @SuppressWarnings("unchecked") Map<Integer, Set<FilePath>> deletes = joinChangelistFiles(
                    split.notOpened, split.integrated, split.edited);

            for (Entry<Integer, Set<FilePath>> entry : deletes.entrySet()) {
                try {
                    final List<P4StatusMessage> msgs =
                            exec.deleteFiles(FileSpecUtil.getFromFilePaths(entry.getValue()),
                                    entry.getKey(), true);
                    markUpdated(clientCacheManager, entry.getValue(), msgs);
                    // Note: any errors here will be displayed to the
                    // user, but they do not indicate that the actual
                    // actions were errors.  Instead, they are notifications
                    // to the user that they must do something again
                    // with a correction.
                    alerts.addWarnings(exec.getProject(),
                            P4Bundle.message("warning.edit.file.edit",
                                    FilePathUtil.toStringList(entry.getValue())),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.delete.title"),
                            P4Bundle.message("error.delete",
                                    FilePathUtil.toStringList(entry.getValue())),
                            e, entry.getValue());
                    returnCode = ExecutionStatus.FAIL;
                }
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Nullable
        @Override
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Move Action

    public static class MoveFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new MoveAction(states);
        }
    }


    static class MoveAction extends AbstractFileAction {

        MoveAction(final Collection<PendingUpdateState> states) {
            super(states);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            LOG.debug("Running move");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, false);
            if (split.status != null) {
                return split.status;
            }

            // ignore, because perforce doesn't know about it
            //     split.notInPerforce
            // move
            //     split.notOpened
            // revert and move
            //     split.edited
            //     deleted
            //     integrated
            //     move_deleted

            boolean hasUpdate = false;
            ExecutionStatus returnCode = ExecutionStatus.NO_OP;


            // TODO check logic for whether the source is in the client.  If not, then this is either
            // an integrate or a no-op.


            // Perform the reverts first
            @SuppressWarnings("unchecked") final Map<Integer, Set<FilePath>> revertSets =
                    joinChangelistFiles(split.edited, split.deleted, split.integrated, split.move_deleted);
            if (!revertSets.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<IFileSpec> results = exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    final List<P4StatusMessage> msgs = P4StatusMessage.getErrors(results);
                    alerts.addNotices(exec.getProject(),
                            P4Bundle.message("warning.move.file.revert",
                                    FilePathUtil.toStringList(reverts)),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert.title"),
                            P4Bundle.message("error.revert",
                                    FilePathUtil.toStringList(reverts)),
                            e, reverts);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }


            @SuppressWarnings("unchecked") Map<Integer, Set<FilePath>> moves = joinChangelistFiles(
                    split.notOpened, revertSets);
            final Map<FilePath, FilePath> moveTo = matchFileMoves(moves, getPendingUpdateStates());
            for (Entry<Integer, Set<FilePath>> entry : moves.entrySet()) {
                for (FilePath filePath : entry.getValue()) {
                    try {
                        final List<P4StatusMessage> msgs = exec.moveFile(FileSpecUtil.getFromFilePath(filePath),
                                FileSpecUtil.getFromFilePath(moveTo.get(filePath)), entry.getKey(), false);
                        markUpdated(clientCacheManager, entry.getValue(), msgs);
                        // Note: any errors here will be displayed to the
                        // user, but they do not indicate that the actual
                        // actions were errors.  Instead, they are notifications
                        // to the user that they must do something again
                        // with a correction.
                        alerts.addWarnings(exec.getProject(),
                                P4Bundle.message("warning.edit.file.edit",
                                        FilePathUtil.toStringList(entry.getValue())),
                                msgs, false);
                        hasUpdate = true;
                    } catch (P4DisconnectedException e) {
                        // error already handled as critical
                        return ExecutionStatus.RETRY;
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("error.move.title"),
                                P4Bundle.message("error.move",
                                        FilePathUtil.toStringList(entry.getValue())),
                                e, entry.getValue());
                        returnCode = ExecutionStatus.FAIL;
                    }
                }
            }

            if (hasUpdate && returnCode == ExecutionStatus.NO_OP) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Nullable
        @Override
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }


    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Move Action

    public static class RevertFactory implements ServerUpdateActionFactory {
        @NotNull
        @Override
        public ServerUpdateAction create(@NotNull Collection<PendingUpdateState> states) {
            return new RevertAction(states);
        }
    }


    static class RevertAction extends AbstractFileAction {

        RevertAction(final Collection<PendingUpdateState> states) {
            super(states);
        }

        @NotNull
        @Override
        protected ExecutionStatus executeAction(@NotNull final P4Exec2 exec,
                @NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            LOG.debug("Running move");

            final ActionSplit split = new ActionSplit(exec, getPendingUpdateStates(), alerts, false);
            if (split.status != null) {
                return split.status;
            }

            // ignore, because perforce doesn't know about it
            //     split.notInPerforce
            // ignore, because it's not open for edit
            //     split.notOpened
            // revert and move
            //     split.edited
            //     deleted
            //     integrated
            //     move_deleted

            boolean hasUpdate = false;
            ExecutionStatus returnCode = ExecutionStatus.NO_OP;

            @SuppressWarnings("unchecked") final Map<Integer, Set<FilePath>> revertSets =
                    joinChangelistFiles(split.edited, split.deleted, split.integrated, split.move_deleted);
            if (!revertSets.isEmpty()) {
                final Set<FilePath> reverts = new HashSet<FilePath>();
                for (Collection<FilePath> fpList : split.deleted.values()) {
                    reverts.addAll(fpList);
                }
                try {
                    final List<IFileSpec> results = exec.revertFiles(FileSpecUtil.getFromFilePaths(reverts));
                    final List<P4StatusMessage> msgs = P4StatusMessage.getErrors(results);
                    alerts.addNotices(exec.getProject(),
                            P4Bundle.message("warning.revert.file",
                                    FilePathUtil.toStringList(reverts)),
                            msgs, false);
                    hasUpdate = true;
                } catch (P4DisconnectedException e) {
                    // error already handled as critical
                    return ExecutionStatus.RETRY;
                } catch (VcsException e) {
                    alerts.addWarning(exec.getProject(),
                            P4Bundle.message("error.revert.title"),
                            P4Bundle.message("error.revert",
                                    FilePathUtil.toStringList(reverts)),
                            e, reverts);
                    // cannot continue; just fail?
                    return ExecutionStatus.FAIL;
                }
            }

            if (hasUpdate) {
                returnCode = ExecutionStatus.RELOAD_CACHE;
            }

            return returnCode;
        }

        @Nullable
        @Override
        protected ServerQuery updateCache(@NotNull final ClientCacheManager clientCacheManager,
                @NotNull final AlertManager alerts) {
            return clientCacheManager.createFileActionsRefreshQuery();
        }
    }


}
