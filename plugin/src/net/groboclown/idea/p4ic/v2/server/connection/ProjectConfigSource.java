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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Maps the specific Perforce configuration object to the corresponding Project directories it covers.
 * <p/>
 * These are created via {@link P4ConfigProject#loadProjectConfigSources()}.
 */
public class ProjectConfigSource {

    private final Project project;
    private final List<VirtualFile> projectSourceDirs;
    private final String clientName;
    private final ServerConfig configuration;
    private final ClientServerId clientServerId;
    private final P4Config baseConfig;

    public static class Builder {
        private final Project project;
        private final String clientName;
        private final ServerConfig serverConfig;
        private final P4Config baseConfig;
        private final Set<VirtualFile> dirs = new HashSet<VirtualFile>();
        private VcsException error;

        public Builder(@NotNull Project project, @NotNull P4Config config) {
            this.project = project;
            this.baseConfig = config;
            this.clientName = config.getClientname();
            this.serverConfig = ServerConfig.createNewServerConfig(config);
        }

        private Builder(@NotNull Project project, @NotNull P4Config baseConfig,
                @Nullable String clientName, @NotNull ServerConfig serverConfig,
                @NotNull Collection<VirtualFile> dirs, final VcsException error) {
            this.project = project;
            this.clientName = clientName;
            this.serverConfig = serverConfig;
            this.baseConfig = baseConfig;
            this.dirs.addAll(dirs);
            this.error = error;
        }

        public boolean isInvalid() {
            return serverConfig == null || error != null;
        }

        @Nullable
        public VcsException getError() {
            return error;
        }


        public P4Config getBaseConfig() {
            return baseConfig;
        }


        public Collection<VirtualFile> getDirs() {
            return Collections.unmodifiableCollection(dirs);
        }


        public void setError(@NotNull VcsException error) {
            this.error = error;
        }


        public boolean isSame(@NotNull P4Config other) {
            return new ManualP4Config(serverConfig, clientName).equals(other);
        }

        public void add(@NotNull VirtualFile dir) {
            dirs.add(dir);
        }

        public ProjectConfigSource create() throws P4InvalidConfigException {
            if (isInvalid()) {
                throw new IllegalStateException("must call isInvalid before calling this function");
            }
            return new ProjectConfigSource(project, new ArrayList<VirtualFile>(dirs),
                    clientName, serverConfig, baseConfig);
        }

        @NonNls
        public String getPresentableName() {
            // TODO localize

            StringBuilder ret = new StringBuilder();
            if (serverConfig != null) {
                ret.append(serverConfig.getServiceName());
            }
            String configFile = baseConfig.getConfigFile();
            if (configFile != null) {
                if (configFile.indexOf('/') >= 0 || configFile.indexOf('\\') >= 0 ||
                        configFile.indexOf(File.separatorChar) >= 0) {
                    ret.append(" from ").append(configFile);
                } else if (dirs.isEmpty()) {
                    ret.append(" from ").append(configFile);
                } else {
                    ret.append(" from ").append(new File(dirs.iterator().next().getPath(), configFile));
                }
            }
            if (clientName != null) {
                ret.append(" @").append(clientName);
            }
            return ret.toString().trim();
        }

        @Override
        public String toString() {
            return clientName + "; " + serverConfig + "; " + dirs;
        }
    }


    ProjectConfigSource(@NotNull Project project, @NotNull List<VirtualFile> projectSourceDirs,
            @Nullable String clientName, @NotNull ServerConfig configuration, @NotNull P4Config baseConfig) {
        this.project = project;
        this.projectSourceDirs = Collections.unmodifiableList(projectSourceDirs);
        this.clientName = clientName;
        this.configuration = configuration;
        this.clientServerId = ClientServerId.create(configuration, clientName);
        this.baseConfig = baseConfig;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @Nullable
    public String getClientName() {
        return clientName;
    }

    @NotNull
    public ServerConfig getServerConfig() {
        return configuration;
    }

    @NotNull
    public ClientServerId getClientServerId() {
        return clientServerId;
    }

    @NotNull
    public List<VirtualFile> getProjectSourceDirs() {
        return projectSourceDirs;
    }

    @NotNull
    public Builder causedError(@NotNull VcsException error) {
        return new Builder(getProject(), baseConfig, getClientName(), getServerConfig(), getProjectSourceDirs(), error);
    }

    @Override
    public String toString() {
        return getClientServerId() + " - " + getProjectSourceDirs();
    }
}
