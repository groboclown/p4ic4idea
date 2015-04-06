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
package net.groboclown.idea.p4ic.server.connection;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Properties;

public class AuthTicketConnectionHandler extends ConnectionHandler {
    public static AuthTicketConnectionHandler INSTANCE = new AuthTicketConnectionHandler();

    private AuthTicketConnectionHandler() {
        // Utility class
    }

    @Override
    public Properties getConnectionProperties(@NotNull ServerConfig config, @Nullable String clientName) {
        Properties ret = initializeConnectionProperties(config);
        ret.setProperty(PropertyDefs.USER_NAME_KEY, config.getUsername());
        if (config.getAuthTicket() != null) {
            ret.setProperty(PropertyDefs.TICKET_PATH_KEY, config.getAuthTicket().getAbsolutePath());
        }

        return ret;
    }

    @Override
    public boolean isConfigValid(@NotNull ServerConfig config) {
        return (
            // password can be empty or null
            config.getAuthTicket() != null);
    }

    @Override
    public void defaultAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, @NotNull char[] password) throws P4JavaException {
        // no need to login - assume the ticket is valid.
    }

    @Override
    public boolean forcedAuthentication(@NotNull IOptionsServer server, @NotNull ServerConfig config, @NotNull char[] password) throws P4JavaException {
        try {
            if (password.length > 0) {
                // If the password is blank, then there's no need for the
                // user to log in; in fact, that wil raise an error.
                server.login(new String(password), new LoginOptions(false, true));
                return true;
            } else {
                return false;
            }
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }
}
