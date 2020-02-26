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

import com.intellij.openapi.vcs.history.VcsFileRevision;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.HistoryMessageFormatter;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ListFileHistoryResult
        implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final VcsFileRevisionFactory factory;

    public ListFileHistoryResult(@NotNull ClientConfig config,
            @NotNull VcsFileRevisionFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    public List<VcsFileRevision> getRevisions(@Nullable HistoryMessageFormatter formatter,
            @Nullable HistoryContentLoader loader) {
        return factory.create(formatter, loader);
    }


    public interface VcsFileRevisionFactory {
        List<VcsFileRevision> create(@Nullable HistoryMessageFormatter formatter,
                @Nullable HistoryContentLoader loader);
    }
}
