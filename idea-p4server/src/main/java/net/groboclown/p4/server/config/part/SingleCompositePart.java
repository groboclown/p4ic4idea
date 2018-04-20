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

package net.groboclown.p4.server.config.part;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SingleCompositePart extends CompositePart {
    static final String TAG_NAME = "single-composite-part";
    static final ConfigPartFactory<SingleCompositePart> FACTORY = new Factory();

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

    @NotNull
    @Override
    public Element marshal() {
        Element ret = new Element(TAG_NAME);
        CompositePart.marshalAppend(ret, config);
        return ret;
    }

    private static class Factory extends ConfigPartFactory<SingleCompositePart> {
        @Override
        SingleCompositePart create(@NotNull Project project, @NotNull Element element) {
            ConfigPart child;
            List<Element> children = element.getChildren();
            if (children.isEmpty()) {
                child = new SimpleDataPart(project, Collections.<String, String>emptyMap());
            } else {
                child = Unmarshal.from(project, children.get(0));
            }
            return new SingleCompositePart(child);
        }
    }
}
