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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

public abstract class AbstractP4FileContentRevision
        implements ContentRevision {
    private final FilePath filePath;
    private final String serverFilePath;
    private final VcsRevisionNumber.Int rev;
    private final HistoryContentLoader loader;
    private final Charset charset;


    AbstractP4FileContentRevision(
            @NotNull FilePath filePath,
            @NotNull String serverFilePath,
            @NotNull VcsRevisionNumber.Int rev,
            @Nullable HistoryContentLoader loader,
            @Nullable Charset charset) {
        this.filePath = filePath;
        this.serverFilePath = serverFilePath;
        this.loader = loader;
        this.charset = ContentRevisionUtil.getNonNullCharset(charset);
        this.rev = rev;
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

    @NotNull
    public VcsRevisionNumber.Int getIntRevisionNumber() {
        return rev;
    }

    @NotNull
    public Charset getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        return serverFilePath + "@" + rev;
    }

    @Nullable
    HistoryContentLoader getLoader() {
        return loader;
    }
}
