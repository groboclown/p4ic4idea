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

package net.groboclown.p4.server.api.commands.changelist;

import com.intellij.openapi.vcs.AbstractVcs;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ListSubmittedChangelistsResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final List<P4CommittedChangelist> changes;

    public ListSubmittedChangelistsResult(@NotNull ClientConfig config, @NotNull List<P4CommittedChangelist> changes) {
        this.config = config;
        this.changes = Collections.unmodifiableList(changes);
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public List<P4CommittedChangelist> getChangesForVcs(@Nullable final AbstractVcs vcs) {
        changes.forEach((c) -> {
            c.setVcs(vcs);
        });
        return changes;
    }
}
