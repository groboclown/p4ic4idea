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

package net.groboclown.p4.server.impl.connection;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.FixJobsOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.async.AnswerSink;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobCacheMessage;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.AddJobToChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.EditChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.ListJobsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListJobsResult;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsResult;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistResult;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.AddEditResult;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileResult;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.commands.file.GetFileContentsResult;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileResult;
import net.groboclown.p4.server.api.commands.file.ShelveFilesAction;
import net.groboclown.p4.server.api.commands.file.ShelveFilesResult;
import net.groboclown.p4.server.api.commands.server.ListLabelsQuery;
import net.groboclown.p4.server.api.commands.server.ListLabelsResult;
import net.groboclown.p4.server.api.commands.server.SwarmConfigQuery;
import net.groboclown.p4.server.api.commands.server.SwarmConfigResult;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.messagebus.SpecialFileEventMessage;
import net.groboclown.p4.server.api.util.CollectionUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.api.values.P4WorkspaceSummary;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.client.OpenedFilesChangesFactory;
import net.groboclown.p4.server.impl.commands.ActionAnswerImpl;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4.server.impl.commands.QueryAnswerImpl;
import net.groboclown.p4.server.impl.connection.impl.FileAnnotationParser;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.OpenFileStatus;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.connection.operations.MoveFile;
import net.groboclown.p4.server.impl.connection.operations.SubmitChangelist;
import net.groboclown.p4.server.impl.repository.AddedExtendedFileSpec;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4ChangelistSummaryImpl;
import net.groboclown.p4.server.impl.values.P4CommittedChangelistImpl;
import net.groboclown.p4.server.impl.values.P4FileRevisionImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import net.groboclown.p4.server.impl.values.P4LabelImpl;
import net.groboclown.p4.server.impl.values.P4LocalFileImpl;
import net.groboclown.p4.server.impl.values.P4RemoteChangelistImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import net.groboclown.p4.server.impl.values.P4UserImpl;
import net.groboclown.p4.server.impl.values.P4WorkspaceSummaryImpl;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.SwarmClientFactory;
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.exceptions.InvalidSwarmServerException;
import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A simple version of the {@link net.groboclown.p4.server.api.P4CommandRunner}
 * that uses a {@link ConnectionManager} to open connections to the server.
 * <p>
 * All commands that construct a response based on server data MUST send a
 * message to the corresponding cache message bus with the loaded data.  The
 * pattern here is to invoke a private method that constructs the response object,
 * and it handles sending out the message.  The actions, on the other hand,
 * must be sent by the invoker.
 */
