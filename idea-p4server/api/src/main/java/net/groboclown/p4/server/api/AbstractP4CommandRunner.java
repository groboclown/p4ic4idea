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

package net.groboclown.p4.server.api;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecQuery;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsFixedByJobQuery;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsFixedByJobResult;
import net.groboclown.p4.server.api.commands.changelist.ListJobsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListJobsResult;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsResult;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistResult;
import net.groboclown.p4.server.api.commands.client.ListClientFetchStatusQuery;
import net.groboclown.p4.server.api.commands.client.ListClientFetchStatusResult;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.commands.file.GetFileContentsResult;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesQuery;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesResult;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.server.ListLabelsQuery;
import net.groboclown.p4.server.api.commands.server.ListLabelsResult;
import net.groboclown.p4.server.api.commands.server.LoginAction;
import net.groboclown.p4.server.api.commands.server.LoginResult;
import net.groboclown.p4.server.api.commands.server.ServerInfoResult;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class AbstractP4CommandRunner implements P4CommandRunner {
    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> ActionAnswer<R> perform(
            @NotNull ServerConfig config, @NotNull ServerAction<R> action) {
        switch (action.getCmd()) {
            case LOGIN:
                return (ActionAnswer<R>) login(config, (LoginAction) action);
            case CREATE_JOB:
                return (ActionAnswer<R>) createJob(config, (CreateJobAction) action);
            default:
                throw new IllegalStateException("Incompatible class: should match " + ServerActionCmd.class);
        }
    }

    @NotNull
    protected abstract ActionAnswer<CreateJobResult> createJob(ServerConfig config, CreateJobAction action);

    @NotNull
    protected abstract ActionAnswer<LoginResult> login(ServerConfig config, LoginAction action);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> ActionAnswer<R> perform(
            @NotNull ClientConfig config, @NotNull ClientAction<R> action) {
        switch (action.getCmd()) {
            case ADD_EDIT_FILE: {
                AddEditAction fileAction = (AddEditAction) action;
                return performFileAction(config, action, fileAction.getFile(), fileAction.getFileType(),
                        P4FileAction.ADD_EDIT);
            }
            case DELETE_FILE: {
                DeleteFileAction fileAction = (DeleteFileAction) action;
                return performFileAction(config, action, fileAction.getFile(), null,
                        P4FileAction.DELETE);
            }
            case REVERT_FILE: {
                RevertFileAction fileAction = (RevertFileAction) action;
                return performFileAction(config, action, fileAction.getFile(), null,
                        P4FileAction.REVERTED);
            }

            case ADD_JOB_TO_CHANGELIST:
            case CREATE_CHANGELIST:
            case REMOVE_JOB_FROM_CHANGELIST:
            case DELETE_CHANGELIST:
            case EDIT_CHANGELIST_DESCRIPTION:
            case MOVE_FILES_TO_CHANGELIST:
                return performNonFileAction(config, action);

            // Special cases
            case FETCH_FILES:
                return (ActionAnswer<R>) fetchFiles(config, (FetchFilesAction) action);
            case MOVE_FILE:
                return (ActionAnswer<R>) moveFile(config, (MoveFileAction) action);
            case SUBMIT_CHANGELIST:
                return (ActionAnswer<R>) submitChangelist(config, (SubmitChangelistAction) action);
            default:
                throw new IllegalStateException("Incompatible class: should match " + ClientActionCmd.class);
        }
    }

    @NotNull
    protected abstract <R extends ClientResult> ActionAnswer<R> performFileAction(ClientConfig config,
            ClientAction<R> action, @NotNull FilePath file, @Nullable P4FileType fileType,
            @NotNull P4FileAction fileAction);

    @NotNull
    protected abstract <R extends ClientResult> ActionAnswer<R> performNonFileAction(
            ClientConfig config, ClientAction<R> action);

    @NotNull
    protected abstract ActionAnswer<MoveFileResult> moveFile(ClientConfig config, MoveFileAction action);

    @NotNull
    protected abstract ActionAnswer<FetchFilesResult> fetchFiles(ClientConfig config, FetchFilesAction action);

    @NotNull
    protected abstract ActionAnswer<SubmitChangelistResult> submitChangelist(
            ClientConfig config, SubmitChangelistAction action);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> QueryAnswer<R> query(
            @NotNull ServerConfig config, @NotNull ServerQuery<R> query) {
        switch (query.getCmd()) {
            case ANNOTATE_FILE:
                return (QueryAnswer<R>) getAnnotatedFile(config, (AnnotateFileQuery) query);
            case GET_FILE_CONTENTS:
                return (QueryAnswer<R>) getFileContents(config, (GetFileContentsQuery) query);
            case DESCRIBE_CHANGELIST:
                return (QueryAnswer<R>) describeChangelist(config, (DescribeChangelistQuery) query);
            case GET_JOB_SPEC:
                return (QueryAnswer<R>) getJobSpec(config, (GetJobSpecQuery) query);
            case LIST_CHANGELISTS_FIXED_BY_JOB:
                return (QueryAnswer<R>) listChangelistsFixedByJob(config, (ListChangelistsFixedByJobQuery) query);
            case LIST_CLIENTS_FOR_USER:
                return (QueryAnswer<R>) listClientsForUser(config, (ListClientsForUserQuery) query);
            case LIST_DIRECTORIES:
                return (QueryAnswer<R>) listDirectories(config, (ListDirectoriesQuery) query);
            case LIST_FILES:
                return (QueryAnswer<R>) listFiles(config, (ListFilesQuery) query);
            case LIST_FILES_DETAILS:
                return (QueryAnswer<R>) listFilesDetails(config, (ListFilesDetailsQuery) query);
            case LIST_FILE_HISTORY:
                return (QueryAnswer<R>) listFilesHistory(config, (ListFileHistoryQuery) query);
            case LIST_JOBS:
                return (QueryAnswer<R>) listJobs(config, (ListJobsQuery) query);
            case LIST_SUBMITTED_CHANGELISTS:
                return (QueryAnswer<R>) listSubmittedChangelists(config, (ListSubmittedChangelistsQuery) query);
            case LIST_USERS:
                return (QueryAnswer<R>) listUsers(config, (ListUsersQuery) query);
            case LIST_LABELS:
                return (QueryAnswer<R>) listLabels(config, (ListLabelsQuery) query);
            default:
                throw new IllegalStateException("Incompatible class: should match " + ServerQueryCmd.class);
        }
    }


    @NotNull
    protected abstract QueryAnswer<GetFileContentsResult> getFileContents(ServerConfig config, GetFileContentsQuery query);

    @NotNull
    protected abstract QueryAnswer<AnnotateFileResult> getAnnotatedFile(
            ServerConfig config, AnnotateFileQuery query);

    @NotNull
    protected abstract QueryAnswer<DescribeChangelistResult> describeChangelist(
            ServerConfig config, DescribeChangelistQuery query);

    @NotNull
    protected abstract QueryAnswer<GetJobSpecResult> getJobSpec(
            ServerConfig config, GetJobSpecQuery query);

    @NotNull
    protected abstract QueryAnswer<ListChangelistsFixedByJobResult> listChangelistsFixedByJob(
            ServerConfig config, ListChangelistsFixedByJobQuery query);

    @NotNull
    protected abstract QueryAnswer<ListClientsForUserResult> listClientsForUser(
            ServerConfig config, ListClientsForUserQuery query);

    @NotNull
    protected abstract QueryAnswer<ListDirectoriesResult> listDirectories(
            ServerConfig config, ListDirectoriesQuery query);

    @NotNull
    protected abstract QueryAnswer<ListFilesResult> listFiles(
            ServerConfig config, ListFilesQuery query);

    @NotNull
    protected abstract QueryAnswer<ListFilesDetailsResult> listFilesDetails(
            ServerConfig config, ListFilesDetailsQuery query);

    @NotNull
    protected abstract QueryAnswer<ListFileHistoryResult> listFilesHistory(
            ServerConfig config, ListFileHistoryQuery query);

    @NotNull
    protected abstract QueryAnswer<ListJobsResult> listJobs(
            ServerConfig config, ListJobsQuery query);

    @NotNull
    protected abstract QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(
            ServerConfig config, ListSubmittedChangelistsQuery query);

    @NotNull
    protected abstract QueryAnswer<ListUsersResult> listUsers(
            ServerConfig config, ListUsersQuery query);

    @NotNull
    protected abstract QueryAnswer<ListLabelsResult> listLabels(ServerConfig config, ListLabelsQuery query);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> QueryAnswer<R> query(
            @NotNull ClientConfig config, @NotNull ClientQuery<R> query) {
        switch (query.getCmd()) {
            case LIST_CLIENT_FETCH_STATUS:
                return (QueryAnswer<R>) listClientFetchStatus(config, (ListClientFetchStatusQuery) query);
            case LIST_OPENED_FILES_CHANGES:
                return (QueryAnswer<R>) listOpenedFilesChanges(config, (ListOpenedFilesChangesQuery) query);
            default:
                throw new IllegalStateException("Incompatible class: should match " + ClientQueryCmd.class);
        }
    }

    @NotNull
    protected abstract QueryAnswer<ListClientFetchStatusResult> listClientFetchStatus(
            ClientConfig config, ListClientFetchStatusQuery query);

    @NotNull
    protected abstract QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(
            ClientConfig config, ListOpenedFilesChangesQuery query);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerNameResult> QueryAnswer<R> query(
            @NotNull P4ServerName name, @NotNull ServerNameQuery<R> query) {
        switch (query.getCmd()) {
            case SERVER_INFO:
                return (QueryAnswer<R>) serverInfo(name, (ServerInfo) query);
            default:
                throw new IllegalStateException("Incompatible class: should match " + ServerNameQueryCmd.class);
        }
    }

    @NotNull
    protected abstract QueryAnswer<ServerInfoResult> serverInfo(P4ServerName name, ServerInfo query);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> R syncCachedQuery(@NotNull ServerConfig config, @NotNull SyncServerQuery<R> query) {
        switch (query.getCmd()) {
            default:
                throw new IllegalStateException("Incompatible class: should match " + SyncServerQueryCmd.class);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> R syncCachedQuery(@NotNull ClientConfig config, @NotNull SyncClientQuery<R> query) {
        switch (query.getCmd()) {
            case SYNC_LIST_OPENED_FILES_CHANGES:
                return (R) cachedListOpenedFilesChanges(config, (SyncListOpenedFilesChangesQuery) query);
            default:
                throw new IllegalStateException("Incompatible class: should match " + SyncClientQueryCmd.class);
        }
    }

    @NotNull
    protected abstract ListOpenedFilesChangesResult cachedListOpenedFilesChanges(ClientConfig config, SyncListOpenedFilesChangesQuery query);

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ServerResult> FutureResult<R> syncQuery(@NotNull ServerConfig config,
            @NotNull SyncServerQuery<R> query) {
        switch (query.getCmd()) {
            default:
                throw new IllegalStateException("Incompatible class: should match " + SyncServerQueryCmd.class);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <R extends ClientResult> FutureResult<R> syncQuery(@NotNull ClientConfig config,
            @NotNull SyncClientQuery<R> query) {
        switch (query.getCmd()) {
            case SYNC_LIST_OPENED_FILES_CHANGES: {
                SyncListOpenedFilesChangesQuery q = (SyncListOpenedFilesChangesQuery) query;
                // TODO this looks like a double query on the cache, when it should really be just a single one.
                return (FutureResult<R>) new FutureResult<>(
                        listOpenedFilesChanges(config, new ListOpenedFilesChangesQuery(
                                q.getMaxFileResults(), q.getMaxChangelistResults()
                        )), cachedListOpenedFilesChanges(config, q));
            }
            default:
                throw new IllegalStateException("Incompatible class: should match " + SyncClientQueryCmd.class);
        }
    }
}
