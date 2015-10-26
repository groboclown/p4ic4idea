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
import com.perforce.p4java.env.PerforceEnvironment;
import com.perforce.p4java.server.IServerAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Stores the connection information related to a specific Perforce
 * server.
 * <p>
 * Implementations must specify {@link #equals(Object)} and
 * {@link #hashCode()}, to indicate whether the server connection
 * properties are the same; it should not match on online mode.
 */
public abstract class ServerConfig {

    @Nullable
    public static ServerConfig createNewServerConfig(@NotNull Project project, @NotNull P4Config p4Config) {
        if (! isValid(p4Config)) {
            return null;
        }
        return new ServerConfigImpl(p4Config);
    }


    private static boolean isValid(P4Config p4config) {
        return p4config != null &&
                p4config.getPort() != null &&
                p4config.getProtocol() != null &&
                p4config.getUsername() != null;
    }


    @NotNull
    public abstract String getPort();

    @NotNull
    public abstract IServerAddress.Protocol getProtocol();

    @NotNull
    public abstract String getUsername();

    /**
     *
     * @return the password the user stored in plaintext, if they stored it themselves.
     *      Should only return non-null when the user has a P4CONFIG file with this value
     *      set, or a P4PASSWD env set.
     */
    @Nullable
    public abstract String getPlaintextPassword();

    @NotNull
    public abstract P4Config.ConnectionMethod getConnectionMethod();

    /**
     * Overrides the default method for discovering the user's client
     * host name.  By default, the underlying P4Java API uses INetAddress.
     *
     * @return the overridden client hostname, nor null if not set.
     */
    @Nullable
    public abstract String getClientHostname();

    @Nullable
    public abstract File getAuthTicket();

    @Nullable
    public abstract File getTrustTicket();

    @Nullable
    public abstract String getServerFingerprint();

    public boolean hasServerFingerprint() {
        return getServerFingerprint() != null && getServerFingerprint().length() > 0;
    }

    public boolean hasAuthTicket() {
        return getAuthTicket() != null;
    }

    public boolean hasTrustTicket() {
        return getTrustTicket() != null;
    }

    @Nullable
    public abstract String getIgnoreFileName();

    @NotNull
    public final String getServiceName() {
        return getProtocol().toString() + "://" + getPort();
    }

    //public abstract boolean storePasswordLocally();

    public abstract boolean isAutoOffline();

    @Override
    public String toString() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put(PerforceEnvironment.P4PORT, P4ConfigUtil.toFullPort(getProtocol(), getPort()));
        ret.put(PerforceEnvironment.P4TRUST,
                getTrustTicket() == null ? null :
                getTrustTicket().toString());
        ret.put(PerforceEnvironment.P4USER, getUsername());
        ret.put(PerforceEnvironment.P4TICKETS,
                getAuthTicket() == null ? null :
                getAuthTicket().toString());
        return ret.toString();
    }


    public boolean isSameConnectionAs(@NotNull P4Config config) {
        // same as the equals implementation, but against a P4Config.
        return getPort().equals(config.getPort()) &&
                getProtocol().equals(config.getProtocol()) &&
                getUsername().equals(config.getUsername()) &&
                getConnectionMethod().equals(config.getConnectionMethod());
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
        if (!(other instanceof ServerConfig)) {
            return false;
        }
        ServerConfig sc = (ServerConfig) other;
        return getPort().equals(sc.getPort()) &&
                getProtocol().equals(sc.getProtocol()) &&
                getUsername().equals(sc.getUsername()) &&
                getConnectionMethod().equals(sc.getConnectionMethod());
        // auth ticket & trust ticket & others - not part of comparison!
    }

    @Override
    public int hashCode() {
        return (getPort().hashCode() << 6) +
                (getProtocol().hashCode() << 4) +
                (getUsername().hashCode() << 2) +
                getConnectionMethod().hashCode();
    }
}
