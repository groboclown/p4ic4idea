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

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobCacheMessage;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
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
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.client.OpenedFilesChangesFactory;
import net.groboclown.p4.server.impl.commands.ActionAnswerImpl;
import net.groboclown.p4.server.impl.commands.QueryAnswerImpl;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ConnectionManager connectionManager;

    public ConnectCommandRunner(
            @NotNull ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;

        register(P4CommandRunner.ServerActionCmd.CREATE_JOB,
            (ServerActionRunner<CreateJobResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (server) -> createJob(server, config, (CreateJobAction) action))));

        register(P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE,
            (ClientActionRunner<AddEditResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> addEditFile(client, config, (AddEditAction) action))));
        // FIXME register job handlers
        //register(P4CommandRunner.ClientActionCmd.ADD_JOB_TO_CHANGELIST, null);
        //register(P4CommandRunner.ClientActionCmd.CREATE_CHANGELIST, null);
        //register(P4CommandRunner.ClientActionCmd.DELETE_CHANGELIST, null);
        //register(P4CommandRunner.ClientActionCmd.DELETE_FILE, null);
        //register(P4CommandRunner.ClientActionCmd.EDIT_CHANGELIST_DESCRIPTION, null);
        //register(P4CommandRunner.ClientActionCmd.FETCH_FILES, null);
        //register(P4CommandRunner.ClientActionCmd.MOVE_FILE, null);
        //register(P4CommandRunner.ClientActionCmd.MOVE_FILES_TO_CHANGELIST, null);
        //register(P4CommandRunner.ClientActionCmd.REVERT_FILE, null);
        register(P4CommandRunner.ClientActionCmd.SUBMIT_CHANGELIST,
            (ClientActionRunner<SubmitChangelistResult>) (config, action) ->
                new ActionAnswerImpl<>(connectionManager.withConnection(config,
                    (client) -> submitChangelist(client, config, (SubmitChangelistAction) action))));
    }

    @Override
    public void disconnect(@NotNull ServerConfig config) {
        connectionManager.disconnect(config);
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<AnnotateFileResult> getFileAnnotation(@NotNull ServerConfig config,
            @NotNull AnnotateFileQuery query) {
        // FIXME implement
        return null;
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<DescribeChangelistResult> describeChangelist(@NotNull ServerConfig config,
            @NotNull DescribeChangelistQuery query) {
        // FIXME implement
        return null;
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<GetJobSpecResult> getJobSpec(@NotNull ServerConfig config) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
                (server) -> new GetJobSpecResult(
                        config,
                        new P4JobSpecImpl(P4CommandUtil.getJobSpec(server))
                )
        ));
    }

    @NotNull
    @Override
    public P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(@NotNull ClientConfig config,
            @NotNull ListOpenedFilesChangesQuery query) {
        return new QueryAnswerImpl<>(connectionManager.withConnection(config,
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


    private CreateJobResult createJob(IOptionsServer server, ServerConfig cfg, CreateJobAction action)
            throws ConnectionException, AccessException, RequestException {
        P4JobImpl job = new P4JobImpl(P4CommandUtil.createJob(server, action.getFields()));
        JobCacheMessage.sendEvent(new JobCacheMessage.Event(cfg.getServerName(), job,
                JobCacheMessage.JobUpdateAction.JOB_CREATED));
        return new CreateJobResult(cfg, job);
    }

    private AddEditResult addEditFile(IClient client, ClientConfig config, AddEditAction action)
            throws P4JavaException {
        // First, discover if the file is known by the server.
        GetDepotFilesOptions depotOptions = new GetDepotFilesOptions(1, false);
        // Must put the file path in escaped form when fetching list of depot files.
        List<IFileSpec> srcFiles = FileSpecBuildUtil.forEscapedFilePaths(action.getFile());
        List<IFileSpec> res = client.getServer().getDepotFiles(srcFiles, depotOptions);
        boolean addFile = false;
        if (res.isEmpty()) {
            addFile = true;
        } else {
            IServerMessage msg = res.get(0).getStatusMessage();
            if (msg != null) {
                if (msg.isWarning()) {
                    // This is how the "no such files" appears.
                    addFile = true;
                } else if (msg.isError()) {
                    // error - perhaps not in the client view?
                    throw new RequestException(msg);
                }
            }
        }

        List<IFileSpec> ret;
        if (addFile) {
            // Adding files uses the non-escaped form, because of the 'use wildcards' flag.
            srcFiles = FileSpecBuildUtil.forFilePaths(action.getFile());
            AddFilesOptions addOptions = new AddFilesOptions();
            if (action.getFileType() != null) {
                addOptions.setFileType(action.getFileType().toString());
            }
            if (action.getChangelistId() != null && !action.getChangelistId().isDefaultChangelist()) {
                addOptions.setChangelistId(action.getChangelistId().getChangelistId());
            }
            addOptions.setUseWildcards(true);
            if (action.getCharset() != null) {
                addOptions.setCharset(action.getCharset());
            }
            ret = client.addFiles(srcFiles, addOptions);
        } else {
            EditFilesOptions editOptions = new EditFilesOptions();
            if (action.getFileType() != null) {
                editOptions.setFileType(action.getFileType().toString());
            }
            if (action.getChangelistId() != null && !action.getChangelistId().isDefaultChangelist()) {
                editOptions.setChangelistId(action.getChangelistId().getChangelistId());
            }
            if (action.getCharset() != null) {
                editOptions.setCharset(action.getCharset());
            }
            ret = client.editFiles(srcFiles, editOptions);
        }
        if (ret.isEmpty()) {
            throw new P4JavaException("Unexpected error when " + (addFile ? "add" : "edit") +
                    "ing file: no results from server");
        }
        if (ret.get(0).getOpStatus() != FileSpecOpStatus.VALID) {
            IServerMessage msg = ret.get(0).getStatusMessage();
            if (msg != null) {
                throw new RequestException(msg);
            }
            throw new P4JavaException("Unexpected error when " + (addFile ? "add" : "edit") +
                    "ing file: " + ret.get(0));
        }
        P4ChangelistId retChange;
        if (action.getChangelistId() == null ||
                action.getChangelistId().getChangelistId() != ret.get(0).getChangelistId()) {
            retChange = new P4ChangelistIdImpl(ret.get(0).getChangelistId(), config.getClientServerRef());
        } else {
            retChange = action.getChangelistId();
        }
        return new AddEditResult(config, action.getFile(), addFile, P4FileType.convert(ret.get(0).getFileType()),
                retChange, new P4RemoteFileImpl(ret.get(0)));
    }


    private SubmitChangelistResult submitChangelist(IClient client, ClientConfig config,
            SubmitChangelistAction action)
            throws P4JavaException {
        SubmitOptions options = new SubmitOptions();
        if (action.getJobStatus() != null) {
            options.setJobStatus(action.getJobStatus().getName());
        }
        if (action.getUpdatedJobs() != null) {
            List<String> jobIds = new ArrayList<>(action.getUpdatedJobs().size());
            for (P4Job p4Job : action.getUpdatedJobs()) {
                jobIds.add(p4Job.getJobId());
            }
            options.setJobIds(jobIds);
        }
        IChangelist change;
        if (action.getChangelistId().getState() == P4ChangelistId.State.PENDING_CREATION) {
            change = CoreFactory.createChangelist(client, action.getUpdatedDescription(), true);
        } else if (action.getChangelistId().isDefaultChangelist()) {
            change = client.getServer().getChangelist(IChangelist.DEFAULT);
        } else {
            change = client.getServer().getChangelist(action.getChangelistId().getChangelistId());
        }

        if (change == null || change.getStatus() == ChangelistStatus.SUBMITTED) {
            throw new P4JavaException("No such pending change on server: " + action.getChangelistId());
        }

        if (action.getUpdatedDescription() != null) {
            change.setDescription(action.getUpdatedDescription());
        } else if (change.getDescription() == null) {
            throw new P4JavaException("Must include a description for new changelists");
        }

        List<IFileSpec> res = change.submit(options);
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
        // FIXME what information should be added to the result?
        return new SubmitChangelistResult(config, new P4ChangelistIdImpl(change.getId(), config.getClientServerRef()),
                submitted, info == null ? null : info.getLocalizedMessage());
    }


    private ListOpenedFilesChangesResult listOpenedFilesChanges(IClient client, ClientConfig config,
            int maxChangelistResults, int maxFileResults)
            throws P4JavaException {
        // This is a complex call, because we perform all the open requests in a single method.

        // First, find all the pending changelists for the client.
        List<IChangelistSummary> summaries = P4CommandUtil.getPendingChangelists(client, maxChangelistResults);

        // Then get details about the changelists.
        List<IChangelist> changes = new ArrayList<>(summaries.size());
        List<IFileSpec> pendingChangelistFileSummaries = new ArrayList<>();
        Map<Integer, List<IFileSpec>> shelvedFiles = new HashMap<>();
        for (IChangelistSummary summary : summaries) {
            IChangelist cl = P4CommandUtil.getChangelistDetails(client.getServer(), summary.getId());
            changes.add(cl);
            pendingChangelistFileSummaries.addAll(cl.getFiles(false));

            // Get the list of shelved files, if any
            if (cl.isShelved()) {
                shelvedFiles.put(summary.getId(), P4CommandUtil.getShelvedFiles(
                        client.getServer(), summary.getId(), maxFileResults));
            }
        }

        // Then find details on all the opened files
        List<IExtendedFileSpec> pendingChangelistFiles = P4CommandUtil.getFileDetailsForOpenedSpecs(
                client.getServer(), pendingChangelistFileSummaries);

        // Then get opened files in the default changelist.

        List<IExtendedFileSpec> openedDefaultChangelistFiles =
                P4CommandUtil.getFilesOpenInDefaultChangelist(client.getServer());

        // Then join all the information together.
        ListOpenedFilesChangesResult ret = OpenedFilesChangesFactory.createListOpenedFilesChangesResult(
                config, changes, pendingChangelistFiles, shelvedFiles, openedDefaultChangelistFiles);
        ClientOpenCacheMessage.sendEvent(new ClientOpenCacheMessage.Event(
                config.getClientServerRef(), ret.getOpenedFiles(), ret.getPendingChangelists()
        ));
        return ret;
    }

    private ListClientsForUserResult listClientsForUser(IOptionsServer server, ServerConfig config, String username,
            int maxClients) {
        // FIXME implement
        return null;
    }

}
