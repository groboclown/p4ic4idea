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

package net.groboclown.p4.server.impl.cache.store;

import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class P4JobStore {
    @SuppressWarnings("WeakerAccess")
    public static class State {
        public String jobId;
        public String description;
        public Map<String, Object> details;
    }

    @NotNull
    public static State getState(@NotNull P4Job job) {
        State ret = new State();
        ret.jobId = job.getJobId();
        ret.description = job.getDescription();
        ret.details = new HashMap<>(job.getRawDetails());
        return ret;
    }

    @NotNull
    public static P4Job read(@NotNull State state) {
        return new P4JobImpl(
                state.jobId, state.description, state.details
        );
    }
}
