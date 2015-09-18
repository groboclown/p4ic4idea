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
import com.perforce.p4java.env.PerforceEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 */
public class EnvP4Config implements P4Config {
    private IServerAddress.Protocol protocol;
    private String port;
    private String clientname; // workspace
    private String username;
    private String password; // !!! kept in memory
    private String authTicket;
    private String trustTicket;
    private String configFile;
    private String clientHostname;
    private String ignoreFile;
    private ConnectionMethod connectionMethod;

    public EnvP4Config() {
        loadDefaultEnv();
    }

    private void loadDefaultEnv() {
        String rawPort = PerforceEnvironment.getP4Port();
        port = P4ConfigUtil.getSimplePortFromPort(rawPort);
        protocol = P4ConfigUtil.getProtocolFromPort(rawPort);
        clientname = PerforceEnvironment.getP4Client();
        username = PerforceEnvironment.getP4User();
        password = PerforceEnvironment.getP4Passwd();
        configFile = PerforceEnvironment.getP4Config();
        authTicket = PerforceEnvironment.getP4Tickets();
        trustTicket = PerforceEnvironment.getP4Trust();
        clientHostname = PerforceEnvironment.getP4Host();
        ignoreFile = PerforceEnvironment.getP4Ignore();
        if (trustTicket == null) {
            trustTicket = P4ConfigUtil.getDefaultTrustTicketFile().getAbsolutePath();
        }
        if (password != null) {
            connectionMethod = ConnectionMethod.CLIENT;
        } else {
            connectionMethod = ConnectionMethod.AUTH_TICKET;
            if (authTicket == null) {
                authTicket = P4ConfigUtil.getDefaultTicketFile().getAbsolutePath();
            }
        }

        // What to do with these?
        //PerforceEnvironment.getP4Host();
        //PerforceEnvironment.getP4Ignore();
        //PerforceEnvironment.getP4Charset();
        //PerforceEnvironment.getP4Enviro();

    }

    @Override
    public boolean isAutoOffline() {
        return false;
    }

    @Override
    public boolean hasIsAutoOfflineSet() {
        return false;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public boolean hasPortSet() {
        return port != null;
    }

    @Override
    public IServerAddress.Protocol getProtocol() {
        return protocol;
    }

    @Override
    public boolean hasProtocolSet() {
        return true;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientname != null;
    }

    @Override
    public String getClientname() {
        return clientname;
    }

    @Override
    public boolean hasUsernameSet() {
        return false;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @NotNull
    @Override
    public ConnectionMethod getConnectionMethod() {
        return connectionMethod;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthTicketPath() {
        return authTicket;
    }

    @Override
    public boolean hasTrustTicketPathSet() {
        return trustTicket != null;
    }

    @Override
    public String getTrustTicketPath() {
        return trustTicket;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return null;
    }

    @Override
    public String getConfigFile() {
        return configFile;
    }

    //@Override
    //public boolean isPasswordStoredLocally() {
    //    return getPassword() == null;
    //}

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public String getIgnoreFileName() {
        return ignoreFile;
    }


    public void reload() {
        loadDefaultEnv();
    }
}
