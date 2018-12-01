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

package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.util.Comparing;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.P4ServerName;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;

public final class P4ChangeListIdImpl implements P4ChangeListId {
    private final int clid;

    @NotNull
    private final ClientServerRef id;

    @NotNull
    private final String clientId;

    public P4ChangeListIdImpl(@NotNull ClientServerRef clientServerRef, int clid) {
        if (clientServerRef.getClientName() == null) {
            throw new NullPointerException("client id null");
        }
        this.id = clientServerRef;
        this.clid = clid;
        this.clientId = clientServerRef.getClientName();
        assert clid >= P4ChangeListId.P4_DEFAULT || clid <= P4ChangeListId.P4_LOCAL;
    }

    @Override
    public int getChangeListId() {
        return clid;
    }

    @NotNull
    @Override
    public P4ServerName getServerName() {
        return id.getServerName();
    }

    @NotNull
    @Override
    public String getClientName() {
        return clientId;
    }

    @NotNull
    @Override
    public ClientServerRef getClientServerRef() {
        return id;
    }

    @Override
    public boolean isNumberedChangelist() {
        return clid > 0;
    }

    @Override
    public boolean isDefaultChangelist() {
        return clid == P4ChangeListId.P4_DEFAULT;
    }

    @Override
    public boolean isUnsynchedChangelist() {
        return clid <= P4ChangeListId.P4_LOCAL;
    }

    @Override
    public boolean isIn(@NotNull P4Server client) {
        return id.equals(client.getClientServerId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o.getClass().equals(P4ChangeListIdImpl.class))) {
            return false;
        }
        if (o == this) {
            return true;
        }
        P4ChangeListIdImpl that = (P4ChangeListIdImpl) o;
        // due to the dynamic loading nature of this class, there are some
        // weird circumstances where the scid and client name can be null.
        return that.clid == this.clid &&
                Comparing.equal(that.id, this.id);
    }

    @Override
    public int hashCode() {
        return clid + id.hashCode();
    }

    @Override
    public String toString() {
        return getClientServerRef() + "@" + getChangeListId();
    }
}
