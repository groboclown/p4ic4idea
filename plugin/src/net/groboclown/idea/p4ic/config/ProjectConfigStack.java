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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectConfigStack implements ProjectConfig {



    @Override
    public void refresh() {

    }

    @NotNull
    @Override
    public Iterable<ClientConfig> getClientConfigs() {
        return null;
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull FilePath file) {
        return null;
    }

    @Nullable
    @Override
    public ClientConfig getClientConfigFor(@NotNull VirtualFile file) {
        return null;
    }

    boolean isEmpty() {

    }



}
