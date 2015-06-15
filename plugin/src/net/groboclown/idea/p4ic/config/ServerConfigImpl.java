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
    private final boolean storePasswordLocally;


    ServerConfigImpl(@NotNull P4Config proxy) {
        this(proxy.getPort(), proxy.getProtocol(), proxy.getUsername(), proxy.getConnectionMethod(),
                proxy.getAuthTicketPath() == null ? null : new File(proxy.getAuthTicketPath()),
                proxy.getTrustTicketPath() == null ? null : new File(proxy.getTrustTicketPath()),
                proxy.getServerFingerprint(), proxy.isPasswordStoredLocally());
    }


    ServerConfigImpl(@NotNull String port, @NotNull IServerAddress.Protocol protocol,
            @NotNull String username, @NotNull P4Config.ConnectionMethod connectionMethod,
            @Nullable File authTicket, @Nullable File trustTicket,
            @Nullable String serverFingerprint, boolean storePasswordLocally) {
        this.port = port;
        this.protocol = protocol;
        this.username = username;
        this.connectionMethod = connectionMethod;
        this.authTicket = authTicket;
        this.trustTicket = trustTicket;
        this.serverFingerprint = serverFingerprint;
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

    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return connectionMethod;
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

    @Override
    public boolean storePasswordLocally() {
        return storePasswordLocally;
    }

    // equals only cares about the information that connects
    // to the server, not the individual server setup.  Note that
    // this might have the potential to lose information.
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (! (other instanceof ServerConfig)) {
            return false;
        }
        ServerConfig sc = (ServerConfig) other;
        return getPort().equals(sc.getPort()) &&
                getProtocol().equals(sc.getProtocol()) &&
                getUsername().equals(sc.getUsername()) &&
                getConnectionMethod().equals(sc.getConnectionMethod());
            // auth ticket & trust ticket - not part of comparison!
    }

    @Override
    public int hashCode() {
        return (getPort().hashCode() << 6) +
                (getProtocol().hashCode() << 4) +
                (getUsername().hashCode() << 2) +
                getConnectionMethod().hashCode();
    }
}
