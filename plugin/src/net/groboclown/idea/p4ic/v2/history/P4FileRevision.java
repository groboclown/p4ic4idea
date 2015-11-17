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
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A Perforce file revision.  It allows for looking at files that are outside
 * a client workspace (say, a different branch), which is useful when looking
 * at the revision history across branches.
 */
public class P4FileRevision implements VcsFileRevision {
    private static final Logger LOG = Logger.getInstance(P4FileRevision.class);
    private final Project project;

    private final String revisionDepotPath;
    private final String streamName;
    private final RepositoryLocation changedRepositoryPath;
    private final P4RevisionNumber revision;
    private final String comment;
    private final String author;
    private final Date date;
    private final IFileRevisionData revisionData;
    private final ClientServerId clientServerId;
    private final FilePath baseFile;


    public P4FileRevision(@NotNull Project project, @NotNull ClientServerId clientServerId,
            @NotNull FilePath baseFile,
            @Nullable String rootDepotPath, @NotNull P4AnnotatedLine line) {
        this(project, clientServerId, baseFile, rootDepotPath, line.getDepotPath(), line.getRevisionData());
    }

    public P4FileRevision(@NotNull Project project, @NotNull ClientServerId clientServerId,
            @NotNull FilePath baseFile,
            @Nullable String rootDepotPath, @Nullable String versionDepotPath, @Nullable IFileRevisionData data) {
        this.project = project;
        this.clientServerId = clientServerId;
        this.streamName = null;
        this.baseFile = baseFile;
        this.revisionDepotPath = data != null
                ? data.getDepotFileName()
                : versionDepotPath == null ? rootDepotPath : versionDepotPath;
        this.changedRepositoryPath = (versionDepotPath == null || versionDepotPath.equals(revisionDepotPath))
                ? null
                : new P4SimpleRepositoryLocation(revisionDepotPath);
        if (data == null) {
            LOG.info("null revision data for " + revisionDepotPath);
            this.revision = new P4RevisionNumber(baseFile, rootDepotPath, revisionDepotPath, 0);
        } else {
            this.revision = new P4RevisionNumber(baseFile, rootDepotPath, data);
        }

        this.comment = createComment(data);
        this.author = data == null ? null : data.getUserName();
        this.date = data == null ? null : data.getDate();
        this.revisionData = data;
    }


    @Nullable
    public String getRevisionDepotPath() {
        return revisionDepotPath;
    }


    @Nullable
    @Override
    public String getBranchName() {
        return streamName;
    }

    @NotNull
    @Override
    public RepositoryLocation getChangedRepositoryPath() {
        return changedRepositoryPath;
    }

    @Override
    public byte[] loadContent() throws IOException, VcsException {
        P4Server server = getServer();
        if (server == null) {
            return null;
        }
        try {
            return revision.loadContentAsBytes(server, null);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }


    @Nullable
    private P4Server getServer() {
        final List<P4Server> servers = P4Vcs.getInstance(project).getP4Servers();
        for (P4Server server: servers) {
            if (clientServerId.equals(server.getClientServerId())) {
                return server;
            }
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

    public int getRev() {
        return revision.getRev();
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

    @NotNull
    @Override
    public String toString() {
        return "#" + getRev();
    }

    @Nullable
    private String createComment(@Nullable final IFileRevisionData data) {
        if (data == null) {
            return null;
        }
        StringBuilder comment = new StringBuilder();
        if (data.getDescription() != null) {
            comment.append(P4Bundle.message("file-revision.comment", data.getDescription().trim()));
        }
        comment.append(P4Bundle.message("file-revision.location",
                data.getDepotFileName(),
                data.getChangelistId()));
        final List<IRevisionIntegrationData> integrations =
                data.getRevisionIntegrationData();
        if (integrations != null && ! integrations.isEmpty()) {
            comment.append(P4Bundle.message("file-revision.integrations.header"));
            for (IRevisionIntegrationData integration : integrations) {
                comment.append(P4Bundle.message("file-revision.integrations.item",
                        integration.getFromFile(),
                        integration.getStartFromRev(),
                        integration.getEndFromRev(),
                        integration.getHowFrom()));
            }
        }
        return comment.toString();
    }
}
