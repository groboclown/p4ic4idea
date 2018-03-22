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
package net.groboclown.idea.p4ic.v2.history;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffMixin;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class P4DiffProvider implements DiffProvider, DiffMixin {
    private static final Logger LOG = Logger.getInstance(P4DiffProvider.class);

    private final Project project;

    public P4DiffProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public VcsRevisionNumber getCurrentRevision(VirtualFile file) {
        FilePath fp = FilePathUtil.getFilePath(file);
        final IExtendedFileSpec spec = getFileInfo(getServerFor(fp), fp);
        if (spec == null) {
            return null;
        }

        // NOTE: have vs. head!
        if (spec.getHaveRev() >= 0) {
            return new P4RevisionNumber(fp, spec.getDepotPathString(), spec, P4RevisionNumber.RevType.HAVE);
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
        final IExtendedFileSpec spec = getFileInfo(getServerFor(filePath), filePath);
        if (spec == null) {
            return null;
        }

        return new ItemLatestState(
                new P4RevisionNumber(filePath, spec.getDepotPathString(), spec, P4RevisionNumber.RevType.HEAD),
                spec.getHeadRev() != 0 && spec.getHeadAction() != null,
                false);
    }

    @Nullable
    @Override
    public ContentRevision createFileContent(final VcsRevisionNumber revisionNumber, VirtualFile selectedFile) {
        final FilePath file = VcsUtil.getFilePath(selectedFile);
        if (! (revisionNumber instanceof P4RevisionNumber)) {
            throw new IllegalArgumentException(P4Bundle.message("error.diff.bad-revision", revisionNumber));
        }
        return new P4ContentRevision(project, file, (P4RevisionNumber) revisionNumber);
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
        FilePath fp = FilePathUtil.getFilePath(file);
        P4Server server = getServerFor(fp);
        final IExtendedFileSpec spec = getFileInfo(server, fp);
        if (spec == null) {
            return null;
        }
        final String requestedPath = spec.getDepotPathString();
        if (spec.getHaveRev() <= 0) {
            return new P4RevisionDescription(fp, requestedPath, null);
        }

        // TODO this is really bad in terms of performance.
        List<P4FileRevision> history;
        try {
            history = server.getRevisionHistoryOnline(spec, 1);
        } catch (InterruptedException e) {
            LOG.info(e);
            return null;
        }
        // just choose the top one
        if (history == null || history.isEmpty()) {
            return new P4RevisionDescription(fp, requestedPath, null);
        } else {
            return new P4RevisionDescription(fp, requestedPath, history.get(0).getRevisionData());
        }
    }


    @Nullable
    private P4Server getServerFor(@Nullable FilePath file) {
        if (file == null || project.isDisposed()) {
            return null;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);

        try {
            return vcs.getP4ServerFor(file);
        } catch (InterruptedException e) {
            // just swallow the exception
            LOG.info(e);
            return null;
        }
    }


    @Nullable
    private IExtendedFileSpec getFileInfo(@Nullable P4Server server, @Nullable FilePath file) {
        if (file == null || server == null) {
            return null;
        }
        try {
            final Map<FilePath, IExtendedFileSpec> specs = server.getFileStatus(Collections.singletonList(file));
            if (specs == null) {
                return null;
            }
            return specs.get(file);
        } catch (InterruptedException e) {
            // just swallow the exception
            LOG.info(e);
            return null;
        }
    }



    private static class P4RevisionDescription implements VcsRevisionDescription {
        private final FilePath baseFile;
        private final String requestedPath;
        private final IFileRevisionData rev;

        private P4RevisionDescription(@NotNull FilePath baseFile, @Nullable final String requestedPath, @Nullable IFileRevisionData rev) {
            this.baseFile = baseFile;
            this.requestedPath = requestedPath;
            this.rev = rev;
        }

        @Override
        public VcsRevisionNumber getRevisionNumber() {
            if (rev == null) {
                // TODO how to eliminate this special case?
                return new P4RevisionNumber(baseFile, requestedPath, null, 0);
            }
            return new P4RevisionNumber(baseFile, requestedPath, rev);
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
