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
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.ConfigPropertiesUtil;
import net.groboclown.p4.server.api.config.P4VcsRootSettings;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.impl.config.P4VcsRootSettingsImpl;
import net.groboclown.p4.server.impl.util.DirectoryMappingUtil;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
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
        this.vcsRoot = DirectoryMappingUtil.getDirectory(project, mapping);
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
            P4VcsRootSettings settings = new P4VcsRootSettingsImpl(vcsRoot);
            MultipleConfigPart parentPart = loadParentPartFromUI();
            settings.setConfigParts(parentPart.getChildren());
            mapping.setRootSettings(settings);
            if (parentPart.hasError()) {
                Collection<ConfigProblem> problems =
                        parentPart.getConfigProblems();
                throw new ConfigurationException(toMessage(problems),
                        P4Bundle.getString("configuration.error.title"));
            }
            // This class just deals with the configuration.
            // The actual generation of the events for the configuration is done by the
            // mapping updates (ClientConfigComponent).
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

    private ConfigPart loadParentPartFromSettings() {
        List<ConfigPart> parts = loadPartsFromSettings();
        return new MultipleConfigPart(
                P4Bundle.getString("configuration.connection-choice.wrapped-container"),
                parts
        );
    }

    private MultipleConfigPart loadParentPartFromUI() {
        List<ConfigPart> parts = panel.getConfigParts();
        return new MultipleConfigPart(
                P4Bundle.getString("configuration.connection-choice.wrapped-container"),
                parts
        );
    }

    @Nullable
    private ClientConfig loadConfigFromSettings() {
        ConfigPart parentPart = loadParentPartFromSettings();
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
            P4VcsRootSettingsImpl ret = new P4VcsRootSettingsImpl(vcsRoot);
            mapping.setRootSettings(ret);
            return ret;
        }
        if (!(rawSettings instanceof P4VcsRootSettings)) {
            throw new IllegalStateException("Invalid plugin root settings class; expected " +
                    P4VcsRootSettings.class + ", found " + rawSettings.getClass());
        }
        return (P4VcsRootSettings) rawSettings;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    private String toMessage(Collection<ConfigProblem> problems) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ConfigProblem problem : problems) {
            if (first) {
                first = false;
            } else {
                sb.append(P4Bundle.getString("config.message.separator"));
            }
            sb.append(problem.getMessage());
        }
        return sb.toString();
    }

    private class Controller extends ConfigConnectionController {
        P4RootConfigPanel configPartContainer;

        private Controller(Project project) {
            super(project);
        }

        @Override
        protected void performRefresh() {
            // TODO This code should probably be moved into a different class.

            LOG.debug("Refreshing configuration in panel");
            ClientConfig tClientConfig = null;
            ServerConfig tServerConfig = null;
            final MultipleConfigPart parentPart = loadParentPartFromUI();
            parentPart.reload();
            if (!parentPart.hasError()) {
                if (ServerConfig.isValidServerConfig(parentPart)) {
                    try {
                        tServerConfig = ServerConfig.createFrom(parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                }
                if (ClientConfig.isValidClientConfig(tServerConfig, parentPart)) {
                    try {
                        tClientConfig = ClientConfig.createFrom(tServerConfig, parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                }
            }

            final ClientConfig clientConfig = tClientConfig;
            final ServerConfig serverConfig = tServerConfig;

            // We will use the server config as a check for errors; the connection
            // can only be  valid if the client is not null, but if the client is
            // null, then that generates its own set of issues.

            Answer.background((sink) -> {
                if (serverConfig != null) {
                    // Check client list, because that only requires a username
                    // and valid login.
                    // An error here will cause the client config check to fail, because of
                    // the rejection.
                    LOG.debug("Attempting to get the list of clients");
                    P4ServerComponent.getInstance(project).checkServerConnection(serverConfig)
                            .whenCompleted(sink::resolve)
                            .whenServerError(sink::reject);
                } else {
                    sink.resolve(null);
                }
            })
            .futureMap((result, sink) -> {
                if (result != null && clientConfig != null) {
                    // Check the opened files, because that requires the client to be
                    // valid for the current user.
                    LOG.debug("Attempting to get the list of opened files for the client");
                    P4ServerComponent.getInstance(project).checkClientConnection(clientConfig)
                            .whenCompleted(sink::resolve)
                            .whenServerError(sink::reject);
                } else {
                    sink.resolve(null);
                }
            })
            .whenCompleted((obj) -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sending refresh complete notice with configuration " +
                            ConfigPropertiesUtil.toProperties(parentPart,
                                    "<unset>", "<empty>", "<set>"));
                }
                fireConfigConnectionRefreshed(parentPart, clientConfig, serverConfig);
            })
            .whenFailed((err) -> {
                LOG.info("Server connection attempt failed", err);
                parentPart.addAdditionalProblem(
                        new ConfigProblem(null, err.getLocalizedMessage(), false));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sending refresh complete notice with configuration " +
                            ConfigPropertiesUtil.toProperties(parentPart,
                                    "<unset>", "<empty>", "<set>"));
                }
                fireConfigConnectionRefreshed(parentPart, clientConfig, serverConfig);
            });
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Changed P4 VCS root panel size from " + prevSize + " to " + size);
            }
        }
    }
}
