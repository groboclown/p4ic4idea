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
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.DefaultChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.DefaultChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientQuery;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import net.groboclown.p4.server.api.commands.file.ListOpenedFilesQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.util.StreamUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangelistFileMapSynchronizer {

    /**
     * Performs a series of queries against the server, and populates the
     * {@link IdeFileMap} and {@link IdeChangelistMap} to match the values in the server at the
     * time of the request.
     *
     * @param clients list of client-server configurations to query against.
     * @param runner server runner that will run the queries.
     * @param fileMap the {@link IdeFileMap} to update to the correct state.
     * @param changeMap the {@link IdeChangelistMap} to update to the correct state.
     * @param deleteNonEmptyChangelists true if the changelist mapper should remove IDE changelists
     *                                  that were linked to submitted/deleted P4 changelists.
     * @return promise for when the request chain completes, or contains an error of the execution.
     */
    public Promise<?> synchronizeWithServer(
            @NotNull final Collection<ClientConfig> clients,
            @NotNull final P4CommandRunner runner,
            @NotNull final IdeFileMap fileMap,
            @NotNull final IdeChangelistMap changeMap,
            final boolean deleteNonEmptyChangelists) {
        final Set<P4ChangelistId> knownChangeIds = changeMap.getLinkedIdeChanges().keySet();
        final Map<ClientServerRef, ClientConfig> clientMap =
                StreamUtil.asReversedMap(clients, ClientConfig::getClientServerRef);
        final Promise<?> start = Promise.resolve(null);

        // FIXME this is all wrong.  It needs to conform to the message bus execution by generating
        // a SINGLE event for the entire sync.  It must also return the event object as the promise value.


        // If the mapping has existing changelist knowledge, discover if any of them are removed or
        // submitted on the server.
        if (! knownChangeIds.isEmpty()) {
            start
                .thenAsync((x) -> Promises.collectResults(
                        queryServerChangelists(runner, clientMap, knownChangeIds).collect(Collectors.toList())))
                .then((resultList) -> reportServerChangelists(changeMap, resultList, deleteNonEmptyChangelists));
        }

        // Next, find all opened files for the clients and report them to the file mapping.
        start.thenAsync((x) ->
                Promises.collectResults(
                        clients.stream().map((cl) -> runner.query(
                                cl, new ListOpenedFilesQuery()
                        )).collect(Collectors.toList())
                ))
        .then((openedFilesList) -> {
            fileMap.updateAllLinkedFiles(openedFilesList.stream()
                            .flatMap((openedFiles) -> openedFiles.getOpenedFiles().stream())
                    );
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
            changeMap.updateForOpenChanges(fileMap, clDetailList.stream().map(ChangelistDetailResult::getChangelist));
            return null;
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
}
