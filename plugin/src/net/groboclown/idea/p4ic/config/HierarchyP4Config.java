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

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 *
 * @author Matt Albrecht
 */
public class HierarchyP4Config implements P4Config {
    private final P4Config[] parents;

    public HierarchyP4Config(P4Config... parents) {
        this.parents = parents;
        assert parents.length > 0;
    }


    @Override
    public boolean hasIsAutoOfflineSet() {
        for (P4Config config: parents) {
            if (config.hasIsAutoOfflineSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAutoOffline() {
        for (P4Config config : parents) {
            if (config.hasIsAutoOfflineSet()) {
                return config.isAutoOffline();
            }
        }
        return false;
    }

    @Override
    public void reload() {
        for (P4Config config: parents) {
            config.reload();
        }
    }

    @Override
    public boolean hasPortSet() {
        for (P4Config config : parents) {
            if (config.hasPortSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPort() {
        for (P4Config config : parents) {
            if (config.hasPortSet()) {
                return config.getPort();
            }
        }
        return null;
    }

    @Override
    public boolean hasProtocolSet() {
        for (P4Config config : parents) {
            if (config.hasProtocolSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IServerAddress.Protocol getProtocol() {
        for (P4Config config : parents) {
            if (config.hasProtocolSet()) {
                return config.getProtocol();
            }
        }
        return null;
    }

    @Override
    public boolean hasClientnameSet() {
        for (P4Config config : parents) {
            if (config.hasClientnameSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getClientname() {
        for (P4Config config : parents) {
            if (config.hasClientnameSet()) {
                return config.getClientname();
            }
        }
        return null;
    }

    @Override
    public boolean hasUsernameSet() {
        for (P4Config config : parents) {
            if (config.hasUsernameSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getUsername() {
        for (P4Config config : parents) {
            if (config.hasUsernameSet()) {
                return config.getUsername();
            }
        }
        return null;
    }

    @NotNull
    @Override
    public ConnectionMethod getConnectionMethod() {
        for (P4Config config : parents) {
            if (config.getConnectionMethod() != ConnectionMethod.DEFAULT) {
                return config.getConnectionMethod();
            }
        }
        return ConnectionMethod.DEFAULT;
    }

    @Override
    public String getPassword() {
        for (P4Config config : parents) {
            if (config.getConnectionMethod() == ConnectionMethod.CLIENT) {
                return config.getPassword();
            }
            if (config.getConnectionMethod() == ConnectionMethod.AUTH_TICKET) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String getAuthTicketPath() {
        for (P4Config config : parents) {
            if (config.getConnectionMethod() == ConnectionMethod.AUTH_TICKET) {
                return config.getAuthTicketPath();
            }
        }
        return null;
    }

    @Override
    public boolean hasTrustTicketPathSet() {
        for (P4Config config : parents) {
            if (config.hasTrustTicketPathSet()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getTrustTicketPath() {
        for (P4Config config : parents) {
            if (config.hasUsernameSet()) {
                return config.getTrustTicketPath();
            }
        }
        return null;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        for (P4Config config : parents) {
            if (config.hasServerFingerprintSet()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        for (P4Config config : parents) {
            if (config.hasServerFingerprintSet()) {
                return config.getServerFingerprint();
            }
        }
        return null;
    }

    @Override
    public String getConfigFile() {
        for (P4Config config : parents) {
            if (config.getConfigFile() != null) {
                return config.getConfigFile();
            }
        }
        return null;
    }

    @Override
    public boolean isPasswordStoredLocally() {
        for (P4Config config : parents) {
            if (config.isPasswordStoredLocally()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getClientHostname() {
        for (P4Config config : parents) {
            if (config.getClientHostname() != null) {
                return config.getClientHostname();
            }
        }
        return null;
    }

    @Override
    public String getIgnoreFileName() {
        for (P4Config config : parents) {
            if (config.getIgnoreFileName() != null) {
                return config.getIgnoreFileName();
            }
        }
        return null;
    }
}
