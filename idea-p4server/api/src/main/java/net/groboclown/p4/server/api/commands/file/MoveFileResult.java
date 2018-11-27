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

import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class MoveFileResult implements P4CommandRunner.ClientResult {
    private final ClientConfig config;
    private final String messages;
    private final List<IFileSpec> serverMessages;

    public MoveFileResult(@NotNull ClientConfig config, @Nullable String messages,
            @NotNull List<IFileSpec> serverMessages) {
        this.config = config;
        this.messages = messages;
        this.serverMessages = serverMessages;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    @Nullable
    public String getMessages() {
        return messages;
    }

    @NotNull
    public List<IFileSpec> getServerMessages() {
        return serverMessages;
    }
}
