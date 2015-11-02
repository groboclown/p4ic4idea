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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the server connection code when setting up an initial connection.
 */
public class ConnectionUIConfiguration {
    public static void checkConnection(@NotNull ProjectConfigSource source)
            throws P4JavaException, IOException, URISyntaxException {
        ClientExec.getServerInfo(source.getServerConfig());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @NotNull
    public static Map<ProjectConfigSource, Exception> findConnectionProblems(@NotNull
            Collection<ProjectConfigSource> sources) {
        final Map<ProjectConfigSource, Exception> ret = new HashMap<ProjectConfigSource, Exception>();
        for (ProjectConfigSource source : sources) {
            try {
                checkConnection(source);
            } catch (P4JavaException e) {
                ret.put(source, e);
            } catch (IOException e) {
                ret.put(source, e);
            } catch (URISyntaxException e) {
                ret.put(source, e);
            }
        }
        return ret;
    }

    @Nullable
    public static Map<ProjectConfigSource, ClientResult> getClients(@Nullable
            Collection<ProjectConfigSource> sources) {
        final Map<ProjectConfigSource, ClientResult> ret = new HashMap<ProjectConfigSource, ClientResult>();
        if (sources == null) {
            return null;
        }
        for (ProjectConfigSource source : sources) {
            try {
                final List<String> clients = ClientExec.getClientNames(source.getServerConfig());
                ret.put(source, new ClientResult(clients));
            } catch (IOException e) {
                ret.put(source, new ClientResult(e));
            } catch (P4JavaException e) {
                ret.put(source, new ClientResult(e));
            } catch (URISyntaxException e) {
                ret.put(source, new ClientResult(e));
            }
        }
        return ret;
    }



    public static class ClientResult {
        private final List<String> clientNames;
        private final Exception connectionProblem;

        private ClientResult(@NotNull List<String> clientNames) {
            this.clientNames = clientNames;
            this.connectionProblem = null;
        }

        private ClientResult(@NotNull Exception ex) {
            this.clientNames = null;
            this.connectionProblem = ex;
        }

        public boolean isInalid() {
            return clientNames == null;
        }

        public List<String> getClientNames() {
            return clientNames;
        }

        public Exception getConnectionProblem() {
            return connectionProblem;
        }
    }
}
