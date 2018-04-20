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

import net.groboclown.p4.server.config.ConfigProblem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A
 */
public abstract class CompositePart implements ConfigPart {
    static void marshalAppend(@NotNull Element parent, @Nullable ConfigPart part) {
        if (part != null && ! (part instanceof DefaultDataPart)) {
            // DefaultDataPart is NEVER marshalled.
            final Element child = part.marshal();
            parent.addContent(child);
        }
    }

    @NotNull
    public abstract List<ConfigPart> getConfigParts();

    public boolean hasError() {
        for (ConfigProblem configProblem : getConfigProblems()) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }
}
