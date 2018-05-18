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

package net.groboclown.p4plugin.ui.vcsroot.part;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.EnvCompositePart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.vcsroot.ConfigConnectionController;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUI;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EnvPartUI
        extends ConfigPartUI<EnvCompositePart> {
    private JComponent panel = new JPanel();

    public static final ConfigPartUIFactory FACTORY = new Factory();

    private static class Factory implements ConfigPartUIFactory {
        @Nls
        @NotNull
        @Override
        public String getName() {
            return P4Bundle.getString("configuration.connection-choice.picker.env");
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }

        @Nullable
        @Override
        public ConfigPartUI createForPart(ConfigPart part, ConfigConnectionController controller) {
            if (part instanceof EnvCompositePart) {
                return new EnvPartUI((EnvCompositePart) part);
            }
            return null;
        }

        @NotNull
        @Override
        public ConfigPartUI createEmpty(@NotNull VirtualFile vcsRoot, ConfigConnectionController controller) {
            return new EnvPartUI(new EnvCompositePart(vcsRoot));
        }
    }

    private EnvPartUI(EnvCompositePart part) {
        super(part);
    }

    @Nls
    @NotNull
    @Override
    public String getPartTitle() {
        return P4Bundle.getString("configuration.connection-choice.picker.env");
    }

    @Nls
    @NotNull
    @Override
    public String getPartDescription() {
        return P4Bundle.getString("connection.env.description");
    }

    @NotNull
    @Override
    protected EnvCompositePart loadUIValuesIntoPart(@NotNull EnvCompositePart part) {
        return part;
    }

    @Override
    public JComponent getPanel() {
        return panel;
    }
}
