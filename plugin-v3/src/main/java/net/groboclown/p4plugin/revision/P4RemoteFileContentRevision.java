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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RemoteFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.function.Supplier;

public class P4RemoteFileContentRevision extends AbstractP4FileContentRevision {
    private static final VcsRevisionNumber.Int HEAD_REVISION = new VcsRevisionNumber.Int(IFileSpec.HEAD_REVISION);
    private final P4RemoteFile file;
    private final Supplier<ClientConfig> clientConfigFactory;

    public static P4RemoteFileContentRevision delayCreation(@NotNull final Project project, @NotNull P4RemoteFile file,
            @Nullable FilePath path,
            @NotNull VcsRevisionNumber.Int rev,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        return new P4RemoteFileContentRevision(file,
                path == null ? new RemoteFilePath(file.getDisplayName(), false) : path,
                rev, loader, charset,
                () -> {
                    ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
                    if (reg == null) {
                        return null;
                    }
                    ClientConfigRoot config = reg.getClientFor(path);
                    if (config == null) {
                        return null;
                    }
                    return config.getClientConfig();
                });
    }

    public static P4RemoteFileContentRevision create(@NotNull P4RemoteFile file,
            @NotNull FilePath path,
            @NotNull VcsRevisionNumber.Int rev,
            @NotNull ClientConfig clientConfig,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        return new P4RemoteFileContentRevision(file, path, rev, loader, charset, () -> clientConfig);
    }

    public static P4RemoteFileContentRevision create(@NotNull P4RemoteFile file,
            @NotNull FilePath path,
            @NotNull ClientConfig clientConfig,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        return new P4RemoteFileContentRevision(file, path, HEAD_REVISION, loader, charset, () -> clientConfig);
    }

    public static P4RemoteFileContentRevision create(@NotNull P4RemoteFile file,
            @NotNull ClientConfig clientConfig,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        return new P4RemoteFileContentRevision(file,
                new RemoteFilePath(file.getDisplayName(), false),
                HEAD_REVISION, loader, charset, () -> clientConfig);
    }

    private P4RemoteFileContentRevision(
            @NotNull P4RemoteFile file,
            @NotNull FilePath path,
            @NotNull VcsRevisionNumber.Int rev,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset,
            @NotNull Supplier<ClientConfig> clientConfigFactory) {
        super(path, file.getDepotPath(), rev, loader, charset);
        this.file = file;
        this.clientConfigFactory = clientConfigFactory;
    }

    @NotNull
    public P4RemoteFile getDepotPath() {
        return file;
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

    @Nullable
    @Override
    public String getContent()
            throws VcsException {
        ClientConfig config = clientConfigFactory.get();
        if (getLoader() == null || config == null || config.getClientname() == null) {
            return null;
        }
        return ContentRevisionUtil.getContent(config, getLoader(),
                getFile(), getIntRevisionNumber().getValue(), getCharset());
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
