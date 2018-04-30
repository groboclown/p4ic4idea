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

package net.groboclown.p4.server.impl.changelists;

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheUpdateMessage;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.DefaultChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.DefaultChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientQuery;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import net.groboclown.p4.server.api.commands.file.ListOpenedFilesQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangelistFileMapSynchronizer {

    /**
     * Performs a series of queries against the server, and sends the cache update messages
     * based on the collected data.
     *
     * @param clients list of client-server configurations to query against.
     * @param runner server runner that will run the queries.
     * @return promise for when the request chain completes, or contains an error of the execution.
     *      Contains the events that are sent to the message bus.
     */
    public Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> synchronizeWithServer(
            @NotNull final Collection<ClientConfig> clients,
            @NotNull final P4CommandRunner runner) {
        final Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> start;
        final Map<ClientConfig, EventBuilder> builders = new HashMap<>();
        for (ClientConfig client : clients) {
            builders.put(client, new EventBuilder(client));
        }

        // TODO because the client can be shared between ClientConfig, need to optimize
        // this to run once per ClientServerRef, not per ClientConfig.

        // Find all opened files for the clients and report them to the file mapping.
        start = Promise.resolve(null)
        .thenAsync((x) ->
                Promises.collectResults(
                        clients.stream().map((cl) -> runner.query(
                                cl, new ListOpenedFilesQuery()
                        )).collect(Collectors.toList())
                ))
        .then((openedFilesList) -> {
                openedFilesList.forEach((res) ->
                        builders.get(res.getClientConfig()).openedFiles.addAll(res.getOpenedFiles()));
                return null;
        })

        // Then find all the opened changelists for the clients
        .thenAsync((x) ->
                Promises.collectResults(
                        clients.stream().map((cl) -> runner.query(
                                        cl.getServerConfig(),
                                        new ListChangelistsForClientQuery(cl.getClientname())))
                                .collect(Collectors.toList())
                ))

        // And gather up summary data for each of them
        .thenAsync((resList) -> Promises.collectResults(queryClientChangelists(runner, clients, resList)))

        // And finally send those changelist details to the changelist mapping.
        .then((clDetailList) -> {
                clDetailList.forEach((detail) -> {
                    for (Map.Entry<ClientConfig, EventBuilder> entry : builders.entrySet()) {
                        if (entry.getKey().getServerConfig().getServerName().equals(detail.getServerConfig().getServerName()) &&
                                entry.getKey().getClientname() != null &&
                                entry.getKey().getClientname().equals(detail.getChangelist().getClientname())) {
                            entry.getValue().openedChangelists.add(detail.getChangelist());
                        }
                    }
                });
                return null;
        })
        .then((x) -> {
            Map<ClientConfig, ClientOpenCacheUpdateMessage.Event> ret = new HashMap<>();
            for (Map.Entry<ClientConfig, EventBuilder> entry : builders.entrySet()) {
                ClientOpenCacheUpdateMessage.Event event = entry.getValue().build();
                ret.put(entry.getKey(), event);
                ClientOpenCacheUpdateMessage.sendEvent(event);
            }
            return ret;
        });

        return start;
    }

    private List<Promise<ChangelistDetailResult>> queryClientChangelists(
            P4CommandRunner runner,
            Collection<ClientConfig> clients,
            List<ListChangelistsForClientResult> resList) {

        // Unroll these lists of lists into a query to get details for each discovered open changelist.
        List<Promise<ChangelistDetailResult>> ret = new ArrayList<>();
        for (ListChangelistsForClientResult result : resList) {
            for (P4ChangelistSummary p4ChangelistSummary : result.getChangelistSummaryList()) {
                ret.add(runner.query(
                        result.getServerConfig(),
                        new ChangelistDetailQuery(p4ChangelistSummary.getChangelistId())));
            }
        }
        // Include the default changelist, too.
        for (ClientConfig client : clients) {
            ret.add(runner.query(client, new DefaultChangelistDetailQuery())
                    .then(DefaultChangelistDetailResult::getChangelistDetailResult));
        }
        return ret;
    }

    private Map<ClientConfig, Collection<P4ChangelistId>> sortChangelists(
            @NotNull Map<ClientServerRef, ClientConfig> clientMap, @NotNull Collection<P4ChangelistId> changes) {
        Map<ClientConfig, Collection<P4ChangelistId>> ret = new HashMap<>();
        for (P4ChangelistId change : changes) {
            ClientConfig clientConfig = clientMap.get(change.getClientServerRef());
            if (clientConfig != null) {
                Collection<P4ChangelistId> p4changes = ret.computeIfAbsent(clientConfig, k -> new ArrayList<>());
                p4changes.add(change);
            }
        }
        return ret;
    }

    private Stream<Promise<ChangelistDetailResult>> queryServerChangelists(
            @NotNull P4CommandRunner runner,
            @NotNull Map<ClientServerRef, ClientConfig> clientMap,
            @NotNull Collection<P4ChangelistId> changelistIds) {
        Stream.Builder<Promise<ChangelistDetailResult>> builder = Stream.builder();
        for (Map.Entry<ClientConfig, Collection<P4ChangelistId>> entry : sortChangelists(
                clientMap, changelistIds).entrySet()) {
            for (P4ChangelistId p4id : entry.getValue()) {
                // Cannot query default changelist details; it will never be marked as deleted or submitted,
                // anyway.
                if (!p4id.isDefaultChangelist()) {
                    builder.accept(runner.query(entry.getKey().getServerConfig(), new ChangelistDetailQuery(p4id)));
                }
            }
        }
        return builder.build();
    }

    private Void reportServerChangelists(
            @NotNull IdeChangelistMap changeMap,
            @NotNull List<ChangelistDetailResult> resultList,
            boolean deleteNonEmpty) {
        changeMap.updateForDeletedSubmittedChanges(
                resultList.stream().map((res) -> res.getChangelist().getSummary())
                    .filter((cl) -> cl.isDeleted() || cl.isSubmitted()),
                deleteNonEmpty
        );
        return null;
    }

    private static class EventBuilder {
        final ClientConfig config;
        final List<P4LocalFile> openedFiles = new ArrayList<>();
        final List<P4RemoteChangelist> openedChangelists = new ArrayList<>();

        EventBuilder(@NotNull ClientConfig config) {
            this.config = config;
        }

        ClientOpenCacheUpdateMessage.Event build() {
            return new ClientOpenCacheUpdateMessage.Event(config.getClientServerRef(), openedFiles, openedChangelists);
        }
    }
}
