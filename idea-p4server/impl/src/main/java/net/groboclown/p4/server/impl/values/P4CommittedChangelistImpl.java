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

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.changes.Change;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4CommittedChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class P4CommittedChangelistImpl implements P4CommittedChangelist {
    private final P4ChangelistSummary summary;
    private final List<Change> changes;
    private final Date commitDate;
    private AbstractVcs vcs;

    public P4CommittedChangelistImpl(@NotNull P4ChangelistSummary summary,
            Collection<Change> changes, Date commitDate) {
        this.summary = summary;
        // The list of changes MUST be modifiable (the IDE calls sort on it)
        this.changes = new ArrayList<>(changes);
        this.commitDate = commitDate;
    }

    @NotNull
    @Override
    public P4ChangelistSummary getSummary() {
        return summary;
    }

    @Override
    public void setVcs(@Nullable AbstractVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public String getCommitterName() {
        return summary.getUsername();
    }

    @Override
    public Date getCommitDate() {
        return commitDate;
    }

    @Override
    public long getNumber() {
        return summary.getChangelistId().getChangelistId();
    }

    @Nullable
    @Override
    public String getBranch() {
        return null;
    }

    @Override
    public AbstractVcs getVcs() {
        return vcs;
    }

    @Override
    public Collection<Change> getChangesWithMovedTrees() {
        // Not supported in 17.3 or later.
        // return CommittedChangeListImpl.getChangesWithMovedTreesImpl(this);
        return getChanges();
    }

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    public void setDescription(String s) {
        // ignore
    }

    @Override
    public Collection<Change> getChanges() {
        return changes;
    }

    @NotNull
    @Override
    public String getName() {
        // TODO truncate the name?
        return summary.getComment();
    }

    @Override
    public String getComment() {
        return summary.getComment();
    }

    @Override
    public String toString() {
        return summary.getChangelistId().toString();
    }
}
