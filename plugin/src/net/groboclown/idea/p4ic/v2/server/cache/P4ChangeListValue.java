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

import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListIdImpl;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ChangeListState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * Immutable view of a changelist state.
 */
public class P4ChangeListValue {
    private final ClientServerRef clientServerRef;
    private final P4ChangeListState state;
    private P4ChangeListId id;

    public P4ChangeListValue(@NotNull ClientServerRef clientServerRef, @NotNull P4ChangeListState state) {
        this.clientServerRef = clientServerRef;
        this.state = state;
    }

    public P4ChangeListValue(final ClientServerRef clientServerRef, final boolean isServerDefaultChange) {
        assert isServerDefaultChange;
        this.clientServerRef = clientServerRef;
        this.state = new P4ChangeListState(new ChangelistSummary(
                P4ChangeListId.P4_DEFAULT, clientServerRef.getClientName(), null,
                ChangelistStatus.NEW, new Date(), "", false));
    }


    public ClientServerRef getClientServerRef() {
        return clientServerRef;
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

    @NotNull
    public Collection<P4JobState> getJobStates() {
        return state.getJobs();
    }

    @NotNull
    public Collection<P4ShelvedFile> getShelved() {
        return state.getShelved();
    }

    @Override
    public int hashCode() {
        return clientServerRef.hashCode() + state.hashCode();
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
            return (that.clientServerRef.equals(this.clientServerRef) &&
                that.state.getChangelistId() == this.state.getChangelistId());
        }
        return false;
    }

    public boolean isDefaultChangelist() {
        return state.isDefault();
    }

    // TODO is this still necessary
    public synchronized P4ChangeListId getIdObject() {
        if (id == null) {
            id = new P4ChangeListIdImpl(clientServerRef, getChangeListId());
        }
        return id;
    }

    @Override
    public String toString() {
        return clientServerRef + "@" + getChangeListId();
    }
}
