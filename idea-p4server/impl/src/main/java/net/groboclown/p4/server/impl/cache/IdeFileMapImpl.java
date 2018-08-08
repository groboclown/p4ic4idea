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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 */
public class IdeFileMapImpl implements IdeFileMap {
    private final Project project;
    private final CacheQueryHandler cache;

    // Really unoptimized.

    public IdeFileMapImpl(@NotNull Project project, @NotNull CacheQueryHandler queryHandler) {
        this.project = project;
        this.cache = queryHandler;
    }

    @Nullable
    @Override
    public P4LocalFile forIdeFile(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return null;
        }
        ClientConfigRoot clientConfig = registry.getClientFor(file);
        if (clientConfig == null) {
            return null;
        }
        for (P4LocalFile openedFile : cache.getCachedOpenedFiles(clientConfig.getClientConfig())) {
            if (file.equals(openedFile.getFilePath().getVirtualFile())) {
                return openedFile;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public P4LocalFile forIdeFile(@Nullable FilePath file) {
        if (file == null) {
            return null;
        }
        ClientConfigRoot clientConfig = getClientFor(file);
        if (clientConfig == null) {
            return null;
        }
        for (P4LocalFile openedFile : cache.getCachedOpenedFiles(clientConfig.getClientConfig())) {
            if (file.equals(openedFile.getFilePath())) {
                return openedFile;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public P4LocalFile forDepotPath(@Nullable P4RemoteFile file) {
        if (file == null) {
            return null;
        }
        for (ClientConfigRoot root : getClientConfigRoots()) {
            for (P4LocalFile openedFile : cache.getCachedOpenedFiles(root.getClientConfig())) {
                if (file.equals(openedFile.getDepotPath())) {
                    return openedFile;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Stream<P4LocalFile> getLinkedFiles() {
        List<P4LocalFile> ret = new LinkedList<>();
        for (ClientConfigRoot root : getClientConfigRoots()) {
            ret.addAll(cache.getCachedOpenedFiles(root.getClientConfig()));
        }
        return ret.stream();
    }

    @NotNull
    @Override
    public Stream<P4LocalFile> getLinkedFiles(@NotNull ClientConfig config) {
        return cache.getCachedOpenedFiles(config).stream();
    }

    private ClientConfigRoot getClientFor(FilePath file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? null : reg.getClientFor(file);
    }

    @NotNull
    private Collection<ClientConfigRoot> getClientConfigRoots() {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? Collections.emptyList() : reg.getClientConfigRoots();
    }
}
