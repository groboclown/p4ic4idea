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

public class ListFileHistoryQuery
        implements P4CommandRunner.ServerQuery<ListFileHistoryResult> {
    // Because this is a server query, but it uses a file input, it means we need a
    // client to perform local location to depot path conversion.
    private final ClientServerRef ref;
    private final FilePath file;
    private final int maxResults;

    public ListFileHistoryQuery(@NotNull ClientServerRef ref, @NotNull FilePath file, int maxResults) {
        this.ref = ref;
        this.file = file;
        this.maxResults = maxResults;
    }

    @NotNull
    @Override
    public Class<? extends ListFileHistoryResult> getResultType() {
        return ListFileHistoryResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return P4CommandRunner.ServerQueryCmd.LIST_FILE_HISTORY;
    }

    @NotNull
    public FilePath getFile() {
        return file;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public ClientServerRef getClientServerRef() {
        return ref;
    }
}
