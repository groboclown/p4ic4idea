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
import org.jetbrains.annotations.NotNull;

public class ListClientsForUserQuery implements P4CommandRunner.ServerQuery<ListClientsForUserResult> {
    private final String username;
    private final int maxClients;

    public ListClientsForUserQuery(@NotNull String username, int maxClients) {
        this.username = username;
        this.maxClients = maxClients;
    }

    @NotNull
    @Override
    public Class<? extends ListClientsForUserResult> getResultType() {
        return ListClientsForUserResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return P4CommandRunner.ServerQueryCmd.LIST_CLIENTS_FOR_USER;
    }

    public String getUsername() {
        return username;
    }

    public int getMaxClients() {
        return maxClients;
    }
}
