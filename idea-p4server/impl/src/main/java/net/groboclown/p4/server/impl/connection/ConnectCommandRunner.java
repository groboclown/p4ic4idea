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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheUpdateMessage;
import net.groboclown.p4.server.api.cache.messagebus.JobCacheMessage;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.commands.sync.SyncListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.AbstractServerCommandRunner;
import net.groboclown.p4.server.impl.cache.CacheQueryHandler;
import net.groboclown.p4.server.impl.client.OpenedFilesChangesFactory;
import net.groboclown.p4.server.impl.connection.impl.P4CommandUtil;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

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
            @NotNull ConnectionManager connectionManager,
            @NotNull CacheQueryHandler cache) {
        this.connectionManager = connectionManager;

        register(ServerActionCmd.CREATE_JOB,
            (ServerActionRunner<CreateJobResult>) (config, action) ->
                connectionManager.withConnection(config,
                    (server) -> createJob(server, config, (CreateJobAction) action)));

        //register(ClientActionCmd.ADD_EDIT_FILE, null);
        //register(ClientActionCmd.ADD_JOB_TO_CHANGELIST, null);
        //register(ClientActionCmd.CREATE_CHANGELIST, null);
        //register(ClientActionCmd.DELETE_CHANGELIST, null);
        //register(ClientActionCmd.DELETE_FILE, null);
        //register(ClientActionCmd.EDIT_CHANGELIST_DESCRIPTION, null);
        //register(ClientActionCmd.FETCH_FILES, null);
        //register(ClientActionCmd.MOVE_FILE, null);
        //register(ClientActionCmd.MOVE_FILES_TO_CHANGELIST, null);
        //register(ClientActionCmd.REVERT_FILE, null);

        //register(ServerNameQueryCmd.SERVER_INFO, null);

        //register(ServerQueryCmd.ANNOTATE_FILE, null);
        //register(ServerQueryCmd.CHANGELIST_DETAIL, null);
        //register(ServerQueryCmd.DESCRIBE_CHANGELIST, null);
        //register(ServerQueryCmd.LIST_CHANGELISTS_FIXED_BY_JOB, null);
        //register(ServerQueryCmd.LIST_CHANGELISTS_FOR_CLIENT, null);
        //register(ServerQueryCmd.LIST_CLIENTS_FOR_USER, null);
        //register(ServerQueryCmd.LIST_DIRECTORIES, null);
        //register(ServerQueryCmd.LIST_FILES, null);
        //register(ServerQueryCmd.LIST_FILES_DETAILS, null);
        //register(ServerQueryCmd.LIST_FILES_HISTORY, null);
        //register(ServerQueryCmd.LIST_JOBS, null);
        //register(ServerQueryCmd.LIST_SUBMITTED_CHANGELISTS, null);
        //register(ServerQueryCmd.LIST_USERS, null);

        register(ServerQueryCmd.GET_JOB_SPEC,
                (ServerQueryRunner<GetJobSpecResult>) (config, query) ->
                    connectionManager.withConnection(config,
                        (server) -> new GetJobSpecResult(
                            config,
                            new P4JobSpecImpl(P4CommandUtil.getJobSpec(server))
                        )
                    )
                );

        //register(ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL, null);
        //register(ClientQueryCmd.LIST_CLIENT_FETCH_STATUS, null);
        //register(ClientQueryCmd.LIST_OPENED_FILES, null);

        register(SyncClientQueryCmd.SYNC_LIST_OPENED_FILES_CHANGES,
                (SyncCacheClientQueryRunner<ListOpenedFilesChangesResult>)(config, query) -> {
                    // Immediately return the cached value.
                    return new ListOpenedFilesChangesResult(
                            config, cache.getCachedOpenedFiles(config),
                            cache.getCachedOpenedChangelists(config));
                });
        register(SyncClientQueryCmd.SYNC_LIST_OPENED_FILES_CHANGES,
                (SyncClientQueryRunner<ListOpenedFilesChangesResult>)(config, query) -> {
                    SyncListOpenedFilesChangesQuery olcq = (SyncListOpenedFilesChangesQuery) query;
                    // Run the fetch in the background
                    Promise<ListOpenedFilesChangesResult> promise =
                            connectionManager.withConnection(config,
                                    (client) -> listOpenedFilesChanges(client, config,
                                            olcq.getMaxChangelistResults(), olcq.getMaxFileResults()));

                    return new FutureResult<>(promise,
                            new ListOpenedFilesChangesResult(
                                    config, cache.getCachedOpenedFiles(config),
                                    cache.getCachedOpenedChangelists(config)));
                });
    }

    private CreateJobResult createJob(IOptionsServer server, ServerConfig cfg, CreateJobAction action)
            throws ConnectionException, AccessException, RequestException {
        P4JobImpl job = new P4JobImpl(P4CommandUtil.createJob(server, action.getFields()));
        JobCacheMessage.sendEvent(new JobCacheMessage.Event(cfg.getServerName(), job,
                JobCacheMessage.JobUpdateAction.JOB_CREATED));
        return new CreateJobResult(cfg, job);
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
        ClientOpenCacheUpdateMessage.sendEvent(new ClientOpenCacheUpdateMessage.Event(
                config.getClientServerRef(), ret.getOpenedFiles(), ret.getPendingChangelists()
        ));
        return ret;
    }

    @Override
    public void disconnect(@NotNull ServerConfig config) {
        connectionManager.disconnect(config);
    }
}
