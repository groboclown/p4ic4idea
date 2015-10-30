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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.*;
import net.groboclown.idea.p4ic.server.tasks.*;
import net.groboclown.idea.p4ic.v2.events.Events;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Higher level actions on Perforce objects.
 * @deprecated being kept around until the other code that's copying this is finished.
 */
public class RawServerExecutor {
    private static final Logger LOG = Logger.getInstance(RawServerExecutor.class);

    private final ServerStatus config;
    private final String clientName;
    private final ConnectionHandler connectionHandler;

    private final List<P4Exec> idlePool = new ArrayList<P4Exec>();
    private final List<P4Exec> activePool = new ArrayList<P4Exec>();
    private final List<P4Exec> closingPool = new ArrayList<P4Exec>();
    private final Object poolSync = new Object();
    private boolean disposed = false;
    private boolean closed = false;

    private final FileInfoCache fileInfoCache = new FileInfoCache();


    /**
     *
     */
    public RawServerExecutor(@NotNull ServerStatus config, String clientName) {
        this.config = config;
        this.clientName = clientName;
        this.connectionHandler = ConnectionHandler.getHandlerFor(config);
    }


    /**
     *
     * @deprecated see ServerConnectionController
     */
    public boolean isWorkingOnline() {
        return config.isWorkingOnline();
    }


    /**
     *
     * @deprecated see ServerConnectionController
     */
    public boolean isOffline() {
        return config.isWorkingOffline();
    }


    void invalidateFileInfoCache() {
        fileInfoCache.invalidateCache();
    }


    public void dispose() {
        // This doesn't handle the in-progress connections that are running.
        // Instead, those dispose their P4Exec when they complete, rather
        // than putting back into this pool.
        // This means that after this dispose method returns, there still may
        // be active connections running.
        synchronized (poolSync) {
            wentOffline();
            invalidateFileInfoCache();
            disposed = true;
        }
    }


    private <T> T performAction(@NotNull Project project, @NotNull ServerTask<T> runner)
            throws VcsException, CancellationException {
        // The actions should not run in the dispatch thread,
        // but there are some solid reasons why this sometimes can run
        // in the AWT (specifically, edit operations are expected to run in EDT)

        final UserProjectPreferences userPreferences =
                P4Vcs.getInstance(project).getUserPreferences();

        // This call indicates an attempt to reconnect to the server.
        closed = false;

        P4Exec exec = null;
        long startTime = System.currentTimeMillis();
        synchronized (poolSync) {
            while (exec == null) {
                if (closed || disposed) {
                    // went offline during the connection attempts
                    throw new P4DisconnectedException();
                } else if (! idlePool.isEmpty()) {
                    exec = idlePool.remove(idlePool.size() - 1);
                } else if (activePool.size() < userPreferences.getMaxServerConnections()) {
                    exec = new P4Exec(config, clientName, connectionHandler, null);
                    //        new OnServerConfigurationProblem.WithMessageBus(project));
                } else if (System.currentTimeMillis() - startTime > userPreferences.getMaxConnectionWaitTimeMillis()) {
                    throw new P4DisconnectedException(P4Bundle.message("connection.timeout"));
                } else {
                    // wait for the pool to open up
                    LOG.info(config.getConfig().getServiceName() + "/" + clientName +
                            ": Too many active connections; waiting for one to free up.");
                    try {
                        poolSync.wait(userPreferences.getMaxConnectionWaitTimeMillis());
                    } catch (InterruptedException e) {
                        throw new CancellationException();
                    }
                }
            }
            activePool.add(exec);

            LOG.debug(config.getConfig().getServiceName() + "/" + clientName +
                    ": number of active connections: " + activePool.size() +
                    "; idle connections: " + idlePool.size());
        }

        try {
            return runner.run(exec);
        } finally {
            synchronized (poolSync) {
                if (disposed || isOffline() || closingPool.contains(exec) || closed) {
                    exec.dispose();
                    closingPool.remove(exec);
                    // do not put back into any pool.
                } else {
                    activePool.remove(exec);
                    idlePool.add(exec);
                }
                poolSync.notifyAll();
            }
        }
    }

