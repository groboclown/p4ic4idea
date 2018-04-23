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

package net.groboclown.p4.server.api.config.part;

import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SingleCompositePart extends CompositePart {
    private final ConfigPart config;

    public SingleCompositePart(@NotNull ConfigPart part) {
        this.config = part;
    }

    @Override
    public boolean reload() {
        return config.reload();
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return config.getConfigProblems();
    }

    @NotNull
    @Override
    public List<ConfigPart> getConfigParts() {
        return Collections.singletonList(config);
    }
}
