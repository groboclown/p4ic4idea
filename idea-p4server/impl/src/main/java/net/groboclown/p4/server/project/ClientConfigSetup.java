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

package net.groboclown.p4.server.todo.project;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ConfigProblem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates the state of a client configuration, from where it came from (source), to its list of
 * problems at the time of the last refresh, to its actual configuration.  Additionally, because the
 * configuration is explicitly one client/server configuration per VCS root, there is exactly one root path.
 * <p>
 * Because this contains the source, which is mutable, this class can't be labeled immutable.
 */
public final class ClientConfigSetup {
    @Nullable
    private final ClientConfig config;

    private final Collection<ConfigProblem> configProblems;
    private final ConfigPart source;
    private final VirtualFile rootPath;

    ClientConfigSetup(@Nullable ClientConfig config, @Nullable Collection<ConfigProblem> configProblems,
            @NotNull ConfigPart source, @NotNull VirtualFile rootPath) {
        this.config = config;
        this.source = source;
        this.rootPath = rootPath;
        Set<ConfigProblem> problems = new HashSet<ConfigProblem>(source.getConfigProblems());
        if (configProblems != null) {
            problems.addAll(configProblems);
        }
        this.configProblems = Collections.unmodifiableCollection(problems);
    }

    public int getConfigVersion() {
        return config == null ? -1 : config.getConfigVersion();
    }

    @Nullable
    public ClientConfig getClientConfig() {
        return config;
    }

    @NotNull
    public Collection<ConfigProblem> getConfigProblems() {
        return configProblems;
    }

    @NotNull
    public ConfigPart getSource() {
        return source;
    }

    public boolean hasErrors() {
        for (ConfigProblem configProblem : configProblems) {
            if (configProblem.isError()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClientConfig() {
        return config != null;
    }

    @NotNull
    public VirtualFile getRootPath() {
        return rootPath;
    }
}
