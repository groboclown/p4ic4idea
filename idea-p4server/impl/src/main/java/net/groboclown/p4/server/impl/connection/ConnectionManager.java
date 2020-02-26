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

package net.groboclown.p4.server.impl.connection;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface ConnectionManager {
    @NotNull
    <R> Answer<R> withConnection(@NotNull ClientConfig config, @Nullable File cwd, @NotNull P4Func<IClient, R> fun);

    @NotNull
    <R> Answer<R> withConnection(@NotNull ClientConfig config, @NotNull P4Func<IClient, R> fun);

    @NotNull
    <R> Answer<R> withConnection(@NotNull OptionalClientServerConfig server,
            @NotNull P4Func<IOptionsServer, R> fun);

    @NotNull
    <R> Answer<R> withConnection(@NotNull P4ServerName config, P4Func<IOptionsServer, R> fun);

    void disconnect(@NotNull P4ServerName config);
}
