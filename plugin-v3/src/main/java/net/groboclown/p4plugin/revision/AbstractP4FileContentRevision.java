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

package net.groboclown.p4plugin.revision;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RemoteFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

public class AbstractP4FileContentRevision
        implements ContentRevision {
    private final FilePath filePath;
    private final String serverFilePath;
    private final VcsRevisionNumber.Int rev;
    private final ServerConfig serverConfig;
    private final ClientConfig clientConfig;
    private final HistoryContentLoader loader;
    private final Charset charset;


    public AbstractP4FileContentRevision(
            @NotNull ClientConfig clientConfig,
            @NotNull FilePath filePath,
            @NotNull String serverFilePath,
            @NotNull VcsRevisionNumber.Int rev,
            @Nullable HistoryContentLoader loader,
            @Nullable String charset) {
        this.clientConfig = clientConfig;
        this.serverConfig = clientConfig.getServerConfig();
        this.filePath = filePath;
        this.serverFilePath = serverFilePath;
        this.loader = loader;
        this.charset = charset == null
                // TODO better charset
                ? Charset.defaultCharset()
                : Charset.forName(charset);
        this.rev = rev;
    }


    public AbstractP4FileContentRevision(
            @Nullable ServerConfig serverConfig,
            @NotNull FilePath filePath,
            @NotNull String serverFilePath,
            @NotNull VcsRevisionNumber.Int rev,
            @Nullable HistoryContentLoader loader,
            @Nullable String charset) {
        this.clientConfig = null;
        this.serverConfig = serverConfig;
        this.filePath = filePath;
        this.serverFilePath = serverFilePath;
        this.loader = loader;
        this.charset = charset == null
            // TODO better charset
            ? Charset.defaultCharset()
            : Charset.forName(charset);
        this.rev = rev;
    }

    @Nullable
    @Override
    public String getContent()
            throws VcsException {
        if (serverConfig == null || loader == null) {
            return null;
        }
        try {
            byte[] ret;
            if (clientConfig != null && clientConfig.getClientname() != null) {
                ret = loader.loadContentForLocal(serverConfig, clientConfig.getClientname(), getFile(), rev.getValue());
            } else {
                ret = loader.loadContentForRev(serverConfig, serverFilePath, rev.getValue());
            }
            if (ret == null) {
                return null;
            }
            return new String(ret, charset);
        } catch (IOException e) {
            throw new VcsException(e);
        }
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return filePath;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return rev;
    }

    @Override
    public String toString() {
        return serverFilePath + "@" + rev;
    }
}
