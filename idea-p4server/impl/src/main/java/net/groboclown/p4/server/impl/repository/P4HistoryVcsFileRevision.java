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

package net.groboclown.p4.server.impl.repository;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevisionEx;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.HistoryMessageFormatter;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4Revision;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;

public class P4HistoryVcsFileRevision
        extends VcsFileRevisionEx {
    private final FilePath file;
    private final ServerConfig config;
    private final IFileRevisionData data;
    private final HistoryMessageFormatter formatter;
    private final HistoryContentLoader loader;
    private boolean loadedContent;
    private byte[] content;

    public P4HistoryVcsFileRevision(@NotNull FilePath file,
            @NotNull ServerConfig config,
            @NotNull IFileRevisionData data,
            @Nullable HistoryMessageFormatter formatter,
            @Nullable HistoryContentLoader loader) {
        this.loader = loader;
        this.file = file;
        this.data = data;
        this.formatter = formatter;
        this.config = config;
    }

    /**
     *
     * @return a changelist ID with a client name used to submit the changelist.
     */
    public P4ChangelistId getChangelistId() {
        ClientServerRef ref = new ClientServerRef(config.getServerName(), data.getClientName());
        return new P4ChangelistIdImpl(data.getChangelistId(), ref);
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return config;
    }

    @Nullable
    @Override
    public String getAuthorEmail() {
        // TODO Requires fetching user data...
        return null;
    }

    @Nullable
    @Override
    public String getCommitterName() {
        return data.getUserName();
    }

    @Nullable
    @Override
    public String getCommitterEmail() {
        // TODO Requires fetching user data...
        return null;
    }

    @NotNull
    @Override
    public FilePath getPath() {
        return file;
    }

    @Nullable
    @Override
    public String getBranchName() {
        // TODO Could get the stream name, but that's a lot of guess work.
        return null;
    }

    @Nullable
    @Override
    public RepositoryLocation getChangedRepositoryPath() {
        return null;
    }

    @Override
    public byte[] loadContent()
            throws IOException, VcsException {
        return getContent();
    }

    @Nullable
    @Override
    public byte[] getContent()
            throws IOException, VcsException {
        if (!loadedContent && loader != null) {
            loadedContent = true;
            content = loader.loadContentForRev(config, data.getDepotFileName(), data.getRevision());
        }
        return content;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return new P4Revision(data.getRevision());
    }

    @Override
    public Date getRevisionDate() {
        return data.getDate();
    }

    @Nullable
    @Override
    public String getAuthor() {
        return data.getUserName();
    }

    @Nullable
    //@Override TODO 2018 capability
    public Date getAuthorDate() {
        return data.getDate();
    }

    @Nullable
    @Override
    public String getCommitMessage() {
        return formatter == null
                ? data.getDescription()
                : formatter.format(data);
    }
}
