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

package net.groboclown.p4.server.api.config;

import net.groboclown.p4.server.api.P4ServerName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionalClientServerConfig {
    private @NotNull final ServerConfig serverConfig;
    private @Nullable final ClientConfig clientConfig;

    public OptionalClientServerConfig(@NotNull ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.serverConfig = clientConfig.getServerConfig();
    }


    public OptionalClientServerConfig(@NotNull ServerConfig serverConfig, @Nullable ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.serverConfig = serverConfig;
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Nullable
    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    @NotNull
    public P4ServerName getServerName() {
        return serverConfig.getServerName();
    }

    @NotNull
    public String getServerId() {
        return serverConfig.getServerId();
    }

    @NotNull
    public String getUsername() {
        return serverConfig.getUsername();
    }

    @Nullable
    public String getClientname() {
        if (clientConfig != null) {
            return clientConfig.getClientname();
        }
        return null;
    }

    public String getLoginSso() {
        return serverConfig.getLoginSso();
    }

    public boolean usesStoredPassword() {
        return serverConfig.usesStoredPassword();
    }
}
