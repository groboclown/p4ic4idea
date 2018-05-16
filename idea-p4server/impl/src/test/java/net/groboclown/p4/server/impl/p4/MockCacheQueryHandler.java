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

package net.groboclown.p4.server.impl.p4;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4JobSpec;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockCacheQueryHandler implements CacheQueryHandler {
    private Map<ClientServerRef, List<P4LocalFile>> openFiles = new HashMap<>();
    private Map<ClientServerRef, List<P4LocalChangelist>> openChangelists = new HashMap<>();

    @NotNull
    @Override
    public Collection<P4LocalFile> getCachedOpenedFiles(@NotNull ClientConfig config) {
        return openFiles.computeIfAbsent(config.getClientServerRef(), k -> new ArrayList<>());
    }

    @Nullable
    @Override
    public P4RemoteChangelist getCachedChangelist(P4ServerName config, P4ChangelistId changelistId) {
        return null;
    }

    @Nullable
    @Override
    public P4JobSpec getCachedJobSpec(P4ServerName serverName) {
        return null;
    }

    @Override
    public Collection<P4WorkspaceSummary> getCachedClientsForUser(@NotNull P4ServerName serverName,
            @NotNull String username) {
        return null;
    }

    public MockCacheQueryHandler withCachedOpenFile(ClientServerRef ref, P4LocalFile... files) {
        openFiles.put(ref, Arrays.asList(files));
        return this;
    }

    @NotNull
    @Override
    public Collection<P4LocalChangelist> getCachedOpenedChangelists(@NotNull ClientConfig config) {
        return openChangelists.computeIfAbsent(
                new ClientServerRef(config.getServerConfig().getServerName(), config.getClientname()),
                k -> new ArrayList<>()
        );
    }

    public MockCacheQueryHandler withCachedChangelistsForClient(ClientServerRef ref, P4LocalChangelist... summaries) {
        openChangelists.put(ref, Arrays.asList(summaries));
        return this;
    }


}
