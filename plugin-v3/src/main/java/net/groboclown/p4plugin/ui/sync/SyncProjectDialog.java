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

package net.groboclown.p4plugin.ui.sync;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.extension.P4Vcs;
import net.groboclown.p4plugin.ui.SwingUtil;
import net.groboclown.p4plugin.modules.clientconfig.view.P4VcsRootConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SyncProjectDialog {
    private final Project project;

    @Nls(capitalization = Nls.Capitalization.Sentence)
    private final String setupProblem;

    private final TextFieldWithBrowseButton directoryField;
    private final DialogBuilder dialog;
    private final P4VcsRootConfigurable configurable;
    private final VcsDirectoryMapping directoryMapping;


    public SyncProjectDialog(@NotNull Project project) {
        this.project = project;


        this.directoryMapping = createDirectoryMapping(project);
        final UnnamedConfigurable configurable =
                P4Vcs.getInstance(project).getRootConfigurable(directoryMapping);
        if (!(configurable instanceof P4VcsRootConfigurable)) {
            this.setupProblem = P4Bundle.getString("checkout.config.error.invalid-project");
            this.directoryField = null;
            this.dialog = null;
            this.configurable = null;
            return;
        }
        this.configurable = (P4VcsRootConfigurable) configurable;

        JComponent component = configurable.createComponent();
        if (component == null) {
            this.setupProblem = P4Bundle.getString("checkout.config.error.invalid-project");
            this.directoryField = null;
            this.dialog = null;
            return;
        }

        this.setupProblem = null;
        JPanel dirPanel = new JPanel(new BorderLayout());
        this.directoryField = new TextFieldWithBrowseButton();
        dirPanel.add(directoryField, BorderLayout.CENTER);
        directoryField.setButtonEnabled(true);
        directoryField.addBrowseFolderListener(
                P4Bundle.getString("checkout.config.directory.chooser.title"),
                P4Bundle.getString("checkout.config.directory.chooser.desc"),
                project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );
        JLabel dirLabel = SwingUtil.createLabelFor(P4Bundle.getString("checkout.config.directory"), directoryField);
        dirPanel.add(dirLabel, BorderLayout.WEST);

        this.dialog = new DialogBuilder(project)
                .centerPanel(component)
                .title(P4Bundle.getString("checkout.config.title"));
        dialog.setNorthPanel(dirPanel);
    }

    @Nullable
    public ClientConfig showAndGet() {
        if (setupProblem != null) {
            Messages.showErrorDialog(project, setupProblem, P4Bundle.getString("checkout.config.error.title"));
            return null;
        }

        if (dialog == null || configurable == null || !dialog.showAndGet()) {
            // user pressed cancel - no error.
            return null;
        }

        // Internal settings check
        VirtualFile baseDir = getDirectory();
        if (baseDir == null) {
            notifyError(P4Bundle.getString("checkout.config.error.no-directory"));
            return null;
        }
        try {
            configurable.apply();
        } catch (ConfigurationException e) {
            notifyError(e.getMessage());
            return null;
        }

        ClientConfig config = configurable.loadConfigFromSettings();
        if (config == null || config.getClientname() == null) {
            notifyError(P4Bundle.getString("checkout.config.error.bad-config"));
            return null;
        }

        // Setup the directory mapping and the configuration.
        // TODO the project object passed in to this method is the project where the action initiated.
        // If it was done from an open project, then it will be that one.  If it's from the no-project
        // select-a-project dialog, then it will be the default project.  In either case, we *cannot*
        // add this new configuration directory to the project.  This is something the user will need
        // to do again.
        /*
        VcsDirectoryMapping mapping = ProjectLevelVcsManager.getInstance(project)
                .getDirectoryMappingFor(VcsUtil.getFilePath(baseDir));
        if (mapping == null) {
            List<VcsDirectoryMapping> mappings = new ArrayList<>(ProjectLevelVcsManager.getInstance(project)
                    .getDirectoryMappings());
            mapping = new VcsDirectoryMapping(baseDir.getPath(), P4VcsKey.VCS_NAME, null);
            mappings.add(mapping);
            ProjectLevelVcsManager.getInstance(project).setDirectoryMappings(mappings);
        }
        mapping.setRootSettings(directoryMapping.getRootSettings());
        */

        return config;
    }

    @Nullable
    public VirtualFile getDirectory() {
        if (directoryField == null) {
            return null;
        }
        String text = directoryField.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        final File parent = new File(text);
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        VirtualFile file = lfs.findFileByIoFile(parent);
        if (file == null) {
            file = lfs.refreshAndFindFileByIoFile(parent);
        }
        return file;
    }

    private void notifyError(@Nls(capitalization = Nls.Capitalization.Sentence) String message) {
        VcsNotifier.getInstance(project).notifyError(null, P4Bundle.getString("checkout.config.error.title"), message);
    }

    @NotNull
    private static VcsDirectoryMapping createDirectoryMapping(@NotNull Project project) {
        // The directory doesn't really matter for this.  We'll be using the directory the user
        // specifies in the dialog.
        String path = ".";
        if (project.getBasePath() != null) {
            path = project.getBasePath();
        }
        return new VcsDirectoryMapping(path, P4VcsKey.VCS_NAME);
    }
}
