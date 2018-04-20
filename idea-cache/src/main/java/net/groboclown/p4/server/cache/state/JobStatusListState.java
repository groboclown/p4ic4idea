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

package net.groboclown.p4.server.cache.state;

import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The valid job statuses that can be set for a job.
 */
public class JobStatusListState extends CachedState {
    private final List<String> statuses;

    public JobStatusListState(@NotNull final List<String> statuses) {
        this.statuses = new ArrayList<String>(statuses);
    }

    public JobStatusListState() {
        this.statuses = new ArrayList<String>(P4ChangeListJob.DEFAULT_JOB_STATUS);
    }

    public void setJobStatuses(@Nullable List<String> jobStatusValues) {
        statuses.clear();
        if (jobStatusValues == null || jobStatusValues.isEmpty()) {
            statuses.addAll(P4ChangeListJob.DEFAULT_JOB_STATUS);
        } else {
            statuses.addAll(jobStatusValues);
        }
    }

    @NotNull
    public List<String> getJobStatuses() {
        return Collections.unmodifiableList(statuses);
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        serializeDate(wrapper);
        for (String status: statuses) {
            Element el = new Element("s");
            wrapper.addContent(el);
            el.setAttribute("s", status);
        }
    }

    @SuppressWarnings("UnusedParameters")
    @Nullable
    protected static JobStatusListState deserialize(@NotNull final Element wrapper, @NotNull final DecodeReferences refs) {
        List<String> statuses = new ArrayList<String>();
        for (Element el : wrapper.getChildren("s")) {
            String status = getAttribute(el, "s");
            if (status != null && status.length() > 0) {
                statuses.add(status);
            }
        }
        if (statuses.isEmpty()) {
            return new JobStatusListState(P4ChangeListJob.DEFAULT_JOB_STATUS);
        }
        return new JobStatusListState(statuses);
    }
}
