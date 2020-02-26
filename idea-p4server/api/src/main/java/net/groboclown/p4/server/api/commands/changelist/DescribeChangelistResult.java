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

package net.groboclown.p4.server.api.commands.changelist;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class DescribeChangelistResult implements P4CommandRunner.ServerResult {
    private final OptionalClientServerConfig config;
    private final P4ChangelistId requestedChangelist;
    private final P4RemoteChangelist remoteChangelist;
    private final boolean fromCache;

    public DescribeChangelistResult(@NotNull OptionalClientServerConfig config,
            @NotNull P4ChangelistId requestedChangelist,
            @Nullable P4RemoteChangelist changelist,
            boolean fromCache) {
        this.config = config;
        this.requestedChangelist = requestedChangelist;
        this.remoteChangelist = changelist;
        this.fromCache = fromCache;
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config.getServerConfig();
    }

    public P4ChangelistId getRequestedChangelist() {
        return requestedChangelist;
    }

    @Nullable
    public P4RemoteChangelist getRemoteChangelist() {
        return remoteChangelist;
    }

    /**
     * If the changelist is null, and this returns true, then the changelist
     * might exist on the server, but it isn't in the cache.
     *
     * @return true if the changelist was loaded from the cache, or false
     *      if from the server.
     */
    public boolean isFromCache() {
        return fromCache;
    }
}
