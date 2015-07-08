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

import com.perforce.p4java.exception.*;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ConfigurationProblem;
import net.groboclown.idea.p4ic.server.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.util.*;


/**
 * Used by unit tests to create mock connections.
 */
public class TestConnectionHandler extends ConnectionHandler {
    private static final Map<String, IOptionsServer> SERVERS = new Hashtable<String, IOptionsServer>();

    public static final TestConnectionHandler INSTANCE = new TestConnectionHandler();



    public static void registerServer(@NotNull String serverUriString, @NotNull IOptionsServer server) {
        SERVERS.put(serverUriString, server);
    }


    public static void deregisterServer(@NotNull String serverUriString) {
        SERVERS.remove(serverUriString);
    }



    @NotNull
    @Override
    public IOptionsServer getOptionsServer(@NotNull String serverUriString, @NotNull Properties props,
            final ServerConfig config) throws URISyntaxException, ConnectionException, NoSuchObjectException, ConfigException, ResourceException {
        final IOptionsServer server = SERVERS.get(serverUriString);
        if (server == null) {
            throw new IllegalStateException("No server registered for " + serverUriString);
        }
        return server;
    }

    @Override
    public Properties getConnectionProperties(@NotNull final ServerConfig config, @Nullable final String clientName) {
        return new Properties();
    }

    @Override
    public void defaultAuthentication(@NotNull final IOptionsServer server, @NotNull final ServerConfig config, final char[] password) throws P4JavaException {
        // Intentionally empty
    }

    @Override
    public boolean forcedAuthentication(@NotNull final IOptionsServer server, @NotNull final ServerConfig config, final char[] password) throws P4JavaException {
        return false;
    }

    @NotNull
    @Override
    public List<ConfigurationProblem> getConfigProblems(@NotNull final ServerConfig config) {
        return Collections.emptyList();
    }
}
