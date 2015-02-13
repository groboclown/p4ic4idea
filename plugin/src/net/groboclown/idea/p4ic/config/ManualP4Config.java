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

import com.intellij.openapi.util.Comparing;
import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 *
 * @author Matt Albrecht
 */
public class ManualP4Config implements P4Config {
    @Nullable
    private IServerAddress.Protocol protocol;
    @Nullable
    private String port;
    @Nullable
    private String clientname; // workspace
    @Nullable
    private String username;
    @Nullable
    private String authTicket;
    @Nullable
    private String trustTicket;
    @Nullable
    private String configFile;
    @NotNull
    private ConnectionMethod connectionMethod =
            ConnectionMethod.DEFAULT;
    private boolean autoOffline = true;
    @Nullable
    private String password;
    private boolean storePassword;

    public ManualP4Config() {
        // do nothing
    }

    public ManualP4Config(P4Config copy) {
        this.protocol = copy.getProtocol();
        this.port = copy.getPort();
        this.clientname = copy.getClientname();
        this.username = copy.getUsername();
        this.authTicket = copy.getAuthTicketPath();
        this.trustTicket = copy.getTrustTicketPath();
        this.configFile = copy.getConfigFile();
        this.connectionMethod = copy.getConnectionMethod();
        this.autoOffline = copy.isAutoOffline();
        this.password = copy.getPassword();
        this.storePassword = copy.isPasswordStoredLocally();
    }

    public ManualP4Config(@NotNull ServerConfig copy, @Nullable String clientName) {
        this.protocol = copy.getProtocol();
        this.port = copy.getPort();
        this.clientname = clientName;
        this.username = copy.getUsername();
        this.authTicket = null;
        this.trustTicket = null;
        this.configFile = null;
        this.connectionMethod = copy.getConnectionMethod();
        this.autoOffline = false;
        this.password = null;
        this.storePassword = copy.storePasswordLocally();
        this.clientname = clientName;
    }

    @Override
    public void reload() {
        // No-op for this implementation
    }

    @Override
    public boolean hasIsAutoOfflineSet() {
        return true;
    }

    @Override
    public boolean isAutoOffline() {
        return autoOffline;
    }

    public void setAutoOffline(boolean autoOffline) {
        this.autoOffline = autoOffline;
    }

    @Override
    public boolean hasPortSet() {
        return port != null;
    }

    @Nullable
    @Override
    public String getPort() {
        return port;
    }

    public void setPort(@Nullable String port) {
        this.port = port;
    }

    @Override
    public boolean hasProtocolSet() {
        return protocol != null;
    }

    @Nullable
    @Override
    public IServerAddress.Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(@Nullable IServerAddress.Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientname != null;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientname;
    }

    public void setClientname(@Nullable String clientname) {
        this.clientname = clientname;
    }

    @Override
    public boolean hasUsernameSet() {
        return username != null;
    }

    @Nullable
    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    @NotNull
    @Override
    public ConnectionMethod getConnectionMethod() {
        return connectionMethod;
    }

    public void setConnectionMethod(@NotNull ConnectionMethod method) {
        connectionMethod = method;
    }

    @Nullable
    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    @Override
    public String getAuthTicketPath() {
        return authTicket;
    }

    public void setAuthTicketPath(String authTicket) {
        this.authTicket = authTicket;
    }

    @Override
    public boolean hasTrustTicketPathSet() {
        return trustTicket != null;
    }

    @Nullable
    @Override
    public String getTrustTicketPath() {
        return trustTicket;
    }

    public void setTrustTicketPath(@Nullable String trustTicket) {
        this.trustTicket = trustTicket;
    }

    @Nullable
    @Override
    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(@Nullable String configFile) {
        this.configFile = configFile;
    }

    @Override
    public boolean isPasswordStoredLocally() {
        return storePassword;
    }

    public void setPasswordStoredLocally(boolean store) {
        this.storePassword = store;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (! (o instanceof P4Config)) {
            return false;
        }
        P4Config that = (P4Config) o;
        return protocol == that.getProtocol() &&
                connectionMethod == that.getConnectionMethod() &&
                autoOffline == that.isAutoOffline() &&
                storePassword == that.isPasswordStoredLocally() &&
                Comparing.equal(port, that.getPort()) &&
                Comparing.equal(clientname, that.getClientname()) &&
                Comparing.equal(username, that.getUsername()) &&
                Comparing.equal(authTicket, that.getAuthTicketPath()) &&
                Comparing.equal(trustTicket, that.getTrustTicketPath()) &&
                Comparing.equal(configFile, that.getConfigFile()) &&
                Comparing.equal(password, that.getPassword());
    }

    @Override
    public int hashCode() {
        return (protocol == null ? 1 : protocol.hashCode()) +
                (autoOffline ? 2 : 300) +
                (storePassword ? 3 : 400) +
                connectionMethod.hashCode() +
                (port == null ? 5 : port.hashCode()) +
                (clientname == null ? 6 : clientname.hashCode()) +
                (username == null ? 7 : username.hashCode()) +
                (authTicket == null ? 8 : authTicket.hashCode()) +
                (trustTicket == null ? 9 : trustTicket.hashCode()) +
                (configFile == null ? 10 : configFile.hashCode()) +
                (password == null ? 11 : password.hashCode());
    }
}
