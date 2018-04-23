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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MutableCompositePart extends CompositePart {
    private final List<ConfigPart> parts = new ArrayList<ConfigPart>();

    public MutableCompositePart(@NotNull ConfigPart... parts) {
        this.parts.addAll(Arrays.asList(parts));
    }

    @Override
    public boolean reload() {
        boolean ret = true;
        for (ConfigPart part : parts) {
            ret &= part.reload();
        }
        return ret;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        List<ConfigProblem> ret = new ArrayList<ConfigProblem>();
        for (ConfigPart part : parts) {
            ret.addAll(part.getConfigProblems());
        }
        return ret;
    }

    @NotNull
    @Override
    public List<ConfigPart> getConfigParts() {
        return Collections.unmodifiableList(new ArrayList<ConfigPart>(parts));
    }

    public void addPriorityConfigPart(@NotNull ConfigPart part) {
        parts.add(0, part);
    }

    public void addConfigPart(@NotNull ConfigPart part) {
        parts.add(part);
    }

    public boolean removeConfigPart(@Nullable ConfigPart part) {
        return parts.remove(part);
    }

    public void clear() {
        parts.clear();
    }
}
