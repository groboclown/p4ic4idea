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

import com.intellij.openapi.components.*;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Manages the configuration of the Perforce setup.  It should handle the normal method
 * for looking for the configuration - the environment variables, configuration file,
 * and user overrides.
 *
 * @author Matt Albrecht
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
    private final Project project;
    private ManualP4Config config = new ManualP4Config();
    private P4Config previous = null;

    public P4ConfigProject(@NotNull Project project) {
        this.project = project;
    }

    public static P4ConfigProject getInstance(final Project project) {
        return ServiceManager.getService(project, P4ConfigProject.class);
    }

    public List<Client> loadClients(@NotNull Project project) throws P4InvalidConfigException, P4InvalidClientException {
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
                P4InvalidConfigException ex = new P4InvalidConfigException(P4Bundle.message("error.config.no-file"));
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
            P4Config fullConfig = P4ConfigUtil.loadCmdP4Config(base);
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


        return ret;
    }


    public ManualP4Config getBaseConfig() {
        return config;
    }

    @Override
    public ManualP4Config getState() {
        P4Config real = new ManualP4Config(config);
        if (previous != null && ! previous.equals(real)) {
            project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configChanges(project, previous, real);
        }
        previous = real;

        return config;
    }

    @Override
    public void loadState(@NotNull ManualP4Config state) {
        this.config = state;

        P4Config real = new ManualP4Config(config);
        P4Config original = previous;
        if (original == null) {
            original = new ManualP4Config();
        }
        project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configChanges(project, original, real);
        previous = real;
    }



    private static class ClientImpl implements Client {
        private final Project project;
        private final P4Config sourceConfig;
        private final ServerConfig config;

        @Nullable
        private ServerStatus status;
        private final String clientName;
        private final List<VirtualFile> inputRoots;
        private List<VirtualFile> roots;
        private List<FilePath> fpRoots;

        private ClientImpl(@NotNull Project project, @NotNull P4Config config, @NotNull List<VirtualFile> roots)
                throws P4InvalidConfigException, P4InvalidClientException {
            this.project = project;
            this.sourceConfig = config;
            this.config = ServerConfig.createNewServerConfig(config);
            if (this.config == null) {
                P4InvalidConfigException ex = new P4InvalidConfigException(config);
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
                throw new P4InvalidConfigException(P4Bundle.message("error.config.no-roots"));
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
                project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, sourceConfig, ex);
                throw ex;
            } catch (VcsException e) {
                P4InvalidConfigException ex = new P4InvalidConfigException(e.getMessage());
                ex.initCause(e);
                if (! (e instanceof P4WorkingOfflineException)) {
                    project.getMessageBus().syncPublisher(P4ConfigListener.TOPIC).configurationProblem(project, sourceConfig, ex);
                }
                throw ex;
            }
        }
    }
}
