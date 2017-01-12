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

package net.groboclown.idea.p4ic.config.part;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MutableCompositePart extends CompositePart {
    static final String TAG_NAME = "mutable-composite-part";
    static final ConfigPartFactory<MutableCompositePart> FACTORY = new Factory();

    private final List<ConfigPart> parts = new ArrayList<ConfigPart>();

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

    @NotNull
    @Override
    public Element marshal() {
        Element ret = new Element(TAG_NAME);
        for (ConfigPart part : parts) {
            CompositePart.marshalAppend(ret, part);
        }
        return ret;
    }

    private static class Factory extends ConfigPartFactory<MutableCompositePart> {
        @Override
        MutableCompositePart create(@NotNull Project project, @NotNull Element element) {
            MutableCompositePart ret = new MutableCompositePart();
            if (isTag(TAG_NAME, element)) {
                for (Element child : element.getChildren()) {
                    ret.addPriorityConfigPart(Unmarshal.from(project, child));
                }
            }
            return ret;
        }
    }
}
