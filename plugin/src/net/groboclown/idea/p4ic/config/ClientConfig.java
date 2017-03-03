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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.DataPart;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores information regarding a server configuration and the specific client/workspace in that
 * server.
 */
public class ClientConfig {
    // Just some character that won't appear in any real text, but
    // is still viewable in a debugger.
    private static final char SEP = (char) 0x2202;

    private final Project project;
    private final Set<VirtualFile> rootDirs;
    private final ServerConfig serverConfig;
    private final String clientName;
    private final String clientHostName;
    private final String defaultCharSet;
    private final String ignoreFileName;

    private final ClientServerRef clientServerRef;
    private final String clientId;

    @NotNull
    public static ClientConfig createFrom(@NotNull Project project, @NotNull ServerConfig serverConfig,
            @NotNull DataPart data, @NotNull Collection<VirtualFile> clientProjectBaseDirectories) {
        if (data.hasError()) {
            throw new IllegalArgumentException("did not validate data");
        }
        return new ClientConfig(project, serverConfig, data, clientProjectBaseDirectories);
    }

    private ClientConfig(@NotNull Project project, @NotNull ServerConfig serverConfig, @NotNull DataPart data,
            @NotNull Collection<VirtualFile> clientProjectBaseDirectories) {
        if (! serverConfig.isSameServer(data)) {
            throw new IllegalArgumentException("Server config " + serverConfig +
                    " does not match data config " + data);
        }

        this.project = project;
        this.rootDirs = Collections.unmodifiableSet(new HashSet<VirtualFile>(clientProjectBaseDirectories));
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
                this.defaultCharSet + SEP;
                // root directories are not listed, because all client configs
                // for the same client and server should be a shared object.
        this.clientServerRef = new ClientServerRef(serverConfig.getServerName(), clientName);
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
     * Returns all the lowest project source directories that
     * share this client.  This is a bit troublesome in its implications
     * in the implementation.  It means that if you have two directories,
     * each with their own p4 config file, referencing the same client
     * on the same server, but with different configuration
     * (say, a different p4ignore setting), then this one instance
     * will reference just one version of that setting.
     * <p>
     * This is a big assumption.  Other sections of the code should
     * alert the user when this scenario happens.  However, for the
     * moment, this is considered an acceptable shortcoming, as the
     * listed scenario should be rare.
     *
     * @return all root directories in the project that share
     *      this client config.  Note that some of the directories
     *      might be a subdirectory of another, which is fine, because
     *      there may be some level in a tree that is covered by another
     *      client.
     */
    @NotNull
    public Collection<VirtualFile> getProjectSourceDirs() {
        return rootDirs;
    }

    @NotNull
    public ClientServerRef getClientServerRef() {
        return clientServerRef;
    }

    @NotNull
    public Project getProject() {
        return project;
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
        return
                getProject().equals(that.getProject())
                && getClientId().equals(that.getClientId());
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }
}
