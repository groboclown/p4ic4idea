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
package net.groboclown.idea.p4ic.server;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.connection.AuthTicketConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.ClientPasswordConnectionHandler;
import net.groboclown.idea.p4ic.server.connection.EnvConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

public abstract class ConnectionHandler {
    public static ConnectionHandler getHandlerFor(@NotNull ServerStatus status) {
        return getHandlerFor(status.getConfig());
    }

    public static ConnectionHandler getHandlerFor(@NotNull ServerConfig config) {
        switch (config.getConnectionMethod()) {
            case AUTH_TICKET:
                return AuthTicketConnectionHandler.INSTANCE;
            case CLIENT:
                return ClientPasswordConnectionHandler.INSTANCE;
            case P4CONFIG:
            case DEFAULT:
                return EnvConnectionHandler.INSTANCE;
            default:
                throw new IllegalStateException(
                        "Unknown connection method: " + config.getConnectionMethod());
        }
    }


    public abstract Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName);


    public static String createUrlFor(@NotNull ServerConfig config) {
        return getHandlerFor(config).createUrl(config);
    }

    public static boolean isConfigValidFor(@NotNull ServerConfig config) {
        return getHandlerFor(config).isConfigValid(config);
    }


    public String createUrl(@NotNull ServerConfig config) {
        return config.getProtocol().toString() + "://" + config.getPort();
    }


    /**
     * Always called on a new server connection.
     *
     * @param server server connection
     * @param config configuration
     * @param password
     * @throws P4JavaException
     */
    public abstract void defaultAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, char[] password) throws P4JavaException;

    /**
     * Called when the server challenges us for authentication.
     *
     * @param server server connection
     * @param config configuration
     * @param password new password
     * @return false if authentication could not be done, or true if it was attempted.
     * @throws P4JavaException
     */
    public abstract boolean forcedAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, char[] password) throws P4JavaException;


    // server configs are required to have all values set in a pretty
    // solid valid config.
    public abstract boolean isConfigValid(@NotNull ServerConfig config);


    protected Properties initializeConnectionProperties(@NotNull ServerConfig config) {
        Properties ret = new Properties();

        ret.setProperty(PropertyDefs.PROG_NAME_KEY, "IntelliJ Perforce Community Plugin");
        ret.setProperty(PropertyDefs.PROG_VERSION_KEY, "1");

        ret.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, P4Config.P4_IGNORE_FILE);
        ret.setProperty(PropertyDefs.ENABLE_PROGRESS, "1");
        ret.setProperty(PropertyDefs.ENABLE_TRACKING, "1");
        ret.setProperty(PropertyDefs.WRITE_IN_PLACE_KEY, "1");
        //ret.setProperty(PropertyDefs.AUTO_CONNECT_KEY, "0");
        //ret.setProperty(PropertyDefs.AUTO_LOGIN_KEY, "0");
        //ret.setProperty(PropertyDefs.ENABLE_PROGRESS, "0");
        //ret.setProperty(PropertyDefs.ENABLE_TRACKING, "0");
        //ret.setProperty(PropertyDefs.NON_CHECKED_SYNC, "0");

        if (config.hasTrustTicket()) {
            ret.setProperty(PropertyDefs.TRUST_PATH_KEY, config.getTrustTicket().getAbsolutePath());
        }

        return ret;
    }

}
