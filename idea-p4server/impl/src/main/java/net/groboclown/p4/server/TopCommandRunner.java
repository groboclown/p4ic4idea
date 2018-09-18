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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import net.groboclown.p4.server.api.AbstractP4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4.server.api.cache.CachePendingActionHandler;
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
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.commands.file.GetFileContentsResult;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesQuery;
import net.groboclown.p4.server.api.commands.file.ListDirectoriesResult;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.file.ListFilesQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesResult;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
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
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.LoginFailureMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.ServerErrorEvent;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.commands.ErrorQueryAnswerImpl;
import net.groboclown.p4.server.impl.commands.OfflineActionAnswerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;


/**
 * Layers a queryCache and server.
 */
public class TopCommandRunner extends AbstractP4CommandRunner
        implements Disposable {
    private static final Logger LOG = Logger.getInstance(TopCommandRunner.class);


    private final CacheQueryHandler queryCache;
    private final CachePendingActionHandler pendingActionCache;
    private final AbstractServerCommandRunner server;
    private final Map<String, ServerConnectionState> stateCache = new HashMap<>();
    private boolean disposed;


    /** NOTE: callers must properly register the dispose chain. */
    public TopCommandRunner(@NotNull Project project,
            @NotNull CacheQueryHandler queryCache, @NotNull CachePendingActionHandler pendingActionCache,
            @NotNull AbstractServerCommandRunner server) {
        this.queryCache = queryCache;
        this.pendingActionCache = pendingActionCache;
        this.server = server;

        final Map<VirtualFile, ClientConfig> clientConfigs = new HashMap<>();

        MessageBusClient.ApplicationClient appClient = MessageBusClient.forApplication(this);
        MessageBusClient.ProjectClient projClient = MessageBusClient.forProject(project, this);
        ClientConfigAddedMessage.addListener(projClient, this, (e) -> clientConfigs.put(e.getRoot(), e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(projClient, this,
                (event) -> clientConfigs.remove(event.getVcsRootDir()));
        ClientConfigAddedMessage.addServerListener(appClient, this,
                // Creates the state for the server config, if it doesn't already exist.
                // This prevents issues with the user going offline before any action on the
                // server happens.
                (e) -> this.getProjectStateFor(e.getServerConfig()));
        ServerConnectedMessage.addListener(appClient, this, (e) -> {
            // This is an application-level event listener for server config state changes.
            Optional<ServerConnectionState> op = getAppStateFor(e.getServerConfig());
            if (!op.isPresent()) {
                return;
            }
            ServerConnectionState state = op.get();
            state.badConnection = false;
            state.userOffline = false;

            if (e.isLoggedIn()) {
                state.needsLogin = false;
                state.badLogin = false;
            }

            for (ClientConfig clientConfig: clientConfigs.values()) {
                if (clientConfig.isIn(e.getServerConfig())) {
                    sendCachedPendingRequests(clientConfig);
                }
            }
        });
        UserSelectedOfflineMessage.addListener(projClient, this, e -> {
            // User wants to work offline, regardless of connection status.
            for (ServerConnectionState state : getStatesFor(e.getName())) {
                state.userOffline = true;
                server.disconnect(state.config.getServerName());
            }
        });
        ReconnectRequestMessage.addListener(projClient, this, new ReconnectRequestMessage.Listener() {
            // The user requested to go online, so clear out the states
            // that might cause a request to not be fulfilled.
            @Override
            public void reconnectToAllClients(@NotNull ReconnectRequestMessage.ReconnectAllEvent e) {
                for (ServerConnectionState state : getAllStates()) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;
                }
                for (ClientConfig clientConfig: clientConfigs.values()) {
                    sendCachedPendingRequests(clientConfig);
                }
            }

            @Override
            public void reconnectToClient(@NotNull ReconnectRequestMessage.ReconnectEvent e) {
                for (ServerConnectionState state : getStatesFor(e.getRef().getServerName())) {
                    state.userOffline = false;
                    state.badConnection = false;
                    state.badLogin = false;
                    state.needsLogin = false;
                }
                for (ClientConfig clientConfig: clientConfigs.values()) {
                    if (clientConfig.getClientServerRef().equals(e.getRef())) {
                        sendCachedPendingRequests(clientConfig);
                    }
                }
            }
        });
        LoginFailureMessage.addListener(appClient, this, new LoginFailureMessage.Listener() {
            @Override
            public void singleSignOnFailed(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                getAppStateFor(e.getConfig()).ifPresent(s -> s.badLogin = true);
            }

            @Override
            public void singleSignOnExecutionFailed(@NotNull LoginFailureMessage.SingleSignOnExecutionFailureEvent e) {
                getAppStateFor(e.getConfig()).ifPresent(s -> s.badLogin = true);
            }

            @Override
            public void sessionExpired(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                getAppStateFor(e.getConfig()).ifPresent(s -> {
                    s.badLogin = false;
                    s.needsLogin = true;
                });
            }

            @Override
            public void passwordInvalid(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                getAppStateFor(e.getConfig()).ifPresent(s -> s.badLogin = true);
            }

            @Override
            public void passwordUnnecessary(@NotNull ServerErrorEvent.ServerConfigErrorEvent<AuthenticationFailedException> e) {
                getAppStateFor(e.getConfig()).ifPresent(s -> {
                    s.badLogin = false;
                    s.passwordUnnecessary = true;
                });
            }
        });
        ConnectionErrorMessage.addListener(appClient, this, new ConnectionErrorMessage.AllErrorListener() {
            @Override
            public <E extends Exception> void onHostConnectionError(@NotNull ServerErrorEvent<E> event) {
                if (event.getConfig() != null) {
                    getAppStateFor(event.getConfig()).ifPresent(s -> s.badConnection = true);
                } else {
                    for (ServerConnectionState state : getStatesFor(event.getName())) {
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
        return onlineExec(config,
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
        ServerConnectionState state = getProjectStateFor(config);

        // Login has special state checking, because all it cares about is online vs. offline, not login
        // status.
        if (state.badLogin && !state.userOffline && !state.badConnection) {
            return server.perform(config, action)
                    .whenCompleted((resp) -> {
                        // Login was good.
                        ServerConnectedMessage.send().serverConnected(
                                new ServerConnectedMessage.ServerConnectedEvent(config, true));
                        state.needsLogin = false;
                        state.badConnection = false;
                    });
        }
        return new OfflineActionAnswerImpl<>();
    }


    @NotNull
    @Override
    protected ActionAnswer<FetchFilesResult> fetchFiles(ClientConfig config, FetchFilesAction action) {
        // No queryCache updates
        return onlineExec(config,
                () -> server.perform(config, action),
                OfflineActionAnswerImpl::new);
    }


    @NotNull
    protected ActionAnswer<SubmitChangelistResult> submitChangelist(
            ClientConfig config, SubmitChangelistAction action) {
        // Submits are never cached.  Instead, offline submits generate an error.
        return onlineExec(config,
                () -> server.perform(config, action),
                OfflineActionAnswerImpl::new
                );
    }

    @NotNull
    @Override
    protected ActionAnswer<MoveFileResult> moveFile(ClientConfig config, MoveFileAction action) {
        // Move file is turned into 2 messages.
        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                action.getSourceFile(), P4FileAction.MOVE_DELETE, null, action));
        FileActionMessage.sendEvent(new FileActionMessage.Event(config.getClientServerRef(),
                action.getTargetFile(), P4FileAction.MOVE_ADD_EDIT, null, action));
        return onlineExec(config,
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
        return onlineExec(config,
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
        return onlineExec(config,
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
        // No queryCache for file annotations.
        return onlineQuery(config,
                () -> server.getFileAnnotation(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<DescribeChangelistResult> describeChangelist(ServerConfig config, DescribeChangelistQuery query) {
        return onlineQuery(config,
                () -> server.describeChangelist(config, query)
                        .whenCompleted((result) ->
                            DescribeChangelistCacheMessage.sendEvent(
                                new DescribeChangelistCacheMessage.Event(config.getServerName(),
                                        result.getRequestedChangelist(), result.getRemoteChangelist()))),
                () -> new DoneQueryAnswer<>(new DescribeChangelistResult(config,
                        query.getChangelistId(),
                        queryCache.getCachedChangelist(config.getServerName(),
                                query.getChangelistId()),
                        true))
        );
    }

    @NotNull
    protected QueryAnswer<GetFileContentsResult> getFileContents(ServerConfig config,
            GetFileContentsQuery query) {
        return onlineQuery(config,
                () -> server.getFileContents(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<GetJobSpecResult> getJobSpec(ServerConfig config, GetJobSpecQuery query) {
        return onlineQuery(config,
                () -> server.getJobSpec(config)
                        .whenCompleted((result) ->
                            JobSpecCacheMessage.sendEvent(new JobSpecCacheMessage.Event(
                                config.getServerName(), result.getJobSpec()))
                        ),
                () -> new DoneQueryAnswer<>(new GetJobSpecResult(config,
                        queryCache.getCachedJobSpec(config.getServerName())))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListChangelistsFixedByJobResult> listChangelistsFixedByJob(ServerConfig config,
            ListChangelistsFixedByJobQuery query) {
        // FIXME implement
        LOG.warn("FIXME implement listChangelistsFixedByJob");
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientsForUserResult> listClientsForUser(ServerConfig config, ListClientsForUserQuery query) {
        return onlineQuery(config,
                () -> server.getClientsForUser(config, query)
                        .whenCompleted((result) ->
                                ListClientsForUserCacheMessage.sendEvent(new ListClientsForUserCacheMessage.Event(
                                        config.getServerName(), result.getRequestedUser(), result.getClients()
                                ))
                        ),
                () -> new DoneQueryAnswer<>(new ListClientsForUserResult(config, query.getUsername(),
                        queryCache.getCachedClientsForUser(config.getServerName(), query.getUsername())))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListDirectoriesResult> listDirectories(ServerConfig config, ListDirectoriesQuery query) {
        // FIXME implement
        LOG.warn("FIXME implement listDirectories");
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesResult> listFiles(ServerConfig config, ListFilesQuery query) {
        // FIXME implement
        LOG.warn("FIXME implement listFiles");
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesDetailsResult> listFilesDetails(ServerConfig config, ListFilesDetailsQuery query) {
        return onlineQuery(config,
                () -> server.listFilesDetails(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFileHistoryResult> listFilesHistory(ServerConfig config, ListFileHistoryQuery query) {
        return onlineQuery(config,
                () -> server.listFilesHistory(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListJobsResult> listJobs(ServerConfig config, ListJobsQuery query) {
        return onlineQuery(config,
                () -> server.listJobs(config, query),
                // TODO cache jobs?
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(ServerConfig config,
            ListSubmittedChangelistsQuery query) {
        return onlineQuery(config,
                () -> server.listSubmittedChangelists(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListUsersResult> listUsers(ServerConfig config, ListUsersQuery query) {
        // FIXME implement
        LOG.warn("FIXME implement listUsers");
        return null;
    }

    @NotNull
    @Override
    protected QueryAnswer<ListLabelsResult> listLabels(ServerConfig config, ListLabelsQuery query) {
        return onlineQuery(config,
                () -> server.listLabels(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientFetchStatusResult> listClientFetchStatus(ClientConfig config,
            ListClientFetchStatusQuery query) {
        // FIXME implement
        LOG.warn("FIXME implement listClientFetchStatus");
        return null;
    }


    @NotNull
    @Override
    protected QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(ClientConfig config,
            ListOpenedFilesChangesQuery query) {
        return onlineQuery(config,
                () -> server.listOpenedFilesChanges(config, query),
                () -> new DoneQueryAnswer<>(cachedListOpenedFilesChanges(config, null))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ServerInfoResult> serverInfo(P4ServerName name, ServerInfo query) {
        // FIXME implement
        LOG.warn("FIXME implement serverInfo");
        return null;
    }


    @NotNull
    @Override
    protected ListOpenedFilesChangesResult cachedListOpenedFilesChanges(ClientConfig config,
            SyncListOpenedFilesChangesQuery query) {
        return new ListOpenedFilesChangesResult(config,
                queryCache.getCachedOpenedFiles(config),
                queryCache.getCachedOpenedChangelists(config));
    }


    @Override
    public void dispose() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    /** Should be called for application-level invocations. */
    @NotNull
    private Optional<ServerConnectionState> getAppStateFor(@NotNull ServerConfig config) {
        ServerConnectionState state;
        synchronized (stateCache) {
            state = stateCache.get(config.getServerId());
        }
        return Optional.ofNullable(state);
    }

    /**
     * Should be called for project-level invocations only, because it will create a project-level reference to
     * the server config.
     *
     * @param config config
     * @return state for the config
     */
    @NotNull
    private ServerConnectionState getProjectStateFor(@NotNull ServerConfig config) {
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

    /**
     * Fire-and-forget send cached pending requests.
     *
     * @param clientConfig configuration
     */
    @SuppressWarnings("unchecked")
    private void sendCachedPendingRequests(ClientConfig clientConfig) {
        try {
            // Note: must be serially applied, because the pending actions have a strict order.
            // Also note: the error catching is only logged; user reporting is done by
            // event listeners.
            pendingActionCache.copyActions(clientConfig)
                .reduce(new DoneActionAnswer(null),
                    (BiFunction<ActionAnswer, ActionChoice, ActionAnswer>) (answer, action) -> answer.mapActionAsync((x) ->
                            action.when(
                                    (c) -> perform(clientConfig, c),
                                    (s) -> perform(clientConfig.getServerConfig(), s)
                            ).whenCompleted((ev) -> {
                                try {
                                    pendingActionCache.writeActions(clientConfig.getClientServerRef(),
                                            (cache) -> cache.removeActionById(action.getActionId()));
                                } catch (InterruptedException ex) {
                                    LOG.warn(ex);
                                }
                            }).whenServerError((ex) -> {
                                // This will be bubbled up to the outer error trap.
                                // Additionally, the underlying code has its own error reporting.
                                LOG.debug("Problem committing pending action " + action, ex);
                            }).whenOffline(() -> {
                                LOG.warn("Went offline while committing pending action " + action);
                            })),
                    (actionLeft, actionRight) -> actionLeft.mapActionAsync((x) -> actionRight))
                .whenServerError((e) -> {
                    LOG.warn("Encountered unexpected error: " + e);
                })
                .whenOffline(() -> {
                    LOG.warn("Went offline while sending pending actions");
                });
        } catch (InterruptedException e) {
            LOG.warn(e);
        }
    }

    private <R> ActionAnswer<R> onlineExec(@NotNull ClientConfig clientConfig, Exec<ActionAnswer<R>> serverExec,
            Exec<ActionAnswer<R>> cacheExec) {
        return onlineExec(clientConfig.getServerConfig(), serverExec, cacheExec);
    }

    private <R> ActionAnswer<R> onlineExec(@NotNull ServerConfig serverConfig, Exec<ActionAnswer<R>> serverExec,
            Exec<ActionAnswer<R>> cacheExec) {
        final ServerConnectionState firstState = getProjectStateFor(serverConfig);
        final ActionAnswer<R> ret;
        if (firstState.needsLogin && !(firstState.badLogin || firstState.badConnection || firstState.userOffline)) {
            ret = login(serverConfig, new LoginAction()).mapAction((x) -> null);
        } else {
            ret = new DoneActionAnswer<>(null);
        }
        return ret.mapActionAsync((x) -> {
            final ServerConnectionState nextState = getProjectStateFor(serverConfig);
            if (!(nextState.badConnection || nextState.badLogin || nextState.userOffline || nextState.needsLogin)) {
                return serverExec.exec();
            }
            return cacheExec.exec();
        });
    }

    private <R> QueryAnswer<R> onlineQuery(@NotNull ClientConfig clientConfig, Exec<QueryAnswer<R>> serverExec,
            Exec<QueryAnswer<R>> cacheExec) {
        return onlineQuery(clientConfig.getServerConfig(), serverExec, cacheExec);
    }

    private <R> QueryAnswer<R> onlineQuery(@NotNull ServerConfig serverConfig, Exec<QueryAnswer<R>> serverExec,
            Exec<QueryAnswer<R>> cacheExec) {
        final ServerConnectionState firstState = getProjectStateFor(serverConfig);
        final QueryAnswer<R> ret;
        if (firstState.needsLogin && !(firstState.badLogin || firstState.badConnection || firstState.userOffline)) {
            ret = login(serverConfig, new LoginAction()).mapQuery((x) -> null);
        } else {
            ret = new DoneQueryAnswer<>(null);
        }
        return ret.mapQueryAsync((x) -> {
            final ServerConnectionState nextState = getProjectStateFor(serverConfig);
            if (!(nextState.badConnection || nextState.badLogin || nextState.userOffline || nextState.needsLogin)) {
                return serverExec.exec();
            }
            return cacheExec.exec();
        });
    }

    // Could replace this with "Supplier"
    interface Exec<R> {
        R exec();
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
