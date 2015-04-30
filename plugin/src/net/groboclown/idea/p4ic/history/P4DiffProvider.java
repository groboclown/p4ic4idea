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
package net.groboclown.idea.p4ic.history;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffMixin;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.ui.ErrorDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class P4DiffProvider implements DiffProvider, DiffMixin {
    private static final Logger LOG = Logger.getInstance(P4DiffProvider.class);

    private final Project project;

    public P4DiffProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public VcsRevisionNumber getCurrentRevision(VirtualFile file) {
        P4FileInfo fileInfo = getFileInfo(getClientFor(file), file);
        if (fileInfo == null) {
            return null;
        }

        // NOTE: have vs. head!
        if (fileInfo.getHaveRev() >= 0) {
            return new VcsRevisionNumber.Int(fileInfo.getHaveRev());
        }
        return null;
    }

    @Nullable
    @Override
    public ItemLatestState getLastRevision(VirtualFile virtualFile) {
        return getLastRevision(VcsUtil.getFilePath(virtualFile));
    }

    /**
     * Get the current version of the file as it exists in the depot
     *
     * @param filePath file to fetch a revision
     * @return state of the file
     */
    @Nullable
    @Override
    public ItemLatestState getLastRevision(FilePath filePath) {
        VirtualFile vf = filePath.getVirtualFile();
        P4FileInfo fileInfo = getFileInfo(getClientFor(vf), vf);
        if (fileInfo == null) {
            return null;
        }

        return new ItemLatestState(new VcsRevisionNumber.Int(fileInfo.getHeadRev()),
                !fileInfo.isDeletedInDepot() && fileInfo.isInDepot(), false);
    }

    @Nullable
    @Override
    public ContentRevision createFileContent(final VcsRevisionNumber revisionNumber, VirtualFile selectedFile) {
        final FilePath file = VcsUtil.getFilePath(selectedFile);
        if (! (revisionNumber instanceof VcsRevisionNumber.Int)) {
            throw new IllegalArgumentException(P4Bundle.message("error.diff.bad-revision", revisionNumber));
        }
        return new P4ContentRevision(file, (VcsRevisionNumber.Int) revisionNumber);
    }

    @Nullable
    @Override
    public VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot) {
        // Doesn't really mean anything in Perforce
        return null;
    }

    @Nullable
    @Override
    public VcsRevisionDescription getCurrentRevisionDescription(VirtualFile file) {
        Client client = getClientFor(file);
        P4FileInfo fileInfo = getFileInfo(client, file);
        if (fileInfo == null) {
            return null;
        }
        if (fileInfo.getHaveRev() <= 0) {
            return new P4RevisionDescription(null);
        }

        // TODO this is really bad in terms of performance.
        List<P4FileRevision> history;
        try {
            history = client.getServer().getRevisionHistory(fileInfo, 1);
        } catch (VcsException e) {
            ErrorDialog.logError(project, P4Bundle.message("error.diff.history.title", file), e);
            return new P4RevisionDescription(null);
        }

        /*
        if (! history.containsKey(fileInfo)) {
            LOG.info("no revision history for " + fileInfo);
            return new P4RevisionDescription(null);
        }
        */

        // just choose the top one
        if (history.isEmpty()) {
            return new P4RevisionDescription(null);
        } else {
            return new P4RevisionDescription(history.get(0).getRevisionData());
        }
    }


    @Nullable
    private Client getClientFor(@Nullable VirtualFile file) {
        if (file == null || project.isDisposed()) {
            return null;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);

        Client client = vcs.getClientFor(file);
        if (client == null || client.isWorkingOffline()) {
            return null;
        }
        return client;
    }


    @Nullable
    private P4FileInfo getFileInfo(@Nullable Client client, @Nullable VirtualFile file) {
        if (file == null || client == null) {
            return null;
        }
        List<P4FileInfo> files;
        try {
            files = client.getServer().getVirtualFileInfo(Collections.singletonList(file));
        } catch (VcsException e) {
            ErrorDialog.logError(project, P4Bundle.message("error.diff.current-revision.title", file), e);
            return null;
        }
        if (files.isEmpty()) {
            LOG.info("No files for " + file);
            return null;
        }
        if (files.size() > 1) {
            LOG.info("More than 1 file for " + file + ": " + files);
        }
        return files.get(0);
    }


    // TODO this may not be the smartest tactic - caching the previous results.
    // However, the cache is in a soft reference, which should help with memory.

    private class P4ContentRevision implements ContentRevision {
        private final FilePath file;
        private final VcsRevisionNumber.Int revisionNumber;
        private Reference<String> previous = null;

        public P4ContentRevision(FilePath file, VcsRevisionNumber.Int revisionNumber) {
            this.file = file;
            this.revisionNumber = revisionNumber;
        }

        @Nullable
        @Override
        public String getContent() throws VcsException {
            // we can work disconnected if it was cached...
            if (previous != null) {
                String ret = previous.get();
                if (ret != null) {
                    return ret;
                }
                previous = null;
            }

            if (project.isDisposed()) {
                throw new P4Exception(P4Bundle.message("exception.disposed"));
            }
            Client client = P4Vcs.getInstance(project).getClientFor(file);
            if (client == null) {
                throw new P4InvalidClientException(P4Bundle.message("error.filespec.no-client", file));
            }
            if (client.isWorkingOffline()) {
                throw new P4DisconnectedException(P4Bundle.message("error.config.disconnected"));
            }
            String ret = client.getServer().loadFileAsString(file, revisionNumber.getValue());
            if (ret == null) {
                // cache the null value as an empty string, so we
                // don't need to go through this again.
                ret = "";
            }
            previous = new SoftReference<String>(ret);
            return ret;
        }

        @NotNull
        @Override
        public FilePath getFile() {
            return file;
        }

        @NotNull
        @Override
        public VcsRevisionNumber getRevisionNumber() {
            return revisionNumber;
        }
    }


    private static class P4RevisionDescription implements VcsRevisionDescription {
        private final IFileRevisionData rev;

        private P4RevisionDescription(@Nullable IFileRevisionData rev) {
            this.rev = rev;
        }

        @Override
        public VcsRevisionNumber getRevisionNumber() {
            return new VcsRevisionNumber.Int(rev == null ? 0 : rev.getRevision());
        }

        @Override
        public Date getRevisionDate() {
            return rev == null ? null : rev.getDate();
        }

        @Nullable
        @Override
        public String getAuthor() {
            return rev == null ? "" : rev.getUserName();
        }

        @Nullable
        @Override
        public String getCommitMessage() {
            return rev == null ? "" : rev.getDescription();
        }
    }
}
