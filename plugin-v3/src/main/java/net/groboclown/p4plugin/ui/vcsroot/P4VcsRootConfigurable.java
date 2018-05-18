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
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
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
        return new WrapperPanel(panel.getRootPane());
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
            MultipleConfigPart parentPart = loadParentPartFromSettings();
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

    private MultipleConfigPart loadParentPartFromSettings() {
        List<ConfigPart> parts = loadPartsFromSettings();
        return  new MultipleConfigPart(
                P4Bundle.getString("configuration.connection-choice.wrapped-container"),
                parts
        );
    }

    @Nullable
    private ClientConfig loadConfigFromSettings() {
        MultipleConfigPart parentPart = loadParentPartFromSettings();
        if (!parentPart.hasError() && ServerConfig.isValidServerConfig(parentPart)) {
            try {
                ServerConfig serverConfig = ServerConfig.createFrom(parentPart);
                if (ClientConfig.isValidClientConfig(serverConfig, parentPart)) {
                    return ClientConfig.createFrom(serverConfig, parentPart);
                }
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

    private class Controller extends ConfigConnectionController {
        P4RootConfigPanel configPartContainer;

        private Controller(Project project) {
            super(project);
        }

        @Override
        public void refreshConfigConnection() {
            ClientConfig clientConfig = null;
            ServerConfig serverConfig = null;
            MultipleConfigPart parentPart = loadParentPartFromSettings();
            if (!parentPart.hasError()) {
                if (ServerConfig.isValidServerConfig(parentPart)) {
                    try {
                        serverConfig = ServerConfig.createFrom(parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                }
                if (ClientConfig.isValidClientConfig(serverConfig, parentPart)) {
                    try {
                        clientConfig = ClientConfig.createFrom(serverConfig, parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                }
            }

            fireConfigConnectionRefreshed(clientConfig, serverConfig);
        }
    }


    // The scrolling outer panel can cause the inner tabs to get sized all wrong,
    // because of the scrollpanes in scrollpane.
    // This helps keep the tabs sized right so we essentially ignore the outer scroll pane.
    private class WrapperPanel
            extends JPanel
            implements Scrollable {
        private final JPanel wrapped;
        private Dimension size;

        private WrapperPanel(JPanel wrapped) {
            this.wrapped = wrapped;

            setLayout(new BorderLayout());
            add(wrapped, BorderLayout.CENTER);

            updateSize();

            addAncestorListener(new AncestorListener() {
                @Override
                public void ancestorAdded(AncestorEvent event) {
                    updateSize();
                }

                @Override
                public void ancestorRemoved(AncestorEvent event) {
                    updateSize();
                }

                @Override
                public void ancestorMoved(AncestorEvent event) {
                    updateSize();
                }
            });

            addHierarchyBoundsListener(new HierarchyBoundsListener() {
                @Override
                public void ancestorMoved(HierarchyEvent e) {
                    updateSize();
                }

                @Override
                public void ancestorResized(HierarchyEvent e) {
                    updateSize();
                }
            });
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return size;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 0;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 0;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private void updateSize() {
            final Dimension prevSize = this.size;
            final Container parent = getParent();
            if (parent != null) {
                final Container parent2 = parent.getParent();
                if (parent2 != null) {
                    size = new Dimension(parent2.getPreferredSize());
                } else {
                    size = new Dimension(parent.getPreferredSize());
                }
            } else {
                size = new Dimension(wrapped.getPreferredSize());
            }
            if (!size.equals(prevSize)) {
                setPreferredSize(size);
                wrapped.revalidate();
                wrapped.doLayout();
                wrapped.repaint();
            }
            LOG.info("Changed P4 VCS root panel size from " + prevSize + " to " + size);
        }
    }
}
