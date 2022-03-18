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

package net.groboclown.p4plugin.modules.clientconfig.view;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRootSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.ConfigPropertiesUtil;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.modules.clientconfig.VcsRootConfigController;
import net.groboclown.p4plugin.ui.WrapperPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * Manages the state with the module's controller.
 */
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating configurable for vcs root " + vcsRoot);
        }
        this.mapping = mapping;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating component for root " + vcsRoot + "; mapping " + mapping);
            VcsRootSettings settings = mapping.getRootSettings();
            LOG.debug("VCS Settings: " + (settings == null ? null : settings.getClass().getName()));
            LOG.debug("P4 Settings for " + mapping.getDirectory() + ": " + loadConfigFromSettings());
        }
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
        Collection<ConfigProblem> problems = null;
        if (isModified()) {
            LOG.info("Updating root configuration for " + vcsRoot);
            MultipleConfigPart parentPart = loadParentPartFromUI();
            VcsRootConfigController.getInstance().setRootConfigParts(
                    project, vcsRoot, parentPart.getChildren());

            if (parentPart.hasError()) {
                problems = parentPart.getConfigProblems();
            }
        } else {
            LOG.info("Skipping root configuration update; nothing modified for " + vcsRoot);
        }

        // Send the update events regarding the root configuration updates.  Do this even in the
        // case of an error, or if there were no updates.  The no updates use case is for the situation where
        // events were fired in an unexpected order, then the user can rerun the apply to re-trigger everything.
        // It's a hack, yes.
        //   - Not only is it a hack, but in IDE v212 with the new service model, this fails due to
        //     different class loader between the returned listener object and this object.
        // project.getMessageBus().syncPublisher(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED)
        //        .directoryMappingChanged();

        if (problems != null) {
            LOG.warn("Configuration problems discovered: " + problems);
            throw new ConfigurationException(toMessage(problems),
                    P4Bundle.getString("configuration.error.title"));
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Disposing VCS root configuration GUI for root " + vcsRoot);
        }
        panel = null;
        controller = null;
    }

    private List<ConfigPart> loadPartsFromSettings() {
        return VcsRootConfigController.getInstance().getConfigPartsForRoot(project, vcsRoot);
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
    public ClientConfig loadConfigFromSettings() {
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
                    LOG.debug("No errors in server configuration");
                    try {
                        tServerConfig = ServerConfig.createFrom(parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                } else {
                    LOG.debug("Errors found in server configuration");
                }
                if (ClientConfig.isValidClientConfig(tServerConfig, parentPart)) {
                    LOG.debug("No errors in client configuration");
                    try {
                        tClientConfig = ClientConfig.createFrom(tServerConfig, parentPart);
                    } catch (IllegalArgumentException e) {
                        LOG.info("Should have not caused an error due to previous error check", e);
                    }
                } else {
                    LOG.debug("Errors found in client configuration");
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
                    P4ServerComponent.checkServerConnection(project,
                            new OptionalClientServerConfig(serverConfig, clientConfig))
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
                    LOG.debug("Checking client connection");
                    P4ServerComponent.checkClientConnection(project, clientConfig)
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
}
