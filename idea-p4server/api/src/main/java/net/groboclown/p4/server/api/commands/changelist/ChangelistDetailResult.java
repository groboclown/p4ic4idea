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
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of querying the server for a changelist.
 * If the requested changelist does not exist, then the changelist
 * object should be a skeleton with the {@link P4RemoteChangelist#isDeleted()}
 * set to <tt>true</tt>.
 */
public class ChangelistDetailResult implements P4CommandRunner.ServerResult {
    private final ServerConfig config;
    private final P4RemoteChangelist changelist;

    public ChangelistDetailResult(@NotNull ServerConfig config,
            @NotNull P4RemoteChangelist changelist) {
        this.config = config;
        this.changelist = changelist;
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config;
    }

    @NotNull
    public P4RemoteChangelist getChangelist() {
        return changelist;
    }

    public boolean foundChangelist() {
        return !changelist.isDeleted();
    }
}
