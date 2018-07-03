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
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import org.jetbrains.annotations.NotNull;

public class ListSubmittedChangelistsQuery implements P4CommandRunner.ServerQuery<ListSubmittedChangelistsResult> {
    private final P4RepositoryLocation location;
    private final int maxCount;

    public ListSubmittedChangelistsQuery(@NotNull P4RepositoryLocation location, int maxCount) {
        this.location = location;
        this.maxCount = maxCount;
    }

    @NotNull
    @Override
    public Class<? extends ListSubmittedChangelistsResult> getResultType() {
        return ListSubmittedChangelistsResult.class;
    }

    @Override
    public P4CommandRunner.ServerQueryCmd getCmd() {
        return P4CommandRunner.ServerQueryCmd.LIST_SUBMITTED_CHANGELISTS;
    }

    @NotNull
    public P4RepositoryLocation getLocation() {
        return location;
    }
}
