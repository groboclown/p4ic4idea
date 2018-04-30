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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.MockCommandRunner;
import net.groboclown.p4.server.api.MockCommandRunner.ClientQueryAnswer;
import net.groboclown.p4.server.api.MockCommandRunner.ServerQueryAnswer;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.cache.messagebus.ClientOpenCacheUpdateMessage;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailQuery;
import net.groboclown.p4.server.api.commands.changelist.ChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.DefaultChangelistDetailResult;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientQuery;
import net.groboclown.p4.server.api.commands.changelist.ListChangelistsForClientResult;
import net.groboclown.p4.server.api.commands.file.ListOpenedFilesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.ide.MockLocalChangeList;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import net.groboclown.p4.server.api.values.MockP4LocalFile;
import net.groboclown.p4.server.api.values.MockP4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.groboclown.idea.ExtAsserts.assertContainsAll;
import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class ChangelistFileMapSynchronizerTest {
    private static final String CLIENTNAME = "my-client";

    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void synchronizeWithServer_empty() {
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        runner.setResult(P4CommandRunner.ClientQueryCmd.LIST_OPENED_FILES,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new ListOpenedFilesResult(cc, Collections.emptyList());
                });
        runner.setResult(P4CommandRunner.ServerQueryCmd.LIST_CHANGELISTS_FOR_CLIENT,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ListChangelistsForClientQuery.class, query.getClass());
                    ListChangelistsForClientQuery q = (ListChangelistsForClientQuery) query;
                    assertEquals(config.getClientname(), q.getClientname());
                    return new ListChangelistsForClientResult(sc, config.getClientname(), Collections.emptyList());
                });
        MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        MessageBusClient mbClient = MessageBusClient.forApplication(idea.getDisposableParent());
        final List<ClientOpenCacheUpdateMessage.Event> events = new ArrayList<>();
        ClientOpenCacheUpdateMessage.addListener(mbClient, "1234", events::add);

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> results =
                sync.synchronizeWithServer(Collections.singleton(config), runner);

        assertSize(1, events);
        assertEmpty(events.get(0).getOpenedFiles());
        assertContainsAll(events.get(0).getOpenedChangelists(), defaultChange);

        Map<ClientConfig, ClientOpenCacheUpdateMessage.Event> res = results.blockingGet(1000);
        assertNotNull(res);
        assertSize(1, res.values());
        assertTrue(res.containsKey(config));
    }

    @Test
    void synchronizeWithServer_newChange() {
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        final MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();

        final MockP4RemoteChangelist newChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withChangelistId(2)
                .withComment("New stuff");

        runner.setResult(P4CommandRunner.ClientQueryCmd.LIST_OPENED_FILES,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new ListOpenedFilesResult(cc, Collections.emptyList());
                });

        runner.setResult(P4CommandRunner.ServerQueryCmd.LIST_CHANGELISTS_FOR_CLIENT,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ListChangelistsForClientQuery.class, query.getClass());
                    ListChangelistsForClientQuery q = (ListChangelistsForClientQuery) query;
                    assertEquals(config.getClientname(), q.getClientname());
                    return new ListChangelistsForClientResult(sc, config.getClientname(),
                            Collections.singletonList(newChange.getSummary()));
                });
        runner.setResult(P4CommandRunner.ServerQueryCmd.CHANGELIST_DETAIL,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ChangelistDetailQuery.class, query.getClass());
                    ChangelistDetailQuery q = (ChangelistDetailQuery) query;
                    assertEquals(q.getChangelistId(), newChange.getChangelistId());
                    return new ChangelistDetailResult(sc, newChange);
                });
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        MessageBusClient mbClient = MessageBusClient.forApplication(idea.getDisposableParent());
        final List<ClientOpenCacheUpdateMessage.Event> events = new ArrayList<>();
        ClientOpenCacheUpdateMessage.addListener(mbClient, "abcd", events::add);
        // by adding the listener twice, we double check that the event isn't double processed.
        ClientOpenCacheUpdateMessage.addListener(mbClient, "abcd", events::add);

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> result =
                sync.synchronizeWithServer(Collections.singleton(config), runner);

        assertSize(1, events);
        assertContainsAll(events.get(0).getOpenedChangelists(),
                defaultChange, newChange);
        assertEmpty(events.get(0).getOpenedFiles());

        assertNotNull(result);
        Map<ClientConfig, ClientOpenCacheUpdateMessage.Event> res = result.blockingGet(1000);
        assertNotNull(res);
        assertContainsAll(res.keySet(), config);
        assertContainsAll(res.get(config).getOpenedChangelists(),
                defaultChange, newChange);
        assertEmpty(res.get(config).getOpenedFiles());
    }

    @Test
    void synchronizeWithServer_openFiles() {
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        final MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();

        MockVirtualFile root = MockVirtualFileSystem.createRoot();

        MockP4LocalFile file1 = new MockP4LocalFile()
                .withDepotPath("//my/path/name.txt")
                .withFilePath(root.addChildFile(this, "name.txt", "", null))
                .withFileAction(P4FileAction.EDIT)
                .withFileType(P4FileType.TEXT)
                .withChangelistId(config.getClientServerRef(), 99)
                .withHaveRevision(13)
                .withResolveType(null)
                .withDefaultHeadFileRevision();
        MockP4LocalFile file2 = new MockP4LocalFile()
                .withDepotPath("//my/path/other.txt")
                .withFilePath(root.addChildFile(this, "other.txt", "", null))
                .withFileAction(P4FileAction.DELETE)
                .withFileType(P4FileType.TEXT)
                .withChangelistId(config.getClientServerRef(), 99)
                .withHaveRevision(1)
                .withResolveType(null)
                .withDefaultHeadFileRevision();
        runner.setResult(P4CommandRunner.ClientQueryCmd.LIST_OPENED_FILES,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new ListOpenedFilesResult(cc, Arrays.asList(file1, file2));
                });

        runner.setResult(P4CommandRunner.ServerQueryCmd.LIST_CHANGELISTS_FOR_CLIENT,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ListChangelistsForClientQuery.class, query.getClass());
                    ListChangelistsForClientQuery q = (ListChangelistsForClientQuery) query;
                    assertEquals(config.getClientname(), q.getClientname());
                    return new ListChangelistsForClientResult(sc, config.getClientname(), Collections.emptyList());
                });
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        MessageBusClient mbClient = MessageBusClient.forApplication(idea.getDisposableParent());
        final List<ClientOpenCacheUpdateMessage.Event> events = new ArrayList<>();
        ClientOpenCacheUpdateMessage.addListener(mbClient, "abcd", events::add);

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> result =
                sync.synchronizeWithServer(Collections.singleton(config), runner);

        assertSize(1, events);
        assertSize(1, events.get(0).getOpenedChangelists());
        assertSame(defaultChange, events.get(0).getOpenedChangelists().iterator().next());
        assertContainsAll(events.get(0).getOpenedFiles(),
                file1, file2);

        assertNotNull(result);
        Map<ClientConfig, ClientOpenCacheUpdateMessage.Event> res = result.blockingGet(1000);
        assertNotNull(res);
        assertContainsAll(res.keySet(), config);
        assertSize(1, res.get(config).getOpenedChangelists());
        assertSame(defaultChange, res.get(config).getOpenedChangelists().iterator().next());
        assertContainsAll(res.get(config).getOpenedFiles(),
                file1, file2);
    }

    @Test
    void synchronizeWithServer_newFilesChanges() {
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        final MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();
        final MockP4RemoteChangelist newChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withChangelistId(2)
                .withComment("Old stuff");

        MockVirtualFile root = MockVirtualFileSystem.createRoot();

        MockP4LocalFile file1 = new MockP4LocalFile()
                .withDepotPath("//my/path/name.txt")
                .withFilePath(root.addChildFile(this, "name.txt", "", null))
                .withFileAction(P4FileAction.EDIT)
                .withFileType(P4FileType.TEXT)
                .withChangelistId(config.getClientServerRef(), 99)
                .withHaveRevision(13)
                .withResolveType(null)
                .withDefaultHeadFileRevision();
        MockP4LocalFile file2 = new MockP4LocalFile()
                .withDepotPath("//my/path/other.txt")
                .withFilePath(root.addChildFile(this, "other.txt", "", null))
                .withFileAction(P4FileAction.DELETE)
                .withFileType(P4FileType.TEXT)
                .withChangelistId(config.getClientServerRef(), 99)
                .withHaveRevision(1)
                .withResolveType(null)
                .withDefaultHeadFileRevision();
        runner.setResult(P4CommandRunner.ClientQueryCmd.LIST_OPENED_FILES,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new ListOpenedFilesResult(cc, Arrays.asList(file1, file2));
                });

        runner.setResult(P4CommandRunner.ClientQueryCmd.LIST_OPENED_FILES,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new ListOpenedFilesResult(cc, Arrays.asList(file1, file2));
                });

        runner.setResult(P4CommandRunner.ServerQueryCmd.LIST_CHANGELISTS_FOR_CLIENT,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ListChangelistsForClientQuery.class, query.getClass());
                    ListChangelistsForClientQuery q = (ListChangelistsForClientQuery) query;
                    assertEquals(config.getClientname(), q.getClientname());
                    return new ListChangelistsForClientResult(sc, config.getClientname(),
                            Collections.singletonList(newChange.getSummary()));
                });
        runner.setResult(P4CommandRunner.ServerQueryCmd.CHANGELIST_DETAIL,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ChangelistDetailQuery.class, query.getClass());
                    ChangelistDetailQuery q = (ChangelistDetailQuery) query;
                    assertEquals(q.getChangelistId(), newChange.getChangelistId());
                    return new ChangelistDetailResult(sc, newChange);
                });
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        MessageBusClient mbClient = MessageBusClient.forApplication(idea.getDisposableParent());
        final List<ClientOpenCacheUpdateMessage.Event> events = new ArrayList<>();
        ClientOpenCacheUpdateMessage.addListener(mbClient, "qwerty", events::add);

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        Promise<Map<ClientConfig, ClientOpenCacheUpdateMessage.Event>> result =
                sync.synchronizeWithServer(Collections.singleton(config), runner);

        assertSize(1, events);
        assertContainsAll(events.get(0).getOpenedChangelists(),
                defaultChange, newChange);
        assertContainsAll(events.get(0).getOpenedFiles(),
                file1, file2);

        assertNotNull(result);
        Map<ClientConfig, ClientOpenCacheUpdateMessage.Event> res = result.blockingGet(1000);
        assertNotNull(res);
        assertContainsAll(res.keySet(), config);
        assertContainsAll(res.get(config).getOpenedChangelists(),
                defaultChange, newChange);
        assertContainsAll(res.get(config).getOpenedFiles(),
                file1, file2);
    }


    private static ClientConfig mkClientConfig() {
        ConfigPart part = new MockConfigPart()
                .withServerName("a12:12")
                .withClientname(CLIENTNAME)
                .withUsername("my-user");
        return ClientConfig.createFrom(ServerConfig.createFrom(part), part);
    }
}