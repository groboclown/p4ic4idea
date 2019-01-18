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

import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import org.jetbrains.annotations.NotNull;

public class P4ChangelistSummaryImpl implements P4ChangelistSummary {
    private final P4ChangelistId id;
    private final String comment;
    private final String username;
    private final boolean submitted;
    private final boolean hasShelved;

    public P4ChangelistSummaryImpl(@NotNull ClientServerRef ref, @NotNull IChangelistSummary summary) {
        id = new P4ChangelistIdImpl(summary.getId(), ref);
        comment = summary.getDescription();
        submitted = summary.getStatus() == ChangelistStatus.SUBMITTED;
        hasShelved = summary.isShelved();
        username = summary.getUsername();
    }

    P4ChangelistSummaryImpl(P4LocalChangelist changelist) {
        id = changelist.getChangelistId();
        comment = changelist.getComment();
        submitted = false;
        hasShelved = !changelist.getShelvedFiles().isEmpty();
        username = changelist.getUsername();
    }

    /** Should only be used for serialization */
    public P4ChangelistSummaryImpl(@NotNull P4ChangelistId id, @NotNull String comment,
            @NotNull String username, boolean submitted, boolean hasShelved) {
        this.id = id;
        this.comment = comment;
        this.username = username;
        this.submitted = submitted;
        this.hasShelved = hasShelved;
    }


    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return id;
    }

    @NotNull
    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isSubmitted() {
        return submitted;
    }

    @Override
    public boolean isOnServer() {
        return id.getState() == P4ChangelistId.State.DEFAULT ||
                id.getState() == P4ChangelistId.State.NUMBERED;
    }

    @Override
    public boolean hasShelvedFiles() {
        return hasShelved;
    }

    @NotNull
    @Override
    public String getClientname() {
        return id.getClientname();
    }

    @NotNull
    @Override
    public String getUsername() {
        return username;
    }
}
