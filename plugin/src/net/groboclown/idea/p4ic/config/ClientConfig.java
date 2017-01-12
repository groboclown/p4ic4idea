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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.part.DataPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores information regarding a server configuration and the specific client/workspace in that
 * server.
 */
public class ClientConfig {
    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    private static final char SEP = (char) 0x2202;

    private final VirtualFile rootDir;
    private final ServerConfig serverConfig;
    private final String clientName;
    private final String clientHostName;
    private final String defaultCharSet;
    private final String ignoreFileName;

    private final String clientId;

    public static ClientConfig createFrom(@NotNull ServerConfig serverConfig, @NotNull DataPart data) {
        if (! data.getConfigProblems().isEmpty()) {
            throw new IllegalArgumentException("did not validate data");
        }
        return new ClientConfig(serverConfig, data);
    }

    private ClientConfig(@NotNull ServerConfig serverConfig, @NotNull DataPart data) {
        if (! serverConfig.isSameServer(data)) {
            throw new IllegalArgumentException("Server config " + serverConfig +
                    " does not match data config " + data);
        }

        this.rootDir = data.getRootPath();
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

        this.clientId = serverConfig.getServerId() + SEP +
                this.clientName + SEP +
                this.clientHostName + SEP +
                this.ignoreFileName + SEP +
                this.defaultCharSet + SEP +
                this.rootDir;
    }

    /**
     *
     * @return unique ID for this client, which is shared for all clients with the
     *      same setup.
     */
    @NotNull
    public String getClientId() {
        return clientId;
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    /**
     * Checks if this client setup is able to run commands that require
     * a workspace / client.
     *
     * @return true if able to run workspace commands.
     */
    public boolean isWorkspaceCapable() {
        return clientName != null && ! clientName.isEmpty();
    }

    @Nullable
    public String getClientName() {
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

    /**
     * @return the root directory.
     */
    public VirtualFile getRootDir() {
         return rootDir;
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
        return getClientId().equals(((ClientConfig) obj).getClientId());
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }
}
