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

package net.groboclown.p4.server.api;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

class MockRootedClientConfig
        implements RootedClientConfig {
    final ClientConfig config;
    final VirtualFile root;

    public MockRootedClientConfig(ClientConfig config, VirtualFile vcsRootDir) {
        this.config = config;
        this.root = vcsRootDir;
    }

    @NotNull
    @Override
    public ClientConfig getClientConfig() {
        return config;
    }

    @Override
    public boolean isLoadedFromServer() {
        return false;
    }

    @Override
    public boolean isPendingActionsListResendRequired() {
        return false;
    }

    @Override
    public void setPendingActionsListResendRequired(boolean required) {

    }

    @Nullable
    @Override
    public Boolean isCaseSensitive() {
        return null;
    }

    @Nullable
    @Override
    public VirtualFile getClientRootDir() {
        return null;
    }

    @NotNull
    @Override
    public List<VirtualFile> getProjectVcsRootDirs() {
        return Collections.singletonList(root);
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config.getServerConfig();
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isServerConnectionProblem() {
        return false;
    }

    @Override
    public boolean isUserWorkingOffline() {
        return false;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public boolean isPasswordUnnecessary() {
        return false;
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

    }
}
