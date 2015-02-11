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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;

public class P4FileRevision implements VcsFileRevision {
    private final Project project;
    private final String streamName;
    private final P4RepositoryLocation changedRepositoryPath;
    private final VcsRevisionNumber.Int revision;
    private final String comment;
    private final String author;
    private final Date date;
    private final IFileRevisionData revisionData;

    public P4FileRevision(Project project, P4FileInfo file, IFileRevisionData data) {
        this.project = project;
        this.streamName = null;
        this.changedRepositoryPath =
                file == null ? null : new P4RepositoryLocation(file);
        this.revision = new VcsRevisionNumber.Int(data == null ? 0 : data.getRevision());
        this.comment = data == null ? null : data.getDescription();
        this.author = data == null ? null : data.getUserName();
        this.date = data == null ? null : data.getDate();
        this.revisionData = data;
    }


    @Nullable
    @Override
    public String getBranchName() {
        return streamName;
    }

    @Nullable
    @Override
    public RepositoryLocation getChangedRepositoryPath() {
        return changedRepositoryPath;
    }

    @Override
    public byte[] loadContent() throws IOException, VcsException {
        if (changedRepositoryPath == null) {
            return null;
        }
        FilePath file = changedRepositoryPath.getP4FileInfo().getPath();
        Client client = P4Vcs.getInstance(project).getClientFor(file);
        if (client != null) {
            return client.getServer().loadFileAsBytes(file, revision.getValue());
        }
        return null;
    }

    @Nullable
    @Override
    public byte[] getContent() throws IOException, VcsException {
        return loadContent();
    }

    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return revision;
    }

    @Override
    public Date getRevisionDate() {
        return date;
    }

    @Nullable
    @Override
    public String getAuthor() {
        return author;
    }

    @Nullable
    @Override
    public String getCommitMessage() {
        return comment;
    }

    @Nullable
    public IFileRevisionData getRevisionData() {
        return revisionData;
    }
}
