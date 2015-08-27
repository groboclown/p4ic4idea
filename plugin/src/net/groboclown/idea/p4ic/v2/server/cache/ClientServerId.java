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

package net.groboclown.idea.p4ic.v2.server.cache;


import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClientServerId {
    private final String serverConfigId;
    private final String clientId;


    @NotNull
    public static ClientServerId create(@NotNull String serverConfigId, @NotNull String clientName) {
        return new ClientServerId(serverConfigId, clientName);
    }

    @NotNull
    public static ClientServerId create(@NotNull ServerConfig serverConfig, @NotNull String clientName) {
        return new ClientServerId(serverConfig.getServiceName(), clientName);
    }

    @NotNull
    public static ClientServerId create(@NotNull Client client) {
        return create(client.getConfig(), client.getClientName());
    }

    @Nullable
    public static ClientServerId create(@NotNull P4Config config)
            throws NullPointerException {
        final ServerConfig serverConfig = ServerConfig.createNewServerConfig(config);
        if (serverConfig == null || config.getClientname() == null) {
            return null;
        }
        return create(serverConfig, config.getClientname());
    }


    public ClientServerId(@NotNull final String serverConfigId, @NotNull final String clientId) {
        this.serverConfigId = serverConfigId;
        this.clientId = clientId;
    }


    @NotNull
    public String getServerConfigId() {
        return serverConfigId;
    }


    @NotNull
    public String getClientId() {
        return clientId;
    }


    public boolean isSameServer(@NotNull ClientServerId id) {
        return id.serverConfigId.equals(serverConfigId);
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass().equals(getClass())) {
            ClientServerId that = (ClientServerId) o;
            return that.serverConfigId.equals(serverConfigId) && that.clientId.equals(clientId);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return (serverConfigId.hashCode() << 3) + clientId.hashCode();
    }
}
