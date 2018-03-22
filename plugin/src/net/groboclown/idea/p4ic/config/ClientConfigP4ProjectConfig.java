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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class ClientConfigP4ProjectConfig implements P4ProjectConfig {
    private final ClientConfig config;

    public ClientConfigP4ProjectConfig(@NotNull ClientConfig config) {
        this.config = config;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void refresh() {
        // Do nothing
    }

    @NotNull
    @Override
    public Collection<ClientConfigSetup> getClientConfigSetups() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ClientConfig> getClientConfigs() {
        return Collections.singleton(config);
    }

    @NotNull
    @Override
    public Collection<ServerConfig> getServerConfigs() {
        return Collections.singleton(config.getServerConfig());
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull FilePath file) {
        return config;
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull VirtualFile file) {
        return config;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasConfigErrors() {
        return false;
    }

    @NotNull
    @Override
    public Project getProject() {
        return config.getProject();
    }

    @Override
    public void dispose() {
        // nothing to do
    }
}
