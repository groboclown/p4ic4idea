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
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Not thread safe.
public class RootedClientConfigImpl
        implements RootedClientConfig {
    private static final Logger LOG = Logger.getInstance(RootedClientConfigImpl.class);
    private final ClientConfig config;
    private final ServerStatusImpl serverConfigState;
    private final List<VirtualFile> vcsRoots;
    private VirtualFile clientRoot;
    private boolean loaded = false;
    private Boolean caseSensitive = null;

    // must monitor for client config removed.
    private boolean disposed;

    public RootedClientConfigImpl(
            @NotNull final ClientConfig config,
            @NotNull ServerStatusImpl serverConfigState,
            @NotNull VirtualFile vcsRootDir) {
        this.config = config;
        this.serverConfigState = serverConfigState;
        this.disposed = false;
        this.vcsRoots = new ArrayList<>();
        this.vcsRoots.add(vcsRootDir);

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
    public boolean isPasswordUnnecessary() {
        return serverConfigState.isPasswordUnnecessary();
    }

    @Override
    public boolean isLoginNeeded() {
        return false;
    }

    @Override
    public boolean isLoginBad() {
        return false;
    }

    @Override
    public boolean isServerConnectionBad() {
        return false;
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
    public boolean isPendingActionsListResendRequired() {
        return serverConfigState.isPendingActionsListResendRequired();
    }

    @Override
    public void setPendingActionsListResendRequired(boolean required) {
        serverConfigState.setPendingActionsListResendRequired(required);
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

    public void setClientRootDir(@NotNull VirtualFile root) {
        this.clientRoot = root;
    }

    @NotNull
    @Override
    public List<VirtualFile> getProjectVcsRootDirs() {
        return Collections.unmodifiableList(vcsRoots);
    }

    public void addVcsRoot(@NotNull VirtualFile root) {
        if (!vcsRoots.contains(root)) {
            vcsRoots.add(root);
        }
    }

    public void removeVcsRoot(@NotNull VirtualFile root) {
        if (! vcsRoots.contains(root)) {
            throw new IllegalArgumentException("root not registered");
        }
        if (vcsRoots.size() <= 1) {
            throw new IllegalStateException("Cannot remove last root");
        }
        vcsRoots.remove(root);
    }

    public boolean isOneRootRemaining() {
        return vcsRoots.size() == 1;
    }

    @Override
    public String toString() {
        return "RootedClientConfig(" + vcsRoots + " @ " + config + ")";
    }
}
