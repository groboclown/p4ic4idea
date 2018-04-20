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
package net.groboclown.p4.server.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Manages the configuration for an entire project.  The configuration must be refined through a file path,
 * or by getting a list of configurations.
 */
public interface P4ProjectConfig extends Disposable {

    boolean isDisposed();

    void refresh();

    /**
     *
     * @return all valid and invalid client configurations.
     */
    @NotNull
    Collection<ClientConfigSetup> getClientConfigSetups();

    /**
     *
     * @return all valid client configurations.
     */
    @NotNull
    Collection<ClientConfig> getClientConfigs();

    @NotNull
    Collection<ServerConfig> getServerConfigs();

    @Nullable
    ClientConfig getClientConfigFor(@NotNull FilePath file);

    @Nullable
    ClientConfig getClientConfigFor(@NotNull VirtualFile file);

    @NotNull
    Collection<ConfigProblem> getConfigProblems();

    /**
     * Should not consider "no client name defined" as an error.
     *
     * @return true if there are config errors.
     */
    boolean hasConfigErrors();

    @NotNull
    Project getProject();
}
