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

import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.JobStatusNames;
import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.api.values.P4JobSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JobStatusNamesImpl implements JobStatusNames {
    // Default list of status, as per the Perforce docs.
    public static final JobStatusNames DEFAULT_STATUSES = new JobStatusNamesImpl(
            new JobStatusImpl("open"), new JobStatusImpl("suspended"), new JobStatusImpl("closed"));
    private static final String STATUS_FIELD_NAME = "Status";

    private final Set<JobStatus> statuses;

    @NotNull
    public static JobStatusNames load(P4JobSpec spec) {
        for (P4JobField field : spec.getFields()) {
            if (STATUS_FIELD_NAME.equalsIgnoreCase(field.getName())) {
                return new JobStatusNamesImpl(field.getSelectValues().stream()
                    .map(JobStatusImpl::new)
                    .collect(Collectors.toList()));
            }
        }
        return DEFAULT_STATUSES;
    }

    public JobStatusNamesImpl(JobStatus... names) {
        this.statuses = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    public JobStatusNamesImpl(Collection<JobStatus> names) {
        this.statuses = Collections.unmodifiableSet(new HashSet<>(names));
    }

    @NotNull
    @Override
    public Set<JobStatus> getJobStatusNames() {
        return statuses;
    }

    @Nullable
    @Override
    public JobStatus toJobStatus(String name) {
        if (name == null) {
            return null;
        }
        String n = name.toLowerCase().trim();
        for (JobStatus status : statuses) {
            String js = status.getName().toLowerCase().trim();
            if (js.equals(n)) {
                return status;
            }
        }
        return null;
    }
}
