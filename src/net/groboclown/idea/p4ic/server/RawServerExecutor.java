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
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4ConfigListener;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4AnnotatedLine;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.tasks.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Higher level actions on Perforce objects.
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


    /**
     *
     */
    public RawServerExecutor(@NotNull ServerStatus config, String clientName) {
        this.config = config;
        this.clientName = clientName;
        this.connectionHandler = ConnectionHandler.getHandlerFor(config);
    }


    public boolean isWorkingOnline() {
        return config.isWorkingOnline();
    }


    public boolean isOffline() {
        return config.isWorkingOffline();
    }


    public void dispose() {
        // This doesn't handle the in-progress connections that are running.
        // Instead, those dispose their P4Exec when they complete, rather
        // than putting back into this pool.
        // This means that after this dispose method returns, there still may
        // be active connections running.
        synchronized (poolSync) {
            wentOffline();
            disposed = true;
        }
    }


    private <T> T performAction(@NotNull Project project, @NotNull ServerTask<T> runner)
            throws VcsException, CancellationException {
        // This is a suggestion, but there are some solid reasons why this should
        // be in the AWT (specifically, edit)
        //final Application appManager = ApplicationManager.getApplication();
        //if (appManager.isDispatchThread()) {
        //    //LOG.info("Should not ever run P4 commands in the EDT");
        //    throw new IllegalStateException("Must not ever run P4 commands in the EDT");
        //}
        P4Exec exec;
        synchronized (poolSync) {
            if (idlePool.isEmpty()) {
                LOG.info("Creating a new Perforce connection object");
                exec = new P4Exec(config, clientName, connectionHandler,
                        new OnServerConfigurationProblem.WithMessageBus(project));
            } else {
                exec = idlePool.remove(idlePool.size() - 1);
            }
        }
        try {
            return runner.run(exec);
        } finally {
            synchronized (poolSync) {
                if (disposed || isOffline() || closingPool.contains(exec)) {
                    exec.dispose();
                    closingPool.remove(exec);
                    // do not put back into any pool.
                } else {
                    activePool.remove(exec);
                    idlePool.add(exec);
                }
            }
        }
    }


    @Nullable
    public IChangelist getChangelist(@NotNull final Project project, final int rev)
            throws VcsException, CancellationException {
        return performAction(project, new ServerTask<IChangelist>() {
            @Override
            public IChangelist run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                return exec.getChangelist(project, rev);
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
                new AddCopyRunner(project, addFiles, copiedFiles, changelist));
        assert ret != null;
        return ret;
    }

    @NotNull
    public List<P4StatusMessage> moveFiles(@NotNull Project project,
            @NotNull Map<FilePath, FilePath> movedFiles, int changelist)
            throws VcsException, CancellationException {
        return performAction(project, new MoveRunner(project, movedFiles, changelist));
    }

    @NotNull
    public List<P4StatusMessage> deleteFiles(@NotNull Project project, @NotNull Collection<FilePath> files,
            int changelist) throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new DeleteRunner(project, files, changelist));
    }

    @NotNull
    public List<P4StatusMessage> editFiles(@NotNull Project project, @NotNull List<VirtualFile> files, int changelist)
            throws VcsException, CancellationException {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        return performAction(project, new EditRunner(project, files, changelist));
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
    public List<P4StatusMessage> submitChangelist(@NotNull Project project,
            @Nullable List<FilePath> actualFiles, @Nullable Collection<String> jobIds, int changelistId)
            throws VcsException, CancellationException {
        if (changelistId <= 0) {
            throw new VcsException("Invalid changelist ID " + changelistId);
        }
        return performAction(project, new SubmitRunner(project, actualFiles, jobIds, changelistId));
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
                List<P4FileInfo> ret = exec.loadFileInfo(project, FileSpecUtil.getFromFilePaths(fps));
                if (ret.size() != fps.size()) {
                    P4ApiException e = new P4ApiException("Did not fetch same number of files as requested");
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
                List<P4FileInfo> ret = exec.loadFileInfo(project, FileSpecUtil.getFromVirtualFiles(vfs));
                if (ret.size() != vfs.size()) {
                    P4ApiException e = new P4ApiException("Did not fetch same number of files as requested");
                    LOG.info("Requested " + vfs + "; retrieved " + ret, e);
                    throw e;
                }
                return ret;
            }
        });
    }


    public Collection<VirtualFile> findRoots(@NotNull final Project project, @Nullable final Collection<VirtualFile> requestedRoots) throws VcsException, CancellationException {
        return performAction(project, new ServerTask<Collection<VirtualFile>>() {
            @Override
            public Collection<VirtualFile> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                IClientSummary client = exec.getClient(project);
                FilePath clientFp = VcsUtil.getFilePath(client.getRoot());
                if (clientFp == null) {
                    throw new P4Exception("No client root found for client " + clientName + " (" + client.getRoot() + ")");
                } else if (requestedRoots == null) {
                    return Collections.singletonList(clientFp.getVirtualFile());
                } else {
                    Set<FilePath> missed = new HashSet<FilePath>();
                    Set<VirtualFile> ret = new HashSet<VirtualFile>();
                    for (VirtualFile root: requestedRoots) {
                        if (root == null) {
                            throw new P4Exception("null root");
                        }
                        if (! root.exists() || ! root.isDirectory()) {
                            throw new P4Exception("invalid root directory " + root);
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
                            // FIXME clean up this message, and localize it.
                            // This is user facing!
                            P4InvalidConfigException ex = new P4InvalidConfigException("Roots (" + missed + ") are not under the client root (" + clientFp +
                                    ") or are not the parent of the client root.  Is the P4CONFIG file referencing the wrong client, or in the wrong directory?");
                            project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).
                                    configurationProblem(project, new ManualP4Config(config.getConfig(), clientName), ex);
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
                return exec.loadOpenedFiles(project, specs);
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
                return exec.getFilesInChangelist(project, changelistId);
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
                for (P4FileInfo fi: p4files) {
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

        // FIXME encode the file correctly
        if (bytes != null) {
            return new String(bytes);
        }
        return null;
    }

    public byte[] loadFileAsBytes(@NotNull final Project project, @NotNull FilePath file, int rev)
                throws VcsException, CancellationException {
        if (rev == 0) {
            return null;
        }
        final IFileSpec spec = FileSpecUtil.getOneSpecWithRev(file, rev);

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


    public List<P4FileRevision> getRevisionHistory(@NotNull Project project,
            @NotNull P4FileInfo files, int maxRevs)
            throws VcsException, CancellationException {
        return performAction(project, new GetRevisionHistoryTask(project, files, maxRevs));
    }


    @Deprecated
    public void deleteChangelist(@NotNull Project project, @NotNull final IChangelist p4)
            throws VcsException, CancellationException {
        if (p4.getStatus() == ChangelistStatus.SUBMITTED || p4.getId() <= 0) {
            // can't delete submitted changelists, and can't delete the
            // default changelist.
            return;
        }
        performAction(project, new DeleteChangelistTask(project, p4.getId()));
    }


    public void deleteChangelist(@NotNull Project project, int changelistId)
            throws VcsException, CancellationException {
        if (changelistId <= 0) {
            return;
        }
        performAction(project, new DeleteChangelistTask(project, changelistId));
    }

    public List<P4StatusMessage> moveFilesBetweenChangelists(@NotNull Project project,
            final int sourceChangelist, final int targetChangelist,
            @Nullable List<FilePath> filesToMove) throws VcsException, CancellationException {
        if (filesToMove == null) {
            filesToMove = Collections.emptyList();
        }
        return performAction(project,
                new MoveFilesBetweenChangelistsTask(project, sourceChangelist, targetChangelist, filesToMove));
    }


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
        P4Vcs.getInstance(project).getChangeListMapping().updateP4Changelist(new RawClient(), changelistId, ret);
        return ret;
    }


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
        P4Vcs.getInstance(project).getChangeListMapping().updatePendingP4ChangeLists(config.getConfig(), ret);
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
                List<P4FileInfo> ret = exec.loadFileInfo(project, FileSpecUtil.getFromFilePathsAt(
                        files, "#head", true));
                return ret;
            }
        });
    }

    @NotNull
    public List<P4AnnotatedLine> getAnnotationsFor(@NotNull final Project project, @NotNull final VirtualFile file, final int rev)
            throws VcsException, CancellationException {
        String revStr = "#have";
        if (rev >= 0) {
            revStr = "#" + Integer.toString(rev);
        }
        return performAction(project, new AnnotateFileTask(project, file, revStr));
    }

    public void synchronizeFiles(@NotNull final Project project, @NotNull final Collection<VirtualFile> files,
            final int revision, final int changelist)
            throws VcsException, CancellationException {
        performAction(project, new ServerTask<Void>() {
            @Override
            public Void run(@NotNull P4Exec exec) throws VcsException, CancellationException {
                List<IFileSpec> specs;
                if (revision >= 0) {
                    specs = FileSpecUtil.getFromVirtualFilesAt(files, "#" + Integer.toString(revision), true);
                } else if (changelist >= 0) {
                    specs = FileSpecUtil.getFromVirtualFilesAt(files, "@" + Integer.toString(changelist), true);
                } else {
                    specs = FileSpecUtil.getFromVirtualFilesAt(files, "#head", true);
                }
                exec.synchronizeFiles(project, specs);
                return null;
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
        } catch (CancellationException e) {
            throw e;
        } catch (VcsException e) {
            e.printStackTrace();
        }
    }

    void wentOnline() {
        // do nothing
    }

    void wentOffline() {
        synchronized (poolSync) {
            for (P4Exec exec : idlePool) {
                exec.dispose();
            }
            closingPool.addAll(activePool);
            activePool.clear();
            idlePool.clear();
        }
    }


    private class RawClient implements Client {

        @NotNull
        @Override
        public ServerExecutor getServer() {
            throw new IllegalStateException("not implemented");
        }

        @NotNull
        @Override
        public String getClientName() {
            return clientName;
        }

        @NotNull
        @Override
        public ServerConfig getConfig() {
            return config.getConfig();
        }

        @NotNull
        @Override
        public List<VirtualFile> getRoots() {
            throw new IllegalStateException("not implemented");
        }

        @NotNull
        @Override
        public List<FilePath> getFilePathRoots() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public boolean isWorkingOffline() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public boolean isWorkingOnline() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void forceDisconnect() {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void dispose() {
            throw new IllegalStateException("not implemented");
        }
    }
}
