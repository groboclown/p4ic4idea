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

import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class P4RemoteFileStore {

    public static class State {
        public String displayName;
        public String path;
        public String localPath;
    }


    @NotNull
    public static State getState(@NotNull P4RemoteFile file) {
        State ret = new State();
        ret.displayName = file.getDisplayName();
        ret.path = file.getDepotPath();
        return ret;
    }

    @Nullable
    public static State getStateNullable(@Nullable P4RemoteFile depotPath) {
        if (depotPath == null) {
            return null;
        }
        return getState(depotPath);
    }

    @NotNull
    public static P4RemoteFile read(@NotNull State state) {
        return new P4RemoteFileImpl(state.path, state.displayName, state.localPath);
    }

    @Nullable
    public static P4RemoteFile readNullable(@Nullable State state) {
        if (state == null) {
            return null;
        }
        return read(state);
    }
}
