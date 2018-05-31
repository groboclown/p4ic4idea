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

import com.intellij.openapi.diagnostic.Logger;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * All the information for a single client config that was cached from queries.
 */
public class ClientQueryCacheStore {
    public static class State {
        public ClientServerRefStore.State source;
        public List<P4LocalChangelistStore.State> changelists;
        public List<P4LocalFileStore.State> files;
    }

    private final ClientServerRef source;
    private final List<P4LocalChangelist> changelists = new ArrayList<>();
    private final List<P4LocalFile> files = new ArrayList<>();


    public ClientQueryCacheStore(@NotNull ClientServerRef source) {
        this.source = source;
    }

    ClientQueryCacheStore(@NotNull State state) {
        this.source = ClientServerRefStore.read(state.source);
        for (P4LocalChangelistStore.State changelist : state.changelists) {
            changelists.add(P4LocalChangelistStore.read(changelist));
        }
        for (P4LocalFileStore.State file : state.files) {
            files.add(P4LocalFileStore.read(file));
        }
    }

    @TestOnly
    public void setChangelists(P4LocalChangelist... changelists) {
        this.changelists.clear();
        this.changelists.addAll(Arrays.asList(changelists));
    }

    @TestOnly
    public void setChangelists(Collection<P4LocalChangelist> changelists) {
        this.changelists.clear();
        this.changelists.addAll(changelists);
    }

    @NotNull
    public List<P4LocalChangelist> getChangelists() {
        return Collections.unmodifiableList(changelists);
    }

    @NotNull
    public List<P4LocalFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public void setFiles(Collection<P4LocalFile> files) {
        this.files.clear();
        this.files.addAll(files);
    }

    @NotNull
    public ClientServerRef getClientServerRef() {
        return source;
    }

    @NotNull
    public State getState() {
        State ret = new State();
        ret.source = ClientServerRefStore.getState(source);
        ret.changelists = new ArrayList<>(changelists.size());
        for (P4LocalChangelist changelist : changelists) {
            ret.changelists.add(P4LocalChangelistStore.getState(changelist));
        }
        ret.files = new ArrayList<>(files.size());
        for (P4LocalFile file : files) {
            ret.files.add(P4LocalFileStore.getState(file));
        }
        return ret;
    }
}
