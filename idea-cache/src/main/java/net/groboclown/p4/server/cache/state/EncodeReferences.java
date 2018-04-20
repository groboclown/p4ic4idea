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

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Shared cache of the generic objects that are at a top level, but the
 * lower-level objects may reference.
 */
public class EncodeReferences {
    private final Map<P4ClientFileMapping, String> fileMappings = new HashMap<P4ClientFileMapping, String>();
    private final Map<P4JobState, String> jobs = new HashMap<P4JobState, String>();

    @NotNull
    public String getFileMappingId(@Nullable P4ClientFileMapping file) {
        if (file == null) {
            return "";
        }
        String ret = fileMappings.get(file);
        if (ret == null) {
            ret = CachedState.encodeLong(fileMappings.size());
            fileMappings.put(file, ret);
        }
        return ret;
    }

    @NotNull
    public String getJobId(@Nullable P4JobState job) {
        if (job == null) {
            return "";
        }
        String ret = jobs.get(job);
        if (ret == null) {
            // Do not use the job ID; instead, make a custom
            // number (this is shorter and uses less space)
            ret = CachedState.encodeLong(jobs.size());
            jobs.put(job, ret);
        }
        return ret;
    }

    void serialize(@NotNull Element parent) {
        // NOTE: order is very important here.
        for (Entry<P4JobState, String> entry : jobs.entrySet()) {
            Element el = new Element("j");
            parent.addContent(el);
            el.setAttribute("k", entry.getValue());
            entry.getKey().serialize(el, this);
        }

        // We always encode the file mappings last, because other encodings may add values to it.
        for (Entry<P4ClientFileMapping, String> entry : fileMappings.entrySet()) {
            Element el = new Element("m");
            parent.addContent(el);
            el.setAttribute("k", entry.getValue());
            entry.getKey().serialize(el);
        }
    }
}
