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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.local.IgnoreFiles;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Public front-end to the cache infrastructure.
 */
public class ClientCacheManager {
    private final ClientLocalServerState state;
    private final WorkspaceServerCacheSync workspace;
    private final FileActionsServerCacheSync fileActions;
    private final IgnoreFiles ignoreFiles;

    public ClientCacheManager(@NotNull ServerConfig config, @NotNull ClientLocalServerState state) {
        this.state = state;

        final CacheImpl cache = new CacheImpl();

        workspace = new WorkspaceServerCacheSync(cache, state.getFileMappingRepo(),
                state.getCachedServerState().getWorkspaceView());
        fileActions = new FileActionsServerCacheSync(cache,
                state.getLocalClientState().getUpdatedFiles(),
                state.getCachedServerState().getUpdatedFiles());
        ignoreFiles = new IgnoreFiles(config);
    }


    @NotNull
    public ServerQuery createWorkspaceRefreshQuery() {
        return workspace.createWorkspaceRefreshQuery();
    }

    @NotNull
    public ServerQuery createFileActionsRefreshQuery() {
        return fileActions.createFileActionsRefreshQuery();
    }

    @Nullable
    public PendingUpdateState editFile(@NotNull FilePath file, int changeListId) {
        return fileActions.editFile(file, changeListId);
    }


    private class CacheImpl implements Cache {

        @NotNull
        @Override
        public List<VirtualFile> getClientRoots(@NotNull final Project project, @NotNull AlertManager alerts) {
            return workspace.getClientRoots(project, alerts);
        }

        @Nullable
        @Override
        public VirtualFile getBestClientRoot(@NotNull final File referenceDir, @NotNull AlertManager alerts) {
            return workspace.getBestClientRoot(referenceDir, alerts);
        }

        @NotNull
        @Override
        public ClientServerId getClientServerId() {
            return state.getClientServerId();
        }

        @NotNull
        @Override
        public String getClientName() {
            return getClientServerId().getClientId();
        }

        @NotNull
        @Override
        public P4ClientFileMapping getClientMappingFor(@NotNull final FilePath file) {
            return workspace.getClientMappingFor(file);
        }

        @NotNull
        @Override
        public Collection<P4FileUpdateState> fromOpenedToAction(@NotNull final List<IFileSpec> validSpecs,
                @NotNull final AlertManager alerts) {
            return workspace.fromOpenedToAction(validSpecs, alerts);
        }

        @Override
        public void refreshServerState() {
            // FIXME
            throw new IllegalStateException("not implemented");
        }

        @Override
        public boolean isFileIgnored(@Nullable final FilePath file) {
            return ignoreFiles.isFileIgnored(file);
        }
    }
}
