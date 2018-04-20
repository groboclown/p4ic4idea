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

import com.perforce.p4java.core.IJob;
import org.jdom.Element;
import org.jdom.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class P4JobState extends CachedState {
    private final String id;
    private String description;
    private Map<String, String> details = new HashMap<String, String>();

    public P4JobState(@NotNull final String id) {
        this.id = id;
    }

    /*
    public P4JobState(final P4Job job) {
        this.id = job.getJobId();
        this.description = job.getDescription();
        for (Map.Entry<String, Object> entry: job.getRawFields().entrySet()) {
            if (entry.getValue() instanceof String && entry.getValue() != null &&
                    entry.getKey() != null) {
                details.put(entry.getKey(), entry.getValue().toStringList());
            }
        }
    }
    */

    public P4JobState(final IJob job) {
        this.id = job.getId();
        this.description = job.getDescription();
        if (job.getRawFields() != null) {
            for (Entry<String, Object> entry : job.getRawFields().entrySet()) {
                if (entry.getValue() instanceof String && entry.getValue() != null &&
                        entry.getKey() != null) {
                    details.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }

    public P4JobState(@NotNull final String jobId, @Nullable final String errorMessage) {
        this.id = jobId;
        this.description = errorMessage;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        serializeDate(wrapper);
        wrapper.setAttribute("n", id);
        if (description != null) {
            wrapper.addContent(new Text(description));
        }
        for (Entry<String, String> entry : details.entrySet()) {
            Element el = new Element("x");
            wrapper.addContent(el);
            el.setAttribute("k", entry.getKey());
            el.setAttribute("v", entry.getValue());
        }
    }

    @Nullable
    protected static P4JobState deserialize(@NotNull final Element wrapper, @NotNull final DecodeReferences refs) {
        String jobId = getAttribute(wrapper, "n");
        if (jobId == null) {
            return null;
        }
        P4JobState ret = new P4JobState(jobId);
        ret.deserializeDate(wrapper);
        ret.description = wrapper.getText();
        for (Element el: wrapper.getChildren("x")) {
            String k = getAttribute(el, "k");
            String v = getAttribute(el, "v");
            if (k != null && v != null) {
                ret.details.put(k, v);
            }
        }
        return ret;
    }
}
