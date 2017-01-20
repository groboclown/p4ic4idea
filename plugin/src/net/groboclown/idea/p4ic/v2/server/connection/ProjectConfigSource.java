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
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Maps the specific Perforce configuration object to the corresponding Project directories it covers.
 * <p/>
 * These are created via {@link P4ConfigProject#loadProjectConfigSources()}.
 */
@Deprecated
public class ProjectConfigSource {

    private final Project project;
    private final List<VirtualFile> projectSourceDirs;
    private final String clientName;
    private final ServerConfig configuration;
    private final ClientServerRef clientServerRef;
    private final P4Config baseConfig;

    ProjectConfigSource(@NotNull Project project, @NotNull List<VirtualFile> projectSourceDirs,
            @Nullable String clientName, @NotNull ServerConfig configuration, @NotNull P4Config baseConfig) {
        this.project = project;
        this.projectSourceDirs = Collections.unmodifiableList(projectSourceDirs);
        this.clientName = clientName;
        this.configuration = configuration;
        this.clientServerRef = ClientServerRef.create(configuration, clientName);
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
    public ClientServerRef getClientServerRef() {
        return clientServerRef;
    }

    @NotNull
    public List<VirtualFile> getProjectSourceDirs() {
        return projectSourceDirs;
    }

    @Override
    public String toString() {
        return getClientServerRef().getServerDisplayId() + " - " + getProjectSourceDirs();
    }
}
