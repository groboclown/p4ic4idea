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
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListFilesDetailsQuery implements P4CommandRunner.ServerQuery<ListFilesDetailsResult> {
    private final ClientServerRef ref;
    private final List<FilePath> files;
    private final int maxResultCount;
    private final RevState revState;

    public enum RevState {
        HAVE, HEAD
    }

    public ListFilesDetailsQuery(@NotNull ClientServerRef ref,
            @NotNull List<FilePath> files, @NotNull RevState revState, int maxResultCount) {
        this.ref = ref;
        this.files = files;
        this.maxResultCount = maxResultCount;
        this.revState = revState;
    }

    @NotNull
    @Override
    public Class<? extends ListFilesDetailsResult> getResultType() {
        return ListFilesDetailsResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return P4CommandRunner.ServerQueryCmd.LIST_FILES_DETAILS;
    }

    public ClientServerRef getClientServerRef() {
        return ref;
    }

    public List<FilePath> getFiles() {
        return files;
    }

    public int getMaxResultCount() {
        return maxResultCount;
    }

    public RevState getRevState() {
        return revState;
    }
}
