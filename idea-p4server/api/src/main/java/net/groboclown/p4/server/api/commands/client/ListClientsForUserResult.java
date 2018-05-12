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

package net.groboclown.p4.server.api.commands.client;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ListClientsForUserResult implements P4CommandRunner.ServerResult {
    private final ServerConfig config;
    private final String requestedUser;
    private final Collection<P4WorkspaceSummary> clients;


    public ListClientsForUserResult(@NotNull ServerConfig config,
            @NotNull String requestedUser,
            @NotNull Collection<P4WorkspaceSummary> clients) {
        this.config = config;
        this.requestedUser = requestedUser;
        this.clients = clients;
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config;
    }

    public String getRequestedUser() {
        return requestedUser;
    }

    public Collection<P4WorkspaceSummary> getClients() {
        return clients;
    }
}
