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
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource.Builder;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSourceLoader;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
                id = "other",
                file = StoragePathMacros.WORKSPACE_FILE
        )
    }
)
public class P4ProjectConfigComponent implements ProjectComponent, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(P4ProjectConfigComponent.class);
    public static final String STATE_TAG_NAME = "project-config-component";

    private final Project project;

    private MessageBusConnection projectMessageBus;

    // The actual list of user-defined parts.
    private List<ConfigPart> state = null;

    private P4ProjectConfigStack config = null;

    public P4ProjectConfigComponent(@NotNull Project project) {
        this.project = project;
    }

    public static P4ProjectConfigComponent getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, P4ProjectConfigComponent.class);
    }


    public void announceBaseConfigUpdated()
            throws P4InvalidConfigException {
        LOG.debug("Sending announcement that the base config is updated");

        // This is a really rare circumstance, but it can happen.  It seems
        // to be due to a synchronization issue, so rather than just checking
        // whether the config sources have been setup right, we need to wrap
        // it in a synchronized block to ensure all initialization is
        // finished before checking the config sources state.
        // Bug #82
        synchronized (this) {
            if (configSources == null) {
                loadProjectConfigSources();
            }
            assert configSources != null;
        }


        // Must follow the strict ordering
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                BaseConfigUpdatedListener.TOPIC_SERVERCONFIG).
                configUpdated(project, configSources);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                BaseConfigUpdatedListener.TOPIC_P4SERVER).
                configUpdated(project, configSources);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                BaseConfigUpdatedListener.TOPIC_NORMAL).
                configUpdated(project, configSources);
    }


    /**
     *
     * @return a copy of the user's configured parts.
     */
    public synchronized List<ConfigPart> getUserConfigParts() {
        checkConfigState();
        return new ArrayList<ConfigPart>(state);
    }


    public synchronized void setUserConfigParts(@NotNull List<ConfigPart> parts) {

    }


    public P4ProjectConfig getP4ProjectConfig() {
        checkConfigState();
        return config;
    }


    private synchronized void checkConfigState() {
        if (state == null) {
            this.state = new ArrayList<ConfigPart>();

            // Backwards compatibility!
            // Attempt to load the state from the previous setting.
            P4ConfigProject origConfig = P4ConfigProject.getInstance(project);
            if (origConfig != null) {
                ManualP4Config config = origConfig.getBaseConfig()
                SimpleDataPart part = new SimpleDataPart(project, null);
                part.setIgnoreFilename(config.getIgnoreFileName());
                part.setServerName(config.getProtocol() + "://" + config.getPort());
                part.setAuthTicketFile(config.getAuthTicketPath());
                part.setTrustTicketFile(config.getTrustTicketPath());
                part.setClientHostname(config.getClientHostname());
                part.setClientname(config.getClientname());
                part.setDefaultCharset(config.getDefaultCharset());
                part.setServerFingerprint(config.getServerFingerprint());
                part.setUsername(config.getUsername());
            }

            this.config = new P4ProjectConfigStack(project, state);
        }
    }


    @Override
    public Element getState() {
        checkConfigState();
        Element ret = new Element(STATE_TAG_NAME);
        if (state != null) {
            for (ConfigPart configPart : state) {
                ret.addContent(configPart.marshal());
            }
        }
        return ret;
    }

    @Override
    public void loadState(@NotNull Element state) {
        if (! STATE_TAG_NAME.equals(state.getName()) || state.getChildren().isEmpty())
        if (state.isEmpty()) {
            // Attempt to load the configuration from the old method.

        }

        LOG.info("Loading config state");
        final ManualP4Config original = config;

        // save a copy of the config
        this.config = new ManualP4Config(state);
        synchronized (this) {
            // Just always change it.
            // This can happen when the user wants to explicitly
            // reset the connections.

            //if (!original.equals(state)) {
                // When loaded, this can cause errors if we actually initialize
                // the values here, because the file system for the project isn't
                // initialized yet.  That can lead to the project view being
                // empty.  IntelliJ 13 shows the massive amount of errors, but
                // 15 hides it.
                sourcesInitialized = false;
                clientsInitialized = false;
            //}
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

                        try {
                            Events.configInvalid(project, config, ex);
                        } catch (P4InvalidConfigException e) {
                            // Ignore; don't even log.
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

    private void initializeConfigSources() {
        // This situation can happen if the user is in the middle
        // of configuring the plugin as the new, active vcs.
        // if (!P4Vcs.isProjectValid(project)) {
        // So, instead, we'll only check to ensure that the
        // project is not disposed.
        // See bug #111

        if (project.isDisposed()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring reload for invalid project " + project.getName());
            }
            return;
        }

        boolean announce = false;
        synchronized (this) {
            if (!sourcesInitialized) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("reloading project " + project.getName() + " config sources");
                }

                // Mark as initialized first, so we don't re-enter this function.
                sourcesInitialized = true;

                try {
                    sourceConfigEx = null;
                    configSources = readProjectConfigSources();
                    // now that the sources are reloaded, we can make the announcement
                    announce = true;
                } catch (P4InvalidConfigException e) {
                    // ignore; already sent notifications
                    configSources = null;
                    sourceConfigEx = e;
                }
            }
        }
        // Perform this logic outside the synchronization block.
        // However, this can still block many other things because it can
        // result in a password update request during initialization.
        if (announce) {
            try {
                announceBaseConfigUpdated();
            } catch (P4InvalidConfigException e) {
                // ignore; already sent notifications
                configSources = null;
                sourceConfigEx = e;
            }
        }
    }


    @Deprecated
    @NotNull
    private List<ProjectConfigSource> readProjectConfigSources()
            throws P4InvalidConfigException {

        final Collection<Builder> sourceBuilders;
        sourceBuilders = ProjectConfigSourceLoader.loadSources(project, getBaseConfig());

        List<ProjectConfigSource> ret = new ArrayList<ProjectConfigSource>(sourceBuilders.size());
        List<P4Config> invalidConfigs = new ArrayList<P4Config>();
        for (Builder sourceBuilder : sourceBuilders) {
            if (sourceBuilder.isInvalid()) {
                LOG.warn("Invalid config: " +
                        P4ConfigUtil.getProperties(sourceBuilder.getBaseConfig()), sourceBuilder.getError());
                invalidConfigs.add(sourceBuilder.getBaseConfig());
            } else {
                final ProjectConfigSource source = sourceBuilder.create();
                LOG.info("Created config source " + source + " from " +
                        P4ConfigUtil.getProperties(sourceBuilder.getBaseConfig()) +
                        "; config dirs = " + source.getProjectSourceDirs());
                ret.add(source);
            }
        }
        if (!invalidConfigs.isEmpty()) {
            P4InvalidConfigException ex = new P4InvalidConfigException(invalidConfigs);
            for (P4Config invalidConfig : invalidConfigs) {
                Events.configInvalid(project, invalidConfig, new P4InvalidConfigException(invalidConfig));
            }
            throw ex;
        }

        // don't call the reloaded event until we store the value.

        return ret;
    }

}