public class ConnectCommandRunner
        extends AbstractServerCommandRunner {
    private static final Logger LOG = Logger.getInstance(ConnectCommandRunner.class);
    private final ConnectionManager connectionManager;
    private final P4CommandUtil cmd = new P4CommandUtil();
    private final MoveFile moveFile;

    // "project" is only used for sending messages to the client.  If that's removed, then this
    // dependency can be removed.
    private final Project project;


    public ConnectCommandRunner(
            @NotNull Project project,
            @NotNull ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.project = project;
        this.moveFile = new MoveFile(project, cmd);
        SubmitChangelist.INSTANCE.withCmd(cmd);

        register(P4CommandRunner.ServerActionCmd.CREATE_JOB,
            (ServerActionRunner<CreateJobResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (server) -> createJob(server, config, (CreateJobAction) action))));

        register(P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE,
            (ClientActionRunner<AddEditResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((AddEditAction) action).getFile().getIOFile().getParentFile(),
                    (client) -> addEditFile(client, config, (AddEditAction) action))));

        register(P4CommandRunner.ClientActionCmd.CREATE_CHANGELIST,
            (ClientActionRunner<CreateChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> createChangelist(client, config, (CreateChangelistAction) action))));

        register(P4CommandRunner.ClientActionCmd.MOVE_FILES_TO_CHANGELIST,
            (ClientActionRunner<MoveFilesToChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((MoveFilesToChangelistAction) action).getCommonDir(),
                    (client) -> moveFilesToChangelist(client, config, (MoveFilesToChangelistAction) action))));

        register(P4CommandRunner.ClientActionCmd.SHELVE_FILES,
            (ClientActionRunner<ShelveFilesResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((ShelveFilesAction) action).getCommonDir(),
                    (client) -> shelveFiles(client, config, (ShelveFilesAction) action))));

        register(P4CommandRunner.ClientActionCmd.ADD_JOB_TO_CHANGELIST,
            (ClientActionRunner<AddJobToChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> addJobToChangelist(client, config, (AddJobToChangelistAction) action))));

        register(P4CommandRunner.ClientActionCmd.DELETE_CHANGELIST,
            (ClientActionRunner<DeleteChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> deleteChangelist(client, config, (DeleteChangelistAction) action))));

        register(P4CommandRunner.ClientActionCmd.DELETE_FILE,
            (ClientActionRunner<DeleteFileResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((DeleteFileAction) action).getFile().getIOFile().getParentFile(),
                    (client) -> deleteFile(client, config, (DeleteFileAction) action))));

        register(P4CommandRunner.ClientActionCmd.EDIT_CHANGELIST_DESCRIPTION,
            (ClientActionRunner<EditChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> editChangelistDescription(client, config, (EditChangelistAction) action))));

        register(P4CommandRunner.ClientActionCmd.FETCH_FILES,
            (ClientActionRunner<FetchFilesResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((FetchFilesAction) action).getCommonDir(),
                    (client) -> fetchFiles(client, config, (FetchFilesAction) action))));

        register(P4CommandRunner.ClientActionCmd.MOVE_FILE,
            (ClientActionRunner<MoveFileResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    moveFile.getExecDir(action),
                    (client) -> moveFile.moveFile(client, config, action))));

        register(P4CommandRunner.ClientActionCmd.REVERT_FILE,
            (ClientActionRunner<RevertFileResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((RevertFileAction) action).getFile().getIOFile().getParentFile(),
                    (client) -> revertFile(client, config, (RevertFileAction) action))));

        register(P4CommandRunner.ClientActionCmd.SUBMIT_CHANGELIST,
            (ClientActionRunner<SubmitChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    SubmitChangelist.INSTANCE.getExecDir(action),
                    (client) -> SubmitChangelist.INSTANCE.submitChangelist(client, config, action))));
    }

    @Override
    public void disconnect(@NotNull P4ServerName config) {
        connectionManager.disconnect(config);
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<AnnotateFileResult> getFileAnnotation(
            @NotNull ClientConfig config,
            @NotNull final AnnotateFileQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (client) -> {
            List<IFileSpec> specs;
            if (query.getLocalFile() != null) {
                specs = FileSpecBuildUtil.escapedForFilePathRev(query.getLocalFile(), query.getRev());
            } else if (query.getRemoteFile() != null) {
                // FIXME the getFileAnnotation requires a FilePath.
                // specs = FileSpecBuildUtil.escapedForRemoteFileRev(query.getRemoteFile(), query.getRev());
                throw new IllegalStateException("Not implemented");
            } else {
                throw new IllegalStateException("both local file and remote file are null");
            }
            ClientServerRef ref = config.getClientServerRef();
            IExtendedFileSpec headSpec = cmd.getFileDetails(client, specs);
            P4FileRevision headRevision = P4FileRevisionImpl.getHead(ref, headSpec);
            String content = new String(cmd.loadContents(client, headSpec),
                    headSpec.getHeadCharset() == null
                        ? (config.getDefaultCharSet() == null
                            ? Charset.defaultCharset().name()
                            : config.getDefaultCharSet())
                        : headSpec.getHeadCharset());
            List<IFileAnnotation> annotations = cmd.getAnnotations(client, specs);
            List<IFileSpec> requiredHistory = FileAnnotationParser.getRequiredHistorySpecs(annotations);
            List<Pair<IFileSpec, IFileRevisionData>> history = cmd.getExactHistory((IOptionsServer) client.getServer(), requiredHistory);
            return new AnnotateFileResult(config,
                    FileAnnotationParser.getFileAnnotation(
                            ref, client.getServer().getUserName(), headSpec, query.getLocalFile(), annotations, history),
                    headRevision, content);
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<DescribeChangelistResult> describeChangelist(
            @NotNull OptionalClientServerConfig config,
            @NotNull DescribeChangelistQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            // TODO is this the right client name, or do we need an explicit client name given?
            server.setCurrentClient(server.getClient(query.getChangelistId().getClientname()));
            IChangelist changelist = cmd.getChangelistDetails(server,
                    query.getChangelistId().getChangelistId());
            return new DescribeChangelistResult(config, query.getChangelistId(),
                    new P4RemoteChangelistImpl.Builder()
                        .withChangelist(config, changelist)
                        .withJobs(changelist.getJobs())
                        .withFiles(changelist.getFiles(true))
                        .build(),
                    false);
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<GetJobSpecResult> getJobSpec(
            @NotNull OptionalClientServerConfig config) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
                (server) -> new GetJobSpecResult(
                        config,
                        new P4JobSpecImpl(cmd.getJobSpec(server))
                )
        ));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(@NotNull ClientConfig config,
            @NotNull ListOpenedFilesChangesQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
                query.getRoot(),
                (client) -> listOpenedFilesChanges(client, config,
                        query.getMaxChangelistResults(), query.getMaxFileResults())
        ));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(
            @NotNull OptionalClientServerConfig config,
            @NotNull ListClientsForUserQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
                (server) -> listClientsForUser(server, config, query.getUsername(), query.getMaxClients())));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(
            @NotNull final ClientConfig config, @NotNull final ListSubmittedChangelistsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (client) -> {
            // TODO use cmd
            GetChangelistsOptions options = new GetChangelistsOptions();
            options.setMaxMostRecent(query.getMaxCount());

            // Null values are fine on the filters.
            options.setClientName(query.getClientNameFilter());
            options.setUserName(query.getUsernameFilter());
            options.setLongDesc(true);

            List<IFileSpec> specs = query.getLocation().getFileSpecs();
            if (!specs.isEmpty() && query.getSpecFilter() != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Using revision range " + query.getSpecFilter() + " for " + specs);
                }
                specs = FileSpecBuildUtil.replaceBestPathRevisions(specs, query.getSpecFilter());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Querying changelists with specs " + specs);
            }

            List<IChangelistSummary> res = client.getServer().getChangelists(specs, options);

            return new ListSubmittedChangelistsResult(config,
                    res.stream()
                    .map((summary) -> {
                        // FIXME scan for files on changelist, using the "describe changelist" action.
                        LOG.warn("FIXME scan for files on changelist, using the \"describe changelist\" action.");
                        return new P4CommittedChangelistImpl(
                                new P4ChangelistSummaryImpl(
                                    config.getClientServerRef(), summary),
                                Collections.emptyList(),
                                summary.getDate());
                    })
                    .collect(Collectors.toList()));
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<GetFileContentsResult> getFileContents(
            @NotNull ClientConfig config,
            @NotNull final GetFileContentsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (client) -> {
            final byte[] contents = query.when(
                    config,
                    (depot) -> {
                        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(depot);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("get file contents for " + depot + ": " + specs);
                        }
                        return cmd.loadContents(client, specs.get(0));
                    },
                    (clientname, localFile, rev) -> {
                        if (rev <= 0 && localFile.getIOFile().exists()) {
                            return FileUtil.loadFileBytes(localFile.getIOFile());
                        } else {
                            // Note: filter out the specs that don't map to anything in the client.
                            List<IFileSpec> locations = CollectionUtil.filterNulls(cmd.getSpecLocations(client,
                                    FileSpecBuildUtil.escapedForFilePathRev(localFile, -1)));
                            if (locations.isEmpty()) {
                                locations = FileSpecBuildUtil.escapedForFilePathRev(localFile, rev);
                            } else {
                                // #213 - replaceDepotRevisions can discover a null if the depot path is null.
                                // This ends up to be because there's a request for a file for the wrong client.
                                MessageStatusUtil.throwIfError(locations);
                                locations = FileSpecBuildUtil.replaceBestPathRevisions(locations, "#" + rev);
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("get file contents for " + locations + " (only care about the first one)");
                            }
                            return cmd.loadContents(client, locations.get(0));
                        }
                    }
            );
            return query.when(
                    config,
                    (depot) -> new GetFileContentsResult(config, depot, contents, config.getDefaultCharSet()),
                    (clientname, localFile, rev) -> new GetFileContentsResult(config, localFile, contents, null)
            );
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListFileHistoryResult> listFilesHistory(
            @NotNull ClientConfig config,
            @NotNull final ListFileHistoryQuery query) {
        final List<IFileSpec> fileSpec = FileSpecBuildUtil.escapedForFilePaths(query.getFile());
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (client) ->
                new ListFileHistoryResult(config, createFileHistoryList(
                    config, query.getFile(),
                    cmd.getHistory(client, fileSpec, query.getMaxResults())))));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListFilesDetailsResult> listFilesDetails(
            @NotNull ClientConfig config,
            @NotNull final ListFilesDetailsQuery query) {
        final List<IFileSpec> fileSpecs = FileSpecBuildUtil.escapedForFilePathsAnnotated(
                query.getFiles(),
                // TODO replace with more Perforce API way of creating the annotation.
                query.getRevState() == ListFilesDetailsQuery.RevState.HEAD ? "#head" : "#have",
                true);
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (client) ->
            new ListFilesDetailsResult(config,
                cmd.getFilesDetails(
                    client,
                    fileSpecs,
                    query.getMaxResultCount()
                ).stream()
                    .map((e) ->
                        query.getRevState() == ListFilesDetailsQuery.RevState.HEAD
                            ? P4FileRevisionImpl.getHead(config.getClientServerRef(), e)
                            : P4FileRevisionImpl.getHave(config.getClientServerRef(), e)
                    )
                    .collect(Collectors.toList()))));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListJobsResult> listJobs(
            @NotNull OptionalClientServerConfig config,
            @NotNull final ListJobsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            List<P4Job> jobs = new ArrayList<>();
            if (query.getJobId() != null) {
                IJob job = cmd.getJob(server, query.getJobId());
                if (job != null) {
                    jobs.add(new P4JobImpl(job));
                }
            }

            // TODO Finding the assigned-to user requires additional knowledge of the job spec that we don't have.
            // So for now, we only have job ID exact search and description search
            if (query.getDescription() != null) {
                cmd.findJobs(server, query.getDescription(), query.getMaxResults())
                        .forEach((j) -> jobs.add(new P4JobImpl(j)));
            }
            return new ListJobsResult(config, jobs);
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListLabelsResult> listLabels(
            @NotNull OptionalClientServerConfig config,
            @NotNull final ListLabelsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            List<ILabelSummary> labels = cmd.findLabels(server,
                    query.hasNameFilter() ? query.getNameFilter() : null,
                    query.getMaxResults());
            return new ListLabelsResult(config, labels.stream().map(P4LabelImpl::new).collect(Collectors.toList()));
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListUsersResult> listUsers(
            @NotNull OptionalClientServerConfig config,
            @NotNull final ListUsersQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            List<IUserSummary> users = cmd.findUsers(server, query.getMaxResults());
            return new ListUsersResult(config, users.stream()
                .map(P4UserImpl::new)
                .collect(Collectors.toList()));
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<SwarmConfigResult> getSwarmConfig(
            @NotNull OptionalClientServerConfig config,
            @NotNull final SwarmConfigQuery query) {
        // TODO this conflates password fetching and the swarm config fetch.
        // May instead want it to be a function on the query object.
        return new QueryAnswerImpl<>(
                query.getAuthorization(config)
                .mapAsync((auth) -> auth.on(
                        (password) ->
                            connectionManager.withConnection(config, (server) ->
                                new SwarmConfig()
                                        .withUsername(config.getUsername())
                                        .withServerInfo(server, new String(password.toCharArray(true)))
                                        .withLogger(query.getLogger())
                            )
                            .futureMap((BiConsumer<SwarmConfig, AnswerSink<SwarmClient>>) (swarmConfig, sink) -> {
                                try {
                                    sink.resolve(SwarmClientFactory.createSwarmClient(swarmConfig));
                                } catch (UnauthorizedAccessException e) {
                                    auth.onAuthenticationFailure();
                                    sink.reject(AnswerUtil.createSwarmError(e));
                                } catch (IOException | InvalidSwarmServerException e) {
                                    sink.reject(AnswerUtil.createSwarmError(e));
                                }
                            }),
                        (ticket) ->
                            connectionManager.withConnection(config, (server) ->
                                    new SwarmConfig()
                                            .withUsername(config.getUsername())
                                            .withServerInfo(server)
                                            .withTicket(ticket)
                                            .withLogger(query.getLogger())
                            )
                            .futureMap((swarmConfig, sink) -> {
                                try {
                                    sink.resolve(SwarmClientFactory.createSwarmClient(swarmConfig));
                                } catch (IOException | InvalidSwarmServerException e) {
                                    sink.reject(AnswerUtil.createSwarmError(e));
                                }
                            })
                ))
                .map(swarmClient -> new SwarmConfigResult(config, swarmClient))
        );
    }

    @NotNull
    private ListFileHistoryResult.VcsFileRevisionFactory createFileHistoryList(
            @NotNull final ClientConfig config,
            @NotNull final FilePath file,
            @NotNull final List<IFileRevisionData> history) {
        return (formatter, loader) -> {
            List<VcsFileRevision> ret = new ArrayList<>(history.size());
            history.forEach((d) -> {
                P4HistoryVcsFileRevision rev = new P4HistoryVcsFileRevision(
                        file, config, d, formatter, loader);
                ret.add(rev);
            });
            return ret;
        };
    }

    private CreateJobResult createJob(IOptionsServer server, OptionalClientServerConfig cfg, CreateJobAction action)
            throws ConnectionException, AccessException, RequestException {
        P4JobImpl job = new P4JobImpl(cmd.createJob(server, action.getFields()));
        JobCacheMessage.sendEvent(new JobCacheMessage.Event(cfg.getServerName(), job,
                JobCacheMessage.JobUpdateAction.JOB_CREATED));
        return new CreateJobResult(cfg, job);
    }

    // TODO move to an operations class.
    private AddEditResult addEditFile(IClient client, ClientConfig config, AddEditAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running add/edit against the server for " + action.getFile());
            LOG.debug("Working directory: " + ((Server)client.getServer()).getUsageOptions().getWorkingDirectory());
        }

        // First, discover if the file is known by the server.

        // TODO check for symlink handling; without this, the setup:
        //       a/b.txt
        //       c -> symlink to a
        // This would cause the local file "c/b.txt" to be detected as an ADD operation, when it should be an
        // EDIT of a/b.txt.
        // See FileActionsServerCacheSync.updateFilesForSymlink

        List<IFileSpec> srcFiles = FileSpecBuildUtil.escapedForFilePaths(action.getFile());
        OpenFileStatus status = new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(), srcFiles, 1000));
        status.throwIfError();

        if (status.hasAddEdit()) {
            LOG.info("Already opened for add/edit: " + action.getFile());
            return new AddEditResult(config, action.getFile(), false, action.getFileType(),
                    action.getChangelistId() == null
                            ? P4ChangelistIdImpl.createDefaultChangelistId(config.getClientServerRef())
                            : action.getChangelistId(),
                    new P4RemoteFileImpl(status.getOpen().get(0)));
        }

        boolean addFile = false;
        if (status.isNotOnServer()) {
            addFile = true;
        } else if (status.hasDelete()) {
            // Revert the file
            // Note that the new file may already exist.  If that's the case, we do not want to alter
            // the existing file.  See #181
            SpecialFileEventMessage.send(project).fileReverted(new SpecialFileEventMessage.SpecialFileEvent(
                    status.getDelete().stream().map(IFileSpec::toString).collect(Collectors.toList()),
                    "File marked for delete in changelist, so request to add the file reverted delete, but not the "
                            + "file contents on disk."));

            List<IFileSpec> reverted = cmd.revertFileChangesPreserveFiles(client,
                    new ArrayList<>(status.getDelete()));

            LOG.info("File added, but server says it's deleted.  Reverted " + action.getFile() + ": " +
                    MessageStatusUtil.getMessages(reverted, "\n"));
            MessageStatusUtil.throwIfError(reverted);
            addFile = true;
        } else if (LOG.isDebugEnabled() && !status.hasOpen()) {
            LOG.debug("Status message " + status + " assumed to mean 'edit'");
            for (IExtendedFileSpec spec : status.getSkipped()) {
                LOG.debug(":: " + spec + " - " + spec.getAction());
            }
        }

        List<IFileSpec> ret;
        if (addFile) {
            // Adding files uses the non-escaped form, because of the 'use wildcards' flag.
            srcFiles = FileSpecBuildUtil.forFilePaths(action.getFile());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Opening " + action.getFile() + " for add as " + srcFiles);
            }
            // For adding files, let Perforce server decide which file type is best, depending on the
            // file type map.  The user can change this outside the IDE if required.
            // See #179
            ret = cmd.addFiles(client, srcFiles, null, action.getChangelistId(), action.getCharset());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Opening " + action.getFile() + " for edit as " + srcFiles);
            }
            // For editing files, ignore the file type, and keep it as Perforce knows it.
            // See #179
            ret = cmd.editFiles(client, srcFiles, null, action.getChangelistId(), action.getCharset());
        }

        MessageStatusUtil.throwIfMessageOrEmpty(addFile ? "add" : "edit", ret);

        P4ChangelistId retChange;
        if (action.getChangelistId() == null || ret.isEmpty() ||
                action.getChangelistId().getChangelistId() != ret.get(0).getChangelistId()) {
            int retChangeId = ret.get(0).getChangelistId();
            if (retChangeId == IChangelist.UNKNOWN) {
                // Use the default changelist.
                retChangeId = IChangelist.DEFAULT;
            }
            retChange = new P4ChangelistIdImpl(retChangeId, config.getClientServerRef());
        } else {
            retChange = action.getChangelistId();
        }
        return new AddEditResult(config, action.getFile(), addFile, P4FileType.convert(ret.get(0).getFileType()),
                retChange, new P4RemoteFileImpl(ret.get(0)));
    }

    private CreateChangelistResult createChangelist(IClient client, ClientConfig config, CreateChangelistAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running create changelist against the server for " + action.getComment());
        }

        IChangelist changelist = cmd.createChangelist(client, action.getComment());
        return new CreateChangelistResult(config, changelist.getId());
    }

    private MoveFilesToChangelistResult moveFilesToChangelist(IClient client, ClientConfig config,
            MoveFilesToChangelistAction action)
            throws ConnectionException, AccessException, RequestException {
        if (action.getFiles().isEmpty()) {
            throw new IllegalArgumentException("must provide at least one file to move");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running move files to changelist against the server for " + action.getChangelistId() + " / " +
                    action.getFiles());
        }

        List<IFileSpec> res = cmd.reopenFiles(client, action.getFiles(), action.getChangelistId());
        String info = null;
        List<P4RemoteFile> files = new ArrayList<>(res.size());
        for (IFileSpec spec : res) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null) {
                if (msg.isInfo()) {
                    info = msg.getLocalizedMessage();
                }
                if (msg.isWarning() || msg.isError()) {
                    throw new RequestException(msg);
                }
            } else {
                files.add(new P4RemoteFileImpl(spec));
            }
        }
        return new MoveFilesToChangelistResult(config, action.getChangelistId(), info, files);
    }

    private ShelveFilesResult shelveFiles(IClient client, ClientConfig config, ShelveFilesAction action)
            throws P4JavaException {
        if (action.getFiles().isEmpty()) {
            throw new IllegalArgumentException("must provide at least one file to shelve");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Shelving files for " + action.getChangelistId() + " / " +
                    action.getFiles());
        }

        List<IFileSpec> specs = FileSpecBuildUtil.forFilePaths(action.getFiles());
        List<IFileSpec> res = cmd.shelveFiles(client, specs, action.getChangelistId().getChangelistId());
        MessageStatusUtil.throwIfError(res);
        LOG.info("Server returned " + res);
        return new ShelveFilesResult(config, action.getChangelistId(), action.getFiles());
    }

    private AddJobToChangelistResult addJobToChangelist(IClient client, ClientConfig config,
            AddJobToChangelistAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running add job to changelist against the server for " + action.getChangelistId() + " / " +
                    action.getJob());
        }

        IChangelist changelist = cmd.getChangelistDetails(client.getServer(),
                action.getChangelistId().getChangelistId());
        if (changelist == null) {
            throw new P4JavaException("No such changelist " + action.getChangelistId().getChangelistId());
        }
        IJob job = cmd.getJob(client.getServer(), action.getJob().getJobId());
        if (job == null) {
            throw new P4JavaException("No such job " + action.getJob().getJobId());
        }
        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");
        List<IFix> res = client.getServer().fixJobs(Collections.singletonList(action.getJob().getJobId()),
                        action.getChangelistId().getChangelistId(),
                        new FixJobsOptions(null, false));
        return new AddJobToChangelistResult(config, res);
    }

    private DeleteChangelistResult deleteChangelist(IClient client, ClientConfig config,
            DeleteChangelistAction action)
            throws ConnectionException, AccessException, RequestException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running delete changelist against the server for " + action.getChangelistId());
        }

        String res = cmd.deletePendingChangelist(client, action.getChangelistId());
        return new DeleteChangelistResult(config, res);
    }

    // TODO move to an operations class
    private DeleteFileResult deleteFile(IClient client, ClientConfig config, DeleteFileAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running delete against the server for " + action.getFile());
        }

        List<IFileSpec> files = FileSpecBuildUtil.escapedForFilePaths(action.getFile());
        OpenFileStatus status = new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(), files, 1000));
        status.throwIfError();

        if (status.hasDelete()) {
            LOG.info("Skipping delete on file already open for delete: " + action.getFile());
            return new DeleteFileResult(
                    config,
                    P4RemoteFileImpl.createForExtended(status.getDelete()),
                    // TODO use P4Bundle
                    MessageStatusUtil.getExtendedMessages(status.getFilesWithMessages(), "\n"));
        }
        if (status.hasAdd()) {
            // See #181
            SpecialFileEventMessage.send(project).fileReverted(new SpecialFileEventMessage.SpecialFileEvent(
                    status.getAdd().stream().map(IFileSpec::toString).collect(Collectors.toList()),
                    "File marked as open for add, and delete request reverted the add, but does not alter the on-disk"
                            + " file contents."));

            List<IFileSpec> res = cmd.revertFileChangesPreserveFiles(client,
                    new ArrayList<>(status.getAdd()));

            LOG.info("Reverted files open for add rather than deleting them: " + status.getAdd() + "; results = " +
                    MessageStatusUtil.getMessages(res, "\n"));
            MessageStatusUtil.throwIfError(res);
            return new DeleteFileResult(config, P4RemoteFileImpl.createFor(files),
                    // TODO use P4Bundle
                    MessageStatusUtil.getMessages(res, "\n"));
        }
        if (status.hasEdit()) {
            // See #181
            SpecialFileEventMessage.send(project).fileReverted(new SpecialFileEventMessage.SpecialFileEvent(
                    status.getEdit().stream().map(IFileSpec::toString).collect(Collectors.toList()),
                    "File marked for delete in changelist, so Add request reverted delete but does not alter the "
                            + "on-disk file contents."));

            List<IFileSpec> res = cmd.revertFileChangesPreserveFiles(client,
                    new ArrayList<>(status.getEdit()));
            LOG.info("Reverting files open for edit in preparation for delete: " + action.getFile() + "; results = " +
                    MessageStatusUtil.getMessages(res, "\n"));
            MessageStatusUtil.throwIfError(res);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Opening " + files + " for delete");
        }

        List<IFileSpec> res = cmd.deleteFiles(client, files, action.getChangelistId());

        MessageStatusUtil.throwIf(res, (msg) -> {
            if (msg.isWarning() || msg.isError()) {
                // FIXME if the warning is something like "file is not on server can't delete", then ignore.
                // Check with a unit test.
                LOG.warn("FIXME check if the error message says that the file isn't on the server, and, if so, "
                        + "don't raise this as an error: " + msg.getSubSystem() + "/" + msg.getGeneric() + "/"
                        + msg.getSubCode() + " - " + msg);
                return true;
            }
            return false;
        });
        return new DeleteFileResult(config, P4RemoteFileImpl.createFor(res),
                // TODO use P4Bundle, or allow the result to contain the messages as a list.
                MessageStatusUtil.getMessages(res, "\n"));
    }

    private EditChangelistResult editChangelistDescription(IClient client, ClientConfig config,
            EditChangelistAction action)
            throws P4JavaException {
        cmd.updateChangelistDescription(client, action.getChangelistId(), action.getComment());
        return new EditChangelistResult(config);
    }

    private FetchFilesResult fetchFiles(IClient client, ClientConfig config, FetchFilesAction action)
            throws P4JavaException {
        List<IFileSpec> files = FileSpecBuildUtil.escapedForFilePathsAnnotated(
                action.getSyncPaths(), action.getPathAnnotation(), true);
        List<IFileSpec> res = cmd.syncFiles(client, files, action.isForce());

        List<P4LocalFile> resFiles = new ArrayList<>(res.size());
        StringBuilder info = new StringBuilder();
        for (IFileSpec spec : res) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null) {
                if (msg.isInfo()) {
                    info.append("\n").append(msg.getLocalizedMessage());
                }
                // 17 (x11) = "file(s) up-to-date"
                if (msg.getGeneric() != MessageGenericCode.EV_EMPTY) {
                    throw new RequestException(msg);
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring message " + msg);
                }
            } else {
                resFiles.add(new P4LocalFileImpl(config.getClientServerRef(), spec));
            }
        }
        return new FetchFilesResult(config, resFiles, info.toString());
    }

    private RevertFileResult revertFile(IClient client, ClientConfig config, RevertFileAction action)
            throws P4JavaException {
        // Explicit request to revert the file.  So the file contents will be changed in the operation.

        List<IFileSpec> srcFiles = FileSpecBuildUtil.escapedForFilePaths(action.getFile());
        // Let people know about the change.  See #181.
        SpecialFileEventMessage.send(project).fileReverted(new SpecialFileEventMessage.SpecialFileEvent(
                srcFiles.stream().map(IFileSpec::toString).collect(Collectors.toList()),
                action.isRevertOnlyIfUnchanged() ?
                        "User request to revert unchanged files; on-disk contents should not change." :
                        "User request to revert files; on-disk contents changed."
        ));
        List<IFileSpec> reverted = cmd.revertFiles(client, srcFiles, action.isRevertOnlyIfUnchanged());
        LOG.info("Explicit revert " + action.getFile() + ": " + MessageStatusUtil.getMessages(reverted, "\n"));
        MessageStatusUtil.throwIfError(reverted);
        return new RevertFileResult(config, action.getFile(), reverted);
    }

    // TODO move to an operations class
    private ListOpenedFilesChangesResult listOpenedFilesChanges(IClient client, ClientConfig config,
            int maxChangelistResults, int maxFileResults)
            throws P4JavaException {
        final Date startDate = new Date();
        LOG.info("Starting listOpenedFilesChanges at " + startDate);
        try {
            // This is a complex call, because we perform all the open requests in a single method.

            // First, find all the pending changelists for the client.
            List<IChangelistSummary> summaries = new ArrayList<>(cmd.getPendingChangelists(client,
                    maxChangelistResults));

            // Then get details about the changelists.
            List<IChangelist> changes = new ArrayList<>(summaries.size());
            List<IFileSpec> pendingChangelistFileSummaries = new ArrayList<>();
            List<IExtendedFileSpec> pendingAddedFiles = new ArrayList<>();
            Map<Integer, List<IFileSpec>> shelvedFiles = new HashMap<>();
            // Calling
            for (IChangelistSummary summary : summaries) {
                IChangelist cl = cmd.getChangelistDetails(client.getServer(), summary.getId());
                changes.add(cl);
                List<IFileSpec> clFiles = new ArrayList<>(cl.getFiles(false));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("listOpenedFilesChanges: Fetched @" + cl.getId() + " files " + clFiles);
                }
                pendingAddedFiles.addAll(splitAddedFilesFromChangelistFileList(clFiles));
                pendingChangelistFileSummaries.addAll(clFiles);

                // Get the list of shelved files, if any
                if (cl.isShelved()) {
                    shelvedFiles.put(summary.getId(), cmd.getShelvedFiles(
                            client.getServer(), summary.getId(), maxFileResults));
                }
            }

            // Then find details on all the opened files
            if (LOG.isDebugEnabled()) {
                LOG.debug("listOpenedFilesChanges@" + startDate + ": getting file details (requesting " +
                        pendingChangelistFileSummaries.size() + " files, maximum file count " +
                        maxFileResults + ")");
            }
            // #210 and #207 - don't write to a potentially read-only list.
            List<IExtendedFileSpec> pendingChangelistFiles = new ArrayList<>(cmd.getFileDetailsForOpenedSpecs(
                    client.getServer(), pendingChangelistFileSummaries, maxFileResults));
            Iterator<IExtendedFileSpec> pendingIter = pendingChangelistFiles.iterator();
            // TODO DEBUG variable; remove when that code path stablizes.
            boolean foundNonOpened = false;
            while (pendingIter.hasNext()) {
                IExtendedFileSpec next = pendingIter.next();
                if (next.getStatusMessage() != null) {
                    // This can be a "not on server" message if nothing is checked out.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Opened File Spec " + next + "; message: " + next.getStatusMessage());
                    }
                    pendingIter.remove();
                } else if (next.getAction() == null) {
                    // TODO better understand why this situation happens; when it does, the CL is always -1.
                    if (LOG.isDebugEnabled()) {
                        if (!foundNonOpened) {
                            LOG.debug(
                                    "Found non-opened file spec for request of just opened file specs in opened change.  "
                                            + next.getDepotPathString() + " :: open:" + next.getOpenChangelistId()
                                            + ", cl:" + next.getChangelistId() + ", owner:" + next.getOpenActionOwner());
                            foundNonOpened = true;
                        }
                    }
                    pendingIter.remove();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pending changelist file" +
                                ": " + next.getDepotPathString() +
                                "; opened change " + next.getOpenChangelistId() +
                                "; change " + next.getChangelistId() +
                                "; action " + next.getAction());
                    }
                    // Ensure the changelist is as expected
                    if (next.getOpenChangelistId() <= IChangelist.DEFAULT) {
                        next.setOpenChangelistId(next.getChangelistId());
                    }
                }
            }

            // #210 and #207 - don't write to a potentially read-only list.

            // Then get opened files in the default changelist.
            LOG.debug("listOpenedFilesChanges@" + startDate + ": getting file details for default changelist");
            List<IExtendedFileSpec> openedDefaultChangelistFiles =
                    new ArrayList<>(cmd.getFilesOpenInDefaultChangelist(client.getServer(), client.getName(),
                            maxFileResults));
            List<IExtendedFileSpec> defaultAddedFiles =
                    new ArrayList<>(splitAddedFilesFromChangelistFileList(openedDefaultChangelistFiles));
            Iterator<IExtendedFileSpec> defaultIter = openedDefaultChangelistFiles.iterator();
            while (defaultIter.hasNext()) {
                IExtendedFileSpec next = defaultIter.next();
                if (next.getStatusMessage() != null) {
                    // This can be a "not on server" message if nothing is checked out.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Opened File Spec " + next + "; message: " + next.getStatusMessage());
                    }
                    defaultIter.remove();
                } else if (next.getAction() == null) {
                    // This seems to happen when there's a double entry in the returned list.
                    // TODO better understand why this situation happens; when it does, the CL is always -1.
                    if (LOG.isDebugEnabled()) {
                        if (!foundNonOpened) {
                            LOG.debug(
                                    "Found non-opened file spec for request of just opened file specs in default change.  "
                                            +
                                            next.getDepotPathString() + " :: open:" + next.getOpenChangelistId()
                                            + ", cl:" +
                                            next.getChangelistId() + ", owner:" + next.getOpenActionOwner());
                            foundNonOpened = true;
                        }
                    }
                    defaultIter.remove();
                } else {
                    // Ensure the changelist ID is correct - we know that these are in the default changelist.
                    next.setChangelistId(IChangelist.DEFAULT);
                    next.setOpenChangelistId(IChangelist.DEFAULT);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pending Default changelist file" +
                                ": depot:" + next.getDepotPathString() +
                                "; local:" + next.getLocalPathString() +
                                "; client:" + next.getClientPathString() +
                                "; original:" + next.getOriginalPathString() +
                                "; preferred:" + next.getPreferredPathString() +
                                "; annotated:" + next.getAnnotatedPreferredPathString() +
                                "; change " + next.getOpenChangelistId() +
                                "; action " + next.getAction());
                    }
                }
            }

            // Need to find the local path for the added files.
            addLocalPathsFromDepotPaths(client, pendingAddedFiles, defaultAddedFiles);
            pendingChangelistFiles.addAll(pendingAddedFiles);
            openedDefaultChangelistFiles.addAll(defaultAddedFiles);

            // Then join all the information together.
            if (LOG.isDebugEnabled()) {
                LOG.debug("listOpenedFilesChanges@" + startDate + ": generating result");
            }
            ListOpenedFilesChangesResult ret = OpenedFilesChangesFactory.createListOpenedFilesChangesResult(
                    config, changes, pendingChangelistFiles, shelvedFiles, openedDefaultChangelistFiles);
            if (LOG.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Final opened file list: ");
                for (P4LocalFile file : ret.getOpenedFiles()) {
                    sb.append(file.getDepotPath()).append(" :: ").append(file.getClientDepotPath()).append(" :: ")
                        .append(file.getFilePath());
                    sb.append("; ");
                }
                LOG.debug(sb.toString());
            }
            ClientOpenCacheMessage.sendEvent(new ClientOpenCacheMessage.Event(
                    config.getClientServerRef(), ret.getOpenedFiles(), ret.getPendingChangelists()
            ));
            return ret;
        } finally {
            LOG.info("Finished listOpenedFilesChanges; started at " + startDate + ", ended at " + (new Date()));
        }
    }

    private ListClientsForUserResult listClientsForUser(IOptionsServer server, OptionalClientServerConfig config,
            String username, int maxClients)
            throws P4JavaException {
        // TODO use P4CommandUtil
        GetClientsOptions opts = new GetClientsOptions(maxClients, username, null);
        List<P4WorkspaceSummary> summaries = new ArrayList<>();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching clients");
        }
        for (IClientSummary client : server.getClients(opts)) {
            summaries.add(new P4WorkspaceSummaryImpl(client));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetched " + summaries.size() + " clients");
        }
        return new ListClientsForUserResult(config, username, summaries);
    }


    @NotNull
    private List<IExtendedFileSpec> splitAddedFilesFromChangelistFileList(List<? extends IFileSpec> clFiles) {
        // Extract all the files that are marked as a variation of "add".  These will not return any information
        // from fstat, because they are not on the server.  All such files are removed from the clFiles list
        // and returned as extended file specs.
        List<IExtendedFileSpec> added = new ArrayList<>();

        Iterator<? extends IFileSpec> iter = clFiles.iterator();
        while (iter.hasNext()) {
            IFileSpec next = iter.next();
            FileAction action = next.getAction();
            if (action != null) {
                switch (next.getAction()) {
                    case ADD:
                    case COPY_FROM:
                    case BRANCH:
                    case MOVE_ADD:
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Marking " + next + " as added");
                        }
                        added.add(AddedExtendedFileSpec.create(next));
                        iter.remove();
                        break;
                    default:
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Checking " + next.getAction() + " as not added for " + next);
                        }
                }
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Null action found for spec " + next + ": " + next.getStatusMessage());
            }
        }

        return added;
    }

    private void addLocalPathsFromDepotPaths(IClient client,
            List<IExtendedFileSpec> addedFiles1, List<IExtendedFileSpec> addedFiles2)
            throws ConnectionException, AccessException {
        List<IFileSpec> origSpecs = new ArrayList<>(addedFiles1);
        origSpecs.addAll(addedFiles2);
        if (origSpecs.isEmpty()) {
            // Nothing to do.
            return;
        }

        List<IFileSpec> where = cmd.getSpecLocations(client, FileSpecBuildUtil.stripDepotRevisions(origSpecs));
        int specPos = 0;
        for (int wherePos = 0; wherePos < where.size(); ++wherePos) {
            IFileSpec whereSpec = where.get(wherePos);
            if (whereSpec.getLocalPath() == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring where return value for " + whereSpec + ": " + whereSpec.getStatusMessage());
                }
            } else {
                if (specPos >= origSpecs.size()) {
                    throw new IllegalStateException("Where request returned too many values: requested " + origSpecs +
                            ", returned " + where);
                }
                IFileSpec spec = origSpecs.get(specPos++);
                spec.setLocalPath(whereSpec.getLocalPathString());
                spec.setClientPath(whereSpec.getClientPathString());
                // Do not set the depot path.
                // spec.setDepotPath(whereSpec.getDepotPathString());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("WHERE: Setting " + spec + " to local " + spec.getLocalPath() + "; client " +
                            spec.getClientPath() + "; depot " + spec.getDepotPath() + "; whereSpec depot " +
                            whereSpec.getDepotPath());
                }
            }
        }
        if (specPos < origSpecs.size()) {
            throw new IllegalStateException("Where request did not return enough values: requested " + origSpecs +
                    ", returned " + where);
        }
    }
}
