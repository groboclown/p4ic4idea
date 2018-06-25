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

package net.groboclown.p4.server;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import net.groboclown.p4.server.api.AbstractP4CommandRunner;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.messagebus.ClientActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.DescribeChangelistCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.FileActionMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobSpecCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ListClientsForUserCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.ServerActionCacheMessage;
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
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesQuery;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.file.ListFilesHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesResult;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.commands.server.LoginAction;
import net.groboclown.p4.server.api.commands.server.LoginResult;
import net.groboclown.p4.server.api.commands.server.ServerInfoResult;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.commands.ActionAnswerImpl;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.commands.OfflineActionAnswerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Layers a cache and server.
 */
public class TopCommandRunner extends AbstractP4CommandRunner
        implements Disposable {

    private final CacheQueryHandler cache;
    private final AbstractServerCommandRunner server;
    private final Map<String, ServerConnectionState> stateCache = new HashMap<>();
    private boolean disposed;


    public TopCommandRunner(@NotNull Project project,
            @NotNull CacheQueryHandler cache, @NotNull AbstractServerCommandRunner server) {
        this.cache = cache;
        this.server = server;

        MessageBusClient.ApplicationClient appClient = MessageBusClient.forApplication(this);
        MessageBusClient.ProjectClient projClient = MessageBusClient.forProject(project, this);
        ServerConnectedMessage.addListener(appClient, (serverConfig, loggedIn) -> {
            ServerConnectionState state = getStateFor(serverConfig);
            state.badConnection = false;
            state.userOffline = false;

            if (loggedIn) {
                state.needsLogin = false;
                state.badLogin = false;
            }

            sendPendingCacheRequests(serverConfig);
        });
        UserSelectedOfflineMessage.addListener(projClient, name -> {
            // User wants to work offline, regardless of connection status.
            for (ServerConnectionState state : getStatesFor(name)) {
                state.userOffline = true;
                server.disconnect(state.config);
            }
        });
        ReconnectRequestMessage.addListener(projClient, new ReconnectRequestMessage.Listener() {
            // The user requested to go online, so clear out the states
            // that might cause a request to not be fulfilled.
            @Override
            public void reconnectToAllClients(boolean mayDisplayDialogs) {
                for (ServerConnectionState state : getAllStates()) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;

                    sendPendingCacheRequests(state.config);
                }
            }

            @Override
            public void reconnectToClient(@NotNull ClientServerRef ref, boolean mayDisplayDialogs) {
                for (ServerConnectionState state : getStatesFor(ref.getServerName())) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;

                    sendPendingCacheRequests(state.config);
                }
            }
        });
        LoginFailureMessage.addListener(appClient, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = true;
            }

            @Override
            public void sessionExpired(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = false;
                getStateFor(config).needsLogin = true;
            }

            @Override
            public void passwordInvalid(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = true;
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerConfig config, @NotNull AuthenticationFailedException e) {
                getStateFor(config).badLogin = false;
                getStateFor(config).passwordUnnecessary = true;
            }
        });
        ConnectionErrorMessage.addListener(appClient, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public void onHostConnectionError(@NotNull P4ServerName serverName, @Nullable ServerConfig serverConfig,
                    @Nullable Exception e) {
                if (serverConfig != null) {
                    getStateFor(serverConfig).badConnection = true;
                } else {
                    for (ServerConnectionState state : getStatesFor(serverName)) {
                        state.badConnection = true;
                    }
                }
            }
        });
    }


    @NotNull
    @Override
    protected ActionAnswer<CreateJobResult> createJob(ServerConfig config, CreateJobAction action) {
        ServerActionCacheMessage.sendEvent(new ServerActionCacheMessage.Event(
                config.getServerName(), action));
        return onlineCheck(config,
                () -> server.perform(config, action)
                    .whenCompleted((result) ->
                        ServerActionCacheMessage.sendEvent(new ServerActionCacheMessage.Event(
                            config.getServerName(), action, result)))
                    .whenServerError((err) ->
                        ServerActionCacheMessage.sendEvent(new ServerActionCacheMessage.Event(
                            config.getServerName(), action, err))
                    ),
                OfflineActionAnswerImpl::new);
    }


    @NotNull
    @Override
    protected ActionAnswer<LoginResult> login(ServerConfig config, LoginAction action) {
        ServerConnectionState state = getStateFor(config);

        // Login has special state checking, because all it cares about is online vs. offline, not login
        // status.
        if (state.badLogin && !state.userOffline && !state.badConnection) {
            return server.perform(config, action)
                    .whenCompleted((resp) -> {
                        // Login was good.
                        ServerConnectedMessage.send().serverConnected(config, true);
                        state.needsLogin = false;
                        state.badConnection = false;
                    });
        }
        return new OfflineActionAnswerImpl<>();
    }


    @NotNull
    @Override
    protected ActionAnswer<FetchFilesResult> fetchFiles(ClientConfig config, FetchFilesAction action) {
        // No cache updates
        return onlineCheck(config,
                () -> server.perform(config, action),
                OfflineActionAnswerImpl::new);
    }


    @NotNull
    protected ActionAnswer<SubmitChangelistResult> submitChangelist(
            ClientConfig config, SubmitChangelistAction action) {
        // Submits are never cached.  Instead, offline submits generate an error.
        return onlineCheck(config,
                () -> server.perform(config, action),
                () -> new ActionAnswerImpl<>(Answer.reject(
                        // FIXME implement correct offline answer
                        null
                )));
    }

    @NotNull
    @Override
    protected ActionAnswer<MoveFileResult> moveFile(ClientConfig config, MoveFileAction action) {
        // Move file is turned into 2 messages.
        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                action.getSourceFile(), P4FileAction.MOVE_DELETE, null, action));
        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                action.getTargetFile(), P4FileAction.MOVE_ADD_EDIT, null, action));
        return onlineCheck(config,
                () -> server.perform(config, action)
                    .whenCompleted((result) -> {
                        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                action.getSourceFile(), P4FileAction.MOVE_DELETE, null, action, result));
                        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                action.getTargetFile(), P4FileAction.MOVE_ADD_EDIT, null, action, result));
                    })
                    .whenServerError((t) -> {
                        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                action.getSourceFile(), P4FileAction.MOVE_DELETE, null, action, t));
                        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                action.getTargetFile(), P4FileAction.MOVE_ADD_EDIT, null, action, t));

                    }),
                OfflineActionAnswerImpl::new
        );
    }


    @NotNull
    @Override
    protected <R extends ClientResult> ActionAnswer<R> performFileAction(ClientConfig config,
            ClientAction<R> action, @NotNull FilePath file, @Nullable P4FileType fileType,
            @NotNull P4FileAction fileAction) {
        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                file, fileAction, fileType, action));
        return onlineCheck(config,
                () -> server.perform(config, action)
                        .whenCompleted((result) ->
                            FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                file, fileAction, fileType, action, result)))
                        .whenServerError((t) ->
                            FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                                    file, fileAction, fileType, action, t))),
                OfflineActionAnswerImpl::new
        );
    }


    @NotNull
    @Override
    protected <R extends ClientResult> ActionAnswer<R> performNonFileAction(ClientConfig config, ClientAction<R> action) {
        ClientActionMessage.sendEvent(new ClientActionMessage.Event(
                config.getClientServerRef(), action));
        return onlineCheck(config,
                () -> server.perform(config, action)
                        .whenCompleted((result) ->
                            ClientActionMessage.sendEvent(new ClientActionMessage.Event(
                                config.getClientServerRef(), action, result)))
                        .whenServerError((t) ->
                            ClientActionMessage.sendEvent(new ClientActionMessage.Event(
                                config.getClientServerRef(), action, t))),
                OfflineActionAnswerImpl::new
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<AnnotateFileResult> getAnnotatedFile(ServerConfig config, AnnotateFileQuery query) {
        // No cache for file annotations.
        return onlineCheck(config,
                () -> server.getFileAnnotation(config, query),
                () -> new DoneQueryAnswer<>(new AnnotateFileResult(config, annotatedFile))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<DescribeChangelistResult> describeChangelist(ServerConfig config, DescribeChangelistQuery query) {
        return onlineCheck(config,
                () -> server.describeChangelist(config, query)
                        .whenCompleted((result) ->
                            DescribeChangelistCacheMessage.sendEvent(
                                new DescribeChangelistCacheMessage.Event(config.getServerName(),
                                        result.getRequestedChangelist(), result.getRemoteChangelist()))),
                () -> new DoneQueryAnswer<>(new DescribeChangelistResult(config,
                        query.getChangelistId(),
                        cache.getCachedChangelist(config.getServerName(),
                                query.getChangelistId()),
                        true))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<GetJobSpecResult> getJobSpec(ServerConfig config, GetJobSpecQuery query) {
        return onlineCheck(config,
                () -> server.getJobSpec(config)
                        .whenCompleted((result) ->
                            JobSpecCacheMessage.sendEvent(new JobSpecCacheMessage.Event(
                                config.getServerName(), result.getJobSpec()))
                        ),
                () -> new DoneQueryAnswer<>(new GetJobSpecResult(config,
                        cache.getCachedJobSpec(config.getServerName())))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListChangelistsFixedByJobResult> listChangelistsFixedByJob(ServerConfig config,
            ListChangelistsFixedByJobQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientsForUserResult> listClientsForUser(ServerConfig config, ListClientsForUserQuery query) {
        return onlineCheck(config,
                () -> server.getClientsForUser(config, query)
                        .whenCompleted((result) ->
                                ListClientsForUserCacheMessage.sendEvent(new ListClientsForUserCacheMessage.Event(
                                        config.getServerName(), result.getRequestedUser(), result.getClients()
                                ))
                        ),
                () -> new DoneQueryAnswer<>(new ListClientsForUserResult(config, query.getUsername(),
                        cache.getCachedClientsForUser(config.getServerName(), query.getUsername())))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListDirectoriesResult> listDirectories(ServerConfig config, ListDirectoriesQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesResult> listFiles(ServerConfig config, ListFilesQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesDetailsResult> listFilesDetails(ServerConfig config, ListFilesDetailsQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesHistoryResult> listFilesHistory(ServerConfig config, ListFilesHistoryQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListJobsResult> listJobs(ServerConfig config, ListJobsQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(ServerConfig config,
            ListSubmittedChangelistsQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListUsersResult> listUsers(ServerConfig config, ListUsersQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientFetchStatusResult> listClientFetchStatus(ClientConfig config,
            ListClientFetchStatusQuery query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(ClientConfig config,
            ListOpenedFilesChangesQuery query) {
        return onlineCheck(config,
                () -> server.listOpenedFilesChanges(config, query),
                () -> new DoneQueryAnswer<>(cachedListOpenedFilesChanges(config, null))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ServerInfoResult> serverInfo(P4ServerName name, ServerInfo query) {
        // FIXME implement
        return null;
    }


    @NotNull
    @Override
    protected ListOpenedFilesChangesResult cachedListOpenedFilesChanges(ClientConfig config,
            SyncListOpenedFilesChangesQuery query) {
        return new ListOpenedFilesChangesResult(config,
                cache.getCachedOpenedFiles(config),
                cache.getCachedOpenedChangelists(config));
    }


    @Override
    public void dispose() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    private ServerConnectionState getStateFor(@NotNull ServerConfig config) {
        ServerConnectionState state;
        synchronized (stateCache) {
            state = stateCache.get(config.getServerId());
            if (state == null) {
                state = new ServerConnectionState(config);
                stateCache.put(config.getServerId(), state);
            }
        }
        return state;
    }

    private Collection<ServerConnectionState> getStatesFor(P4ServerName name) {
        List<ServerConnectionState> ret = new ArrayList<>();
        synchronized (stateCache) {
            for (ServerConnectionState state : stateCache.values()) {
                if (state.config.getServerName().equals(name)) {
                    ret.add(state);
                }
            }
        }
        return ret;
    }

    private Collection<ServerConnectionState> getAllStates() {
        synchronized (stateCache) {
            return new ArrayList<>(stateCache.values());
        }
    }

    private void sendPendingCacheRequests(ServerConfig serverConfig) {
        // FIXME pull the pending actions and perform them, and perform
        // and requested cache updates.
    }

    private <R> R onlineCheck(@NotNull ClientConfig clientConfig, Exec<R> serverExec, Exec<R> cacheExec) {
        return onlineCheck(clientConfig.getServerConfig(), serverExec, cacheExec);
    }

    private <R> R onlineCheck(@NotNull ServerConfig serverConfig, Exec<R> serverExec, Exec<R> cacheExec) {
        ServerConnectionState state = getStateFor(serverConfig);
        // TODO if the state is something that requires a login, should that be done here?
        if (!(state.badConnection || state.badLogin || state.userOffline || state.needsLogin)) {
            return serverExec.exec();
        }
        return cacheExec.exec();
    }

    interface Exec<R> {
        R exec();
    }

    interface ExecThrows<R> {
        R exec() throws P4CommandRunner.ServerResultException;
    }

    private static class ServerConnectionState {
        final ServerConfig config;
        boolean badConnection;
        boolean badLogin;
        boolean needsLogin;
        boolean passwordUnnecessary;
        boolean userOffline;

        private ServerConnectionState(ServerConfig config) {
            this.config = config;
        }
    }
}
