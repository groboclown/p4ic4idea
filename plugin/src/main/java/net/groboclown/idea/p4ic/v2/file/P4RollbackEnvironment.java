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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.FileSyncResult;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.connection.MessageResult;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

public class P4RollbackEnvironment implements RollbackEnvironment {
    private static final Logger LOG = Logger.getInstance(P4RollbackEnvironment.class);

    private final P4Vcs vcs;

    /**
     * See the {@link P4VFSListener} and {@link P4EditFileProvider} for the other places this
     * object is shared.
     *
     * It may not be necessary to include this sync here, but it
     * makes logical sense to do so.
     */
    private final Lock vfsLock;

    P4RollbackEnvironment(@NotNull P4Vcs vcs, @NotNull Lock vfsLock) {
        this.vcs = vcs;
        this.vfsLock = vfsLock;
    }

    @Override
    public String getRollbackOperationName() {
        return P4Bundle.message("rollback.action.name");
    }

    @Override
    public void rollbackChanges(List<Change> changes, List<VcsException> vcsExceptions, @NotNull RollbackProgressListener listener) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        Set<FilePath> paths = new HashSet<FilePath>();
        for (Change change: changes) {
            if (change != null) {
                if (change.getAfterRevision() != null) {
                    paths.add(change.getAfterRevision().getFile());
                }
                if (change.getBeforeRevision() != null) {
                    paths.add(change.getBeforeRevision().getFile());
                }
            }
        }
        if (paths.isEmpty()) {
            return;
        }

        boolean hasRefreshedFiles = false;
        final Map<P4Server, List<FilePath>> mapping;
        try {
            mapping = vcs.mapFilePathsToP4Server(paths);
        } catch (InterruptedException e) {
            LOG.warn(e);
            return;
        }
        vfsLock.lock();
        try {
            for (Entry<P4Server, List<FilePath>> entry : mapping.entrySet()) {
                final P4Server server = entry.getKey();
                final List<FilePath> files = entry.getValue();
                if (server != null && ! files.isEmpty()) {
                    hasRefreshedFiles = true;
                    LOG.info("Reverting in client " + server + ": " + files);

                    server.revertFiles(files, vcsExceptions);
               }
            }
        } finally {
            vfsLock.unlock();
        }

        if (hasRefreshedFiles) {
            // tell the LocalFileSystem to refresh files

            HashSet<File> filesToRefresh = new HashSet<File>();
            for (Change c : changes) {
                ContentRevision before = c.getBeforeRevision();
                if (before != null) {
                    // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
                    // use this instead: getIOFile().getAbsolutePath()
                    filesToRefresh.add(new File(before.getFile().getIOFile().getAbsolutePath()));
                }
                ContentRevision after = c.getAfterRevision();
                if (after != null) {
                    // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
                    // use this instead: getIOFile().getAbsolutePath()
                    filesToRefresh.add(new File(after.getFile().getIOFile().getAbsolutePath()));
                }
            }

            LocalFileSystem lfs = LocalFileSystem.getInstance();
            lfs.refreshIoFiles(filesToRefresh);
        }

        // A refresh of the changes is sometimes needed.
        P4ChangesViewRefresher.refreshLater(vcs.getProject());
    }

    @Override
    public void rollbackMissingFileDeletion(List<FilePath> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        forceSync(files, exceptions, listener);
    }

    @Override
    public void rollbackModifiedWithoutCheckout(List<VirtualFile> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        List<FilePath> paths = new ArrayList<FilePath>(files.size());
        for (VirtualFile vf: files) {
            paths.add(VcsUtil.getFilePath(vf));
        }
        forceSync(paths, exceptions, listener);
    }

    private void forceSync(List<FilePath> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        if (vcs.getProject().isDisposed()) {
            return;
        }

        final Map<P4Server, List<FilePath>> mapping;
        try {
            mapping = vcs.mapFilePathsToP4Server(files);
        } catch (InterruptedException e) {
            LOG.warn(e);
            exceptions.add(new VcsInterruptedException(e));
            return;
        }
        for (Entry<P4Server, List<FilePath>> entry : mapping.entrySet()) {
            listener.checkCanceled();
            listener.accept(entry.getValue());
            final MessageResult<Collection<FileSyncResult>> results;
            try {
                results = entry.getKey().synchronizeFilesOnline(entry.getValue(), -1, null, true);
                exceptions.addAll(results.messagesAsExceptions());
            } catch (P4DisconnectedException e) {
                LOG.warn(e);
                exceptions.add(e);
            } catch (InterruptedException e) {
                LOG.warn(e);
                exceptions.add(new VcsInterruptedException(e));
            }
        }
    }


    @Override
    public void rollbackIfUnchanged(VirtualFile file) {
        if (file == null || vcs.getProject().isDisposed()) {
            return;
        }

        FilePath fp = FilePathUtil.getFilePath(file);
        final P4Server server;
        try {
            server = vcs.getP4ServerFor(fp);
        } catch (InterruptedException e) {
            LOG.warn(e);
            return;
        }
        if (server == null) {
            LOG.debug("No client for file " + file);
            return;
        }
        boolean reverted = true;
        vfsLock.lock();
        try {
            try {
                // The changelist doesn't matter, so pass in a
                // negative number which will mean it doesn't use
                // the "-c" argument.
                server.revertUnchangedFilesOnline(Collections.singletonList(fp),
                        P4ChangeListId.P4_UNKNOWN);
            } catch (InterruptedException e) {
                LOG.warn(e);
                reverted = false;
            } catch (P4DisconnectedException e) {
                LOG.warn(e);
                reverted = false;
            }
        } finally {
            vfsLock.unlock();
        }

        if (reverted) {
            P4ChangesViewRefresher.refreshLater(vcs.getProject());
        }
    }
}
