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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4UnknownLoginException;
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
    private static final Logger LOG = Logger.getInstance(ConnectionUIConfiguration.class);


    @Nullable
    public static ConfigProblem checkConnection(@NotNull ClientConfig clientConfig,
            @NotNull ServerConnectionManager connectionManager) {
        final Project project = clientConfig.getProject();
        try {
            final ServerConnection connection = connectionManager
                    .getConnectionFor(project, clientConfig, true);
            final ClientExec exec = connection.oneOffClientExec();
            try {
                new P4Exec2(clientConfig.getProject(), exec).getServerInfo();
            } finally {
                exec.dispose();
            }
            return null;
        } catch (P4InvalidClientException e) {
            return new ConfigProblem(e);
        } catch (P4InvalidConfigException e) {
            return new ConfigProblem(e);
        } catch (VcsException e) {
            return new ConfigProblem(e);
        } catch (RuntimeException e) {
            return new ConfigProblem(e);
        }
    }


    @Nullable
    public static Map<ClientConfig, ClientResult> getClients(
            @Nullable Collection<ClientConfig> sources,
            @NotNull ServerConnectionManager connectionManager) {
        final Map<ClientConfig, ClientResult> ret = new HashMap<ClientConfig, ClientResult>();
        if (sources == null) {
            return null;
        }
        for (ClientConfig source : sources) {
            try {
                // Bug #115: getting the clients should not require that a
                // client is present.
                final ServerConnection connection =
                        connectionManager.getConnectionFor(source.getProject(),
                                source,false);
                final ClientExec exec = connection.oneOffClientExec();
                try {
                    final List<String> clients = new P4Exec2(source.getProject(), exec).
                            getClientNames();
                    ret.put(source, new ClientResult(clients));
                } finally {
                    exec.dispose();
                }
            } catch (P4InvalidConfigException e) {
                LOG.info(e);
                ret.put(source, new ClientResult(e));
            } catch (P4InvalidClientException e) {
                LOG.info(e);
                ret.put(source, new ClientResult(e));
            } catch (VcsException e) {
                LOG.info(e);
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

        public boolean isInvalid() {
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
