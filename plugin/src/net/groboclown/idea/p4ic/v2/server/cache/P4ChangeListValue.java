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

package net.groboclown.idea.p4ic.v2.server.cache;

import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListIdImpl;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ChangeListState;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable view of a changelist state.
 */
public class P4ChangeListValue {
    private final ClientServerId clientServerId;
    private final P4ChangeListState state;
    private P4ChangeListId id;

    public P4ChangeListValue(@NotNull ClientServerId clientServerId, @NotNull P4ChangeListState state) {
        this.clientServerId = clientServerId;
        this.state = state;
    }

    public P4ChangeListValue(@NotNull ClientServerId clientServerId, int dummyChangeListId) {
        this.clientServerId = clientServerId;
        this.state = new P4ChangeListState(dummyChangeListId);
    }


    public ClientServerId getClientServerId() {
        return clientServerId;
    }


    public int getChangeListId() {
        return state.getChangelistId();
    }


    public String getComment() {
        return state.getComment();
    }

    public boolean isDeleted() {
        return state.isDeleted();
    }

    public boolean isOnServer() {
        return state.isOnServer();
    }


    @Override
    public int hashCode() {
        return clientServerId.hashCode() + state.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass().equals(getClass())) {
            P4ChangeListValue that = (P4ChangeListValue) obj;
            return (that.clientServerId.equals(this.clientServerId) &&
                that.state.getChangelistId() == this.state.getChangelistId());
        }
        return false;
    }

    public boolean isDefaultChangelist() {
        return state.isDefault();
    }

    public synchronized P4ChangeListId getIdObject() {
        if (id == null) {
            id = new P4ChangeListIdImpl(clientServerId, getChangeListId());
        }
        return id;
    }

    @Override
    public String toString() {
        return clientServerId + "@" + getChangeListId();
    }
}
