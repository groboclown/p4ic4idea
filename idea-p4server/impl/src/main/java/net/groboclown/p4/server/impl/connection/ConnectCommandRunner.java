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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.ILabelSummary;
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
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.FixJobsOptions;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.GetJobsOptions;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
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
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
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
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.MoveFileResult;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileResult;
import net.groboclown.p4.server.api.commands.server.ListLabelsQuery;
import net.groboclown.p4.server.api.commands.server.ListLabelsResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
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
import net.groboclown.p4.server.impl.commands.QueryAnswerImpl;
import net.groboclown.p4.server.impl.connection.impl.FileAnnotationParser;
import net.groboclown.p4.server.impl.connection.impl.MessageStatusUtil;
import net.groboclown.p4.server.impl.connection.impl.OpenFileStatus;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4FileRevisionImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import net.groboclown.p4.server.impl.values.P4LabelImpl;
import net.groboclown.p4.server.impl.values.P4LocalFileImpl;
import net.groboclown.p4.server.impl.values.P4RemoteChangelistImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import net.groboclown.p4.server.impl.values.P4WorkspaceSummaryImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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


    public ConnectCommandRunner(@NotNull ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

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
                    ((MoveFileAction) action).getTargetFile().getIOFile().getParentFile(),
                    (client) -> moveFile(client, config, (MoveFileAction) action))));

        register(P4CommandRunner.ClientActionCmd.REVERT_FILE,
            (ClientActionRunner<RevertFileResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    ((RevertFileAction) action).getFile().getIOFile().getParentFile(),
                    (client) -> revertFile(client, config, (RevertFileAction) action))));

        register(P4CommandRunner.ClientActionCmd.SUBMIT_CHANGELIST,
            (ClientActionRunner<SubmitChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    // TODO does this need a directory for AltRoot purposes?  Probably.
                    (client) -> submitChangelist(client, config, (SubmitChangelistAction) action))));
    }

    @Override
    public void disconnect(@NotNull P4ServerName config) {
        connectionManager.disconnect(config);
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<AnnotateFileResult> getFileAnnotation(@NotNull final ServerConfig config,
            @NotNull final AnnotateFileQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
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
            ClientServerRef ref = new ClientServerRef(config.getServerName(), query.getClientname());
            IExtendedFileSpec headSpec = cmd.getFileDetails(server, query.getClientname(), specs);
            P4FileRevision headRevision = P4FileRevisionImpl.getHead(ref, headSpec);
            String content = new String(cmd.loadContents(server, headSpec, null),
                    headSpec.getHeadCharset() == null
                        ? Charset.defaultCharset().name()
                        : headSpec.getHeadCharset());
            List<IFileAnnotation> annotations = cmd.getAnnotations(server, specs);
            List<IFileSpec> requiredHistory = FileAnnotationParser.getRequiredHistorySpecs(annotations);
            List<Pair<IFileSpec, IFileRevisionData>> history = cmd.getExactHistory(server, requiredHistory);
            return new AnnotateFileResult(config,
                    FileAnnotationParser.getFileAnnotation(
                            ref, server.getUserName(), headSpec, query.getLocalFile(), annotations, history),
                    headRevision, content);
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<DescribeChangelistResult> describeChangelist(@NotNull ServerConfig config,
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
    public P4CommandRunner.QueryAnswer<GetJobSpecResult> getJobSpec(@NotNull ServerConfig config) {
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
    public P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(@NotNull ServerConfig config,
            @NotNull ListClientsForUserQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
                (server) -> listClientsForUser(server, config, query.getUsername(), query.getMaxClients())));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(
            @NotNull ServerConfig config, @NotNull ListSubmittedChangelistsQuery query) {
        // FIXME implement
        return null;
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<GetFileContentsResult> getFileContents(@NotNull ServerConfig config,
            @NotNull GetFileContentsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            final byte[] contents = query.when(
                    (depot) -> {
                        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(depot);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("get file contents for " + depot + ": " + specs);
                        }
                        return cmd.loadContents(server, specs.get(0), null);
                    },
                    (clientname, localFile, rev) -> cmd.loadContents(server,
                            FileSpecBuildUtil.escapedForFilePathRev(localFile, rev).get(0), clientname)
            );
            return query.when(
                    // TODO find correct charset
                    (depot) -> new GetFileContentsResult(config, depot, contents, null),
                    (clientname, localFile, rev) -> new GetFileContentsResult(config, localFile, contents, null)
            );
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListFileHistoryResult> listFilesHistory(ServerConfig config,
            ListFileHistoryQuery query) {
        final List<IFileSpec> fileSpec = FileSpecBuildUtil.escapedForFilePaths(query.getFile());
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) ->
                new ListFileHistoryResult(config, createFileHistoryList(
                    config, query.getFile(), cmd.getHistory(server,
                            query.getClientServerRef().getClientName(), fileSpec,
                            query.getMaxResults())))));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListFilesDetailsResult> listFilesDetails(ServerConfig config,
            ListFilesDetailsQuery query) {
        final List<IFileSpec> fileSpecs = FileSpecBuildUtil.escapedForFilePaths(query.getFiles());
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) ->
            new ListFilesDetailsResult(config,
                cmd.getFilesDetails(server, query.getClientServerRef().getClientName(), fileSpecs).entrySet().stream()
                    .map((e) ->
                        query.getRevState() == ListFilesDetailsQuery.RevState.HEAD
                            ? P4FileRevisionImpl.getHead(query.getClientServerRef(), e.getValue())
                            : P4FileRevisionImpl.getHave(query.getClientServerRef(), e.getValue())
                    )
                    .collect(Collectors.toList()))));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListJobsResult> listJobs(ServerConfig config, ListJobsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            // FIXME use P4CommandUtil
            LOG.warn("FIXME use P4CommandUtil");

            List<P4Job> jobs = new ArrayList<>();
            if (query.getJobId() != null) {
                IJob job = server.getJob(query.getJobId());
                if (job != null) {
                    jobs.add(new P4JobImpl(job));
                }
            }
            // TODO Finding the assigned-to user requires additional knowledge of the job spec that we don't have.
            // So for now, we only have job ID exact search and description search
            if (query.getDescription() != null) {
                GetJobsOptions options = new GetJobsOptions()
                        .setJobView(query.getDescription())
                        .setLongDescriptions(true)
                        .setMaxJobs(query.getMaxResults());
                List<IJob> jobsFound = server.getJobs(null, options);
                if (jobsFound != null) {
                    jobsFound.forEach((j) -> jobs.add(new P4JobImpl(j)));
                }
            }
            return new ListJobsResult(config, jobs);
        }));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListLabelsResult> listLabels(ServerConfig config, ListLabelsQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config, (server) -> {
            // FIXME use P4CommandUtil
            LOG.warn("FIXME use P4CommandUtil");

            GetLabelsOptions options = new GetLabelsOptions()
                    .setMaxResults(query.getMaxResults());
            if (query.hasNameFilter()) {
                options.setCaseInsensitiveNameFilter(query.getNameFilter());
            }
            // TODO allow for a list of files to trim down the query.
            List<ILabelSummary> labels = server.getLabels(null, options);
            return new ListLabelsResult(config, labels.stream().map(P4LabelImpl::new).collect(Collectors.toList()));
        }));
    }

    @NotNull
    private ListFileHistoryResult.VcsFileRevisionFactory createFileHistoryList(
            @NotNull final ServerConfig config, @NotNull final FilePath file,
            @NotNull final List<IFileRevisionData> history) {
        return (formatter, loader) -> {
            List<VcsFileRevision> ret = new ArrayList<>(history.size());
            history.forEach((d) -> {
                P4HistoryVcsFileRevision rev = new P4HistoryVcsFileRevision(file, config, d, formatter, loader);
                ret.add(rev);
            });
            return ret;
        };
    }

    private CreateJobResult createJob(IOptionsServer server, ServerConfig cfg, CreateJobAction action)
            throws ConnectionException, AccessException, RequestException {
        P4JobImpl job = new P4JobImpl(cmd.createJob(server, action.getFields()));
        JobCacheMessage.sendEvent(new JobCacheMessage.Event(cfg.getServerName(), job,
                JobCacheMessage.JobUpdateAction.JOB_CREATED));
        return new CreateJobResult(cfg, job);
    }

    private AddEditResult addEditFile(IClient client, ClientConfig config, AddEditAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running add/edit against the server for " + action.getFile());
            LOG.debug("Working directory: " + ((Server)client.getServer()).getUsageOptions().getWorkingDirectory());
        }

        // First, discover if the file is known by the server.
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
            List<IFileSpec> reverted = cmd.revertFiles(client, srcFiles, false);
            MessageStatusUtil.throwIfError(reverted);
            LOG.info("Reverted " + action.getFile() + ": " + MessageStatusUtil.getMessages(reverted, "\n"));
            addFile = true;
        } else if (LOG.isDebugEnabled() && !status.hasOpen()) {
            LOG.debug("Unexpected status message for " + status + "; assuming 'edit'");
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
            ret = cmd.addFiles(client, srcFiles, action.getFileType(), action.getChangelistId(), action.getCharset());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Opening " + action.getFile() + " for edit as " + srcFiles);
            }
            ret = cmd.editFiles(client, srcFiles, action.getFileType(), action.getChangelistId(), action.getCharset());
        }

        MessageStatusUtil.throwIfMessageOrEmpty(addFile ? "add" : "edit", ret);

        P4ChangelistId retChange;
        if (action.getChangelistId() == null ||
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


    private SubmitChangelistResult submitChangelist(IClient client, ClientConfig config,
            SubmitChangelistAction action)
            throws P4JavaException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running submit against the server for " + action.getChangelistId());
        }

        IChangelist change;
        if (action.getChangelistId().getState() == P4ChangelistId.State.PENDING_CREATION) {
            change = cmd.createChangelist(client, action.getUpdatedDescription());
        } else if (action.getChangelistId().isDefaultChangelist()) {
            change = cmd.getChangelistDetails(client.getServer(), IChangelist.DEFAULT);
        } else {
            change = cmd.getChangelistDetails(client.getServer(), action.getChangelistId().getChangelistId());
        }
        if (change == null || change.getStatus() == ChangelistStatus.SUBMITTED) {
            throw new P4JavaException("No such pending change on server: " + change);
        }
        if (action.getUpdatedDescription() != null && !action.getUpdatedDescription().isEmpty()) {
            change.setDescription(action.getUpdatedDescription());
        } else if (change.getDescription() == null) {
            throw new P4JavaException("Must include a description for new changelists");
        }


        if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting changelist " + action.getChangelistId());
        }
        List<IFileSpec> res = cmd.submitChangelist(
                action.getJobStatus(), action.getUpdatedJobs(), change);


        List<P4RemoteFile> submitted = new ArrayList<>(res.size());
        IServerMessage info = null;
        for (IFileSpec spec : res) {
            IServerMessage msg = spec.getStatusMessage();
            if (msg != null) {
                if (msg.isInfo()) {
                    info = msg;
                }
                if (msg.isWarning() || msg.isError()) {
                    throw new RequestException(msg);
                }
            } else {
                submitted.add(new P4RemoteFileImpl(spec));
            }
        }
        return new SubmitChangelistResult(config, new P4ChangelistIdImpl(change.getId(), config.getClientServerRef()),
                submitted, info == null ? null : info.getLocalizedMessage());
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

        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");

        List<IFileSpec> fileSpecs = FileSpecBuildUtil.escapedForFilePaths(action.getFiles());
        List<IFileSpec> res = client.reopenFiles(fileSpecs, action.getChangelistId().getChangelistId(), null);
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

        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");

        String res = client.getServer().deletePendingChangelist(action.getChangelistId().getChangelistId());
        return new DeleteChangelistResult(config, res);
    }

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
            LOG.info("Reverting files open for add rather than deleting them: " + action.getFile());
            List<IFileSpec> res = cmd.revertFiles(client, files, false);
            MessageStatusUtil.throwIfError(res);
            return new DeleteFileResult(config, P4RemoteFileImpl.createFor(files),
                    // TODO use P4Bundle
                    MessageStatusUtil.getMessages(res, "\n"));
        }
        if (status.hasEdit()) {
            LOG.info("Reverting files open for edit in preparation for delete: " + action.getFile());
            List<IFileSpec> res = cmd.revertFiles(client, files, false);
            MessageStatusUtil.throwIfError(res);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Opening " + files + " for delete");
        }

        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");
        DeleteFilesOptions opts = new DeleteFilesOptions(
                action.getChangelistId().getChangelistId(),
                false, false);
        List<IFileSpec> res = client.deleteFiles(files, opts);

        MessageStatusUtil.throwIf(res, (msg) -> {
            if (msg.isWarning() || msg.isError()) {
                // FIXME if the warning is something like "file is not on server can't delete", then ignore.
                // Check with a unit test.
                LOG.warn("FIXME check if the error message says that the file isn't on the server, and, if so, "
                        + "don't raise this as an error: " + msg);
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
        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");

        IChangelist changelist = client.getServer().getChangelist(action.getChangelistId().getChangelistId());
        if (changelist == null) {
            throw new P4JavaException("No such changelist " + action.getChangelistId().getChangelistId());
        }
        changelist.setDescription(action.getComment());
        changelist.update();
        return new EditChangelistResult(config);
    }

    private FetchFilesResult fetchFiles(IClient client, ClientConfig config, FetchFilesAction action)
            throws P4JavaException {
        // FIXME use P4CommandUtil
        LOG.warn("FIXME use P4CommandUtil");

        List<IFileSpec> files = FileSpecBuildUtil.escapedForFilePathsAnnotated(
                action.getSyncPaths(), action.getPathAnnotation(), true);
        SyncOptions options = new SyncOptions(action.isForce(), false, false, false);
        List<IFileSpec> res = client.sync(files, options);
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
        List<IFileSpec> srcFiles = FileSpecBuildUtil.escapedForFilePaths(action.getFile());
        List<IFileSpec> reverted = cmd.revertFiles(client, srcFiles, false);
        MessageStatusUtil.throwIfError(reverted);
        LOG.info("Reverted " + action.getFile() + ": " + MessageStatusUtil.getMessages(reverted, "\n"));
        return new RevertFileResult(config, action.getFile(), reverted);
    }

    private MoveFileResult moveFile(IClient client, ClientConfig config, MoveFileAction action)
            throws P4JavaException {
        List<IFileSpec> srcFile = FileSpecBuildUtil.escapedForFilePaths(action.getSourceFile());
        List<IFileSpec> tgtFile = FileSpecBuildUtil.escapedForFilePaths(action.getTargetFile());
        if (srcFile.size() != 1 || tgtFile.size() != 1) {
            throw new IllegalStateException("Must have 1 source and 1 target, have " + srcFile + "; " + tgtFile);
        }
        LOG.info("Running move command for `" + srcFile + "` to `" + tgtFile + "`");
        // Note the two separate fstat calls.  These are limited, and are okay, but it might
        // be better to join them together into a single call.
        List<IExtendedFileSpec> srcStatusResponse = cmd.getFileDetailsForOpenedSpecs(client.getServer(), srcFile, 1);
        OpenFileStatus srcStatus = new OpenFileStatus(srcStatusResponse);
        OpenFileStatus tgtStatus =
                new OpenFileStatus(cmd.getFileDetailsForOpenedSpecs(client.getServer(), tgtFile, 1));
        if (srcStatus.hasAdd()) {
            // source is open for add.  Revert it and mark the target as open for edit/add.
            List<IFileSpec> reverted = cmd.revertFiles(client, srcFile, false);
            MessageStatusUtil.throwIfError(reverted);
            // TODO bundle message for separator
            if (tgtStatus.hasDelete()) {
                // Currently marked as deleted, so it exists on the server.  Revert the delete then edit it.
                reverted = cmd.revertFiles(client, tgtFile, false);
                MessageStatusUtil.throwIfError(reverted);
                List<IFileSpec> edited = cmd.editFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(edited);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(edited, "\n"));
            } else if (srcStatus.isNotOnServer()) {
                // Target not on server
                List<IFileSpec> added = cmd.addFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(added);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(added, "\n"));
            } else if (!srcStatus.hasOpen()) {
                // On server and not open
                List<IFileSpec> edited = cmd.editFiles(client, tgtFile, null, action.getChangelistId(), null);
                MessageStatusUtil.throwIfError(edited);
                return new MoveFileResult(config, MessageStatusUtil.getMessages(edited, "\n"));
            } else {
                // Nothing to do
                // TODO bundle message
                return new MoveFileResult(config, "Already open");
            }
        } else if (srcStatus.hasDelete()) {
            // source is already open for delete.  Revert it and continue with normal move.
            List<IFileSpec> reverted = cmd.revertFiles(client, srcFile, false);
            MessageStatusUtil.throwIfError(reverted);
        } else if (srcStatus.isNotOnServer()) {
            // Forces an error, because the messages exist.
            MessageStatusUtil.throwIfMessageOrEmpty("move file", srcStatusResponse);
        }

        // Check target status, to see what we need to do there.
        if (tgtStatus.hasOpen()) {
            List<IFileSpec> reverted = cmd.revertFiles(client, tgtFile, false);
            MessageStatusUtil.throwIfError(reverted);
        }

        // Standard move operation.
        List<IFileSpec> results = cmd.moveFile(client, srcFile.get(0), tgtFile.get(0), action.getChangelistId());
        MessageStatusUtil.throwIfError(results);
        return new MoveFileResult(config, MessageStatusUtil.getMessages(results, "\n"));
    }

    private ListOpenedFilesChangesResult listOpenedFilesChanges(IClient client, ClientConfig config,
            int maxChangelistResults, int maxFileResults)
            throws P4JavaException {
        final Date startDate = new Date();
        LOG.info("Starting listOpenedFilesChanges at " + startDate);
        try {
            // This is a complex call, because we perform all the open requests in a single method.

            // First, find all the pending changelists for the client.
            List<IChangelistSummary> summaries = cmd.getPendingChangelists(client, maxChangelistResults);

            // Then get details about the changelists.
            List<IChangelist> changes = new ArrayList<>(summaries.size());
            List<IFileSpec> pendingChangelistFileSummaries = new ArrayList<>();
            Map<Integer, List<IFileSpec>> shelvedFiles = new HashMap<>();
            // Calling
            for (IChangelistSummary summary : summaries) {
                IChangelist cl = cmd.getChangelistDetails(client.getServer(), summary.getId());
                changes.add(cl);
                pendingChangelistFileSummaries.addAll(cl.getFiles(false));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("listOpenedFilesChanges: Fetched @" + cl.getId() + " files " + cl.getFiles(false));
                }

                // Get the list of shelved files, if any
                if (cl.isShelved()) {
                    shelvedFiles.put(summary.getId(), cmd.getShelvedFiles(
                            client.getServer(), summary.getId(), maxFileResults));
                }
            }

            // Then find details on all the opened files
            if (LOG.isDebugEnabled()) {
                LOG.debug("listOpenedFilesChanges@" + startDate + ": getting file details");
            }
            List<IExtendedFileSpec> pendingChangelistFiles = cmd.getFileDetailsForOpenedSpecs(
                    client.getServer(), pendingChangelistFileSummaries, maxFileResults);
            Iterator<IExtendedFileSpec> pendingIter = pendingChangelistFiles.iterator();
            // TODO DEBUG variable
            boolean foundNonOpened = false;
            while (pendingIter.hasNext()) {
                IExtendedFileSpec next = pendingIter.next();
                if (next.getStatusMessage() != null) {
                    LOG.info("Opened File Spec message: " + next.getStatusMessage().getAllInfoStrings());
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

            // Then get opened files in the default changelist.
            LOG.debug("listOpenedFilesChanges@" + startDate + ": getting file details for default changelist");
            List<IExtendedFileSpec> openedDefaultChangelistFiles =
                    cmd.getFilesOpenInDefaultChangelist(client.getServer(), client.getName(), maxFileResults);
            Iterator<IExtendedFileSpec> defaultIter = openedDefaultChangelistFiles.iterator();
            while (defaultIter.hasNext()) {
                IExtendedFileSpec next = defaultIter.next();
                if (next.getStatusMessage() != null) {
                    LOG.info("Default File Spec message: " + next.getStatusMessage().getAllInfoStrings());
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

            // Then join all the information together.
            LOG.debug("listOpenedFilesChanges@" + startDate + ": generating result");
            ListOpenedFilesChangesResult ret = OpenedFilesChangesFactory.createListOpenedFilesChangesResult(
                    config, changes, pendingChangelistFiles, shelvedFiles, openedDefaultChangelistFiles);
            ClientOpenCacheMessage.sendEvent(new ClientOpenCacheMessage.Event(
                    config.getClientServerRef(), ret.getOpenedFiles(), ret.getPendingChangelists()
            ));
            return ret;
        } finally {
            LOG.info("Finished listOpenedFilesChanges; started at " + startDate + ", ended at " + (new Date()));
        }
    }

    private ListClientsForUserResult listClientsForUser(IOptionsServer server, ServerConfig config, String username,
            int maxClients)
            throws P4JavaException {
        GetClientsOptions opts = new GetClientsOptions(maxClients, username, null);
        List<P4WorkspaceSummary> summaries = new ArrayList<>();
        for (IClientSummary client : server.getClients(opts)) {
            summaries.add(new P4WorkspaceSummaryImpl(client));
        }
        return new ListClientsForUserResult(config, username, summaries);
    }

}
