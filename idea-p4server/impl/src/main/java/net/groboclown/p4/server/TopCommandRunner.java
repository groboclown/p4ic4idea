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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.impl.mapbased.server.ServerInfo;
import net.groboclown.p4.server.api.AbstractP4CommandRunner;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.ServerStatus;
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
import net.groboclown.p4.server.api.commands.server.SwarmConfigQuery;
import net.groboclown.p4.server.api.commands.server.SwarmConfigResult;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.exceptions.ConnectionTimeoutException;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ConnectionErrorMessage;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.messagebus.PushPendingActionsRequestMessage;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;


/**
 * Layers a queryCache and server.
 */
public class TopCommandRunner extends AbstractP4CommandRunner
        implements Disposable {
    private static final Logger LOG = Logger.getInstance(TopCommandRunner.class);

    private final Project project;
    private final CacheQueryHandler queryCache;
    private final CachePendingActionHandler pendingActionCache;
    private final AbstractServerCommandRunner server;
    private boolean disposed;


    /** NOTE: callers must properly register the dispose chain. */
    public TopCommandRunner(@NotNull Project project,
            @NotNull CacheQueryHandler queryCache, @NotNull CachePendingActionHandler pendingActionCache,
            @NotNull AbstractServerCommandRunner server,
            @NotNull Disposable parentDisposable) {
        this.project = project;
        this.queryCache = queryCache;
        this.pendingActionCache = pendingActionCache;
        this.server = server;
        Disposer.register(parentDisposable, this);

        MessageBusClient.ProjectClient projClient = MessageBusClient.forProject(project, this);
        PushPendingActionsRequestMessage.addListener(projClient, this, (e) ->
                getClientConfigRootFor(e.getVcsRoot()).ifPresent((r) ->
                        sendCachedPendingRequests(r.getClientConfig())));

        // User wants to work offline, regardless of connection status.
        UserSelectedOfflineMessage.addListener(projClient, this, e ->
                server.disconnect(e.getName()));

        ReconnectRequestMessage.addListener(projClient, this, new ReconnectRequestMessage.Listener() {
            // The user requested to go online, so clear out the states
            // that might cause a request to not be fulfilled.
            @Override
            public void reconnectToAllClients(@NotNull ReconnectRequestMessage.ReconnectAllEvent e) {
                getClientConfigRoots().forEach((r) -> {
                    // Try to connect to the server, and wait for that connection
                    // attempt to complete.
                    tryOnlineAfterReconnect(r.getClientConfig());
                    sendCachedPendingRequests(r.getClientConfig());
                });
            }

            @Override
            public void reconnectToClient(@NotNull ReconnectRequestMessage.ReconnectEvent e) {
                getClientConfigRootsFor(e.getRef()).forEach((r) -> {
                    // Try to connect to the server, and wait for that connection
                    // attempt to complete.
                    tryOnlineAfterReconnect(r.getClientConfig());
                    sendCachedPendingRequests(r.getClientConfig());
                });
            }
        });
    }

    private List<RootedClientConfig> getClientConfigRootsFor(@NotNull ClientConfig config) {
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        final List<RootedClientConfig> roots = new ArrayList<>();
        if (registry != null) {
            for (RootedClientConfig root : registry.getRootedClientConfigs()) {
                if (root.getClientConfig().equals(config)) {
                    roots.add(root);
                }
            }
        }
        return roots;
    }

    private List<RootedClientConfig> getClientConfigRootsFor(@NotNull ClientServerRef config) {
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        final List<RootedClientConfig> roots = new ArrayList<>();
        if (registry != null) {
            for (RootedClientConfig root : registry.getRootedClientConfigs()) {
                if (root.getClientConfig().getClientServerRef().equals(config)) {
                    roots.add(root);
                }
            }
        }
        return roots;
    }

    private List<RootedClientConfig> getClientConfigRootsFor(@NotNull OptionalClientServerConfig config) {
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry != null) {
            if (config.getClientConfig() != null) {
                return registry.getClientConfigsForRef(config.getClientConfig().getClientServerRef());
            }
            return registry.getClientConfigsForServer(config.getServerName());
        }
        return List.of();
    }

    private Optional<RootedClientConfig> getClientConfigRootFor(VirtualFile vcsRoot) {
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry != null) {
            return Optional.ofNullable(registry.getClientConfigFor(vcsRoot));
        }
        return Optional.empty();
    }


    private Collection<RootedClientConfig> getClientConfigRoots() {
        final ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry != null) {
            return registry.getRootedClientConfigs();
        }
        return List.of();
    }


    @NotNull
    @Override
    protected ActionAnswer<CreateJobResult> createJob(
            @NotNull OptionalClientServerConfig config, @NotNull CreateJobAction action) {
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
    protected ActionAnswer<LoginResult> login(
            @NotNull OptionalClientServerConfig config,
            @NotNull LoginAction action) {
        final List<RootedClientConfig> roots = getClientConfigRootsFor(config);
        for (final RootedClientConfig root : roots) {
            // This only tries to connect to the first server... which should be unique across
            // all configurations.
            if (
                    // if login is bad, then don't retry the login.
                    canRunOnline(root) && root.isLoginNeeded()
            ) {
                return server.perform(config, action)
                        .whenCompleted((resp) -> {
                            // Login was good.
                            ServerConnectedMessage.sendServerConnectedMessage(
                                    config, true, resp.isPasswordUsed());
                        });
            }
        }
        return new OfflineActionAnswerImpl<>();
    }


    @NotNull
    @Override
    protected ActionAnswer<FetchFilesResult> fetchFiles(@NotNull ClientConfig config, @NotNull FetchFilesAction action) {
        // No queryCache updates
        return onlineExec(config,
                () -> server.perform(config, action),
                OfflineActionAnswerImpl::new);
    }


    @NotNull
    @Override
    protected ActionAnswer<SubmitChangelistResult> submitChangelist(
            @Nonnull ClientConfig config, @Nonnull SubmitChangelistAction action) {
        // Submits are never cached.  Instead, offline submits generate an error.
        return onlineExec(config,
                () -> server.perform(config, action),
                OfflineActionAnswerImpl::new
                );
    }

    @NotNull
    @Override
    protected ActionAnswer<MoveFileResult> moveFile(@Nonnull ClientConfig config, @Nonnull MoveFileAction action) {
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
    protected <R extends ClientResult> ActionAnswer<R> performFileAction(@Nonnull ClientConfig config,
            @Nonnull ClientAction<R> action, @NotNull FilePath file, @Nullable P4FileType fileType,
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
    protected <R extends ClientResult> ActionAnswer<R> performNonFileAction(@Nonnull ClientConfig config, @Nonnull ClientAction<R> action) {
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
    protected QueryAnswer<AnnotateFileResult> getAnnotatedFile(
            @NotNull ClientConfig config,
            @NotNull AnnotateFileQuery query) {
        // No queryCache for file annotations.
        return onlineQuery(config,
                () -> server.getFileAnnotation(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<DescribeChangelistResult> describeChangelist(
            @NotNull OptionalClientServerConfig config,
            @NotNull DescribeChangelistQuery query) {
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
    @Override
    protected QueryAnswer<GetFileContentsResult> getFileContents(
            @NotNull ClientConfig config,
            @NotNull GetFileContentsQuery query) {
        return onlineQuery(config,
                () -> server.getFileContents(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<GetJobSpecResult> getJobSpec(
            @NotNull OptionalClientServerConfig config,
            @NotNull GetJobSpecQuery query) {
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
    protected QueryAnswer<ListChangelistsFixedByJobResult> listChangelistsFixedByJob(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListChangelistsFixedByJobQuery query) {
        // TODO implement
        LOG.warn("FIXME implement listChangelistsFixedByJob");
        return new DoneQueryAnswer<>(new ListChangelistsFixedByJobResult());
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientsForUserResult> listClientsForUser(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListClientsForUserQuery query) {
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
    protected QueryAnswer<ListDirectoriesResult> listDirectories(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListDirectoriesQuery query) {
        // TODO implement
        LOG.warn("FIXME implement listDirectories");
        return new DoneQueryAnswer<>(new ListDirectoriesResult());
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesResult> listFiles(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListFilesQuery query) {
        // TODO implement
        LOG.warn("FIXME implement listFiles");
        return new DoneQueryAnswer<>(new ListFilesResult());
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFilesDetailsResult> listFilesDetails(
            @NotNull ClientConfig config,
            @NotNull ListFilesDetailsQuery query) {
        return onlineQuery(config,
                () -> server.listFilesDetails(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListFileHistoryResult> listFilesHistory(
            @NotNull ClientConfig config,
            @NotNull ListFileHistoryQuery query) {
        return onlineQuery(config,
                () -> server.listFilesHistory(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListJobsResult> listJobs(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListJobsQuery query) {
        return onlineQuery(config,
                () -> server.listJobs(config, query),
                // TODO cache jobs?
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(@NotNull final ClientConfig config,
            @NotNull final ListSubmittedChangelistsQuery query) {
        return onlineQuery(config,
                () -> server.listSubmittedChangelists(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError())
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ListUsersResult> listUsers(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListUsersQuery query) {
        return onlineQuery(config,
                () -> server.listUsers(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }

    @NotNull
    @Override
    protected QueryAnswer<ListLabelsResult> listLabels(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListLabelsQuery query) {
        return onlineQuery(config,
                () -> server.listLabels(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }

    @NotNull
    @Override
    protected QueryAnswer<SwarmConfigResult> getSwarmConfig(
            @NotNull OptionalClientServerConfig config,
            @NotNull SwarmConfigQuery query) {
        return onlineQuery(config,
                () -> server.getSwarmConfig(config, query),
                () -> new ErrorQueryAnswerImpl<>(AnswerUtil.createOfflineError()));
    }


    @NotNull
    @Override
    protected QueryAnswer<ListClientFetchStatusResult> listClientFetchStatus(@Nonnull ClientConfig config,
            @Nonnull ListClientFetchStatusQuery query) {
        // TODO implement
        LOG.warn("FIXME implement listClientFetchStatus");
        return new DoneQueryAnswer<>(new ListClientFetchStatusResult());
    }


    @NotNull
    @Override
    protected QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(@Nonnull ClientConfig config,
            @Nonnull ListOpenedFilesChangesQuery query) {
        return onlineQuery(config,
                () -> server.listOpenedFilesChanges(config, query),
                () -> new DoneQueryAnswer<>(cachedListOpenedFilesChanges(config, null))
        );
    }


    @NotNull
    @Override
    protected QueryAnswer<ServerInfoResult> serverInfo(@Nonnull P4ServerName name, @Nonnull ServerInfo query) {
        // TODO implement
        LOG.warn("FIXME implement serverInfo");
        return new DoneQueryAnswer<>(new ServerInfoResult());
    }


    @NotNull
    @Override
    protected ListOpenedFilesChangesResult cachedListOpenedFilesChanges(@Nonnull ClientConfig config,
            @Nullable SyncListOpenedFilesChangesQuery query) {
        return new ListOpenedFilesChangesResult(config,
                queryCache.getCachedOpenedFiles(config),
                queryCache.getCachedOpenedChangelists(config));
    }


    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            Disposer.dispose(this);
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Fire-and-forget send cached pending requests.
     *
     * @param clientConfig configuration
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public ActionAnswer<Void> sendCachedPendingRequests(@NotNull ClientConfig clientConfig) {
        LOG.info("Re-sending all pending requests for " + clientConfig);
        // This needs to be protected by sending ONLY on reconnect.  If it's done more  often than that,
        // then this causes 2-4 additional messages that don't need to be sent.
        try {
            // Note: must be serially applied, because the pending actions have a strict order.
            // Also note: the error catching is only logged; user reporting is done by
            // event listeners.
            return pendingActionCache.copyActions(clientConfig)
                .reduce(new DoneActionAnswer<>(null),
                    (BiFunction<ActionAnswer, ActionChoice, ActionAnswer>) (answer, action) -> answer.mapActionAsync((x) ->
                            action.when(
                                    (c) -> perform(clientConfig, c),
                                    (s) -> perform(new OptionalClientServerConfig(clientConfig), s)
                            ).whenCompleted((ev) -> {
                                try {
                                    pendingActionCache.writeActions(clientConfig.getClientServerRef(),
                                            (cache) -> cache.removeActionById(action.getActionId()));
                                } catch (InterruptedException ex) {
                                    InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                                            new VcsInterruptedException(ex)));
                                }
                                // Note: does not notify the file state of updates.
                            }).whenServerError((ex) -> {
                                // This will be bubbled up to the outer error trap.
                                // Additionally, the underlying code has its own error reporting.
                                LOG.debug("Problem committing pending action " + action, ex);
                            }).whenOffline(() ->
                                    LOG.warn("Went offline while committing pending action " + action))),
                    (actionLeft, actionRight) -> actionLeft.mapActionAsync((x) -> actionRight))
                .whenServerError((e) ->
                        LOG.warn("Encountered unexpected error: " + e))
                .whenOffline(() ->
                        LOG.warn("Went offline while sending pending actions"));
        } catch (InterruptedException e) {
            InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(e)));
            return new DoneActionAnswer<>(null);
        }
    }

    private <R> ActionAnswer<R> onlineExec(@NotNull ClientConfig clientConfig, Supplier<ActionAnswer<R>> serverExec,
            Supplier<ActionAnswer<R>> cacheExec) {
        return onlineExec(new OptionalClientServerConfig(clientConfig), serverExec, cacheExec);
    }

    private <R> ActionAnswer<R> onlineExec(
            @NotNull final OptionalClientServerConfig config, @NotNull final Supplier<ActionAnswer<R>> serverExec,
            @NotNull final Supplier<ActionAnswer<R>> cacheExec) {
        // Try each root until we get something good.  Note that, really, any should work.
        ActionAnswer<R> ret = new DoneActionAnswer<>(null);
        for (final RootedClientConfig root : getClientConfigRootsFor(config)) {
            if (root.isLoginNeeded() && canRunOnline(root)) {
                ret = login(config, new LoginAction()).mapAction((x) -> null);
                break;
            }
        }
        return ret.mapActionAsync((x) -> {
            // Need to re-grab the right config.
            for (final RootedClientConfig root : getClientConfigRootsFor(config)) {
                if (canRunOnline(root)) {
                    return serverExec.get();
                }
            }
            return cacheExec.get();
        });
    }

    private <R> QueryAnswer<R> onlineQuery(@NotNull ClientConfig clientConfig,
            @NotNull final Supplier<QueryAnswer<R>> serverExec,
            @NotNull final Supplier<QueryAnswer<R>> cacheExec) {
        return onlineQuery(new OptionalClientServerConfig(clientConfig), serverExec, cacheExec);
    }

    private <R> QueryAnswer<R> onlineQuery(
            @NotNull final OptionalClientServerConfig config,
            @NotNull final Supplier<QueryAnswer<R>> serverExec,
            @NotNull final Supplier<QueryAnswer<R>> cacheExec) {
        QueryAnswer<R> ret = new DoneQueryAnswer<>(null);
        for (final RootedClientConfig root : getClientConfigRootsFor(config)) {
            if (root.isLoginNeeded() && canRunOnline(root)) {
                ret = login(config, new LoginAction()).mapQuery((x) -> null);
                break;
            }
        }
        return ret.mapQueryAsync((x) -> {
            // Need to re-grab the right config.
            for (final RootedClientConfig root : getClientConfigRootsFor(config)) {
                if (canRunOnline(root)) {
                    return serverExec.get();
                }
            }
            return cacheExec.get();
        });
    }

    private void tryOnlineAfterReconnect(@NotNull final ClientConfig clientConfig) {
        // Ensure the connection is allowed.
        for (final RootedClientConfig root : getClientConfigRootsFor(clientConfig)) {
            if (root.isLoginNeeded() || !canRunOnline(root)) {
                // Already checked the online state, and it's not valid.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping another attempt for " + root.getServerConfig().getServerName());
                }
                continue;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attempting to connect to " + clientConfig.getClientServerRef());
            }
            AtomicInteger debugCompleteState = new AtomicInteger(0);
            boolean completed = server.getClientsForUser(new OptionalClientServerConfig(clientConfig),
                            new ListClientsForUserQuery(clientConfig.getServerConfig().getUsername(), 1))
                    .whenCompleted((x) -> {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Completed connection attempt to " + clientConfig.getClientServerRef());
                            debugCompleteState.incrementAndGet();
                        }
                    })
                    .whenServerError((e) -> {
                        // The correct listeners will fire.
                        LOG.info("Reconnect failed for " + clientConfig.getClientServerRef(), e);
                        debugCompleteState.decrementAndGet();
                    })
                    .waitForCompletion(30, TimeUnit.SECONDS);
            if (!completed) {
                LOG.info("Reconnect attempt timed out waiting for connection to " + clientConfig.getClientServerRef());
                ConnectionErrorMessage.send().connectionTimedOut(new ServerErrorEvent.ServerNameErrorEvent<>(
                        clientConfig.getServerConfig().getServerName(),
                        new OptionalClientServerConfig(clientConfig),
                        new ConnectionTimeoutException("Connect to Server", 30)
                ));
            } else if (LOG.isDebugEnabled()) {
                // -1 means failure, 1 means success, 0 means didn't complete.
                LOG.debug("Completed reconnect attempt; either success or failure: " + debugCompleteState.get());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed online check after reconnect for " + clientConfig.getClientServerRef());
            }
            return;
        }
    }


    private static boolean canRunOnline(@NotNull ServerStatus status) {
        return !status.isLoginBad()
                && !status.isServerConnectionBad()
                && !status.isUserWorkingOffline()
                && !status.isServerConnectionProblem();
    }
}
