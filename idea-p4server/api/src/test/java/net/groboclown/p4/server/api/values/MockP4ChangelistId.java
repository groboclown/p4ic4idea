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

package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public class MockP4ChangelistId implements P4ChangelistId {
    private ClientServerRef csRef;
    private int id;
    private State state;

    public MockP4ChangelistId(@NotNull ClientServerRef ref) {
        csRef = ref;
        id = -1;
        state = State.DEFAULT;
    }

    public MockP4ChangelistId(@NotNull ClientServerRef ref, int id) {
        csRef = ref;
        id = id;
        state =
                id == -1
                        ? State.DEFAULT
                        : (id > 0
                                ? State.NUMBERED
                                : State.PENDING_CREATION);
    }

    public MockP4ChangelistId(@NotNull ClientServerRef ref, int id, State state) {
        csRef = ref;
        this.id = id;
        this.state = state;
    }

    @Override
    public int getChangelistId() {
        return id;
    }

    @NotNull
    @Override
    public P4ServerName getServerName() {
        return csRef.getServerName();
    }

    @NotNull
    @Override
    public String getClientName() {
        return csRef.getClientName();
    }

    @NotNull
    @Override
    public ClientServerRef getClientServerRef() {
        return csRef;
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
        return serverConfig.getServerName().equals(csRef.getServerName());
    }

    @Override
    public String asString() {
        return csRef + ":" + id;
    }

    @Override
    public int compareTo(@NotNull VcsRevisionNumber o) {
        if (o instanceof P4ChangelistId) {
            return id - ((P4ChangelistId) o).getChangelistId();
        }
        return -1;
    }
}
