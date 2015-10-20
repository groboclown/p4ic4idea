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
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListIdImpl;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.server.FileSyncResult;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.P4Server.IntegrateFile;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.local.IgnoreFiles;
import net.groboclown.idea.p4ic.v2.server.cache.state.ClientLocalServerState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.cache.state.PendingUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.*;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection.CreateUpdate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Public front-end to the cache infrastructure.
 */
public class ClientCacheManager {
    private final ClientLocalServerState state;
    private final WorkspaceServerCacheSync workspace;
    private final FileActionsServerCacheSync fileActions;
    private final ChangeListServerCacheSync changeLists;
    private final JobStatusListStateServerCacheSync jobStatusList;
    private final IgnoreFiles ignoreFiles;

    // Jobs are only stored in terms of their association with the
    // changelists.  The current design is to have the jobs only
    // be managed in terms of their association with changelists;
    // creation and other actions on jobs is not supported.

    public ClientCacheManager(@NotNull ServerConfig config, @NotNull ClientLocalServerState state) {
        this.state = state;

        final CacheImpl cache = new CacheImpl();

        workspace = new WorkspaceServerCacheSync(cache, state.getFileMappingRepo(),
                state.getCachedServerState().getWorkspaceView());
        fileActions = new FileActionsServerCacheSync(cache,
                state.getLocalClientState().getUpdatedFiles(),
                state.getCachedServerState().getUpdatedFiles());
        changeLists = new ChangeListServerCacheSync(cache,
                state.getLocalClientState().getChanges(),
                state.getCachedServerState().getChanges());
        jobStatusList = new JobStatusListStateServerCacheSync(
                state.getCachedServerState().getJobStatusList());
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

    @NotNull
    public ServerQuery createChangeListRefreshQuery() {
        return changeLists.createRefreshQuery();
    }

    @NotNull
    public ServerQuery createJobStatusListRefreshQuery() {
        return jobStatusList.createRefreshQuery();
    }

    /**
     * Jobs are not cached like other things, because there can be potentially a huge
     * number of jobs.  Instead, only the jobs the user asks about will be cached.
     *
     * @param jobIds the job IDs to refresh in the query.
     * @return the server query
     */
    @NotNull
    public ServerQuery createJobRefreshQuery(final Collection<String> jobIds) {
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }


    /**
     * Caller should be run through a {@link CreateUpdate},
     * so that the returned {@link PendingUpdateState} is handled correctly.
     */
    @Nullable
    public PendingUpdateState addOrEditFile(@NotNull Project project, @NotNull FilePath file, int changeListId) {
        return fileActions.addOrEditFile(project, file, changeListId);
    }


    /**
     * Caller should be run through a {@link CreateUpdate},
     * so that the returned {@link PendingUpdateState} is handled correctly.
     */
    @Nullable
    public PendingUpdateState editFile(@NotNull Project project, @NotNull VirtualFile file, int changeListId) {
        return fileActions.editFile(project, file, changeListId);
    }

    @Nullable
    public PendingUpdateState deleteFile(@NotNull Project project, @NotNull FilePath file, final int changeListId) {
        return fileActions.deleteFile(project, file, changeListId);
    }

    @Nullable
    public PendingUpdateState deleteChangelist(final int changeListId) {
        return changeLists.deleteChangelist(changeListId);
    }

    @NotNull
    public List<VirtualFile> getClientRoots(@NotNull Project project, @NotNull AlertManager alerts) {
        return workspace.getClientRoots(project, alerts);
    }

    @NotNull
    public Collection<P4ChangeListValue> getCachedOpenedChanges() {
        return changeLists.getOpenedChangeLists();
    }

    @NotNull
    public Collection<P4FileAction> getCachedOpenFiles() {
        return fileActions.getOpenFiles();
    }

    @NotNull
    public Collection<String> getCachedJobStatusList() {
        return jobStatusList.getJobStatusList();
    }

    @NotNull
    public Map<String, P4ChangeListJob> getCachedJobIds(final Collection<String> jobIds) {
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }

    /**
     * This method only has one use, and that's for initial setup after loading into a ServerConnection.
     */
    @NotNull
    public Collection<PendingUpdateState> getCachedPendingUpdates() {
        return state.getPendingUpdates();
    }

    public void addPendingUpdateState(@NotNull final PendingUpdateState updateState) {
        state.addPendingUpdate(updateState);
    }

    public void removePendingUpdateStates(@NotNull Collection<PendingUpdateState> pendingUpdateStates) {
        for (PendingUpdateState pendingUpdateState : pendingUpdateStates) {
            state.removePendingUpdate(pendingUpdateState);
        }
    }

    public boolean isIgnored(@NotNull FilePath fp) {
        return ignoreFiles.isFileIgnored(fp);
    }

    /**
     * Create a non-committed changelist ID.  It will still need to be created in a {@link PendingUpdateState}.
     * @return change list ID
     */
    public P4ChangeListId reserveLocalChangelistId() {
        int id = changeLists.reserveLocalChangelistId();
        return new P4ChangeListIdImpl(state.getClientServerId(), id);
    }

    @Nullable
    public PendingUpdateState moveFilesToChangelist(@NotNull Collection<FilePath> files,
            @NotNull LocalChangeList source, int changeListId) {
        return changeLists.moveFileToChangelist(files, source, changeListId);
    }

    @Nullable
    public PendingUpdateState renameChangelist(final int changeListId, final String description) {
        return changeLists.renameChangelist(changeListId, description);
    }

    @NotNull
    public Collection<P4ChangeListJob> getCachedJobsInChangelists(final Collection<P4ChangeListId> changes) {
        return changeLists.getJobsInChangelists(changes);
    }


    @Nullable
    public PendingUpdateState moveFile(@NotNull Project project, @NotNull IntegrateFile file, int changelistId) {
        return fileActions.moveFile(project, file, changelistId);
    }

    @Nullable
    public PendingUpdateState integrateFile(@NotNull final IntegrateFile file, final int changelistId) {
        return fileActions.integrateFile(file, changelistId);
    }

    @Nullable
    public ServerUpdateAction revertFileOnline(@NotNull final List<FilePath> files,
            @NotNull final Ref<MessageResult<Collection<FilePath>>> ret) {
        return fileActions.revertFileOnline(files, ret);
    }

    @Nullable
    public ServerUpdateAction revertFileIfUnchangedOnline(@NotNull final Collection<FilePath> files,
            @NotNull final Ref<MessageResult<Collection<FilePath>>> ret) {
        return fileActions.revertFileIfUnchangedOnline(files, ret);
    }

    @Nullable
    public ServerUpdateAction synchronizeFilesOnline(@NotNull final Collection<FilePath> files, final int revisionNumber,
            @Nullable final String syncSpec, final boolean force,
            final Ref<MessageResult<Collection<FileSyncResult>>> ref) {
        return fileActions.synchronizeFilesOnline(files, revisionNumber, syncSpec, force, ref);
    }

    @Nullable
    public ServerUpdateAction submitChangelistOnline(@NotNull final List<FilePath> files,
            @NotNull final List<P4ChangeListJob> jobs,
            @Nullable final String submitStatus, final int changelistId,
            @NotNull Ref<VcsException> problem) {
        return fileActions.submitChangelist(files, jobs, submitStatus, changelistId, problem);
    }

    // ----------------------------------------------------------------------------------------
    // Package-level behaviors for use in an action

    void markLocalChangelistStateCommitted(int changeListId) {
        changeLists.markLocalStateCommitted(changeListId);
    }

    void markLocalFileStateCommitted(@NotNull FilePath file) {
        fileActions.markLocalFileStateAsCommitted(file);
    }

    private class CacheImpl implements Cache {

        @NotNull
        @Override
        public List<VirtualFile> getClientRoots(@NotNull final Project project, @NotNull AlertManager alerts) {
            return workspace.getClientRoots(project, alerts);
        }

        @Nullable
        @Override
        public VirtualFile getBestClientRoot(@NotNull Project project, @NotNull final File referenceDir, @NotNull AlertManager alerts) {
            return workspace.getBestClientRoot(project, referenceDir, alerts);
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
        public Collection<P4FileUpdateState> fromOpenedToAction(@NotNull Project project,
                @NotNull final List<IExtendedFileSpec> validSpecs,
                @NotNull final AlertManager alerts) {
            return workspace.fromOpenedToAction(project, validSpecs, alerts);
        }

        @Override
        public void refreshServerState(@NotNull P4Exec2 exec, @NotNull AlertManager alerts) {
            // Refresh everything except workspace, as that would cause a recursive loop.
            fileActions.innerLoadServerCache(exec, alerts);
            changeLists.innerLoadServerCache(exec, alerts);
            // not refreshable, because no real server state: ignoreFiles
        }

        @Override
        public boolean isFileIgnored(@Nullable final FilePath file) {
            return ignoreFiles.isFileIgnored(file);
        }
    }

}
