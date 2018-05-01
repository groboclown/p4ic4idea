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
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.cache.CacheQueryHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockCacheQueryHandler implements CacheQueryHandler {
    private Map<ClientServerRef, List<P4LocalFile>> openFiles = new HashMap<>();
    private Map<ClientServerRef, List<P4ChangelistSummary>> openChangelists = new HashMap<>();

    @NotNull
    @Override
    public List<P4LocalFile> getCachedOpenFiles(ClientConfig config) {
        return openFiles.computeIfAbsent(config.getClientServerRef(), k -> new ArrayList<>());
    }

    public MockCacheQueryHandler withCachedOpenFile(ClientServerRef ref, P4LocalFile... files) {
        openFiles.put(ref, Arrays.asList(files));
        return this;
    }

    @NotNull
    @Override
    public List<P4ChangelistSummary> getCachedChangelistsForClient(ServerConfig config, String clientname) {
        return openChangelists.computeIfAbsent(
                new ClientServerRef(config.getServerName(), clientname),
                k -> new ArrayList<>()
        );
    }

    public MockCacheQueryHandler withCachedChangelistsForClient(ClientServerRef ref, P4ChangelistSummary... summaries) {
        openChangelists.put(ref, Arrays.asList(summaries));
        return this;
    }
}
