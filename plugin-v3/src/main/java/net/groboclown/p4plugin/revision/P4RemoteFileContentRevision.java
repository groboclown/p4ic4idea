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
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

public class P4RemoteFileContentRevision extends AbstractP4FileContentRevision {
    private static final VcsRevisionNumber.Int HEAD_REVISION = new VcsRevisionNumber.Int(IFileSpec.HEAD_REVISION);
    private final P4RemoteFile file;


    public P4RemoteFileContentRevision(
            @NotNull P4RemoteFile file,
            @Nullable FilePath path,
            @Nullable VcsRevisionNumber.Int rev,
            @Nullable ServerConfig serverConfig,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        super(serverConfig,
                path == null ? new RemoteFilePath(file.getDisplayName(), false) : path,
                file.getDepotPath(), rev == null ? HEAD_REVISION : rev, loader, charset);
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof P4RemoteFileContentRevision) {
            P4RemoteFileContentRevision that = (P4RemoteFileContentRevision) o;
            return that.file.equals(file)
                    && that.getRevisionNumber().equals(getRevisionNumber());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode() + getRevisionNumber().hashCode();
    }

    @Override
    public String toString() {
        return file.getDisplayName() + "@" + getRevisionNumber();
    }
}
