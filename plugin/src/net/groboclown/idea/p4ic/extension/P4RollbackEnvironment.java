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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class P4RollbackEnvironment implements RollbackEnvironment {
    private static final Logger LOG = Logger.getInstance(P4RollbackEnvironment.class);

    private final P4Vcs vcs;

    /**
     * See the P4VFSListener and P4EditFileProvider for the other places this
     * object is shared.
     *
     * It may not be necessary to include this sync here, but it
     * makes logical sense to do so.
     */
    private final Object vfsSync;

    public P4RollbackEnvironment(@NotNull P4Vcs vcs, @NotNull Object vfsSync) {
        this.vcs = vcs;
        this.vfsSync = vfsSync;
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
        try {
            List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
            synchronized (vfsSync) {
                Map<Client, List<FilePath>> mapping = vcs.mapFilePathToClient(paths);
                for (Map.Entry<Client, List<FilePath>> en: mapping.entrySet()) {
                    Client client = en.getKey();
                    List<FilePath> files = en.getValue();
                    if (client.isWorkingOnline() && ! files.isEmpty()) {
                        hasRefreshedFiles = true;
                        messages.addAll(client.getServer().revertFiles(files));
                    }
                }
            }
            for (P4StatusMessage message : messages) {
                if (message.isError()) {
                    vcsExceptions.add(new VcsException(message.getMessage()));
                }
            }
        } catch (VcsException e) {
            vcsExceptions.add(e);
        }

        if (hasRefreshedFiles) {
            // tell the LocalFileSystem to refresh files
            LocalFileSystem lfs = LocalFileSystem.getInstance();
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
            lfs.refreshIoFiles(filesToRefresh);
        }

        // A refresh of the changes is sometimes needed.
        P4ChangesViewRefresher.refreshLater(vcs.getProject());
    }

    @Override
    public void rollbackMissingFileDeletion(List<FilePath> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        // FIXME is this right?
        throw new UnsupportedOperationException("Missing file delete is not reported by Perforce.");
    }

    @Override
    public void rollbackModifiedWithoutCheckout(List<VirtualFile> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        // No-op operation?
        // FIXME check if this should inspect the depot to see if it is
        // under the dpeot, and should just be force synched.
        LOG.info("rollbackModifiedWithoutCheckout: not implemented " + files);
    }

    @Override
    public void rollbackIfUnchanged(VirtualFile file) {
        // FIXME remove from change list
        LOG.info("rollbackIfUnchanged: not implemented " + file);
    }
}
