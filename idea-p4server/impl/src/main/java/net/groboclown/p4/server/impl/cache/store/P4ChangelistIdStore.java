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

import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class P4ChangelistIdStore {

    public static class State {
        public ClientServerRefStore.State ref;
        public int id;
    }

    @Nullable
    public static State getStateNullable(@Nullable P4ChangelistId changelistId) {
        if (changelistId == null) {
            return null;
        }
        return getState(changelistId);
    }

    @NotNull
    public static State getState(@NotNull P4ChangelistId id) {
        State ret = new State();
        ret.ref = ClientServerRefStore.getState(id.getClientServerRef());
        ret.id = id.getChangelistId();
        return ret;
    }

    @Nullable
    public static P4ChangelistId readNullable(@Nullable State state) {
        if (state == null) {
            return null;
        }
        return read(state);
    }

    @NotNull
    public static P4ChangelistId read(@NotNull State state) {
        return new P4ChangelistIdImpl(state.id, ClientServerRefStore.read(state.ref));
    }
}
