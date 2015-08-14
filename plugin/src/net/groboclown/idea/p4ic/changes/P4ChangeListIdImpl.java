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

package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.util.Comparing;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

class P4ChangeListIdImpl implements P4ChangeListId {
    private final int clid;

    @NotNull
    private final String scid;

    @NotNull
    private final String clientName;

    P4ChangeListIdImpl(@NotNull String serverConfigId, @NotNull String clientName, int clid) {
        this.clid = clid;
        this.scid = serverConfigId;
        this.clientName = clientName;
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }


    P4ChangeListIdImpl(@NotNull Client client, @NotNull IChangelistSummary summary) {
        this.clid = summary.getId();
        this.scid = client.getConfig().getServiceName();
        this.clientName = client.getClientName();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    P4ChangeListIdImpl(@NotNull ServerConfig config, @NotNull IChangelistSummary summary) {
        this.clid = summary.getId();
        this.scid = config.getServiceName();
        this.clientName = summary.getClientId();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    P4ChangeListIdImpl(@NotNull Client client, final int p4id) {
        this.clid = p4id;
        this.scid = client.getConfig().getServiceName();
        this.clientName = client.getClientName();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    @Override
    public int getChangeListId() {
        return clid;
    }

    @NotNull
    @Override
    public String getServerConfigId() {
        return scid;
    }

    @NotNull
    @Override
    public String getClientName() {
        return clientName;
    }

    @Override
    public boolean isNumberedChangelist() {
        return clid > 0;
    }

    @Override
    public boolean isDefaultChangelist() {
        return clid == P4ChangeListCache.P4_DEFAULT;
    }

    @Override
    public boolean isIn(@NotNull Client client) {
        return scid.equals(client.getConfig().getServiceName()) &&
                clientName.equals(client.getClientName());
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
                Comparing.equal(that.scid, this.scid) &&
                Comparing.equal(that.clientName, this.clientName);
    }

    @Override
    public int hashCode() {
        return clid + scid.hashCode();
    }

    @Override
    public String toString() {
        return getServerConfigId() + "/" + getClientName() + "@" + getChangeListId();
    }
}
