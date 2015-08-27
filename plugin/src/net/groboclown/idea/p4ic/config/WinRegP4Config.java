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

import com.intellij.openapi.util.SystemInfo;
import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.server.IServerAddress;
import net.groboclown.idea.p4ic.config.win.PreferencesWinRegistry;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

/**
 * Loads the Perforce registry entries for Windows computers.
 */
public class WinRegP4Config implements P4Config {
    @NonNls
    private static final String USER_KEY = "\\Software\\Perforce\\Environment";
    @NonNls
    private static final String MACHINE_KEY = "\\SOFTWARE\\Perforce\\Environment";

    private final int hive;
    private final String key;

    private String port;
    private IServerAddress.Protocol protocol;
    private String clientName;
    private String userName;
    private String password;
    private String authTicketPath;
    private String trustTicket;
    private String configFile;
    private String clientHostname;
    private String ignoreFileName;


    public static boolean isAvailable() {
        return SystemInfo.isWindows;
    }


    public WinRegP4Config(boolean userReg) {
        if (userReg) {
            hive = PreferencesWinRegistry.HKEY_CURRENT_USER;
            key = USER_KEY;
        } else {
            hive = PreferencesWinRegistry.HKEY_LOCAL_MACHINE;
            key = MACHINE_KEY;
        }
        reload();
    }

    @Override
    public void reload() {
        try {
            String sport = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4PORT);
            port = P4ConfigUtil.getSimplePortFromPort(sport);
            protocol = P4ConfigUtil.getProtocolFromPort(port);
            clientName = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4CLIENT);
            userName = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4USER);
            password = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4PASSWD);
            authTicketPath = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4TICKETS);
            trustTicket = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4TRUST);
            configFile = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4CONFIG);
            clientHostname = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4HOST);
            ignoreFileName = PreferencesWinRegistry.readString(hive, key, PerforceEnvironment.P4IGNORE);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean hasIsAutoOfflineSet() {
        return false;
    }

    @Override
    public boolean isAutoOffline() {
        return false;
    }

    @Override
    public boolean hasPortSet() {
        return port != null;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public boolean hasProtocolSet() {
        return protocol != null;
    }

    @Override
    public IServerAddress.Protocol getProtocol() {
        return protocol;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientName != null;
    }

    @Override
    public String getClientname() {
        return clientName;
    }

    @Override
    public boolean hasUsernameSet() {
        return userName != null;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @NotNull
    @Override
    public ConnectionMethod getConnectionMethod() {
        return ConnectionMethod.DEFAULT;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthTicketPath() {
        return authTicketPath;
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

    @Override
    public boolean isPasswordStoredLocally() {
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public String getIgnoreFileName() {
        return ignoreFileName;
    }
}
