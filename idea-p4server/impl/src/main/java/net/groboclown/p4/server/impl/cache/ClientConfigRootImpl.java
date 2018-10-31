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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public class ClientConfigRootImpl
        implements ClientConfigRoot {
    private static final Logger LOG = Logger.getInstance(ClientConfigRootImpl.class);
    private final ClientConfig config;
    private final ServerStatusImpl serverConfigState;
    private final VirtualFile vcsRoot;
    private boolean loaded = false;
    private Boolean caseSensitive = null;
    private VirtualFile clientRoot;

    // must monitor for client config removed.
    private boolean disposed;

    public ClientConfigRootImpl(@NotNull final ClientConfig config,
            @NotNull ServerStatusImpl serverConfigState,
            @NotNull VirtualFile vcsRootDir) {
        this.config = config;
        this.serverConfigState = serverConfigState;
        this.disposed = false;
        this.vcsRoot = vcsRootDir;

        // Temporary setting until the configuration is properly loaded.
        this.clientRoot = vcsRootDir;
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

    @Override
    public boolean isLoadedFromServer() {
        return loaded;
    }

    @Override
    public Boolean isCaseSensitive() {
        return caseSensitive;
    }

    @NotNull
    @Override
    public VirtualFile getClientRootDir() {
        return clientRoot;
    }

    @NotNull
    @Override
    public VirtualFile getProjectVcsRootDir() {
        return vcsRoot;
    }

    @Override
    public String toString() {
        return "ConfigClientRoot(" + clientRoot + " @ " + config + ")";
    }
}
