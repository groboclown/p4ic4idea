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
    public static ServerConfig createNewServerConfig(P4Config p4Config) {
        System.out.println("config: " + P4ConfigUtil.getProperties(p4Config));
        if (! isValid(p4Config)) {
            return null;
        }
        ServerConfigImpl config = new ServerConfigImpl(p4Config);
        String password = p4Config.getPassword();
        if (password != null) {
            PasswordStore.storePasswordFor(config, password.toCharArray());
        }
        return config;
    }


    @Nullable
    public static ServerConfig createOldServerConfig(P4Config p4Config) {
        System.out.println("config: " + P4ConfigUtil.getProperties(p4Config));
        if (! isValid(p4Config)) {
            return null;
        }
        ServerConfig config = new ServerConfigImpl(p4Config);
        //String password = p4Config.getPassword();
        //if (password != null) {
        //    PasswordStore.storePasswordFor(config, password.toCharArray());
        //}
        return config;
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

    public abstract P4Config.ConnectionMethod getConnectionMethod();

    @Nullable
    public abstract File getAuthTicket();

    @Nullable
    public abstract File getTrustTicket();

    public boolean hasAuthTicket() {
        return getAuthTicket() != null;
    }

    public boolean hasTrustTicket() {
        return getTrustTicket() != null;
    }

    @NotNull
    public final String getServiceName() {
        return getProtocol().toString() + "://" + getPort();
    }

    public abstract boolean storePasswordLocally();
}
