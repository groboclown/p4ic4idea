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

package net.groboclown.p4.server.impl.values;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import org.jetbrains.annotations.NotNull;

public class P4ChangelistIdImpl implements P4ChangelistId {
    private final int id;
    private final ClientServerRef ref;
    private final State state;

    public P4ChangelistIdImpl(int id, ClientServerRef ref) {
        this.id = id;
        this.ref = ref;
        state = id < -1
                ? State.PENDING_CREATION
                    : id == -1
                        ? State.DEFAULT
                        : State.NUMBERED;
    }

    @Override
    public int getChangelistId() {
        return id;
    }

    @NotNull
    @Override
    public P4ServerName getServerName() {
        return ref.getServerName();
    }

    @NotNull
    @Override
    public String getClientName() {
        return ref.getClientName();
    }

    @NotNull
    @Override
    public ClientServerRef getClientServerRef() {
        return ref;
    }

    @NotNull
    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isDefaultChangelist() {
        return state == State.DEFAULT;
    }

    @Override
    public boolean isIn(@NotNull ServerConfig serverConfig) {
        return serverConfig.getServerName().equals(getServerName());
    }

    @Override
    public String asString() {
        return Integer.toString(id);
    }

    @Override
    public int compareTo(@NotNull VcsRevisionNumber o) {
        if (o instanceof P4ChangelistId) {
            return id - ((P4ChangelistId) o).getChangelistId();
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof P4ChangelistId) {
            P4ChangelistId that = (P4ChangelistId) o;
            return that.getChangelistId() == id
                    && that.getClientServerRef().equals(ref);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id + ref.hashCode();
    }
}
