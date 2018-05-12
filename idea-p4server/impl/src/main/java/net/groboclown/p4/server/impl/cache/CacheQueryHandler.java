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

import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4JobSpec;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;

public interface CacheQueryHandler {
    @NotNull
    Collection<P4LocalChangelist> getCachedOpenedChangelists(@NotNull ClientConfig config);

    @NotNull
    Collection<P4LocalFile> getCachedOpenedFiles(@NotNull ClientConfig config);

    /**
     *
     * @param serverName config
     * @param changelistId changelist to fetch the description for
     * @return null if no changelist is cached.
     */
    @Nullable
    P4RemoteChangelist getCachedChangelist(P4ServerName serverName, P4ChangelistId changelistId);

    @Nullable
    P4JobSpec getCachedJobSpec(P4ServerName serverName);

    Collection<P4WorkspaceSummary> getCachedClientsForUser(@NotNull P4ServerName serverName, @NotNull String username);
}
