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

package net.groboclown.p4.server.api.commands.file;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Even though this is a server query, because it doesn't need the client object to run, it
 * still needs a client name if the user passes in a file, because the server needs to be
 * able to map from the client spec's file mapping to the server object.
 */
public class AnnotateFileQuery implements P4CommandRunner.ClientQuery<AnnotateFileResult> {
    private final FilePath localFile;
    private final P4RemoteFile remoteFile;
    private final int rev;

    public AnnotateFileQuery(@NotNull FilePath localFile, int rev) {
        this.localFile = localFile;
        this.remoteFile = null;
        this.rev = rev;
    }

    public AnnotateFileQuery(@NotNull P4RemoteFile remoteFile, int rev) {
        this.localFile = null;
        this.remoteFile = remoteFile;
        this.rev = rev;
    }

    @NotNull
    @Override
    public Class<? extends AnnotateFileResult> getResultType() {
        return AnnotateFileResult.class;
    }

    @Override
    public P4CommandRunner.ClientQueryCmd getCmd() {
        return P4CommandRunner.ClientQueryCmd.ANNOTATE_FILE;
    }

    /**
     *
     * @return null if the remote file is used; if not null, then then clientname is not-null.
     */
    @Nullable
    public FilePath getLocalFile() {
        return localFile;
    }

    @Nullable
    public P4RemoteFile getRemoteFile() {
        return remoteFile;
    }

    public int getRev() {
        return rev;
    }
}
