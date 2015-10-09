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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.server.ServerStatus;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.P4WorkingOfflineException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource.Builder;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 */
@State(
    name = "P4ConfigProject",
    roamingType = RoamingType.DISABLED,
    reloadable = true,
    storages = {
        @Storage(
            file = StoragePathMacros.WORKSPACE_FILE
        )
    }
)
public class P4ConfigProject implements ProjectComponent, PersistentStateComponent<ManualP4Config> {
    private static final Logger LOG = Logger.getInstance(P4ConfigProject.class);

    private final Project project;

    private MessageBusConnection projectMessageBus;

    @NotNull
    private ManualP4Config config = new ManualP4Config();

    @Nullable
    private List<ProjectConfigSource> configSources;
    private P4InvalidConfigException sourceConfigEx;
    boolean sourcesInitialized = false;

    @Nullable
    private List<Client> clients;
    private P4InvalidConfigException clientConfigEx;
    private P4InvalidClientException clientClientEx;
    boolean clientsInitialized = false;
    private ManualP4Config previous;

    // FIXME for testing
    private int badLoadCount = 0;

    public P4ConfigProject(@NotNull Project project) {
        this.project = project;
    }

    public static P4ConfigProject getInstance(final Project project) {
        return ServiceManager.getService(project, P4ConfigProject.class);
    }


    /**
     * @return the project config sources
     * @throws P4InvalidConfigException
     */
    @NotNull
    public List<ProjectConfigSource> loadProjectConfigSources()
            throws P4InvalidConfigException {
        initializeConfigSources();
        if (sourceConfigEx != null) {
            // FIXME for testing; the underlying error seems to get lost.
            if ((++badLoadCount) % 10 == 0) {
                sourcesInitialized = false;
                return loadProjectConfigSources();
            }
            P4InvalidConfigException ret = new P4InvalidConfigException(sourceConfigEx.getMessage());
            ret.initCause(sourceConfigEx);
            throw ret;
        }
        if (configSources == null) {
            return Collections.emptyList();
        }
        return configSources;
    }

    private void initializeConfigSources() {
        if (! P4Vcs.isProjectValid(project)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring reload for invalid project " + project.getName());
            }
            return;
        }

