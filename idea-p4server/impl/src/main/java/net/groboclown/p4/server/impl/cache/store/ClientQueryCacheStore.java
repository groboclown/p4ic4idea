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

package net.groboclown.p4.server.impl.cache.store;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * All the information for a single client config that was cached from queries.
 */
public class ClientQueryCacheStore {

    public static class State {

    }

    private final ClientServerRef source;
    private final List<P4LocalChangelist> changelists = new ArrayList<>();
    private final List<P4LocalFile> files = new ArrayList<>();


    public ClientQueryCacheStore(ClientServerRef source) {
        this.source = source;
    }

    @TestOnly
    public void setChangelists(P4LocalChangelist... changelists) {
        this.changelists.clear();
        this.changelists.addAll(Arrays.asList(changelists));
    }

    @NotNull
    public List<P4LocalChangelist> getChangelists() {
        return changelists;
    }

    @NotNull
    public List<P4LocalFile> getFiles() {
        return files;
    }

    @NotNull
    public ClientServerRef getClientServerRef() {
        return source;
    }
}
