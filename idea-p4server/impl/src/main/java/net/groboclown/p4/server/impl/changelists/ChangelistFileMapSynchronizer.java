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
import net.groboclown.p4.server.api.IdeChangelistMap;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientQuery;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import net.groboclown.p4.server.api.commands.file.ListOpenedFilesQuery;
import net.groboclown.p4.server.api.commands.file.ListOpenedFilesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.util.StreamUtil;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChangelistMapSynchronizer {

    public Promise<?> synchronizeMapWithServer(
            @NotNull final Collection<ClientConfig> clients,
            @NotNull final P4CommandRunner runner,
            @NotNull final IdeChangelistMap changeMap,
            final boolean deleteNonEmptyChangelists) {
        final Set<P4ChangelistId> knownChangeIds = changeMap.getLinkedIdeChanges().keySet();
        final Map<ClientServerRef, ClientConfig> clientMap =
                StreamUtil.asReversedMap(clients, ClientConfig::getClientServerRef);
        final Promise<?> start = Promise.resolve(null);
        final Promise<List<ChangelistDetailResult>> knownChangeSummaries;
        if (knownChangeIds.isEmpty()) {
            knownChangeSummaries = Promise.resolve(Collections.emptyList());
        } else {
            knownChangeSummaries = Promises.collectResults(
                    queryServerChangelists(runner, clientMap, knownChangeIds).collect(Collectors.toList()));
            start
                .thenAsync((x) -> knownChangeSummaries)
                .then((resultList) -> reportServerChangelists(changeMap, resultList, deleteNonEmptyChangelists));
        }
        start.thenAsync((x) ->
                Promises.collectResults(
                        clients.stream().map((cl) -> runner.query(
                                        cl.getServerConfig(),
                                        new ListChangelistsForClientQuery(cl.getClientname())))
                                .collect(Collectors.toList())
                )
        ).thenAsync((resList) -> Promises.collectResults(queryClientChangelists(runner, resList))
        ).thenAsync((clDetailList) -> Promises.collectResults(
                constructOpenRequests(runner, clientMap, clDetailList, knownChangeSummaries))
        ).then((openRemoteChangelists) -> {
            changeMap.updateForOpenChanges(openRemoteChangelists.stream());
            return null;
        });

        return start;
    }

    private List<Promise<P4RemoteChangelist>> constructOpenRequests(
            @NotNull final P4CommandRunner runner,
            @NotNull final Map<ClientServerRef, ClientConfig> clientMap,
            @NotNull final List<ChangelistDetailResult> clDetailList,
            @NotNull final Promise<List<ChangelistDetailResult>> knownChangeSummaries) {
        // This is the main complex logic.  This is the key method that pulls down the data for:
        //   * Change list details:
        //       * associated client / server
        //       * owner username
        //       * description
        //       * jobs fixed and their fix status
        //   * List of opened files on each client
        //       * Associated changelist
        //       * depot path
        //       * local file path
        //       * edit status
        // We need to carefully construct the P4 server calls that can retrieve the best information
        // in the fewest / most efficient calls.
    }

    private List<Promise<ChangelistDetailResult>> queryClientChangelists(
            P4CommandRunner runner,
            List<ListChangelistsForClientResult> resList) {
        List<Promise<ChangelistDetailResult>> ret = new ArrayList<>();
        for (ListChangelistsForClientResult result : resList) {
            for (P4ChangelistSummary p4ChangelistSummary : result.getChangelistSummaryList()) {
                ret.add(runner.query(
                        result.getServerConfig(),
                        new ChangelistDetailQuery(p4ChangelistSummary.getChangelistId())));
            }
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
                builder.accept(runner.query(entry.getKey().getServerConfig(), new ChangelistDetailQuery(p4id)));
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
