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
 * @deprecated Only kept around for historical purposes.  Specifically, for loading
 * the user's old settings when they migrate to the new version.
 * See {@link P4ConfigProject} for where it should only be used.
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
    private String serverFingerprint;
    @Nullable
    private String configFile;
    @NotNull
    private ConnectionMethod connectionMethod =
            ConnectionMethod.DEFAULT;
    private boolean autoOffline = true;
    @Nullable
    private String password;
    //private boolean storePassword;
    @Nullable
    private String clientHostname;
    @Nullable
    private String ignoreFileName;

    private boolean isConfigured;


    public ManualP4Config() {
        // do nothing
    }

    public ManualP4Config(@NotNull P4Config copy) {
        this.protocol = copy.getProtocol();
        this.port = copy.getPort();
        this.clientname = copy.getClientname();
        this.username = copy.getUsername();
        this.authTicket = copy.getAuthTicketPath();
        this.trustTicket = copy.getTrustTicketPath();
        this.serverFingerprint = copy.getServerFingerprint();
        this.configFile = copy.getConfigFile();
        this.connectionMethod = copy.getConnectionMethod();
        this.autoOffline = copy.isAutoOffline();
        this.password = copy.getPassword();
        //this.storePassword = copy.isPasswordStoredLocally();
        this.clientHostname = copy.getClientHostname();
        this.ignoreFileName = copy.getIgnoreFileName();
        this.isConfigured = true;
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
        this.isConfigured = true;
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
        this.isConfigured = true;
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
        this.isConfigured = true;
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
        this.isConfigured = true;
    }

    @Override
    public String getAuthTicketPath() {
        return authTicket;
    }

    public void setAuthTicketPath(String authTicket) {
        this.authTicket = authTicket;
        this.isConfigured = true;
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
        this.isConfigured = true;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return serverFingerprint != null;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return serverFingerprint;
    }

    public void setServerFingerprint(@Nullable String fingerprint) {
        this.serverFingerprint = fingerprint;
        this.isConfigured = true;
    }


    @Nullable
    @Override
    public String getConfigFile() {
        return configFile;
    }

    @Override
    public boolean hasClientHostnameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return false;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return false;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return null;
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
                //storePassword == that.isPasswordStoredLocally() &&
                Comparing.equal(port, that.getPort()) &&
                Comparing.equal(clientname, that.getClientname()) &&
                Comparing.equal(username, that.getUsername()) &&
                Comparing.equal(authTicket, that.getAuthTicketPath()) &&
                Comparing.equal(trustTicket, that.getTrustTicketPath()) &&
                Comparing.equal(configFile, that.getConfigFile()) &&
                Comparing.equal(password, that.getPassword()) &&
                Comparing.equal(clientHostname, that.getClientHostname()) &&
                Comparing.equal(ignoreFileName, that.getIgnoreFileName());
    }

    @Override
    public int hashCode() {
        return (protocol == null ? 1 : protocol.hashCode()) +
                (autoOffline ? 2 : 300) +
                //(storePassword ? 3 : 400) +
                connectionMethod.hashCode() +
                (port == null ? 5 : port.hashCode()) +
                (clientname == null ? 6 : clientname.hashCode()) +
                (username == null ? 7 : username.hashCode()) +
                (authTicket == null ? 8 : authTicket.hashCode()) +
                (trustTicket == null ? 9 : trustTicket.hashCode()) +
                (configFile == null ? 10 : configFile.hashCode()) +
                (password == null ? 11 : password.hashCode()) +
                (clientHostname == null ? 12 : clientHostname.hashCode()) +
                (ignoreFileName == null ? 13 : ignoreFileName.hashCode());
    }
}
