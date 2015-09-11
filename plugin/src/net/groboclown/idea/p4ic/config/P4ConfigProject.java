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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
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
public class P4ConfigProject implements PersistentStateComponent<ManualP4Config> {
    private static final Logger LOG = Logger.getInstance(P4ConfigProject.class);

    private final Project project;

    @NotNull
    private ManualP4Config config = new ManualP4Config();

    @Nullable
    private List<ProjectConfigSource> configSources;
    private P4InvalidConfigException sourceConfigEx;

    @Nullable
    private List<Client> clients;
    private P4InvalidConfigException clientConfigEx;
    private P4InvalidClientException clientClientEx;

    public P4ConfigProject(@NotNull Project project) {
        this.project = project;
    }

    public static P4ConfigProject getInstance(final Project project) {
        return ServiceManager.getService(project, P4ConfigProject.class);
    }


    /**
     * @return
     * @throws P4InvalidConfigException
     */
    public List<ProjectConfigSource> loadProjectConfigSources()
            throws P4InvalidConfigException {
        if (sourceConfigEx != null) {
            P4InvalidConfigException ret = new P4InvalidConfigException(sourceConfigEx.getMessage());
            ret.initCause(sourceConfigEx);
            throw ret;
        }
        return configSources;
    }


    private List<ProjectConfigSource> readProjectConfigSources()
            throws P4InvalidConfigException {
        // If the manual config defines a relative p4config file,
        // then find those under the root for the project.
        // Otherwise, just return the config.

        ManualP4Config base = getBaseConfig();
        String configFile = base.getConfigFile();
        List<Builder> sourceBuilders;
        if (configFile != null && configFile.indexOf('/') < 0 &&
                configFile.indexOf('\\') < 0 &&
                configFile.indexOf(File.separatorChar) < 0) {
            LOG.info("Loading relative config files");

            // Relative config file
            Map<VirtualFile, P4Config> map = P4ConfigUtil.loadProjectP4Configs(project, configFile, true);
            if (map.isEmpty()) {
                P4InvalidConfigException ex = new P4InvalidConfigException(P4Bundle.message("error.config.no-file"));
                Events.configInvalid(project, getBaseConfig(), ex);

                // FIXME old crumbly stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, base, ex);

                throw ex;
            }
            // The map returns one virtual file for each config directory found (and/or VCS root directories).
            // These may be duplicate configs, so need to add them to the matching entry, if any.
            sourceBuilders = new ArrayList<Builder>(map.size());

            for (Map.Entry<VirtualFile, P4Config> en : map.entrySet()) {
                boolean found = false;
                for (Builder sourceBuilder : sourceBuilders) {
                    if (sourceBuilder.isSame(en.getValue())) {
                        LOG.info("found existing builder path " + en.getKey());
                        sourceBuilder.add(en.getKey());
                        found = true;
                        break;
                    }
                }
                if (! found) {
                    LOG.info("found new builder path " + en.getKey() + " - " + en.getValue());
                    final Builder builder = new Builder(project, en.getValue());
                    builder.add(en.getKey());
                    sourceBuilders.add(builder);
                }
            }
        } else {
            P4Config fullConfig;
            try {
                fullConfig = P4ConfigUtil.loadCmdP4Config(base);
            } catch (IOException e) {
                P4InvalidConfigException ex = new P4InvalidConfigException(e);
                Events.configInvalid(project, getBaseConfig(), ex);

                // FIXME old crumbly stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, base, ex);

                throw ex;
            }
            List<VirtualFile> roots = P4ConfigUtil.getVcsRootFiles(project);
            Builder builder = new Builder(project, fullConfig);
            sourceBuilders = Collections.singletonList(builder);
            for (VirtualFile root : roots) {
                builder.add(root);
            }
        }


        List<ProjectConfigSource> ret = new ArrayList<ProjectConfigSource>(sourceBuilders.size());
        for (Builder sourceBuilder : sourceBuilders) {
            final ProjectConfigSource source = sourceBuilder.create();
            LOG.info("Created config source " + source);
            ret.add(source);
        }

        ApplicationManager.getApplication().getMessageBus().syncPublisher(BaseConfigUpdatedListener.TOPIC).
                configUpdated(project, ret);

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

                // FIXME old stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, base, ex);
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

                // FIXME old stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, base, ex);
                throw ex;
            }
            List<VirtualFile> roots = P4ConfigUtil.getVcsRootFiles(project);
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


        // FIXME old stuff
        ApplicationManager.getApplication().getMessageBus().syncPublisher(P4ClientsReloadedListener.TOPIC)
                .clientsLoaded(project, ret);
        if (old != null) {
            project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).
                    configChanges(project, old, base);
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
        final ManualP4Config original = config;

        // save a copy of the config
        this.config = new ManualP4Config(state);
        synchronized (this) {
            if (!original.equals(state)) {
                // Reload the settings and make the announcement.
                try {
                    sourceConfigEx = null;
                    configSources = readProjectConfigSources();
                } catch (P4InvalidConfigException e) {
                    // ignore; already sent notifications
                    configSources = null;
                    sourceConfigEx = e;
                }

                try {
                    clientClientEx = null;
                    clientConfigEx = null;
                    clients = readClients(original);
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
            this.config = ServerConfig.createNewServerConfig(config);
            if (this.config == null) {
                P4InvalidConfigException ex = new P4InvalidConfigException(config);
                Events.configInvalid(project, config, ex);

                // FIXME old stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, config, ex);
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

                    // FIXME old stuff
                    project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, sourceConfig, ex);
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

                // FIXME old stuff
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, sourceConfig, ex);
                throw ex;
            } catch (VcsException e) {
                P4InvalidConfigException ex = new P4InvalidConfigException(e.getMessage());
                ex.initCause(e);
                if (! (e instanceof P4WorkingOfflineException)) {
                    Events.configInvalid(project, sourceConfig, ex);

                    // FIXME old stuff
                    project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, sourceConfig, ex);
                }
                throw ex;
            }
        }
    }

}
