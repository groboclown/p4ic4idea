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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsVFSListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.vcsUtil.VcsFileUtil;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.Background;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4VFSListener extends VcsVFSListener {
    private static final Logger LOG = Logger.getInstance(VcsVFSListener.class);

    public String DELETE = P4Bundle.message("vfs.delete.process");
    public String MOVE = P4Bundle.message("vfs.move.process");
    public String ADD = P4Bundle.message("vfs.add.process");

    private final P4Vcs vcs;
    private final Project project;

    /**
     * Synchronizes on VFS operations; IDEA can send requests to open for edit and move,
     * or open for edit and delete at nearly the same time.  If these are allowed to
     * collide, then the incorrect actions can happen.  By making all these basic
     * actions be synchronized, we guarantee the right operation, but at a slight
     * cost to time.
     *
     * This should be shared with {@link P4EditFileProvider}
     */
    private final Object vfsSync;

    public P4VFSListener(@NotNull Project project, @NotNull P4Vcs vcs, @NotNull Object vfsSync) {
        super(project, vcs);
        this.vcs = vcs;
        this.project = project;
        this.vfsSync = vfsSync;
    }


    @Override
    protected void performDeletion(@NotNull final List<FilePath> filesToDelete) {
        VcsFileUtil.markFilesDirty(myProject, filesToDelete);

        Background.runInBackground(project, DELETE, vcs.getConfiguration().getAddRemoveOption(), new Background.ER() {
            @Override
            public void run(@NotNull ProgressIndicator indicator) throws Exception {
                Map<Client, List<FilePath>> clientMap = vcs.mapFilePathToClient(filesToDelete);
                List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                synchronized (vfsSync) {
                    for (Map.Entry<Client, List<FilePath>> en : clientMap.entrySet()) {
                        Client client = en.getKey();
                        if (client.isWorkingOnline()) {
                            messages.addAll(client.getServer().deleteFiles(en.getValue(),
                                    vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId()));
                        }
                    }
                }
                P4StatusMessage.throwIfError(messages, true);
            }
        });
    }

    @Override
    protected void performMoveRename(@NotNull final List<MovedFileInfo> movedFiles) {
        final Map<FilePath, FilePath> moveMap = new HashMap<FilePath, FilePath>();
        for (MovedFileInfo info : movedFiles) {
            moveMap.put(
                    VcsUtil.getFilePath(info.myOldPath),
                    VcsUtil.getFilePath(info.myNewPath)
            );
        }
        VcsFileUtil.markFilesDirty(myProject, new ArrayList<FilePath>(moveMap.keySet()));
        VcsFileUtil.markFilesDirty(myProject, new ArrayList<FilePath>(moveMap.values()));

        Background.runInBackground(project, MOVE, vcs.getConfiguration().getAddRemoveOption(), new Background.ER() {
            @Override
            public void run(@NotNull ProgressIndicator indicator) throws Exception {
                List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                SplitClientFileMap split = splitMap(moveMap);
                synchronized (vfsSync) {
                    for (Client client: split.getClients()) {
                        if (client.isWorkingOnline()) {
                            int changeListId = vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId();
                            messages.addAll(client.getServer().moveFiles(
                                    split.getFilePathMatch(client), changeListId));

                            // Cross client files.
                            // This situation can happen if the file is moved either from or to outside P4 control,
                            // or if the file moves from one P4 client to another P4 client.
                            // IDE will handle underlying I/O copy and delete operation.

                            messages.addAll(client.getServer().deleteFiles(
                                    split.getCrossSourceFilePathsFor(client), changeListId));

                            messages.addAll(client.getServer().addOrCopyFiles(
                                    split.getCrossTargetFilePathsAsVirtualFilesFor(client),
                                    Collections.<VirtualFile, VirtualFile>emptyMap(), changeListId));

                            // We don't need to tell the user about this as an error message; it should be the
                            // normal, expected operation.
                        }
                    }
                }
                P4StatusMessage.throwIfError(messages, true);

                // There is a bug where, after running the move on a single file, the other files
                // in the same directory (and sometimes the directory itself) are marked in the
                // "Modified but not checked out" changelist.  This refresh is supposed to update
                // that status, but it appears that the call to mark those files as dirty happens after
                // this invocation.

                P4ChangesViewRefresher.refreshLater(vcs.getProject());
            }
        });
    }

    @Override
    protected void performAdding(
            @NotNull final Collection<VirtualFile> addedFiles,
            @NotNull final Map<VirtualFile, VirtualFile> copyFromMap) {
        // IDEA 140.2110.5 changed the markFilesDirty API to just take FilePath objects.
        List<FilePath> dirtyFilePaths = new ArrayList<FilePath>(addedFiles.size());
        for (VirtualFile vf: addedFiles) {
            dirtyFilePaths.add(VcsUtil.getFilePath(vf));
        }
        for (VirtualFile vf: copyFromMap.keySet()) {
            dirtyFilePaths.add(VcsUtil.getFilePath(vf));
        }
        VcsFileUtil.markFilesDirty(myProject, dirtyFilePaths);
        Background.runInBackground(project, ADD, vcs.getConfiguration().getAddRemoveOption(), new Background.ER() {
            @Override
            public void run(@NotNull ProgressIndicator indicator) throws Exception {
                Map<Client, List<VirtualFile>> clientAddedMap = vcs.mapVirtualFilesToClient(addedFiles);
                if (clientAddedMap.isEmpty()) {
                    return;
                }
                SplitClientFileMap splitClient = splitMap(copyFromMap);
                Set<Client> clients = new HashSet<Client>(clientAddedMap.keySet());
                clients.addAll(splitClient.getClients());
                List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                synchronized (vfsSync) {
                    for (Client client: clients) {
                        if (client.isWorkingOnline()) {
                            // For the copy operation, there can not be
                            // any way to integrate a file between clients, so we
                            // change a cross-client copy to be just an add.
                            // This can also happen if a file is copied from outside
                            // Perforce control.

                            List<VirtualFile> added = new ArrayList<VirtualFile>(splitClient.getCrossTargetVirtualFilesFor(client));
                            List<VirtualFile> explicitAdd = clientAddedMap.get(client);
                            if (explicitAdd != null) {
                                added.addAll(explicitAdd);
                            }
                            Map<VirtualFile, VirtualFile> copied = splitClient.getVirtualFileMatch(client);
                            messages.addAll(client.getServer().addOrCopyFiles(added, copied,
                                    vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId()));
                        }
                    }
                }
                P4StatusMessage.throwIfError(messages, true);
            }
        });
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
        if (event.isFromSave() && vcs.fileIsUnderVcs(VcsUtil.getFilePath(file))) {
            LOG.info("edit request on " + file);

            Background.runInBackground(project, P4EditFileProvider.EDIT, vcs.getConfiguration().getEditOption(), new Background.ER() {
                @Override
                public void run(@NotNull ProgressIndicator indicator) throws Exception {
                    List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
                    Client client = vcs.getClientFor(file);
                    if (client != null) {
                        synchronized (vfsSync) {
                            messages.addAll(client.getServer().editFiles(
                                    Collections.singletonList(file),
                                    vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId()));
                        }
                    }
                    P4StatusMessage.throwIfError(messages, true);
                }
            });
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
    private <T> SplitClientFileMap splitMap(@NotNull Map<T ,T> map) {
        // slow, tedious method, but precise
        Map<Client, List<SplitClientFileEntry>> match = new HashMap<Client, List<SplitClientFileEntry>>();
        List<SplitClientFileEntry> cross = new ArrayList<SplitClientFileEntry>();
        for (Map.Entry<T, T> en : map.entrySet()) {
            Client src = getClientFor(en.getKey());
            Client tgt = getClientFor(en.getValue());
            SplitClientFileEntry entry = new SplitClientFileEntry(src, en.getKey(), tgt, en.getValue());
            if (entry.isCrossClient()) {
                cross.add(entry);
            } else {
                Client common = entry.commonClient();
                List<SplitClientFileEntry> list = match.get(common);
                if (list == null) {
                    list = new ArrayList<SplitClientFileEntry>();
                    match.put(common, list);
                }
                list.add(entry);
            }
        }
        return new SplitClientFileMap(match, cross);
    }

    @Nullable
    private Client getClientFor(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof FilePath) {
            return vcs.getClientFor((FilePath) obj);
        }
        if (obj instanceof VirtualFile) {
            return vcs.getClientFor((VirtualFile) obj);
        }
        throw new IllegalStateException(obj.toString());
    }


    private static class SplitClientFileEntry {
        public final Client srcClient;
        public final VirtualFile srcVirtualFile;
        public final FilePath srcFilePath;
        public final Client tgtClient;
        public final VirtualFile tgtVirtualFile;
        public final FilePath tgtFilePath;

        private SplitClientFileEntry(@Nullable Client srcClient, @Nullable Object srcFile,
                @Nullable Client tgtClient, @Nullable Object tgtFile) {
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


        public boolean isCrossClient() {
            return srcClient != null && tgtClient != null && srcClient != tgtClient;
        }


        public Client commonClient() {
            if (srcClient != null) {
                return srcClient;
            }
            return tgtClient;
        }
    }


    private static class SplitClientFileMap {
        public final Map<Client, List<SplitClientFileEntry>> match;
        public final List<SplitClientFileEntry> crossClient;

        private SplitClientFileMap(@NotNull Map<Client, List<SplitClientFileEntry>> match, @NotNull List<SplitClientFileEntry> crossClient) {
            this.match = match;
            this.crossClient = crossClient;
        }

        @NotNull
        public Map<FilePath, FilePath> getFilePathMatch(@NotNull Client client) {
            Map<FilePath, FilePath> ret = new HashMap<FilePath, FilePath>();
            List<SplitClientFileEntry> entries = match.get(client);
            if (entries != null) {
                for (SplitClientFileEntry entry : entries) {
                    ret.put(entry.srcFilePath, entry.tgtFilePath);
                }
            }
            return ret;
        }


        @NotNull
        public Map<VirtualFile, VirtualFile> getVirtualFileMatch(@NotNull Client client) {
            Map<VirtualFile, VirtualFile> ret = new HashMap<VirtualFile, VirtualFile>();
            List<SplitClientFileEntry> entries = match.get(client);
            if (entries != null) {
                for (SplitClientFileEntry entry : entries) {
                    ret.put(entry.srcVirtualFile, entry.tgtVirtualFile);
                }
            }
            return ret;
        }

        @NotNull
        public Set<Client> getClients() {
            Set<Client> ret = new HashSet<Client>(match.keySet());
            for (SplitClientFileEntry entry: crossClient) {
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
        public List<FilePath> getCrossSourceFilePathsFor(@NotNull Client client) {
            List<FilePath> ret = new ArrayList<FilePath>(crossClient.size());
            for (SplitClientFileEntry entry : crossClient) {
                if (client.equals(entry.srcClient)) {
                    ret.add(entry.srcFilePath);
                }
            }
            return ret;
        }

        @NotNull
        public List<VirtualFile> getCrossTargetFilePathsAsVirtualFilesFor(@NotNull Client client) {
            List<VirtualFile> ret = new ArrayList<VirtualFile>(crossClient.size());
            for (SplitClientFileEntry entry : crossClient) {
                if (client.equals(entry.tgtClient)) {
                    ret.add(entry.tgtFilePath.getVirtualFile());
                }
            }
            return ret;
        }

        @NotNull
        public List<VirtualFile> getCrossTargetVirtualFilesFor(@NotNull Client client) {
            List<VirtualFile> ret = new ArrayList<VirtualFile>(crossClient.size());
            for (SplitClientFileEntry entry : crossClient) {
                if (client.equals(entry.tgtClient)) {
                    ret.add(entry.tgtVirtualFile);
                }
            }
            return ret;
        }
    }
}