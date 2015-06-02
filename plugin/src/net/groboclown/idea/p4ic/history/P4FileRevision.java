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
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4RevisionNumber;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4Exec;
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
    private final ServerConfig serverConfig;
    private final String clientName;

    /**
     *
     * @param project project
     * @param exec executor (for discovering the client in the future)
     * @param rootDepotPath file path which the original getHistory was requested.
     * @param versionDepotPath file path for the actual file retrieved
     * @param data revision data
     */
    public P4FileRevision(@NotNull Project project, @NotNull P4Exec exec, @NotNull String rootDepotPath,
            @NotNull String versionDepotPath, @Nullable IFileRevisionData data) {
        this(project, exec.getServerConfig(), exec.getClientName(), rootDepotPath, versionDepotPath, data);
    }


    public P4FileRevision(@NotNull Project project, @NotNull ServerConfig serverConfig, @NotNull String clientName,
            @Nullable String rootDepotPath, @NotNull P4AnnotatedLine line) {
        this(project, serverConfig, clientName, rootDepotPath, line.getFile().getDepotPath(), line.getRevisionData());
    }

    private P4FileRevision(@NotNull Project project, @NotNull ServerConfig serverConfig, @Nullable String clientName,
            @Nullable String rootDepotPath, @Nullable String versionDepotPath, @Nullable IFileRevisionData data) {
        this.project = project;
        this.serverConfig = serverConfig;
        this.clientName = clientName;
        this.streamName = null;
        this.revisionDepotPath = data != null
                ? data.getDepotFileName()
                : versionDepotPath == null ? rootDepotPath : versionDepotPath;
        this.changedRepositoryPath = (versionDepotPath == null || versionDepotPath.equals(revisionDepotPath))
                ? null
                : new P4SimpleRepositoryLocation(revisionDepotPath);
        if (data == null) {
            LOG.info("null revision data for " + revisionDepotPath);
            this.revision = new P4RevisionNumber(rootDepotPath, revisionDepotPath, 0);
        } else {
            this.revision = new P4RevisionNumber(rootDepotPath, data);
        }

        this.comment = data == null ? null : data.getDescription();
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
        Client client = getClient();
        if (client == null) {
            return null;
        }
        return revision.loadContentAsBytes(client, null);
    }

    @Nullable
    private Client getClient() {
        if (clientName == null) {
            return null;
        }
        final List<Client> clients = P4Vcs.getInstance(project).getClients();
        for (Client client : clients) {
            if (client.isWorkingOnline() && client.getClientName().equals(clientName) &&
                    client.getConfig().equals(serverConfig)) {
                return client;
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
}
