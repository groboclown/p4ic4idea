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
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.ServerFingerprintDataPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import net.groboclown.p4plugin.ui.vcsroot.ConfigConnectionController;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUI;
import net.groboclown.p4plugin.ui.vcsroot.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ServerFingerprintPartUI extends ConfigPartUI<ServerFingerprintDataPart> {
    private JComponent root;
    private JTextField fingerprintField;
    public static final ConfigPartUIFactory FACTORY = new Factory();

    private static class Factory implements ConfigPartUIFactory {
        @Nls(capitalization = Nls.Capitalization.Title)
        @NotNull
        @Override
        public String getName() {
            return P4Bundle.getString("configuration.stack.server-fingerprint.title");
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }

        @Nullable
        @Override
        public ConfigPartUI createForPart(ConfigPart part, ConfigConnectionController controller) {
            if (part instanceof ServerFingerprintDataPart) {
                return new ServerFingerprintPartUI((ServerFingerprintDataPart) part);
            }
            return null;
        }

        @NotNull
        @Override
        public ConfigPartUI createEmpty(@NotNull VirtualFile vcsRoot, ConfigConnectionController controller) {
            return new ServerFingerprintPartUI(new ServerFingerprintDataPart(getName()));
        }
    }


    private ServerFingerprintPartUI(ServerFingerprintDataPart part) {
        super(part);
        setupUI();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getPartTitle() {
        return P4Bundle.getString("configuration.stack.server-fingerprint.title");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getPartDescription() {
        return P4Bundle.getString("configuration.stack.server-fingerprint.description");
    }

    @NotNull
    @Override
    protected ServerFingerprintDataPart loadUIValuesIntoPart(@NotNull ServerFingerprintDataPart part) {
        part.setServerFingerprint(fingerprintField.getText());
        return part;
    }

    @Override
    public JComponent getPanel() {
        return root;
    }

    private void setupUI() {
        root = new JPanel();
        root.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:noGrow"));
        fingerprintField = new JTextField();
        JLabel label = SwingUtil.createLabelFor(
                P4Bundle.getString("configuration.properties.serverfingerprint.label"),
                fingerprintField);
        CellConstraints cc = new CellConstraints();
        root.add(label, cc.xy(1, 1));
        root.add(fingerprintField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
    }
}
