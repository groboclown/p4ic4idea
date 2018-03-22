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

package net.groboclown.idea.p4ic.mock;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Responses {
    private final Map<P4Request, P4Response> mapping = new HashMap<P4Request, P4Response>();
    private final Set<P4Request> oneTimeMapping = new HashSet<P4Request>();

    public void addOnce(@NotNull P4Request request, @NotNull P4Response response) {
        add(request, response);
        oneTimeMapping.add(request);
    }

    public void add(@NotNull P4Request request, @NotNull P4Response response) {
        assertThat(mapping.containsKey(request), is(false));
        mapping.put(request, response);
    }

    @NotNull
    public P4Response pull(@NotNull P4Request request) {
        final P4Response response = mapping.get(request);
        assertThat("No registered request for " + request, response, not(nullValue()));
        if (oneTimeMapping.remove(request)) {
            mapping.remove(request);
        }
        return response;
    }
}
