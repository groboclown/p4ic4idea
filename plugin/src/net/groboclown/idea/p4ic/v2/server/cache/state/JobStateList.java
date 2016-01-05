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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JobStateList implements Iterable<P4JobState> {
    private final Map<String, P4JobState> jobs;
    private final Object sync = new Object();

    public JobStateList(@NotNull Collection<P4JobState> jobs) {
        this.jobs = new HashMap<String, P4JobState>();
        for (P4JobState job : jobs) {
            this.jobs.put(job.getId(), job);
        }
    }

    public JobStateList() {
        this.jobs = new HashMap<String, P4JobState>();
    }

    /**
     * Clear out the cached state
     */
    void flush() {
        synchronized (sync) {
            jobs.clear();
        }
    }

    @NotNull
    @Override
    public Iterator<P4JobState> iterator() {
        return copy().values().iterator();
    }

    @NotNull
    public Map<String, P4JobState> copy() {
        synchronized (sync) {
            return new HashMap<String, P4JobState>(jobs);
        }
    }

    public void add(@NotNull P4JobState job) {
        synchronized (sync) {
            jobs.put(job.getId(), job);
        }
    }

    @Nullable
    public P4JobState get(final String jobId) {
        synchronized (sync) {
            return jobs.get(jobId);
        }
    }

    public boolean remove(@NotNull final String jobId) {
        synchronized (sync) {
            return jobs.remove(jobId) != null;
        }
    }
}
