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
package net.groboclown.idea.p4ic.v2.file;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.vcsUtil.VcsFileUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.P4Server.IntegrateFile;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

public class P4VFSListener extends VcsVFSListener {
    private static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

    private final P4Vcs vcs;
    private final AlertManager alerts;
    private final P4ChangeListMapping changeListMapping;

    /**
     * Synchronizes on VFS operations; IDEA can send requests to open for edit and move,
     * or open for edit and delete at nearly the same time.  If these are allowed to
     * collide, then the incorrect actions can happen.  By making all these basic
     * actions be synchronized, we guarantee the right operation, but at a slight
     * cost to time.
     *
     * This should be shared with {@link P4EditFileProvider} and {@link P4RollbackEnvironment}.
     */
    private final Lock vfsLock;

    P4VFSListener(@NotNull P4Vcs vcs, @NotNull AlertManager alerts, @NotNull Lock vfsLock) {
        super(vcs.getProject(), vcs);
        this.vcs = vcs;
        this.alerts = alerts;
        this.vfsLock = vfsLock;
        this.changeListMapping = P4ChangeListMapping.getInstance(vcs.getProject());
    }


    @Override
    protected void performDeletion(@NotNull final List<FilePath> filesToDelete) {
        VcsFileUtil.markFilesDirty(myProject, filesToDelete);

        try {
            final Map<P4Server, List<FilePath>> serverMap =
                    vcs.mapFilePathsToP4Server(filesToDelete);
            for (Entry<P4Server, List<FilePath>> entry : serverMap.entrySet()) {
                final P4Server server = entry.getKey();
                if (server == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.info("deleted non-p4 server files: " + entry.getValue());
                    }
                } else {
                    int changelist = changeListMapping.getProjectDefaultPerforceChangelist(server).getChangeListId();
                    if (LOG.isDebugEnabled()) {
                        LOG.info("Opening for delete on changelist " + changelist + ": " + entry.getValue());
                    }
                    server.deleteFiles(entry.getValue(), changelist);
                }
            }
        } catch (InterruptedException e) {
            alerts.addNotice(vcs.getProject(), P4Bundle.message("interrupted_exception", filesToDelete),
                    new VcsInterruptedException(e));
        }
    }




    @Override
    protected void performMoveRename(@NotNull final List<MovedFileInfo> movedFiles) {
        final Map<FilePath, FilePath> moveMap = new HashMap<FilePath, FilePath>();
        for (MovedFileInfo info : movedFiles) {
            moveMap.put(
                    FilePathUtil.getFilePath(info.myOldPath),
                    FilePathUtil.getFilePath(info.myNewPath)
            );
        }
        VcsFileUtil.markFilesDirty(myProject, new ArrayList<FilePath>(moveMap.keySet()));
        VcsFileUtil.markFilesDirty(myProject, new ArrayList<FilePath>(moveMap.values()));

        try {
            final SplitServerFileMap split = splitMap(moveMap);
            for (P4Server server : split.getServers()) {
                int changelistId = changeListMapping.
                        getProjectDefaultPerforceChangelist(server).getChangeListId();

                vfsLock.lock();
                try {
                    server.moveFiles(split.getFilePathMatch(server), changelistId);

                    // all the files that come from outside the server are just added.
                    server.addOrEditFiles(split.getCrossTargetDifferentServerVirtualFilesFor(server), changelistId);


                    // Cross client files.
                    // This situation can happen if the file is moved either from or to outside P4 control,
                    // or if the file moves from one P4 client to another P4 client.
                    // IDE will handle underlying I/O copy and delete operation.

                    server.deleteFiles(split.getCrossSourceFilePathsFor(server), changelistId);

                    // perform an integrate for cross-client, if they share the same server.
                    server.integrateFiles(split.getSameServerCrossVirtualFilesForTarget(server), changelistId);
                } finally {
                    vfsLock.unlock();
                }
            }

            // TODO ensure all the files were used.

            P4ChangesViewRefresher.refreshLater(vcs.getProject());
        } catch (InterruptedException e) {
            alerts.addNotice(vcs.getProject(), P4Bundle.message("interrupted_exception", movedFiles),
                    new VcsInterruptedException(e));
        }
    }


    @Override
    protected void performAdding(
            @NotNull final Collection<VirtualFile> addedFiles,
            @NotNull final Map<VirtualFile, VirtualFile> copyFromMap) {
        // IDEA 140.2110.5 changed the markFilesDirty API to just take FilePath objects.

        LOG.info("Adding files " + addedFiles);

        List<FilePath> dirtyFilePaths = new ArrayList<FilePath>(addedFiles.size());
        for (VirtualFile vf: addedFiles) {
            dirtyFilePaths.add(FilePathUtil.getFilePath(vf));
        }
        for (VirtualFile vf: copyFromMap.keySet()) {
            dirtyFilePaths.add(FilePathUtil.getFilePath(vf));
        }
        VcsFileUtil.markFilesDirty(myProject, dirtyFilePaths);

        // To prevent a bunch of extra collection manipulation,
        // we'll just split this into two loops.  Also, if the
        // outside tasks want to interrupt one of the actions,
        // they should all be interrupted.

        try {
            for (Map.Entry<P4Server, List<VirtualFile>> entry : vcs.mapVirtualFilesToP4Server(addedFiles).entrySet()) {
                final P4Server server = entry.getKey();
                if (server == null) {
                    // outside of VCS.  Ignore.
                    alerts.addNotice(vcs.getProject(),
                            P4Bundle.message("add.file.no-server", entry.getValue()), null);
                } else {
                    final int changelistId = changeListMapping.
                            getProjectDefaultPerforceChangelist(server).getChangeListId();
                    vfsLock.lock();
                    try {
                        server.addOrEditFiles(entry.getValue(), changelistId);
                    } finally {
                        vfsLock.unlock();
                    }
                }
            }


            final SplitServerFileMap split = splitMap(copyFromMap);
            for (P4Server server : split.getServers()) {
                int changelistId = changeListMapping.
                        getProjectDefaultPerforceChangelist(server).getChangeListId();

                vfsLock.lock();
                try {
                    server.integrateFiles(split.getFilePathMatch(server), changelistId);


                    // Cross client files.
                    // This situation can happen if the file is moved either from or to outside P4 control,
                    // or if the file moves from one P4 client to another P4 client.
                    // IDE will handle underlying I/O copy operation.

                    // perform an integrate for cross-client, if they share the same server.
                    server.integrateFiles(split.getSameServerCrossVirtualFilesForTarget(server), changelistId);

                    // all the files that come from outside the server are just added.
                    server.addOrEditFiles(split.getCrossTargetDifferentServerVirtualFilesFor(server), changelistId);
                } finally {
                    vfsLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // TODO better error message
            alerts.addNotice(vcs.getProject(),
                    P4Bundle.message("interrupted_exception", addedFiles), e);
        }
    }


    /**
     * P4EditFileProvider only handles edits when the
     * user changes from read-only to writable.  If the user
     * has the p4 client setup such that it syncs the files
     * as writable, then they will not be checked out.  This
     * covers that missing aspect.
     *
     * @param event file event
     * @param file file affected
     */
    @Override
    protected void beforeContentsChange(@NotNull VirtualFileEvent event, @NotNull final VirtualFile file) {
        // check that the file is considered "under my vcs"
        if (event.isFromSave()) {
            try {
                final P4Server server = vcs.getP4ServerFor(file);
                if (server != null) {
                    LOG.info("edit request on " + file);

                    // Bug #6
                    //   Add/Edit without adding to Perforce incorrectly
                    //   then adds the file to Perforce
                    // This method is called when a save happens, which can be
                    // at any time.  If the save is called on a file which is
                    // marked as locally updated but not checked out, this will
                    // still be called.  This method should never *add* a file
                    // into Perforce - only open for edit.

                    server.onlyEditFile(file, changeListMapping.
                            getProjectDefaultPerforceChangelist(server).getChangeListId());
                }
            } catch (InterruptedException e) {
                alerts.addNotice(vcs.getProject(),
                        P4Bundle.message("interrupted_exception", file), e);
            }
        }
    }

    @Override
    protected String getAddTitle() {
        return P4Bundle.message("vfs.add.files");
    }

    @Override
    protected String getSingleFileAddTitle() {
        return P4Bundle.message("vfs.add.file");
    }

    @Override
    protected String getSingleFileAddPromptTemplate() {
        return P4Bundle.getString("vfs.add.single.prompt");
    }

    @Override
    protected String getDeleteTitle() {
        return P4Bundle.message("vfs.delete.files");
    }

    @Override
    protected String getSingleFileDeleteTitle() {
        return P4Bundle.message("vfs.delete.file");
    }

    @Override
    protected String getSingleFileDeletePromptTemplate() {
        return P4Bundle.getString("vfs.delete.single.prompt");
    }

    @Override
    protected boolean isDirectoryVersioningSupported() {
        return false;
    }


    @NotNull
    private <T> SplitServerFileMap splitMap(@NotNull Map<T ,T> map) throws InterruptedException {
        // slow, tedious method, but precise
        Map<P4Server, List<SplitServerFileEntry>> match = new HashMap<P4Server, List<SplitServerFileEntry>>();
        List<SplitServerFileEntry> cross = new ArrayList<SplitServerFileEntry>();
        for (Map.Entry<T, T> en : map.entrySet()) {
            P4Server src = getServerFor(en.getKey());
            P4Server tgt = getServerFor(en.getValue());
            SplitServerFileEntry entry = new SplitServerFileEntry(src, en.getKey(), tgt, en.getValue());
            if (entry.isCrossClient()) {
                cross.add(entry);
            } else {
                P4Server common = entry.commonClient();
                List<SplitServerFileEntry> list = match.get(common);
                if (list == null) {
                    list = new ArrayList<SplitServerFileEntry>();
                    match.put(common, list);
                }
                list.add(entry);
            }
        }
        return new SplitServerFileMap(match, cross);
    }

    @Nullable
    private P4Server getServerFor(@Nullable Object obj) throws InterruptedException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof FilePath) {
            return vcs.getP4ServerFor((FilePath) obj);
        }
        if (obj instanceof VirtualFile) {
            return vcs.getP4ServerFor((VirtualFile) obj);
        }
        throw new IllegalStateException("Expected FilePath or VirtualFile, found " + obj.toString());
    }


    private static class SplitServerFileEntry {
        public final P4Server srcClient;
        public final VirtualFile srcVirtualFile;
        public final FilePath srcFilePath;
        public final P4Server tgtClient;
        public final VirtualFile tgtVirtualFile;
        public final FilePath tgtFilePath;

        private SplitServerFileEntry(@Nullable P4Server srcClient, @Nullable Object srcFile,
                @Nullable P4Server tgtClient, @Nullable Object tgtFile) {
            this.srcClient = srcClient;
            this.srcVirtualFile = (srcFile == null) ? null :
                    (srcFile instanceof VirtualFile) ? (VirtualFile) srcFile : null;
            this.srcFilePath = (srcFile == null) ? null :
                    (srcFile instanceof FilePath) ? (FilePath) srcFile : null;
            this.tgtClient = tgtClient;
            this.tgtVirtualFile = (tgtFile == null) ? null :
                    (tgtFile instanceof VirtualFile) ? (VirtualFile) tgtFile : null;
            this.tgtFilePath = (tgtFile == null) ? null :
                    (tgtFile instanceof FilePath) ? (FilePath) tgtFile : null;
        }


        boolean isCrossClient() {
            return srcClient != null && tgtClient != null && ! srcClient.equals(tgtClient);
        }

        boolean isSameServerCrossClient() {
            if (srcClient != null && tgtClient != null) {
                if (srcClient.equals(tgtClient)) {
                    // same client
                    return false;
                }
                if (srcClient.getClientServerId().getServerConfigId().equals(tgtClient.getClientServerId().getServerConfigId())) {
                    // same server, different client
                    return true;
                }
            }
            return false;
        }


        P4Server commonClient() {
            if (srcClient != null) {
                return srcClient;
            }
            return tgtClient;
        }
    }


    private static class SplitServerFileMap {
        public final Map<P4Server, List<SplitServerFileEntry>> match;
        public final List<SplitServerFileEntry> crossClient;

        private SplitServerFileMap(@NotNull Map<P4Server, List<SplitServerFileEntry>> match,
                @NotNull List<SplitServerFileEntry> crossClient) {
            this.match = match;
            this.crossClient = crossClient;
        }

        @NotNull
        public List<P4Server.IntegrateFile> getFilePathMatch(@NotNull P4Server server) {
            List<P4Server.IntegrateFile> ret = new ArrayList<IntegrateFile>();
            List<SplitServerFileEntry> entries = match.get(server);
            if (entries != null) {
                for (SplitServerFileEntry entry : entries) {
                    ret.add(new P4Server.IntegrateFile(entry.srcFilePath, entry.tgtFilePath));
                }
            }
            return ret;
        }

        @NotNull
        public Set<P4Server> getServers() {
            Set<P4Server> ret = new HashSet<P4Server>(match.keySet());
            for (SplitServerFileEntry entry: crossClient) {
                if (entry.srcClient != null) {
                    ret.add(entry.srcClient);
                }
                if (entry.tgtClient != null) {
                    ret.add(entry.tgtClient);
                }
            }
            return ret;
        }

        @NotNull
        public List<FilePath> getCrossSourceFilePathsFor(@NotNull P4Server server) {
            // TODO same-server, different client is not always correctly picked up here.
            // A cross-client move shows the destination
            // as correctly integrated, and deletes the source locally,
            // but p4 is not told of the source deletion.  Sometimes it works?
            // Weird.


            List<FilePath> ret = new ArrayList<FilePath>(crossClient.size());
            for (SplitServerFileEntry entry : crossClient) {
                if (server.equals(entry.srcClient)) {
                    ret.add(entry.srcFilePath);
                }
            }
            return ret;
        }

        @NotNull
        List<VirtualFile> getCrossTargetDifferentServerVirtualFilesFor(@NotNull final P4Server server) {
            List<VirtualFile> ret = new ArrayList<VirtualFile>(crossClient.size());
            for (SplitServerFileEntry entry: crossClient) {
                if (! entry.isSameServerCrossClient() && server.equals(entry.tgtClient)) {
                    ret.add(entry.tgtVirtualFile);
                }
            }
            return ret;
        }

        @NotNull
        List<P4Server.IntegrateFile> getSameServerCrossVirtualFilesForTarget(@NotNull final P4Server server) {
            List<P4Server.IntegrateFile> ret = new ArrayList<IntegrateFile>();
            for (SplitServerFileEntry entry: crossClient) {
                if (entry.isSameServerCrossClient() && server.equals(entry.tgtClient) && entry.srcClient != null) {
                    ret.add(new P4Server.IntegrateFile(
                            entry.srcClient.getClientServerId(), entry.srcFilePath, entry.tgtFilePath));
                }
            }
            return ret;
        }
    }
}
