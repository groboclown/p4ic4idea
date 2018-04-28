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

package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import org.jetbrains.annotations.NotNull;

/**
 * An extension to the VcsRevisionNumber that allows for the revisions to
 * spread into the past history of the file across branches, rather than
 * limited to the current file depot location.
 */
public class P4ChangelistNumber implements VcsRevisionNumber {
    private final int changelist;

    public P4ChangelistNumber(@NotNull final IChangelist changelist) {
        this.changelist = changelist.getId();
    }

    public P4ChangelistNumber(@NotNull IChangelistSummary change) {
        this.changelist = change.getId();
    }

    public int getChangelist() {
        return changelist;
    }


    @Override
    public String asString() {
        return '@' + Integer.toString(changelist);
    }

    @Override
    public int compareTo(final VcsRevisionNumber o) {
        if (o != null && o instanceof P4ChangelistNumber) {
            P4ChangelistNumber that = (P4ChangelistNumber) o;
            return this.changelist - that.changelist;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || ! (obj instanceof VcsRevisionNumber)) {
            return false;
        }
        return compareTo((VcsRevisionNumber) obj) == 0;
    }

    @Override
    public int hashCode() {
        return changelist;
    }
}
