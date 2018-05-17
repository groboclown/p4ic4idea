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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class P4VcsRootConfigurable implements UnnamedConfigurable {
    private static final Logger LOG = Logger.getInstance(P4VcsRootConfigurable.class);

    private final Project project;
    private final VirtualFile vcsRoot;
    private final VcsDirectoryMapping mapping;
    private P4RootConfigPanel panel;
    private Controller controller;

    public P4VcsRootConfigurable(Project project, VcsDirectoryMapping mapping) {
        this.project = project;
        this.vcsRoot = VcsUtil.getVirtualFile(mapping.getDirectory());
        this.mapping = mapping;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        controller = new Controller(project);
        panel = new P4RootConfigPanel(vcsRoot, controller);
        controller.configPartContainer = panel;
        reset();
        return panel.getRootPane();
    }

    @Override
    public boolean isModified() {
        return panel.isModified(loadPartsFromSettings());
    }

    @Override
    public void apply()
            throws ConfigurationException {
        if (isModified()) {
            ClientConfig oldConfig = loadConfigFromSettings();
            P4VcsRootSettings settings = new P4VcsRootSettingsImpl();
            List<ConfigPart> parts = panel.getConfigParts();
            settings.setConfigParts(parts);
            mapping.setRootSettings(settings);
            MultipleConfigPart parentPart = new MultipleConfigPart(
                    // FIXME set the right string
                    P4Bundle.getString("???"),
                    parts
            );
            if (parentPart.hasError()) {
                Collection<ConfigProblem> problems =
                        parentPart.getConfigProblems();
                // FIXME throw the error
            }
            try {
                ServerConfig serverConfig = ServerConfig.createFrom(parentPart);
                ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, parentPart);
                if (oldConfig != null) {
                    ClientConfigRemovedMessage.reportClientConfigRemoved(project, this, oldConfig, vcsRoot);
                }
                ClientConfigAddedMessage.send(project).clientConfigurationAdded(clientConfig);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        e.getLocalizedMessage(),
                        P4Bundle.getString("configuration.error.title")
                );
            }
        }
    }

    @Override
    public void reset() {
        List<ConfigPart> parts = loadPartsFromSettings();
        panel.setConfigParts(parts);
        controller.refreshConfigConnection();
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        controller = null;
    }

    private List<ConfigPart> loadPartsFromSettings() {
        P4VcsRootSettings settings = getRootSettings();
        return settings.getConfigParts();
    }

    @Nullable
    private ClientConfig loadConfigFromSettings() {
        List<ConfigPart> parts = loadPartsFromSettings();
        MultipleConfigPart parentPart = new MultipleConfigPart(
                P4Bundle.getString("???"),
                parts
        );
        if (!parentPart.hasError()) {
            try {
                ServerConfig serverConfig = ServerConfig.createFrom(parentPart);
                return ClientConfig.createFrom(serverConfig, parentPart);
            } catch (IllegalArgumentException e) {
                LOG.info("Should have not caused an error due to previous error check", e);
            }
        }
        return null;
    }

    @NotNull
    private P4VcsRootSettings getRootSettings() {
        VcsRootSettings rawSettings = mapping.getRootSettings();
        if (rawSettings == null) {
            P4VcsRootSettingsImpl ret = new P4VcsRootSettingsImpl();
            mapping.setRootSettings(ret);
            return ret;
        }
        if (!(rawSettings instanceof P4VcsRootSettings)) {
            throw new IllegalStateException("Invalid plugin root settings class; expected " +
                    P4VcsRootSettings.class + ", found " + rawSettings.getClass());
        }
        return (P4VcsRootSettings) rawSettings;
    }

    private static class Controller extends ConfigConnectionController {
        P4RootConfigPanel configPartContainer;

        private Controller(Project project) {
            super(project);
        }

        @Override
        public void refreshConfigConnection() {
            // FIXME pull in the current configuration settings and use those to send out
            // a note to all the listeners.
            // ClientConfig clientConfig;
            // fireConfigConnectionRefreshed(clientConfig);
        }
    }
}