        synchronized (this) {
            if (! sourcesInitialized) {
                // FIXME DEBUG
                LOG.info("reloading project " + project.getName() + " config sources", new Throwable("stack capture"));

                // Mark as initialized first, so we don't re-enter this function.
                sourcesInitialized = true;

                try {
                    sourceConfigEx = null;
                    configSources = readProjectConfigSources();
                    // now that the sources are reloaded, we can make the announcement
                    ApplicationManager.getApplication().getMessageBus().syncPublisher(
                            BaseConfigUpdatedListener.TOPIC).
                            configUpdated(project, configSources);
                } catch (P4InvalidConfigException e) {
                    // ignore; already sent notifications
                    configSources = null;
                    sourceConfigEx = e;
                }
            }
        }
    }


    @NotNull
    private List<ProjectConfigSource> readProjectConfigSources()
            throws P4InvalidConfigException {

        final Collection<Builder> sourceBuilders;
        try {
            sourceBuilders = ProjectConfigSourceLoader.loadSources(project, getBaseConfig());
        } catch (P4InvalidConfigException ex) {
            Events.configInvalid(project, getBaseConfig(), ex);

            throw ex;
        }

        List<ProjectConfigSource> ret = new ArrayList<ProjectConfigSource>(sourceBuilders.size());
        List<P4Config> invalidConfigs = new ArrayList<P4Config>();
        for (Builder sourceBuilder : sourceBuilders) {
            if (sourceBuilder.isInvalid()) {
                LOG.warn("Invalid config: " +
                        P4ConfigUtil.getProperties(sourceBuilder.getBaseConfig()));
                invalidConfigs.add(sourceBuilder.getBaseConfig());
            } else {
                final ProjectConfigSource source = sourceBuilder.create();
                LOG.info("Created config source " + source + " from " +
                        P4ConfigUtil.getProperties(sourceBuilder.getBaseConfig()) +
                        "; config dirs = " + source.getProjectSourceDirs());
                ret.add(source);
            }
        }
        if (! invalidConfigs.isEmpty()) {
            P4InvalidConfigException ex = new P4InvalidConfigException(invalidConfigs);
            for (P4Config invalidConfig : invalidConfigs) {
                Events.configInvalid(project, invalidConfig, new P4InvalidConfigException(invalidConfig));
            }
            throw ex;
        }

        // don't call the reloaded event until we store the value.

        return ret;
    }


    /**
     * @param project
     * @return
     * @throws P4InvalidConfigException
     * @throws P4InvalidClientException
     * @deprecated see #readProjectConfigSources(Project)
     */
    public List<Client> loadClients(@NotNull Project project)
            throws P4InvalidConfigException, P4InvalidClientException {
        initializeClients();
        if (clientConfigEx != null) {
            P4InvalidConfigException ret = new P4InvalidConfigException(clientConfigEx.getMessage());
            ret.initCause(clientConfigEx);
            throw ret;
        }
        if (clientClientEx != null) {
            P4InvalidClientException ret = new P4InvalidClientException(clientClientEx.getMessage());
            ret.initCause(clientClientEx);
            throw ret;
        }
        return clients;
    }

    private void initializeClients() {
        synchronized (this) {
            if (! clientsInitialized) {
                // Mark as initialized first, so we don't re-enter this function.
                clientsInitialized = true;
                try {
                    clientClientEx = null;
                    clientConfigEx = null;
                    clients = readClients(previous);
                } catch (P4InvalidConfigException e) {
                    clientConfigEx = e;
                    clients = null;
                } catch (P4InvalidClientException e) {
                    clientClientEx = e;
                    clients = null;
                }
            }
        }
    }

    /**
     *
     * @return
     * @throws P4InvalidConfigException
     * @throws P4InvalidClientException
     * @deprecated see #readProjectConfigSources(Project)
     */
    private List<Client> readClients(@Nullable P4Config old) throws P4InvalidConfigException, P4InvalidClientException {
        // If the manual config defines a relative p4config file,
        // then find those under the root for the project.
        // Otherwise, just return the config.

        List<Client> ret = new ArrayList<Client>();

        ManualP4Config base = getBaseConfig();
        String configFile = base.getConfigFile();
        if (configFile != null && configFile.indexOf('/') < 0 &&
                configFile.indexOf('\\') < 0 &&
                configFile.indexOf(File.separatorChar) < 0) {
            // Relative config file
            Map<VirtualFile, P4Config> map = P4ConfigUtil.loadProjectP4Configs(project, configFile, true);
            if (map.isEmpty()) {
                LOG.info("No p4config files found for config setup");
                P4InvalidConfigException ex = new P4InvalidConfigException(P4Bundle.message("error.config.no-file"));
                Events.configInvalid(project, base, ex);
                throw ex;
            }
            for (Map.Entry<VirtualFile, P4Config> en: map.entrySet()) {
                Client client = new ClientImpl(project, en.getValue(), Collections.singletonList(en.getKey()));
                // Check that the root directory is fine.
                client.getRoots();
                ret.add(client);
            }
        } else {
            P4Config fullConfig;
            try {
                fullConfig = P4ConfigUtil.loadCmdP4Config(base);
            } catch (IOException e) {
                LOG.info("Error loading p4 config file", e);
                P4InvalidConfigException ex = new P4InvalidConfigException(e);
                Events.configInvalid(project, base, ex);
                throw ex;
            }
            List<VirtualFile> roots = P4Vcs.getInstance(project).getVcsRoots();
            // Not necessary: the roots for the client should only be based on the VCS roots.
            //roots.add(project.getBaseDir());
            //for (Module module: ModuleManager.getInstance(project).getModules()) {
            //    roots.addAll(Arrays.asList(ModuleRootManager.getInstance(module).getContentRoots()));
            //}
            Client client = new ClientImpl(project, fullConfig, roots);
            // Check that the root directory is fine.
            client.getRoots();
            ret.add(client);
        }

        return ret;
    }

    /**
     * 
     * @return a copy of the base config.  The only way to actually update the value is
     *      through a {@link #loadState(ManualP4Config)} call.
     */
    public ManualP4Config getBaseConfig() {
        return new ManualP4Config(config);
    }


    @Override
    public ManualP4Config getState() {
        return getBaseConfig();
    }

    @Override
    public void loadState(@NotNull ManualP4Config state) {
        LOG.info("Loading config state");
        final ManualP4Config original = config;

        // save a copy of the config
        this.config = new ManualP4Config(state);
        synchronized (this) {
            if (!original.equals(state)) {
                // When loaded, this can cause errors if we actually initialize
                // the values here, because the file system for the project isn't
                // initialized yet.  That can lead to the project view being
                // empty.  IntelliJ 13 shows the massive amount of errors, but
                // 15 hides it.
                previous = original;
                sourcesInitialized = false;
                clientsInitialized = false;
            }
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


    static class ClientImpl implements Client {
        private final Project project;
        private final P4Config sourceConfig;
        private final ServerConfig config;

        @Nullable
        private ServerStatus status;
        private final String clientName;
        private final List<VirtualFile> inputRoots;
        private List<VirtualFile> roots;
        private List<FilePath> fpRoots;

        ClientImpl(@NotNull Project project, @NotNull P4Config config, @NotNull List<VirtualFile> roots)
                throws P4InvalidConfigException, P4InvalidClientException {
            this.project = project;
            this.sourceConfig = config;
            this.config = ServerConfig.createNewServerConfig(project, config);
            if (this.config == null) {
                P4InvalidConfigException ex = new P4InvalidConfigException(config);
                Events.configInvalid(project, config, ex);
                throw ex;
            }
            this.clientName = config.getClientname();
            if (clientName == null || clientName.length() <= 0) {
                throw new P4InvalidClientException();
            }
            this.status = ServerStoreService.getInstance().getServerStatus(project, this.config);
            this.inputRoots = roots;
            if (roots.isEmpty()) {
                throw new P4InvalidConfigException(P4Bundle.message("error.config.no-roots", clientName));
            }
        }

        @NotNull
        @Override
        public ServerExecutor getServer() throws P4InvalidConfigException {
            if (status == null) {
                // Note that this error is actually due to the client no longer being
                // valid, rather than a real config problem.
                throw new P4InvalidConfigException(P4Bundle.message("error.config.disconnected"));
            }
            return status.getExecutorForClient(project, clientName);
        }

        @NotNull
        @Override
        public String getClientName() {
            return clientName;
        }

        @NotNull
        @Override
        public ServerConfig getConfig() {
            return config;
        }

        @NotNull
        @Override
        public List<VirtualFile> getRoots() throws P4InvalidConfigException {
            setupRoots();
            return roots;
        }

        @NotNull
        @Override
        public List<FilePath> getFilePathRoots() throws P4InvalidConfigException {
            setupRoots();
            return fpRoots;
        }


        @Override
        public boolean isWorkingOffline() {
            return status == null || status.isWorkingOffline();
        }

        @Override
        public boolean isWorkingOnline() {
            return status != null && status.isWorkingOnline();
        }

        @Override
        public void forceDisconnect() {
            if (status != null) {
                status.forceDisconnect();
            }
        }

        @Override
        public void dispose() {
            if (status != null) {
                status.removeClient(getClientName());
                ServerStoreService.getInstance().removeServerConfig(project, config);
                // FIXME DEBUG
                LOG.info("*** disposed client " + clientName + " (" + config + ")");
                status = null;
            }
        }

        @Override
        public boolean isDisposed() {
            return status == null;
        }

        @Override
        public boolean isSameConfig(@NotNull final P4Config p4config) {
            // FIXME ensure this does the right thing
            return sourceConfig.equals(p4config);
        }


        @Override
        public String toString() {
            return config.getServiceName() + " " + clientName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || ! obj.getClass().equals(ClientImpl.class)) {
                return false;
            }
            ClientImpl that = (ClientImpl) obj;
            return Comparing.equal(config, that.config) &&
                    Comparing.equal(clientName, that.clientName);
        }

        @Override
        public int hashCode() {
            return config.hashCode() + clientName.hashCode();
        }


        private synchronized void setupRoots() throws P4InvalidConfigException {
            if (roots != null || status == null) {
                return;
            }

            // Ensure the root is valid
            try {
                // getServer won't work yet, because we haven't defined the root directory yet.
                // We don't need a root for the call we're making, so don't worry about which one
                // we choose at this point.
                ServerExecutor exec = status.getExecutorForClient(project, clientName);
                Collection<VirtualFile> realRoots = exec.findRoots(inputRoots);
                if (realRoots.isEmpty()) {
                    P4InvalidConfigException ex = new P4InvalidConfigException(P4Bundle.message("error.config.root-not-found", realRoots));
                    Events.configInvalid(project, sourceConfig, ex);
                    throw ex;
                }
                roots = Collections.unmodifiableList(new ArrayList<VirtualFile>(realRoots));
                ArrayList<FilePath> fpr = new ArrayList<FilePath>(roots.size());
                for (VirtualFile vf : roots) {
                    fpr.add(VcsUtil.getFilePath(vf));
                }
                fpRoots = Collections.unmodifiableList(fpr);
            } catch (P4InvalidConfigException e) {
                // the thrower should make the call to the listener.
                throw e;
            } catch (CancellationException e) {
                P4InvalidConfigException ex = new P4InvalidConfigException(e.getMessage());
                ex.initCause(e);
                Events.configInvalid(project, sourceConfig, ex);
                throw ex;
            } catch (VcsException e) {
                P4InvalidConfigException ex = new P4InvalidConfigException(e.getMessage());
                ex.initCause(e);
                if (! (e instanceof P4WorkingOfflineException)) {
                    Events.configInvalid(project, sourceConfig, ex);
                }
                throw ex;
            }
        }
    }

}
