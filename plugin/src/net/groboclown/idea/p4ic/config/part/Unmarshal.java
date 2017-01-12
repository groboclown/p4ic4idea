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
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Unmarshal {
    private static final Map<String, ConfigPartFactory<?>> CONFIG_PART_FACTORY_TAGS;

    @NotNull
    public static ConfigPart from(@NotNull Project project, @NotNull Element element) {
        ConfigPartFactory factory = CONFIG_PART_FACTORY_TAGS.get(element.getName());
        if (factory == null) {
            return new MutableCompositePart();
        }
        return factory.create(project, element);
    }


    static {
        Map<String, ConfigPartFactory<?>> m = new HashMap<String, ConfigPartFactory<?>>();

        m.put(EnvCompositePart.TAG_NAME, EnvCompositePart.FACTORY);
        m.put(FileDataPart.TAG_NAME, FileDataPart.FACTORY);
        m.put(MutableCompositePart.TAG_NAME, MutableCompositePart.FACTORY);
        m.put(RelativeConfigCompositePart.TAG_NAME, RelativeConfigCompositePart.FACTORY);
        m.put(RequirePasswordDataPart.TAG_NAME, RequirePasswordDataPart.FACTORY);
        m.put(SimpleDataPart.TAG_NAME, SimpleDataPart.FACTORY);
        m.put(SingleCompositePart.TAG_NAME, SingleCompositePart.FACTORY);

        CONFIG_PART_FACTORY_TAGS = Collections.unmodifiableMap(m);
    }
}
