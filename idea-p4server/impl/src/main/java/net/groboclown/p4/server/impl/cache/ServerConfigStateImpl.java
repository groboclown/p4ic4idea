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
import net.groboclown.p4.server.api.cache.ServerConfigState;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerConfigStateImpl
        implements ServerConfigState {
    private static final Logger LOG = Logger.getInstance(ServerConfigStateImpl.class);

    private final ServerConfig config;
    private boolean disposed;

    private boolean serverHostProblem;

    private boolean serverLoginProblem;

    // for user explicitly wanting to not try to connect.
    private boolean userWorkingOffline;

    private Boolean serverCaseSensitive = null;

    private boolean loaded = false;

    public ServerConfigStateImpl(@NotNull ServerConfig config) {
        this.config = config;
    }

    @Override
    @NotNull
    public ServerConfig getServerConfig() {
        return config;
    }

    @Override
    public boolean isOffline() {
        return serverHostProblem || serverLoginProblem || userWorkingOffline;
    }

    @Override
    public boolean isOnline() {
        return !serverHostProblem && !serverLoginProblem && !userWorkingOffline;
    }

    @Override
    public boolean isServerConnectionProblem() {
        return serverHostProblem || serverLoginProblem;
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

    @Override
    public boolean isLoadedFromServer() {
        return loaded;
    }

    @Nullable
    @Override
    public Boolean isCaseSensitive() {
        return serverCaseSensitive;
    }

    private void checkDisposed() {
        LOG.assertTrue(!disposed, "Already disposed");
    }

    public void setServerHostProblem(boolean hasProblem) {
        serverHostProblem = hasProblem;
    }

    public void setServerLoginProblem(boolean hasProblem) {
        serverLoginProblem = hasProblem;
    }

    public void setUserOffline(boolean isOffline) {
        userWorkingOffline = isOffline;
    }
}
