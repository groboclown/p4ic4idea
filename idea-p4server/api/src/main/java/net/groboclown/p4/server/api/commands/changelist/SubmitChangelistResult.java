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

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class SubmitChangelistResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final P4ChangelistId changelistId;
    private final List<P4RemoteFile> submitted;
    private final String serverInfoMessage;

    public SubmitChangelistResult(@NotNull ClientConfig config, @NotNull P4ChangelistId changelistId,
            @NotNull List<P4RemoteFile> submitted, @Nullable String serverInfoMessage) {
        this.config = config;
        this.changelistId = changelistId;
        this.submitted = submitted;
        this.serverInfoMessage = serverInfoMessage;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public List<P4RemoteFile> getSubmitted() {
        return submitted;
    }

    public String getServerInfoMessage() {
        return serverInfoMessage;
    }
}
