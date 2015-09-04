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

import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ServerConfigImpl extends ServerConfig {
    private final String port;
    private final IServerAddress.Protocol protocol;
    private final String username;
    private final P4Config.ConnectionMethod connectionMethod;
    private final File authTicket;
    private final File trustTicket;
    private final String serverFingerprint;
    private final String clientHostname;
    private final String ignoreFileName;
    private final boolean isAutoOffline;
    private final boolean storePasswordLocally;


    ServerConfigImpl(@NotNull P4Config proxy) {
        this(proxy.getPort(), proxy.getProtocol(), proxy.getUsername(), proxy.getConnectionMethod(),
                proxy.getAuthTicketPath() == null ? null : new File(proxy.getAuthTicketPath()),
                proxy.getTrustTicketPath() == null ? null : new File(proxy.getTrustTicketPath()),
                proxy.getServerFingerprint(), proxy.getClientHostname(), proxy.getIgnoreFileName(),
                proxy.isAutoOffline(), proxy.isPasswordStoredLocally());
    }


    ServerConfigImpl(@NotNull String port, @NotNull IServerAddress.Protocol protocol,
            @NotNull String username, @NotNull P4Config.ConnectionMethod connectionMethod,
            @Nullable File authTicket, @Nullable File trustTicket,
            @Nullable String serverFingerprint, @Nullable String clientHostname,
            @Nullable String ignoreFileName, boolean isAutoOffline, boolean storePasswordLocally) {
        this.port = port;
        this.protocol = protocol;
        this.username = username;
        this.connectionMethod = connectionMethod;
        this.authTicket = authTicket;
        this.trustTicket = trustTicket;
        this.serverFingerprint = serverFingerprint;
        this.clientHostname = clientHostname;
        this.ignoreFileName = ignoreFileName;
        this.isAutoOffline = isAutoOffline;
        this.storePasswordLocally = storePasswordLocally;
    }


    @NotNull
    @Override
    public String getPort() {
        return port;
    }

    @NotNull
    @Override
    public IServerAddress.Protocol getProtocol() {
        return protocol;
    }

    @NotNull
    @Override
    public String getUsername() {
        return username;
    }

    @NotNull
    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return connectionMethod;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Nullable
    @Override
    public File getAuthTicket() {
        return authTicket;
    }

    @Nullable
    @Override
    public File getTrustTicket() {
        return trustTicket;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return serverFingerprint;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    @Override
    public boolean storePasswordLocally() {
        return storePasswordLocally;
    }

    @Override
    public boolean isAutoOffline() {
        return isAutoOffline;
    }
}
