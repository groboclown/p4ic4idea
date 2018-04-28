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

package net.groboclown.p4.server.impl.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.ServerConfigState;
import net.groboclown.p4.server.api.messagebus.ClientConfigConnectionFailedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;

public class ServerConfigStateImpl
        implements ServerConfigState {
    private static final Logger LOG = Logger.getInstance(ServerConfigStateImpl.class);

    private final ServerConfig config;
    private boolean disposed;

    // must monitor for server connection problems.
    private boolean serverConnectionProblem;

    // for user explicitly wanting to not try to connect.
    private boolean userWorkingOffline;

    public ServerConfigStateImpl(@NotNull ServerConfig config) {
        this.config = config;

        MessageBusClient busClient = MessageBusClient.forApplication(this);
        ClientConfigConnectionFailedMessage.addListener(busClient,
                new ClientConfigConnectionFailedMessage.AnyErrorListener() {
                    @Override
                    public void onError(@NotNull ClientConfig config) {
                        serverConnectionProblem = true;
                    }
                });
    }

    @Override
    @NotNull
    public ServerConfig getServerConfig() {
        return config;
    }

    @Override
    public boolean isOffline() {
        return serverConnectionProblem || userWorkingOffline;
    }

    @Override
    public boolean isOnline() {
        return !serverConnectionProblem && !userWorkingOffline;
    }

    @Override
    public boolean isServerConnectionProblem() {
        return serverConnectionProblem;
    }

    @Override
    public boolean isUserWorkingOffline() {
        return userWorkingOffline;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    private void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }
}
