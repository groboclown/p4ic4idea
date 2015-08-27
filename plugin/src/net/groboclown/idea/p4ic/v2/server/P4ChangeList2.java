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

package net.groboclown.idea.p4ic.v2.server;

import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.server.P4Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A (possibly cached) representation of a Perforce changelist.  Updates must be done through
 * the {@link P4Server} class.
 */
public class P4ChangeList2 {
    private final P4ChangeListId id;
    private final Set<P4FileAction> files;
    private final String comment;
    private final String owner;
    private final List<P4Job> jobIds;
    private final Date lastUpdateTime = new Date();

    public P4ChangeList2(@NotNull final P4ChangeListId id, @NotNull final Collection<P4FileAction> files,
                        @Nullable final String comment, @Nullable final String owner,
                        @Nullable Collection<P4Job> jobIds) {
        this.id = id;
        this.files = Collections.unmodifiableSet(new HashSet<P4FileAction>(files));
        this.comment = comment;
        this.owner = owner;
        if (jobIds == null || jobIds.isEmpty()) {
            this.jobIds = Collections.emptyList();
        } else {
            this.jobIds = Collections.unmodifiableList(new ArrayList<P4Job>(jobIds));
        }
    }

    @NotNull
    public P4ChangeListId getId() {
        return id;
    }

    @NotNull
    public Set<P4FileAction> getFiles() {
        return files;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Nullable
    public String getOwner() {
        return owner;
    }

    @NotNull
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @NotNull
    public List<P4Job> getJobs() {
        return jobIds;
    }

    @Override
    public String toString() {
        return "[" + id + ": jobs " + jobIds + ", files " + files + "]";
    }
}
