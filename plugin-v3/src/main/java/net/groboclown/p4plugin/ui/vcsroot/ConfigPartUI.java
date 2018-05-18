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

package net.groboclown.p4plugin.ui.vcsroot;

import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class ConfigPartUI<T extends ConfigPart> {
    private final T part;

    protected ConfigPartUI(T part) {
        this.part = part;
    }


    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    public abstract String getPartTitle();

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    public abstract String getPartDescription();

    @NotNull
    public T getPart() {
        return part;
    }

    @NotNull
    protected abstract T loadUIValuesIntoPart(@NotNull T part);

    public abstract JComponent getPanel();
}
