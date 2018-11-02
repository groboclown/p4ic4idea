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

package net.groboclown.p4.server.impl;

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4CommandRunner.ServerResult;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistQuery;
import net.groboclown.p4.server.api.commands.changelist.DescribeChangelistResult;
import net.groboclown.p4.server.api.commands.changelist.GetJobSpecResult;
import net.groboclown.p4.server.api.commands.changelist.ListJobsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListJobsResult;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsQuery;
import net.groboclown.p4.server.api.commands.changelist.ListSubmittedChangelistsResult;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserQuery;
import net.groboclown.p4.server.api.commands.client.ListClientsForUserResult;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesQuery;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.commands.file.GetFileContentsResult;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryResult;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.commands.server.ListLabelsQuery;
import net.groboclown.p4.server.api.commands.server.ListLabelsResult;
import net.groboclown.p4.server.api.commands.server.SwarmConfigQuery;
import net.groboclown.p4.server.api.commands.server.SwarmConfigResult;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.commands.user.ListUsersResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A specialized command runner designed around only connections to the Perforce
 * server, without needing to deal with a cache.
 */
public abstract class AbstractServerCommandRunner {

    public interface ServerActionRunner<R extends ServerResult> {
        P4CommandRunner.ActionAnswer<R> perform(@NotNull ServerConfig config, @NotNull P4CommandRunner.ServerAction<R>
        action);
    }

    public interface ClientActionRunner<R extends P4CommandRunner.ClientResult> {
        P4CommandRunner.ActionAnswer<R> perform(
                @NotNull ClientConfig config, @NotNull P4CommandRunner.ClientAction<R> action);
    }


    private final Map<P4CommandRunner.ServerActionCmd, ServerActionRunner<?>> serverActionRunners = new HashMap<>();
    private final Map<P4CommandRunner.ClientActionCmd, ClientActionRunner<?>> clientActionRunners = new HashMap<>();


    protected void register(@NotNull P4CommandRunner.ServerActionCmd cmd, @NotNull ServerActionRunner<?> runner) {
        serverActionRunners.put(cmd, runner);
    }

    protected void register(@NotNull P4CommandRunner.ClientActionCmd cmd, @NotNull ClientActionRunner<?> runner) {
        clientActionRunners.put(cmd, runner);
    }



    @SuppressWarnings("unchecked")
    @NotNull
    public <R extends ServerResult> P4CommandRunner.ActionAnswer<R> perform(
            @NotNull ServerConfig config, @NotNull P4CommandRunner.ServerAction<R> action) {
        ServerActionRunner<?> runner = serverActionRunners.get(action.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + action.getCmd());
        }
        return ((ServerActionRunner<R>) runner).perform(config, action);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <R extends P4CommandRunner.ClientResult> P4CommandRunner.ActionAnswer<R> perform(
            @NotNull ClientConfig config, @NotNull P4CommandRunner.ClientAction<R> action) {
        ClientActionRunner<?> runner = clientActionRunners.get(action.getCmd());
        if (runner == null) {
            throw new IllegalStateException("command not supported: " + action.getCmd());
        }
        return ((ClientActionRunner<R>) runner).perform(config, action);
    }

    /**
     * Force all connections to close, if any are open in a pool.
     *
     * @param config server configuration's connections to close.
     */
    public abstract void disconnect(@NotNull P4ServerName config);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<AnnotateFileResult> getFileAnnotation(
            @NotNull ServerConfig config, @NotNull AnnotateFileQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<DescribeChangelistResult> describeChangelist(
            @NotNull ServerConfig config, @NotNull DescribeChangelistQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<GetJobSpecResult> getJobSpec(@NotNull ServerConfig config);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListOpenedFilesChangesResult> listOpenedFilesChanges(
            @NotNull ClientConfig config, @NotNull ListOpenedFilesChangesQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListClientsForUserResult> getClientsForUser(
            @NotNull ServerConfig config, @NotNull ListClientsForUserQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListSubmittedChangelistsResult> listSubmittedChangelists(
            @NotNull ClientConfig config, @NotNull ListSubmittedChangelistsQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<GetFileContentsResult> getFileContents(
            @NotNull ServerConfig config, @NotNull GetFileContentsQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListFileHistoryResult> listFilesHistory(ServerConfig config, ListFileHistoryQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListFilesDetailsResult> listFilesDetails(ServerConfig config, ListFilesDetailsQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListJobsResult> listJobs(ServerConfig config, ListJobsQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListLabelsResult> listLabels(ServerConfig config, ListLabelsQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<ListUsersResult> listUsers(ServerConfig config, ListUsersQuery query);

    @NotNull
    public abstract P4CommandRunner.QueryAnswer<SwarmConfigResult> getSwarmConfig(ServerConfig config, SwarmConfigQuery query);
}
