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

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.groboclown.idea.p4ic.v2.server.cache.state.CachedState.getAttribute;

public class DecodeReferences {
    private final Map<String, P4ClientFileMapping> fileMappings = new HashMap<String, P4ClientFileMapping>();
    private final Map<String, P4JobState> jobs = new HashMap<String, P4JobState>();

    @Nullable
    public P4ClientFileMapping getFileMapping(@Nullable String id) {
        return fileMappings.get(id);
    }

    @Nullable
    public P4JobState getJob(@Nullable String id) {
        return jobs.get(id);
    }

    Collection<P4ClientFileMapping> getFileMappings() {
        return Collections.unmodifiableCollection(fileMappings.values());
    }

        static DecodeReferences deserialize(@NotNull Element parent) {
                // NOTE: order is very important here.  It is the opposite of the serialize
                DecodeReferences ret = new DecodeReferences();

                for (Element el : parent.getChildren("m")) {
                        String key = getAttribute(el, "k");
                        if (key != null) {
                                P4ClientFileMapping val = P4ClientFileMapping.deserialize(el);
                                ret.fileMappings.put(key, val);
                        }
                }

                for (Element el : parent.getChildren("j")) {
                        String key = getAttribute(el, "k");
                        if (key != null) {
                                P4JobState val = P4JobState.deserialize(el, ret);
                                ret.jobs.put(key, val);
                        }
                }

                return ret;
        }
}
