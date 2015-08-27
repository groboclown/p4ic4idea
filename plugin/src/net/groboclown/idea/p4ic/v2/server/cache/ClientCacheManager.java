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

package net.groboclown.idea.p4ic.v2.server.cache;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.v2.server.cache.local.IgnoreFiles;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Public front-end to the cache infrastructure.
 */
public class ClientCacheManager {
    private final Client client;
    private final ClientLocalServerState state;
    private final WorkspaceView workspaceView;
    private final FileActionsView fileActionsView;
    private final IgnoreFiles ignoreFiles;

    public ClientCacheManager(@NotNull Project project, @NotNull Client client, @NotNull ClientLocalServerState state) {
        this.client = client;
        this.state = state;

        final CacheImpl cache = new CacheImpl();

        workspaceView = new WorkspaceView(project, cache, state.getFileMappingRepo(),
                state.getCachedServerState().getWorkspaceView());
        fileActionsView = new FileActionsView(project, cache,
                state.getLocalClientState().getUpdatedFiles(),
                state.getCachedServerState().getUpdatedFiles());
        ignoreFiles = new IgnoreFiles(client);
    }


    @NotNull
    public ServerQuery createWorkspaceRefreshQuery() {
        return workspaceView.createWorkspaceRefreshQuery();
    }

    @Nullable
    public PendingUpdateState editFile(@NotNull FilePath file, int changeListId) {
        return fileActionsView.editFile(file, changeListId);
    }


    private class CacheImpl implements Cache {

        @NotNull
        @Override
        public Client getClient() {
            return client;
        }

        @NotNull
        @Override
        public String getClientName() {
            return client.getClientName();
        }

        @NotNull
        @Override
        public P4ClientFileMapping getClientMappingFor(@NotNull final FilePath file) {
            return workspaceView.getClientMappingFor(file);
        }

        @NotNull
        @Override
        public Collection<P4FileUpdateState> fromOpenedToAction(@NotNull final List<IFileSpec> validSpecs,
                @NotNull final AlertManager alerts) {
            return workspaceView.fromOpenedToAction(validSpecs, alerts);
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
