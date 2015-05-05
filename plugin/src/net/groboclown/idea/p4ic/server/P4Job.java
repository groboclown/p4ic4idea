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

package net.groboclown.idea.p4ic.server;

import com.perforce.p4java.core.IJob;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class P4Job implements Comparable<P4Job> {
    private final String jobId;
    private final String description;
    private final Map<String, Object> rawFields;

    public P4Job(@NotNull IJob job) {
        if (job.getId() == null) {
            throw new NullPointerException(P4Bundle.message("error.job.null"));
        }
        this.jobId = job.getId();
        this.description = job.getDescription();
        this.rawFields = job.getRawFields() == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(job.getRawFields());
    }

    public P4Job(@NotNull final String jobId, @Nullable final String errorMessage) {
        this.jobId = jobId;
        this.description = errorMessage;
        this.rawFields = Collections.emptyMap();
    }

    @NotNull
    public String getJobId() {
        return jobId;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NotNull
    public Map<String, Object> getRawFields() {
        return rawFields;
    }

    @Override
    public String toString() {
        return jobId;
    }

    @Override
    public int hashCode() {
        return jobId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass().equals(P4Job.class)) {
            return getJobId().equals(((P4Job) o).getJobId());
        }
        return false;
    }

    @Override
    public int compareTo(final P4Job o) {
        if (o == null) {
            return 1;
        }
        if (o == this) {
            return 0;
        }
        return getJobId().compareTo(o.getJobId());
    }
}