    @Nullable
    public IChangelist getChangelist(@NotNull final Project project, final int changelistId)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<IChangelist>() {
            @Override
            public IChangelist run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getChangelist(project, changelistId);
            }
        });
    }


    @NotNull
    public List<IClientSummary> getUserClients(@NotNull final Project project)
            throws VcsException, CancellationException {
        List<IClientSummary> ret = performAction(project, new ServerTask<List<IClientSummary>>() {
            @Override
            public List<IClientSummary> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getClientsForUser(project);
            }
        });
        assert ret != null;
        return ret;
    }


    @NotNull
    public List<P4StatusMessage> addOrCopyFiles(@NotNull Project project, @NotNull Collection<VirtualFile> addFiles,
            Map<VirtualFile, VirtualFile> copiedFiles, int changelist)
            throws VcsException, CancellationException {
        List<P4StatusMessage> ret = performAction(project,
                new AddCopyRunner(project, addFiles, copiedFiles, changelist, fileInfoCache));
        assert ret != null;
        return ret;
    }

    @NotNull
    public List<P4StatusMessage> moveFiles(@NotNull Project project,
            @NotNull Map<FilePath, FilePath> movedFiles, int changelist)
            throws VcsException, CancellationException {
        return performAction(project, new MoveRunner(project, movedFiles, changelist, fileInfoCache));
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull Project project, @NotNull Collection<FilePath> files,
            int changelist) throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new DeleteRunner(project, files, changelist, fileInfoCache));
    }

    @NotNull
    public List<P4StatusMessage> addOrEditFiles(@NotNull Project project, @NotNull List<VirtualFile> files, int changelist)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new EditRunner(project, files, changelist, true, fileInfoCache));
    }

    @NotNull
    public List<P4StatusMessage> editFiles(@NotNull Project project, @NotNull List<VirtualFile> files, int changelist)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new EditRunner(project, files, changelist, false, fileInfoCache));
    }

    @NotNull
    public IChangelist createChangelist(@NotNull final Project project, @NotNull final String comment)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<IChangelist>() {
            @Override
            public IChangelist run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.createChangeList(project, comment);
            }
        });
    }

    @NotNull
    public List<P4FileInfo> getFilePathInfo(@NotNull final Project project, @NotNull Collection<FilePath> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        // Strip out directories, and remove duplicates
        final Set<FilePath> fps = new HashSet<FilePath>(files.size());
        for (FilePath file : files) {
            if (!file.isDirectory()) {
                fps.add(file);
            }
        }
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                List<P4FileInfo> ret = exec.loadFileInfo(project, FileSpecUtil.getFromFilePaths(fps), fileInfoCache);
                if (ret.size() != fps.size()) {
                    P4ApiException e = new P4ApiException(P4Bundle.message("error.file-fetch.count"));
                    LOG.info("Requested " + fps + "; retrieved " + ret, e);
                    //throw e;
                }
                return ret;
            }
        });
    }

    @NotNull
    public List<P4FileInfo> getVirtualFileInfo(@NotNull final Project project, @NotNull Collection<VirtualFile> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        // Strip out directories
        final List<VirtualFile> vfs = new ArrayList<VirtualFile>(files.size());
        for (VirtualFile file: files) {
            if (! file.isDirectory()) {
                vfs.add(file);
            }
        }
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                List<P4FileInfo> ret = exec.loadFileInfo(project, FileSpecUtil.getFromVirtualFiles(vfs), fileInfoCache);
                if (ret.size() != vfs.size()) {
                    P4ApiException e = new P4ApiException("Did not fetch same number of files as requested");
                    LOG.info("Requested " + vfs + "; retrieved " + ret, e);
                    throw e;
                }
                return ret;
            }
        });
    }


    @NotNull
    public Collection<VirtualFile> findRoots(@NotNull final Project project, @Nullable final Collection<VirtualFile> requestedRoots) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<Collection<VirtualFile>>() {
            @Override
            public Collection<VirtualFile> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                IClientSummary client = exec.getClient(project);
                FilePath clientFp = VcsUtil.getFilePath(client.getRoot());
                if (clientFp == null) {
                    throw new P4Exception(P4Bundle.message("error.roots.not-found", clientName, client.getRoot()));
                } else if (requestedRoots == null) {
                    return Collections.singletonList(clientFp.getVirtualFile());
                } else {
                    Set<FilePath> missed = new HashSet<FilePath>();
                    Set<VirtualFile> ret = new HashSet<VirtualFile>();
                    for (VirtualFile root: requestedRoots) {
                        if (root == null) {
                            throw new P4Exception(P4Bundle.message("error.roots.null"));
                        }
                        if (! root.exists() || ! root.isDirectory()) {
                            throw new P4Exception(P4Bundle.message("error.roots.invalid", root));
                        }
                        FilePath fp = VcsUtil.getFilePath(root);
                        if (clientFp.isUnder(fp, false)) {
                            // a weird situation where a .p4config file or the
                            // project root is lower than the client root.
                            ret.add(clientFp.getVirtualFile());
                        } else if (! fp.isUnder(clientFp, false)) {
                            missed.add(fp);
                        } else {
                            ret.add(root);
                        }
                    }
                    if (! missed.isEmpty()) {
                        if (ret.isEmpty()) {
                            P4InvalidConfigException ex = new P4InvalidConfigException(P4Bundle.message("error.roots.mismatch", missed, clientFp));
                            ManualP4Config badConfig = new ManualP4Config(config.getConfig(), clientName);
                            Events.configInvalid(project, badConfig, ex);


                            // FIXME old stuff
                            //project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).
                            //        configurationProblem(project, badConfig, ex);
                            throw ex;
                        }
                        LOG.info("Roots (" + missed + ") are not under the client root (" + clientFp +
                                ") or are not the parent of the client root.  Ignored, and using " + ret);
                    }
                    return ret;
                }
            }
        });
    }


    @NotNull
    public List<P4FileInfo> loadOpenFiles(@NotNull final Project project, @Nullable final VirtualFile[] roots)
            throws VcsException, CancellationException {
        // Do not load all the opened files (//...).  Instead,
        // only load the files that are in the VCS client roots.

        final List<IFileSpec> specs = FileSpecUtil.makeRootFileSpecs(roots);
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.loadOpenedFiles(project, specs, fileInfoCache);
            }
        });
    }


    /**
     *
     * @param changelistId Perforce changelist ID
     * @return null if the changelist doesn't exist
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public List<P4FileInfo> getFilesInChangelist(@NotNull final Project project, final int changelistId)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getFilesInChangelist(project, changelistId, fileInfoCache);
            }
        });
    }


    @NotNull
    public List<P4StatusMessage> revertFiles(@NotNull final Project project, @NotNull final List<FilePath> files)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<List<P4StatusMessage>>() {
            @Override
            public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                // A revert MUST revert both ends of a move/rename
                // command.  We need to grab the extended file spec
                // and check for the "moved" information.

                List<P4FileInfo> p4files = getFilePathInfo(project, files);
                List<IFileSpec> specs = new ArrayList<IFileSpec>(p4files.size());
                for (P4FileInfo fi : p4files) {
                    IFileSpec pair = fi.getOpenedPair();
                    if (pair != null) {
                        specs.add(pair);
                    }
                    specs.add(fi.toClientSpec());
                }

                return exec.revertFiles(project, specs);
            }
        });
    }


    /**
     * Load the contents of the file from the server at the given revision number.
     * A rev number less than zero means head revision, 0 means no content.
     * This also performs the correct encoding of the file.
     *
     * @param p4File file to read
     * @param rev revision to fetch
     * @return text data of the file
     */
    public String loadFileAsString(@NotNull Project project, @NotNull P4FileInfo p4File, int rev)
            throws VcsException, CancellationException {
        return loadFileAsString(project, p4File.getPath(), rev);
    }


    public String loadFileAsString(@NotNull final Project project, @NotNull FilePath file, int rev)
            throws VcsException, CancellationException {
        byte[] bytes = loadFileAsBytes(project, file, rev);
        return encodeFileBytes(project, bytes);
    }

    /**
     * @param file file info to load contents
     * @return null if the file revision is 0; else not null
     * @throws VcsException
     * @throws CancellationException
     */
    @Nullable
    public String loadFileAsString(@NotNull final Project project, @NotNull IFileSpec file) throws VcsException, CancellationException {
        byte[] bytes = loadFileAsBytes(project, file);
        return encodeFileBytes(project, bytes);
    }


    public String encodeFileBytes(@NotNull final Project project, @Nullable final byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        // TODO encode correctly based upon the client settings, and the file information

        return new String(bytes);
    }


    @Nullable
    public byte[] loadFileAsBytes(@NotNull final Project project, @NotNull FilePath file, int rev)
                throws VcsException, CancellationException {
        if (rev == 0) {
            return null;
        }
        final IFileSpec spec = FileSpecUtil.getOneSpecWithRev(file, rev);
        return loadFileAsBytes(project, spec);
    }

    @Nullable
    public byte[] loadFileAsBytes(@NotNull final Project project, @NotNull final IFileSpec spec)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<byte[]>() {
            @Override
            public byte[] run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                try {
                    return exec.loadFile(project, spec);
                } catch (IOException e) {
                    throw new P4Exception(e);
                }
            }
        });
    }


    /*
    @NotNull
    public List<P4FileRevision> getRevisionHistoryOnline(@NotNull Project project,
            @NotNull P4FileInfo files, int maxRevs)
            throws VcsException, CancellationException {
        return performAction(project, new GetRevisionHistoryTask(project, files, maxRevs));
    }
    */


    public void deleteChangelist(@NotNull Project project, int changelistId)
            throws VcsException, CancellationException {
        if (changelistId <= 0) {
            return;
        }
        performAction(project, new DeleteChangelistTask(project, changelistId));
    }

    @NotNull
    public List<P4StatusMessage> moveFilesToChangelist(@NotNull Project project, final int targetChangelist,
            @Nullable List<FilePath> filesToMove) throws VcsException, CancellationException {
        if (filesToMove == null) {
            filesToMove = Collections.emptyList();
        }
        return performAction(project,
                new MoveFilesBetweenChangelistsTask(project, targetChangelist, filesToMove, fileInfoCache));
    }


    /*
    @Nullable
    public IChangelist loadChangeList(@NotNull final Project project, final int changelistId)
            throws VcsException, CancellationException {
        assert changelistId > 0;
        IChangelist ret = performAction(project, new ServerTask<IChangelist>() {
            @Override
            public IChangelist run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getChangelist(project, changelistId);
            }
        });
        return ret;
    }
    */


    public void updateChangelistComment(@NotNull final Project project, final int changelistId,
            @NotNull final String comment) throws VcsException, CancellationException {
        assert changelistId > 0;
        performAction(project, new ServerTask<Void>() {
            @Override
            public Void run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                exec.updateChangelistDescription(project, changelistId, comment);
                return null;
            }
        });
    }

    @NotNull
    public List<IChangelistSummary> getPendingClientChangelists(@NotNull final Project project)
            throws VcsException, CancellationException {
        List<IChangelistSummary> ret = performAction(project, new ServerTask<List<IChangelistSummary>>() {
            @Override
            public List<IChangelistSummary> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getPendingClientChangelists(project);
            }
        });
        assert ret != null;
        LOG.info("pending changelist count: " + ret.size());
        return ret;
    }

    @NotNull
    public List<P4FileInfo> loadDeepFileInfo(@NotNull final Project project, @NotNull final Collection<FilePath> files)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.loadFileInfo(project, FileSpecUtil.getFromFilePathsAt(
                        files, "#head", true), fileInfoCache);
            }
        });
    }

    /*
    @NotNull
    public List<P4AnnotatedLine> getAnnotationsFor(@NotNull final Project project, @NotNull final VirtualFile file, final int rev)
            throws VcsException, CancellationException {
        String revStr = "#have";
        if (rev >= 0) {
            revStr = "#" + Integer.toStringList(rev);
        }
        return performAction(project, new AnnotateFileTask(project, file, revStr, fileInfoCache));
    }
    */

    @NotNull
    public List<P4FileInfo> synchronizeFiles(@NotNull final Project project,
            @NotNull final Collection<FilePath> files, final int revision, @Nullable final String changelist,
            final boolean forceSync, @NotNull final Collection<VcsException> errorsOutput)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new ServerTask<List<P4FileInfo>>() {
            @Override
            public List<P4FileInfo> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                List<IFileSpec> specs;
                if (revision == 0) {
                    specs = FileSpecUtil.getFromFilePathsAt(files, "#none", true);
                } else if (revision > 0) {
                    specs = FileSpecUtil.getFromFilePathsAt(files, "#" + Integer.toString(revision), true);
                } else if (changelist != null) {
                    specs = FileSpecUtil.getFromFilePathsAt(files, "@" + changelist, true);
                } else {
                    specs = FileSpecUtil.getFromFilePathsAt(files, "#head", true);
                }
                for (IFileSpec spec: specs) {
                    LOG.info("Synchronizing " + spec.getAnnotatedPreferredPathString());
                }
                final List<IFileSpec> results = exec.synchronizeFiles(project, specs, forceSync);

                // Explicitly loop through the results to find which error
                // messages are valid.
                //errorsOutput.addAll(P4StatusMessage.messagesAsErrors(P4StatusMessage.getErrors(results)));
                for (IFileSpec spec: results) {
                    if (P4StatusMessage.isErrorStatus(spec)) {
                        final P4StatusMessage msg = new P4StatusMessage(spec);

                        // 17 = "file(s) up-to-date"
                        if (msg.getErrorCode() != 17) {
                            LOG.info(msg + ": error code " + msg.getErrorCode());
                            errorsOutput.add(P4StatusMessage.messageAsError(msg));
                        } else {
                            LOG.info(msg + ": ignored");
                        }
                    } else if (spec.getOpStatus() == FileSpecOpStatus.INFO) {
                        // INFO messages don't have a source, unfortunately.
                        // So we need to extract the path information.
                        LOG.info("info message: " + spec.getStatusMessage());
                        errorsOutput.add(new P4UpdateFileWarning(spec));
                    }
                }

                final List<IFileSpec> nonErrors = P4StatusMessage.getNonErrors(results);
                LOG.info("Synchronized " + nonErrors);
                return exec.loadFileInfo(project, nonErrors, fileInfoCache);
            }
        });
    }

    @NotNull
    public Collection<P4StatusMessage> integrateFiles(@NotNull final Project project, @NotNull final P4FileInfo src,
            @NotNull final FilePath target, final int changeListId) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<Collection<P4StatusMessage>>() {
            @Override
            public Collection<P4StatusMessage> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                // Src must be the depot path, because it can come from a different
                // client.
                final IFileSpec srcSpec = src.toDepotSpec();

                final IFileSpec tgtSpec = FileSpecUtil.getFromFilePaths(Collections.singletonList(target)).get(0);

                // don't copy to client is set to "true" because the IDE will handle the actual copy.
                return exec.integrateFiles(project, srcSpec, tgtSpec, changeListId, true);
            }
        });
    }

    @NotNull
    public Collection<P4FileInfo> revertUnchangedFilesInChangelist(final Project project, final int changeListId,
            @NotNull final List<P4StatusMessage> errors)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<Collection<P4FileInfo>>() {
            @Override
            public Collection<P4FileInfo> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                return exec.revertUnchangedFiles(project, FileSpecUtil.getP4RootFileSpec(), changeListId, errors, fileInfoCache);
            }
        });
    }


    @NotNull
    public Collection<P4FileInfo> revertUnchangedFiles(final Project project, @NotNull final List<FilePath> filePaths,
            @NotNull final List<P4StatusMessage> errors) throws VcsException {
        return performAction(project, new ServerTask<Collection<P4FileInfo>>() {
            @Override
            public Collection<P4FileInfo> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                return exec.revertUnchangedFiles(project, FileSpecUtil.getFromFilePaths(filePaths), -1, errors, fileInfoCache);
            }
        });
    }


    @NotNull
    public List<String> getJobStatusValues(@NotNull final Project project) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<List<String>>() {
            @Override
            public List<String> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                return exec.getJobStatusValues(project);
            }
        });
    }

    @NotNull
    public IClient getClient(@NotNull final Project project) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<IClient>() {
            @Override
            public IClient run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                return exec.getClient(project);
            }
        });
    }

    public void checkConnection(@NotNull final Project project)
            throws P4InvalidConfigException, CancellationException {
        try {
            performAction(project, new ServerTask<Void>() {
                @Override
                public Void run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                    exec.getServerInfo(project);
                    return null;
                }
            });
        } catch (P4InvalidConfigException e) {
            // thrower handles the listener call
            throw e;
        } catch (VcsException e) {
            LOG.info(e);
            throw new P4InvalidConfigException(e.getMessage());
        }
    }

    //@Nullable
    //public Collection<P4Job> getJobsForChangelist(@NotNull final Project project, final int changelistId) throws VcsException, CancellationException {
    //    return performAction(project, new ServerTask<Collection<P4Job>>() {
    //        @Override
    //        public Collection<P4Job> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
    //            return exec.getJobsForChangelist(project, changelistId);
    //        }
    //    });
    //}

    @Nullable
    public Collection<String> getJobIdsForChangelist(@NotNull final Project project, final int changelistId) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<Collection<String>>() {
            @Override
            public Collection<String> run(@NotNull final P4Exec exec) throws VcsException, CancellationException {
                return exec.getJobIdsForChangelist(project, changelistId);
            }
        });
    }

    void wentOnline() {
        // do nothing
    }

    void wentOffline() {
        synchronized (poolSync) {
            closed = true;
            for (P4Exec exec : idlePool) {
                exec.dispose();
            }
            closingPool.addAll(activePool);
            activePool.clear();
            idlePool.clear();
            poolSync.notifyAll();
        }
    }
}
