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
import net.groboclown.p4.server.api.config.ClientConfigState;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;

public class ClientConfigStateImpl
        implements ClientConfigState {
    private static final Logger LOG = Logger.getInstance(ClientConfigStateImpl.class);
    private final ClientConfig config;
    private final ServerConfigStateImpl serverConfigState;

    // must monitor for client config removed.
    private boolean disposed;

    public ClientConfigStateImpl(@NotNull final ClientConfig config,
            @NotNull ServerConfigStateImpl serverConfigState) {
        this.config = config;
        this.serverConfigState = serverConfigState;
        this.disposed = false;

        MessageBusClient client = MessageBusClient.forProject(serverConfigState.getProject(), this);
        ClientConfigRemovedMessage.addListener(client, removedConfig -> {
            if (removedConfig.equals(config)) {
                dispose();
            }
        });
    }

    @NotNull
    @Override
    public Project getProject() {
        return serverConfigState.getProject();
    }

    @Override
    @NotNull
    public ServerConfig getServerConfig() {
        return serverConfigState.getServerConfig();
    }

    @Override
    public boolean isOffline() {
        return serverConfigState.isOffline();
    }

    @Override
    public boolean isOnline() {
        return serverConfigState.isOnline();
    }

    @Override
    public boolean isServerConnectionProblem() {
        return serverConfigState.isServerConnectionProblem();
    }

    @Override
    public boolean isUserWorkingOffline() {
        return serverConfigState.isUserWorkingOffline();
    }

    @Override
    public boolean isDisposed() {
        return disposed || serverConfigState.isDisposed();
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    private void checkDisposed() {
        LOG.assertTrue(!isDisposed(), "Already disposed");
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }
}
