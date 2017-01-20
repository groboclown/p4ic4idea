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

import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * A Perforce Job associated with a changelist.  Immutable.
 */
public final class P4ChangeListJob implements Comparable<P4ChangeListJob> {
    // Default list of status, in case of a problem.
    public static final List<String> DEFAULT_JOB_STATUS = Arrays.asList(
            "open", "suspended", "closed"
    );

    private final ClientServerRef clientServerRef;
    private final P4JobState job;

    public P4ChangeListJob(@NotNull P4ChangeListValue change, @NotNull P4JobState job) {
        this.clientServerRef = change.getClientServerRef();
        this.job = job;
    }

    public P4ChangeListJob(@NotNull ClientServerRef clientServerRef, @NotNull P4JobState job) {
        this.clientServerRef = clientServerRef;
        this.job = job;
    }


    @NotNull
    public String getJobId() {
        return job.getId();
    }


    @Nullable
    public String getDescription() {
        return job.getDescription();
    }


    @Override
    public int compareTo(final P4ChangeListJob o) {
        return job.getId().compareTo(o.job.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof P4ChangeListJob) {
            P4ChangeListJob that = (P4ChangeListJob) o;
            return clientServerRef.equals(that.clientServerRef) &&
                    job.getId().equals(that.job.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (clientServerRef.hashCode() << 2) +
                job.getId().hashCode();
    }
}
