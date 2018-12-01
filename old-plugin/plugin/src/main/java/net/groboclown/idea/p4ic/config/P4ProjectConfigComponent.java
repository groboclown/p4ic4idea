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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.ClientNameDataPart;
import net.groboclown.idea.p4ic.config.part.CompositePart;
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import net.groboclown.idea.p4ic.config.part.EnvCompositePart;
import net.groboclown.idea.p4ic.config.part.MutableCompositePart;
import net.groboclown.idea.p4ic.config.part.RequirePasswordDataPart;
import net.groboclown.idea.p4ic.config.part.ServerFingerprintDataPart;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import net.groboclown.idea.p4ic.config.part.Unmarshal;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 */
@State(
    name = "P4ProjectConfigComponent",
    reloadable = true,
    storages = {
        @Storage(
                // removed in 181
                //id = "other",
                file = StoragePathMacros.WORKSPACE_FILE
        )
    }
)
public class P4ProjectConfigComponent implements ProjectComponent, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(P4ProjectConfigComponent.class);
    private static final String STATE_TAG_NAME = "project-config-component";

    private final static Map<Project, P4ProjectConfigComponent> temporaryComponent =
            new HashMap<Project, P4ProjectConfigComponent>();

    private final Project project;

    private MessageBusConnection projectMessageBus;

    // The actual list of user-defined parts.
    private ConfigState state = new ConfigState();

    public static class ConfigState {
        // Public for use by reflection when loading the state from XML
        @SuppressWarnings("WeakerAccess")
        public List<ConfigPart> configParts;

        @SuppressWarnings("WeakerAccess")
        public ConfigState() {
            this.configParts = null;
        }

        private ConfigState(List<ConfigPart> configParts) {
            this.configParts = configParts;
        }
    }

    @Nullable
    private P4ProjectConfigStack config = null;

    @Nullable
    private P4ProjectConfig previouslyAnnouncedConfig = null;


    public P4ProjectConfigComponent(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static P4ProjectConfigComponent getInstance(@NotNull final Project project) {
        // Some users have reported strange issues where the component isn't in the project.
        // This seems to indicate that there's a load order issue with the plugin.  This is
        // a stop-gap to keep those users from being dead in the water.  It's not
        // perfect by any means - the state can only be saved if the user loads up the
        // configuration again.

        P4ProjectConfigComponent component = project.getComponent(P4ProjectConfigComponent.class);
        if (component == null) {
            synchronized (temporaryComponent) {
                component = temporaryComponent.get(project);
                if (component == null) {
                    LOG.warn("Unable to load P4ProjectConfigComponent; configuration may not be saved.");
                    component = new P4ProjectConfigComponent(project);
                    temporaryComponent.put(project, component);
                }
            }
        } else {
            synchronized (temporaryComponent) {
                P4ProjectConfigComponent tmp = temporaryComponent.get(project);
                if (tmp != null) {
                    LOG.warn("Loading state from temporary component");
                    temporaryComponent.remove(project);
                    component.loadState(tmp.getState());
                }
            }
        }
        return component;
    }


    public void announceBaseConfigUpdated() {
        LOG.debug("Sending announcement that the base config is updated");
        checkConfigState();
        final P4ProjectConfig current;
        final P4ProjectConfig previous;
        synchronized (this) {
            previous = previouslyAnnouncedConfig;
            current = config;

            // Next time we call this, we need to send a copy of
            // what's considered current right now.
            if (config == null) {
                previouslyAnnouncedConfig = null;
            } else {
                previouslyAnnouncedConfig = new SimpleP4ProjectConfig(config);
            }
        }

        // Must follow the strict ordering
        if (current != null) {
            ApplicationManager.getApplication().getMessageBus().syncPublisher(
                    BaseConfigUpdatedListener.TOPIC_SERVERCONFIG).
                    configUpdated(project, current, previous);
            ApplicationManager.getApplication().getMessageBus().syncPublisher(
                    BaseConfigUpdatedListener.TOPIC_P4SERVER).
                    configUpdated(project, current, previous);
            ApplicationManager.getApplication().getMessageBus().syncPublisher(
                    BaseConfigUpdatedListener.TOPIC_NORMAL).
                    configUpdated(project, current, previous);
        }
    }


    /**
     *
     * @return a copy of the user's configured parts.
     */
    public synchronized List<ConfigPart> getUserConfigParts() {
        checkConfigState();
        return new ArrayList<ConfigPart>(state.configParts);
    }


    public synchronized void setUserConfigParts(@NotNull List<ConfigPart> parts) {
        synchronized (this) {
            state = new ConfigState(new ArrayList<ConfigPart>(parts));
            config = new P4ProjectConfigStack(project, state.configParts);
        }
        announceBaseConfigUpdated();
    }


    public synchronized P4ProjectConfig getP4ProjectConfig() {
        checkConfigState();
        return config;
    }


    // This method uses deprecated classes for backwards compatible value loading.
    @SuppressWarnings("deprecation")
    private synchronized void checkConfigState() {
        if (state == null || state.configParts == null) {
            state = new ConfigState(new ArrayList<ConfigPart>());

            // Backwards compatibility!
            // Attempt to load the state from the previous setting.
            P4ConfigProject origConfig = P4ConfigProject.getInstance(project);
            if (origConfig != null) {
                final ManualP4Config config = origConfig.getBaseConfig();
                {
                    final SimpleDataPart basic = new SimpleDataPart(project, (Map<String, String>) null);
                    basic.setIgnoreFilename(config.getIgnoreFileName());
                    basic.setServerName(config.getProtocol() + "://" + config.getPort());
                    basic.setAuthTicketFile(config.getAuthTicketPath());
                    basic.setTrustTicketFile(config.getTrustTicketPath());
                    basic.setClientHostname(config.getClientHostname());
                    basic.setDefaultCharset(config.getDefaultCharset());
                    basic.setUsername(config.getUsername());
                    state.configParts.add(basic);
                }

                if (config.hasClientnameSet()) {
                    final ClientNameDataPart clientName = new ClientNameDataPart();
                    clientName.setClientname(config.getClientname());
                    state.configParts.add(clientName);
                }

                if (config.hasServerFingerprintSet()) {
                    final ServerFingerprintDataPart fingerprint = new ServerFingerprintDataPart();
                    fingerprint.setServerFingerprint(config.getServerFingerprint());
                    state.configParts.add(fingerprint);
                }

                if (config.getConnectionMethod() != P4Config.ConnectionMethod.AUTH_TICKET) {
                    state.configParts.add(0, new RequirePasswordDataPart());
                }
            }

            if (state.configParts.isEmpty()) {
                // Allow a smart default
                state.configParts.add(new EnvCompositePart(project));
            }
        }
        if (config == null) {
            this.config = new P4ProjectConfigStack(project, state.configParts);
        }
    }


    @NotNull
    @Override
    public Element getState() {
        LOG.debug("Fetching XML state");
        checkConfigState();
        Element ret = new Element(STATE_TAG_NAME);
        MutableCompositePart all = new MutableCompositePart();
        synchronized (this) {
            if (state != null) {
                for (ConfigPart configPart : state.configParts) {
                    all.addConfigPart(configPart);
                }
            }
        }
        ret.addContent(all.marshal());
        return ret;
    }

    @Override
    public void loadState(@NotNull Element stateEl) {
        LOG.debug("Reading XML state");
        // Tag name doesn't matter - it's assigned to us by the loader.
        if (stateEl.getChildren().isEmpty()) {
            LOG.info("No XML state.  Going to use defaults.");
            synchronized (this) {
                state = null;
                config = null;
            }
            return;
        }

        final ConfigPart statePart = Unmarshal.from(project, stateEl.getChildren().get(0));
        LOG.debug("Read XML state as " + statePart);
        final List<ConfigPart> newState;
        if (statePart instanceof CompositePart) {
            newState = ((CompositePart) statePart).getConfigParts();
        } else {
            newState = Collections.singletonList(statePart);
        }

        synchronized (this) {
            state = new ConfigState(newState);
            config = new P4ProjectConfigStack(project, state.configParts);
        }
    }

    @Override
    public void projectOpened() {
        // intentionally empty
    }

    @Override
    public void projectClosed() {
        // intentionally empty
    }

    @Override
    public void initComponent() {
        projectMessageBus = project.getMessageBus().connect();
        projectMessageBus.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED,
                new VcsListener() {
                    @Override
                    public void directoryMappingChanged() {
                        // Invalidate the current mappings.  Note that
                        // the config isn't bad, but the underlying
                        // file mappings need to be recomputed.
                        // Therefore, we don't tell the user about it.

                        LOG.info("VCS directory mappings changed; marking P4 configs as needing refresh.");

                        P4InvalidConfigException ex = new P4InvalidConfigException(
                                P4Bundle.message("vcs.directory.mapping.changed"));

                        if (config != null) {
                            try {
                                Events.configInvalid(project, config, ex);
                            } catch (P4InvalidConfigException e) {
                                // Ignore; don't even log.
                            }
                        }
                    }
                });
    }

    @Override
    public void disposeComponent() {
        if (projectMessageBus != null) {
            projectMessageBus.disconnect();
            projectMessageBus = null;
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4ConfigProject";
    }
}
