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

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import net.groboclown.p4.server.api.commands.sync.SyncListChangelistsForClientQuery;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.impl.AbstractCommandRunner;
import net.groboclown.p4.server.impl.cache.CacheQueryHandler;
import net.groboclown.p4.server.impl.values.P4ChangelistSummaryImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4.server.impl.values.P4JobSpecImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple version of the {@link net.groboclown.p4.server.api.P4CommandRunner}
 * that uses a {@link ConnectionManager} to open connections to the server.
 */
public class ConnectCommandRunner
        extends AbstractCommandRunner {
    public ConnectCommandRunner(
            @NotNull ConnectionManager connectionManager,
            @NotNull CacheQueryHandler cache) {
        register(ServerActionCmd.CREATE_JOB,
            (ServerActionRunner<CreateJobResult>) (config, action) ->
                connectionManager.withConnection(config,
                    // FIXME post cache update
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
                            new P4JobSpecImpl(server.getJobSpec())
                        )
                    )
                );

        //register(ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL, null);
        //register(ClientQueryCmd.LIST_CLIENT_FETCH_STATUS, null);
        //register(ClientQueryCmd.LIST_OPENED_FILES, null);

        register(SyncServerQueryCmd.SYNC_LIST_CHANGELISTS_FOR_CLIENT,
                (SyncCacheServerQueryRunner<ListChangelistsForClientResult>)(config, query) -> {
                    String clientname = ((SyncListChangelistsForClientQuery) query).getClientname();
                    // Immediately return the cached value.
                    return new ListChangelistsForClientResult(
                            config, clientname,
                            cache.getCachedChangelistsForClient(
                            config, clientname));
                });
        register(SyncServerQueryCmd.SYNC_LIST_CHANGELISTS_FOR_CLIENT,
                (SyncServerQueryRunner<ListChangelistsForClientResult>)(config, query) -> {
                    String clientname = ((SyncListChangelistsForClientQuery) query).getClientname();
                    int maxResults = ((SyncListChangelistsForClientQuery) query).getMaxResults();
                    // Run the fetch in the background
                    Promise<ListChangelistsForClientResult> promise =
                            connectionManager.withConnection(config,
                                    (server) -> listChangelistsForClient(server, config, clientname,
                                                    maxResults)
                                    );

                    return new FutureResult<>(promise,
                            new ListChangelistsForClientResult(config, clientname,
                                    cache.getCachedChangelistsForClient(config, clientname)));
                });

        //register(SyncClientQueryCmd.SYNC_LIST_OPENED_FILES, null);
    }

    private CreateJobResult createJob(IOptionsServer server, ServerConfig cfg, CreateJobAction action)
            throws ConnectionException, AccessException, RequestException {
        return new CreateJobResult(cfg, new P4JobImpl(server.createJob(action.getFields())));
    }

    private ListChangelistsForClientResult listChangelistsForClient(IOptionsServer server, ServerConfig config,
            String clientname, int maxResults)
            throws P4JavaException {
        GetChangelistsOptions options = new GetChangelistsOptions(
                maxResults, clientname, config.getUsername(), true, IChangelist.Type.PENDING, true
        );
        List<IChangelistSummary> res = server.getChangelists(null, options);
        ClientServerRef ref = new ClientServerRef(config.getServerName(), clientname);
        List<P4ChangelistSummary> txn = res.stream()
                .map((sum) -> new P4ChangelistSummaryImpl(config, ref, sum))
                .collect(Collectors.toList());

        return new ListChangelistsForClientResult(config, clientname, txn);
    }
}
