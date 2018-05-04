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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class P4DiffProvider
        implements DiffProvider, DiffMixin {
    private static final Logger LOG = Logger.getInstance(P4DiffProvider.class);

    private final Project project;

    public P4DiffProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public VcsRevisionNumber getCurrentRevision(VirtualFile file) {
        // FIXME
        throw new IllegalStateException("not implemented");
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
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public ContentRevision createFileContent(final VcsRevisionNumber revisionNumber, VirtualFile selectedFile) {
        // FIXME
        throw new IllegalStateException("not implemented");
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
        // FIXME
        throw new IllegalStateException("not implemented");
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

        @Nonnull
        @Override
        public VcsRevisionNumber getRevisionNumber() {
            // FIXME
            throw new IllegalStateException("not implemented");
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
