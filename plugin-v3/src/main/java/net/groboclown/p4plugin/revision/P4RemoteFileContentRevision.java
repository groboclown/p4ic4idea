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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RemoteFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class P4RemoteFileContentRevision implements ContentRevision {
    private final Project project;
    private final P4RemoteFile file;
    private final FilePath filePath;
    private final VcsRevisionNumber.Int rev;
    private final ServerConfig serverConfig;
    private final HistoryContentLoader loader;
    private final String charset;


    public P4RemoteFileContentRevision(
            @Nullable Project project,
            @NotNull P4RemoteFile file,
            @Nullable VcsRevisionNumber.Int rev,
            @Nullable ServerConfig serverConfig,
            @Nullable HistoryContentLoader loader,
            @Nullable String charset) {
        this.project = project;
        this.file = file;
        this.filePath = new RemoteFilePath(file.getDisplayName(), false);
        this.serverConfig = serverConfig;
        this.loader = loader;
        this.charset = charset;
        if (rev == null) {
            // TODO use a better source for the constant.
            this.rev = new VcsRevisionNumber.Int(IFileSpec.HEAD_REVISION);
        } else {
            this.rev = rev;
        }
    }

    @Nullable
    @Override
    public String getContent()
            throws VcsException {
        if (serverConfig == null || project == null || loader == null) {
            return null;
        }
        try {
            byte[] ret = loader.loadContentForRev(serverConfig, file.getDepotPath(), rev.getValue());
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof P4RemoteFileContentRevision) {
            P4RemoteFileContentRevision that = (P4RemoteFileContentRevision) o;
            return that.file.equals(file)
                    && that.rev.equals(rev);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode() + rev.hashCode();
    }

    @Override
    public String toString() {
        return file.getDisplayName() + "@" + rev;
    }
}
