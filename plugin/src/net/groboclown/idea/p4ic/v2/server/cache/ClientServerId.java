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


import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import org.jdom.Element;
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
    public static ClientServerId create(@NotNull ServerConfig serverConfig, @Nullable String clientName) {
        return new ClientServerId(serverConfig.getServiceName(), clientName);
    }

    @Nullable
    public static ClientServerId create(@NotNull Project project, @NotNull P4Config config)
            throws NullPointerException {
        final ServerConfig serverConfig = ServerConfig.createNewServerConfig(config);
        if (serverConfig == null || config.getClientname() == null) {
            return null;
        }
        return create(serverConfig, config.getClientname());
    }


    public ClientServerId(@NotNull final String serverConfigId, @Nullable final String clientId) {
        this.serverConfigId = serverConfigId;
        this.clientId = clientId;
    }


    // TODO replace usage of this method with getServerDisplayId()
    @NotNull
    public String getServerConfigId() {
        return serverConfigId;
    }


    @NotNull
    public String getServerDisplayId() {
        String display = serverConfigId;
        int pos = display.indexOf("://");
        if (pos >= 0) {
            display = display.substring(pos + 3);
        }
        if (display.contains("ssl://")) {
            display = "ssl:" + display;
        }
        return display;
    }



    @Nullable
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
            return that.serverConfigId.equals(serverConfigId) &&
                    Comparing.equal(that.clientId, clientId);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return (serverConfigId.hashCode() << 3) +
                (clientId == null ? 0 : clientId.hashCode());
    }

    public void serialize(@NotNull Element wrapper) {
        wrapper.setAttribute("scid", serverConfigId);
        wrapper.setAttribute("cid", clientId);
    }

    @Nullable
    public static ClientServerId deserialize(@NotNull Element wrapper) {
        String scid = CachedState.getAttribute(wrapper, "scid");
        String cid = CachedState.getAttribute(wrapper, "cid");
        if (scid != null) {
            return new ClientServerId(scid, cid);
        }
        return null;
    }


    @Override
    public String toString() {
        return clientId + ":" + serverConfigId;
    }
}
