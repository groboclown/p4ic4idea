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

package net.groboclown.p4.server.api.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class SimpleP4ProjectConfig implements P4ProjectConfig {
    private final Project project;
    private final Collection<ClientConfig> configs;
    private final Collection<ClientConfigSetup> configSetups;
    private final Collection<ConfigProblem> problems;
    private boolean disposed;

    SimpleP4ProjectConfig(@NotNull P4ProjectConfig config) {
        this(config.getProject(), config.getClientConfigs(), config.getClientConfigSetups(),
                config.getConfigProblems());
    }

    private SimpleP4ProjectConfig(Project project,
            Collection<ClientConfig> configs,
            Collection<ClientConfigSetup> configSetups,
            Collection<ConfigProblem> problems) {
        this.project = project;
        this.configs = Collections.unmodifiableCollection(new ArrayList<ClientConfig>(configs));
        this.configSetups = Collections.unmodifiableCollection(new ArrayList<ClientConfigSetup>(configSetups));
        this.problems = Collections.unmodifiableCollection(new ArrayList<ConfigProblem>(problems));
    }

    @Override
    public boolean isDisposed() {
        return project.isDisposed() || disposed;
    }

    @Override
    public void refresh() {
        // do nothing
    }

    @NotNull
    @Override
    public Collection<ClientConfigSetup> getClientConfigSetups() {
        return configSetups;
    }

    @NotNull
    @Override
    public Collection<ClientConfig> getClientConfigs() {
        return configs;
    }

    @NotNull
    @Override
    public Collection<ServerConfig> getServerConfigs() {
        Map<String, ServerConfig> ret = new HashMap<String, ServerConfig>();
        for (ClientConfig config : configs) {
            ret.put(config.getServerConfig().getServerId(), config.getServerConfig());
        }
        return ret.values();
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull FilePath file) {
        final VirtualFile vf = file.getVirtualFile();
        if (vf == null) {
            return null;
        }
        return getClientConfigFor(vf);
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull VirtualFile file) {
        ClientConfig bestMatch = null;
        int bestMatchDepth = Integer.MAX_VALUE;
        for (ClientConfig config : configs) {
            for (VirtualFile parent : config.getProjectSourceDirs()) {
                int depth = getDepth(file, parent);
                if (depth < bestMatchDepth) {
                    bestMatchDepth = depth;
                    bestMatch = config;
                }
            }
        }
        return bestMatch;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return problems;
    }

    @Override
    public boolean hasConfigErrors() {
        for (ConfigProblem configProblem : getConfigProblems()) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }


    private int getDepth(@NotNull final VirtualFile child, @NotNull final VirtualFile parent) {
        if (child.equals(parent)) {
            return 0;
        }
        VirtualFile p = child;
        VirtualFile k = child.getParent();
        int depth = 1;
        while (k != null && ! k.equals(p)) {
            if (k.equals(parent)) {
                return depth;
            }
            p = k;
            k = k.getParent();
        }
        // child is not a child of parent.
        return Integer.MAX_VALUE;
    }

    @Override
    public void dispose() {
        disposed = true;
    }
}
