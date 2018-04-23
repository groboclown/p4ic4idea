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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.env.PerforceEnvironment;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.config.ConfigPropertiesUtil;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.config.win.PreferencesWinRegistry;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

class WinRegDataPart implements DataPart {
    private static final Logger LOG = Logger.getInstance(WinRegDataPart.class);

    @NonNls
    private static final String[] USER_KEYS = {
            "Software\\Perforce\\Environment",

    };
    @NonNls
    private static final String[] MACHINE_KEYS = {
            "SOFTWARE\\Perforce\\Environment",
            "SOFTWARE\\Wow6432Node\\Perforce\\Environment"
    };

    private final int hive;
    private final String[] keys;


    private String rawPort;
    private P4ServerName serverName;
    private String clientName;
    private String userName;
    private String password;
    private String authTicketPath;
    private String trustTicket;
    private String configFile;
    private String enviroFile;
    private String clientHostname;
    private String ignoreFileName;
    private String charset;
    private String loginSso;


    static boolean isAvailable() {
        return SystemInfo.isWindows;
    }

    WinRegDataPart(boolean userReg) {
        if (userReg) {
            hive = PreferencesWinRegistry.HKEY_CURRENT_USER;
            keys = USER_KEYS;
        } else {
            hive = PreferencesWinRegistry.HKEY_LOCAL_MACHINE;
            keys = MACHINE_KEYS;
        }
        reload();
    }

    @NotNull
    @Override
    public Element marshal() {
        throw new IllegalStateException("should not be called");
    }

    @Override
    public boolean reload() {
        try {
            rawPort = readRegString(PerforceEnvironment.P4PORT);
            serverName = P4ServerName.forPort(rawPort);
            clientName = readRegString(PerforceEnvironment.P4CLIENT);
            userName = readRegString(PerforceEnvironment.P4USER);
            password = readRegString(PerforceEnvironment.P4PASSWD);
            authTicketPath = readRegString(PerforceEnvironment.P4TICKETS);
            trustTicket = readRegString(PerforceEnvironment.P4TRUST);
            configFile = readRegString(PerforceEnvironment.P4CONFIG);
            enviroFile = readRegString(PerforceEnvironment.P4ENVIRO);
            clientHostname = readRegString(PerforceEnvironment.P4HOST);
            ignoreFileName = readRegString(PerforceEnvironment.P4IGNORE);
            charset = readRegString(PerforceEnvironment.P4CHARSET);
            loginSso = readRegString("P4LOGINSSO");

            if (LOG.isDebugEnabled()) {
                Map<String, String> props = ConfigPropertiesUtil.toProperties(this);
                props.put(PerforceEnvironment.P4CONFIG, configFile);
                props.put(PerforceEnvironment.P4ENVIRO, enviroFile);
                LOG.debug("Loaded windows registry " + keys[0] + " " + props);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // Do not mark as an actual problem.  This is a JVM incompatible issue.
            LOG.debug("Could not access Windows registry", e);
        }
        return hasError();
    }

    private String readRegString(String valueName)
            throws InvocationTargetException, IllegalAccessException {
        for (String key: keys) {
            String ret = PreferencesWinRegistry.readString(hive, key, valueName);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    String getP4ConfigFile() {
        return configFile;
    }

    String getP4EnviroFile() {
        return enviroFile;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        PartValidation validation = new PartValidation();
        validation.checkPort(this, rawPort);
        // validation.checkAuthTicketFile(authTicketPath);
        // validation.checkTrustTicketFile(trustTicket);
        // validation.checkUsername()
        return validation.getProblems();
    }

    @Override
    public boolean hasError() {
        for (ConfigProblem configProblem : getConfigProblems()) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return null;
    }

    @Override
    public boolean hasServerNameSet() {
        return serverName != null;
    }

    @Nullable
    @Override
    public P4ServerName getServerName() {
        return serverName;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientName != null;
    }

    @Nullable
    @Override
    public String getClientname() {
        return clientName;
    }

    @Override
    public boolean hasUsernameSet() {
        return userName != null;
    }

    @Nullable
    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean hasPasswordSet() {
        return password != null;
    }

    @Nullable
    @Override
    public String getPlaintextPassword() {
        return password;
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return authTicketPath != null;
    }

    @Nullable
    @Override
    public File getAuthTicketFile() {
        return authTicketPath == null ? null : new File(authTicketPath);
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return trustTicket != null;
    }

    @Nullable
    @Override
    public File getTrustTicketFile() {
        return trustTicket == null ? null : new File(trustTicket);
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
    public boolean hasClientHostnameSet() {
        return clientHostname != null;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public boolean hasIgnoreFileNameSet() {
        return ignoreFileName != null;
    }

    @Nullable
    @Override
    public String getIgnoreFileName() {
        return ignoreFileName;
    }

    @Override
    public boolean hasDefaultCharsetSet() {
        return charset != null;
    }

    @Nullable
    @Override
    public String getDefaultCharset() {
        return charset;
    }

    @Override
    public boolean hasLoginSsoSet() {
        return loginSso != null;
    }

    @Nullable
    @Override
    public String getLoginSso() {
        return loginSso;
    }
}
