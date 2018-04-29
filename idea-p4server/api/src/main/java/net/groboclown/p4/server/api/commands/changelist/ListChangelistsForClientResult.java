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
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListChangelistsForClientResult implements P4CommandRunner.ServerResult {
    private final ServerConfig config;
    private final String clientname;
    private final List<P4ChangelistSummary> changelistSummaryList;

    public ListChangelistsForClientResult(@NotNull ServerConfig config,
            @NotNull String clientname, @NotNull List<P4ChangelistSummary> changelistSummaryList) {
        this.config = config;
        this.clientname = clientname;
        this.changelistSummaryList = changelistSummaryList;
    }

    @NotNull
    @Override
    public ServerConfig getServerConfig() {
        return config;
    }

    @NotNull
    public List<P4ChangelistSummary> getChangelistSummaryList() {
        return changelistSummaryList;
    }

    @NotNull
    public String getClientname() {
        return clientname;
    }
}
