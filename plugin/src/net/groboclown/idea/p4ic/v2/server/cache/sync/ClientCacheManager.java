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
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection.CreateUpdate;
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
        return workspace.createRefreshQuery();
    }

    @NotNull
    public ServerQuery createFileActionsRefreshQuery() {
        return fileActions.createRefreshQuery();
    }


    /**
     * Caller should be run through a {@link CreateUpdate},
     * so that the returned {@link PendingUpdateState} is handled correctly.
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull FilePath file, int changeListId) {
        return fileActions.editFile(file, changeListId);
    }

    @NotNull
    public List<VirtualFile> getClientRoots(@NotNull Project project, @NotNull AlertManager alerts) {
        return workspace.getClientRoots(project, alerts);
    }

    public void addPendingUpdateState(@NotNull final PendingUpdateState updateState) {
        state.addPendingUpdate(updateState);
    }

    /** This method only has one use, and that's for initial setup after loading into a ServerConnection. */
    public Collection<PendingUpdateState> getCachedPendingUpdates() {
        return state.getPendingUpdates();
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
            String ret = getClientServerId().getClientId();
            if (ret == null) {
                ret = "";
            }
            return ret;
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
        public void refreshServerState(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
            // Refresh everything except workspace, as that would cause a recursive loop.
            fileActions.innerLoadServerCache(exec, alerts);
        }

        @Override
        public boolean isFileIgnored(@Nullable final FilePath file) {
            return ignoreFiles.isFileIgnored(file);
        }
    }

}
