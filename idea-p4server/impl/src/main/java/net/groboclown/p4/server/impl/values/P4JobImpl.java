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

import com.perforce.p4java.core.IJob;
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class P4JobImpl implements P4Job {
    private final String jobId;
    private final String description;
    private final Map<String, Object> details;

    public P4JobImpl(@NotNull IJob job) {
        jobId = job.getId();
        description = job.getDescription();
        HashMap<String, Object> fields = new HashMap<>();
        for (Map.Entry<String, Object> entry : job.getRawFields().entrySet()) {
            fields.put(entry.getKey(), entry.getValue());
        }
        details = Collections.unmodifiableMap(fields);
    }

    public P4JobImpl(@NotNull String jobId, @NotNull String description, @Nullable Map<String, Object> details) {
        this.jobId = jobId;
        this.description = description;
        if (details == null) {
            this.details = new HashMap<>();
        } else {
            this.details = new HashMap<>(details);
        }
    }

    @NotNull
    @Override
    public String getJobId() {
        return jobId;
    }

    @NotNull
    @Override
    public Map<String, Object> getRawDetails() {
        return details;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }
}
