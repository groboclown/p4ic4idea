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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Stores information regarding a server configuration and the specific client/workspace in that
 * server.
 * <p>
 * This is used for server commands that require a valid client workspace.
 * <p>
 * This class MUST be immutable.
 */
@Immutable
public final class ClientConfig {
    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    static final char SEP = (char) 0x263a;

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private final int configVersion;
    private final ServerConfig serverConfig;
    private final String clientName;
    private final String clientHostName;
    private final String defaultCharSet;
    private final String ignoreFileName;

    private final ClientServerRef clientServerRef;
    private final String clientServerUniqueId;

    @NotNull
    public static ClientConfig createFrom(@NotNull ServerConfig serverConfig, @NotNull ConfigPart data) {
        if (!isValidClientConfig(serverConfig, data)) {
            throw new IllegalArgumentException("did not validate data");
        }
        return new ClientConfig(serverConfig, data);
    }


    public static boolean isValidClientConfig(@Nullable ServerConfig serverConfig, @Nullable ConfigPart part) {
        if (serverConfig == null || part == null || part.hasError()) {
            return false;
        }
        if (isBlank(part.getClientname())) {
            return false;
        }
        if (isBlank(serverConfig.getUsername())) {
            return false;
        }
        return true;
    }


    private ClientConfig(@NotNull ServerConfig serverConfig, @NotNull ConfigPart data) {
        // Not needed anymore, because the calling class (P4ProjectConfigStack) does this check, and we don't
        // want a misleading double exception in the logs.
        /*
        if (! serverConfig.isSameServerConnection(data)) {
            LOG.error("Server config " + serverConfig +
                    " does not match data config " + ConfigPropertiesUtil.toProperties(data));
        }
        */
        this.configVersion = COUNT.incrementAndGet();

        this.serverConfig = serverConfig;
        this.clientName =
                data.hasClientnameSet()
                        ? data.getClientname()
                        : null;
        this.clientHostName =
                data.hasClientHostnameSet()
                        ? data.getClientHostname()
                        : null;
        this.defaultCharSet =
                data.hasDefaultCharsetSet()
                        ? data.getDefaultCharset()
                        : null;
        this.ignoreFileName =
                data.hasIgnoreFileNameSet()
                        ? data.getIgnoreFileName()
                        : null;

        this.clientServerUniqueId = serverConfig.getServerId() + SEP +
                this.clientName + SEP +
                this.clientHostName + SEP +
                this.ignoreFileName + SEP +
                this.defaultCharSet + SEP;
                // root directories are not listed, because all client configs
                // for the same client and server should be a shared object.
        this.clientServerRef = new ClientServerRef(serverConfig.getServerName(), clientName);
    }

    public int getConfigVersion() {
        return this.configVersion;
    }

    /**
     *
     * @return unique ID for this client, which is shared for all clients with the
     *      same setup.
     */
    @NotNull
    public String getClientServerUniqueId() {
        return clientServerUniqueId;
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Nullable
    public String getClientname() {
        return clientName;
    }

    @Nullable
    public String getClientHostName() {
        return clientHostName;
    }

    @Nullable
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    @Nullable
    public String getDefaultCharSet() {
        return defaultCharSet;
    }

    @Nullable
    public String getCharSetName() {
        return defaultCharSet;
    }

    @NotNull
    public ClientServerRef getClientServerRef() {
        return clientServerRef;
    }

    public boolean isIn(@NotNull ServerConfig config) {
        return getServerConfig().getServerId().equals(config.getServerId());
    }

    @Override
    public String toString() {
        if (clientName != null) {
            return serverConfig.getServerName().getDisplayName() + "@" + clientName;
        }
        return serverConfig.getServerName().getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (! (obj instanceof ClientConfig)) {
            return false;
        }
        ClientConfig that = (ClientConfig) obj;
        return getClientServerUniqueId().equals(that.getClientServerUniqueId());
    }

    @Override
    public int hashCode() {
        return clientServerUniqueId.hashCode();
    }
}
