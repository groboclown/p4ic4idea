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

package net.groboclown.p4.server.api.commands.sync;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import org.jetbrains.annotations.NotNull;

public class SyncListChangelistsForClientQuery implements P4CommandRunner.SyncServerQuery<ListChangelistsForClientResult> {
    private final String clientname;
    private final int maxResults;

    public SyncListChangelistsForClientQuery(String clientname, int maxResults) {
        this.clientname = clientname;
        this.maxResults = maxResults;
    }


    @NotNull
    @Override
    public Class<? extends ListChangelistsForClientResult> getResultType() {
        return ListChangelistsForClientResult.class;
    }

    @Override
    public P4CommandRunner.SyncServerQueryCmd getCmd() {
        return P4CommandRunner.SyncServerQueryCmd.SYNC_LIST_CHANGELISTS_FOR_CLIENT;
    }

    public String getClientname() {
        return clientname;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
