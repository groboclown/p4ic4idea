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


import com.intellij.openapi.util.Comparing;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used as a reference to a connection object.
 * It is mostly used by the caches, so that it can persist which
 * connection it applies to.
 * <p>
 * Translation to a ClientConfig should be done through the
 * {@link net.groboclown.idea.p4ic.v2.server.connection.ClientConfigDiscoverer}.
 * This must be done through a discoverer, so that the correct configuration
 * setup is loaded.
 */
public final class ClientServerRef {
    private final P4ServerName serverName;
    private final String clientId;


    @NotNull
    public static ClientServerRef create(@NotNull P4ServerName serverName, @NotNull String clientName) {
        return new ClientServerRef(serverName, clientName);
    }

    @NotNull
    public static ClientServerRef create(@NotNull ServerConfig serverConfig, @Nullable String clientName) {
        return new ClientServerRef(serverConfig.getServerName(), clientName);
    }

    @NotNull
    public static ClientServerRef create(@NotNull ClientConfig clientConfig) {
        return new ClientServerRef(clientConfig.getServerConfig().getServerName(), clientConfig.getClientName());
    }

    @Nullable
    public static ClientServerRef unmarshal(@NotNull Element element, boolean requiresClient) {
        String serverNameStr = element.getAttributeValue("scid");
        String clientName = element.getAttributeValue("client");
        if (serverNameStr == null) {
            return null;
        }
        P4ServerName serverName = P4ServerName.forPort(serverNameStr);
        if (serverName == null) {
            return null;
        }
        if (requiresClient && clientName == null) {
            return null;
        }
        return new ClientServerRef(serverName, clientName);
    }

    /*
    @Nullable
    public static ClientServerRef create(@NotNull Project project, @NotNull P4Config config)
            throws NullPointerException {
        final ServerConfig serverConfig = ServerConfig.createNewServerConfig(config);
        if (serverConfig == null || config.getClientname() == null) {
            return null;
        }
        return create(serverConfig, config.getClientname());
    }
    */


    public ClientServerRef(@NotNull final P4ServerName serverName, @Nullable final String clientId) {
        this.serverName = serverName;
        this.clientId = clientId;
    }


    // TODO replace usage of this method with getServerDisplayId()
    @NotNull
    public P4ServerName getServerName() {
        return serverName;
    }


    @NotNull
    public String getServerDisplayId() {
        return getServerName().getDisplayName();
    }



    @Nullable
    public String getClientName() {
        return clientId;
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
            ClientServerRef that = (ClientServerRef) o;
            return that.serverName.equals(serverName) &&
                    Comparing.equal(that.clientId, clientId);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return (serverName.hashCode() << 3) +
                (clientId == null ? 0 : clientId.hashCode());
    }

    public void marshal(@NotNull Element wrapper) {
        wrapper.setAttribute("scid", serverName.getFullPort());
        wrapper.setAttribute("cid", clientId);
    }

    @Nullable
    public static ClientServerRef deserialize(@NotNull Element wrapper) {
        String scid = CachedState.getAttribute(wrapper, "scid");
        P4ServerName serverName = P4ServerName.forPort(scid);
        String cid = CachedState.getAttribute(wrapper, "cid");
        if (serverName != null) {
            return new ClientServerRef(serverName, cid);
        }
        return null;
    }


    @Override
    public String toString() {
        return clientId + ":" + serverName.getDisplayName();
    }
}
