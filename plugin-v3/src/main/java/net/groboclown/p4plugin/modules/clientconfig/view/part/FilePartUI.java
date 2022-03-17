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

package net.groboclown.p4plugin.modules.clientconfig.view.part;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.impl.config.part.FileConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigConnectionController;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigPartUI;
import net.groboclown.p4plugin.modules.clientconfig.view.ConfigPartUIFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class FilePartUI extends ConfigPartUI<FileConfigPart> {
    private static final String DEFAULT_FILE_NAME = ".p4config";
    public static final ConfigPartUIFactory FACTORY = new Factory();

    private JPanel rootPanel;
    private TextFieldWithBrowseButton fileLocation;


    private static class Factory implements ConfigPartUIFactory {
        @Nls
        @NotNull
        @Override
        public String getName() {
            return P4Bundle.getString("configuration.connection-choice.picker.p4config");
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return null;
        }

        @Nullable
        @Override
        public ConfigPartUI createForPart(ConfigPart part, ConfigConnectionController controller) {
            if (part instanceof FileConfigPart) {
                return new FilePartUI((FileConfigPart) part);
            }
            return null;
        }

        @NotNull
        @Override
        public ConfigPartUI createEmpty(@NotNull VirtualFile vcsRoot, ConfigConnectionController controller) {
            return new FilePartUI(new FileConfigPart(vcsRoot, VcsUtil.getVirtualFile(DEFAULT_FILE_NAME)));
        }
    }


    private FilePartUI(FileConfigPart part) {
        super(part);
        setupUI();

        VirtualFile path = part.getConfigFile();
        fileLocation.getTextField().setText(path == null ? null : path.getPath());
        fileLocation.addBrowseFolderListener(
                P4Bundle.message("configuration.connection-choice.picker.p4config"),
                P4Bundle.message("configuration.p4config.chooser"),
                null,
                new FileChooserDescriptor(true, false, false, false, false, false)
        );
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getPartTitle() {
        return P4Bundle.getString("configuration.connection-choice.picker.p4config");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getPartDescription() {
        return P4Bundle.getString("configuration.connection-choice.picker.p4config.description");
    }

    @NotNull
    @Override
    protected FileConfigPart loadUIValuesIntoPart(@NotNull FileConfigPart part) {
        part.setConfigFile(getSelectedLocation());
        return part;
    }

    @Override
    public JComponent getPanel() {
        return rootPanel;
    }

    @Nullable
    private String getSelectedLocation() {
        return fileLocation.getTextField().getText();
    }


    private void setupUI() {
        rootPanel = new JPanel(new BorderLayout());

        fileLocation = new TextFieldWithBrowseButton();
        // TODO make these keys conform to the standards.
        fileLocation.setText(P4Bundle.getString("config.file.location.tooltip"));
        fileLocation.setToolTipText(P4Bundle.getString("configuration.p4config.chooser"));
        fileLocation.setEditable(true);

        JLabel label = SwingUtil.createLabelFor(P4Bundle.getString("configuration.p4config"), fileLocation);
        label.setHorizontalAlignment(SwingConstants.TRAILING);

        rootPanel.add(label, BorderLayout.WEST);
        rootPanel.add(fileLocation, BorderLayout.CENTER);
    }
}
