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
import net.groboclown.idea.ExtAsserts;
import net.groboclown.p4.server.api.MockCommandRunner;
import net.groboclown.p4.server.api.MockCommandRunner.ClientQueryAnswer;
import net.groboclown.p4.server.api.MockCommandRunner.ServerQueryAnswer;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
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
import net.groboclown.p4.server.api.values.MockP4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4ChangelistSummary;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unchecked")
class ChangelistFileMapSynchronizerTest {
    private static final String CLIENTNAME = "my-client";

    @Test
    void synchronizeWithServer_empty() {
        final MockIdeFileMap fileMap = new MockIdeFileMap();
        final MockIdeChangelistMap changelistMap = new MockIdeChangelistMap();
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

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        sync.synchronizeWithServer(
                Collections.singleton(config),
                runner,
                fileMap,
                changelistMap,
                false
        );

        ExtAsserts.assertEmpty(fileMap.files);
        ExtAsserts.assertContainsAll(changelistMap.openChanges, defaultChange);

        // Should never have been called.
        assertNull(changelistMap.closedChanges);
    }

    @Test
    void synchronizeWithServer_oldDefaultChanges() {
        final MockIdeFileMap fileMap = new MockIdeFileMap();
        final MockIdeChangelistMap changelistMap = new MockIdeChangelistMap();
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();
        MockLocalChangeList defaultLocal = new MockLocalChangeList()
                .withName("Default")
                .withIsDefault(true);
        changelistMap.linkedIdeChanges.put(defaultChange.getChangelistId(), defaultLocal);

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
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        sync.synchronizeWithServer(
                Collections.singleton(config),
                runner,
                fileMap,
                changelistMap,
                false
        );

        ExtAsserts.assertEmpty(fileMap.files);
        ExtAsserts.assertContainsAll(changelistMap.openChanges,
                defaultChange);
        ExtAsserts.assertEmpty(changelistMap.closedChanges);
    }

    @Test
    void synchronizeWithServer_deletedChange() {
        final MockIdeFileMap fileMap = new MockIdeFileMap();
        final MockIdeChangelistMap changelistMap = new MockIdeChangelistMap();
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        final MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();
        final MockLocalChangeList defaultLocal = new MockLocalChangeList()
                .withName("Default")
                .withIsDefault(true);
        changelistMap.linkedIdeChanges.put(defaultChange.getChangelistId(), defaultLocal);

        final MockP4RemoteChangelist deletedChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withChangelistId(2)
                .withDeleted(true);
        final MockLocalChangeList oldLocal = new MockLocalChangeList()
                .withName("Old stuff")
                .withIsDefault(false);
        changelistMap.linkedIdeChanges.put(deletedChange.getChangelistId(), oldLocal);

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
        runner.setResult(P4CommandRunner.ServerQueryCmd.CHANGELIST_DETAIL,
                (ServerQueryAnswer) (sc, query) -> {
                    assertSame(config.getServerConfig(), sc);
                    assertEquals(ChangelistDetailQuery.class, query.getClass());
                    ChangelistDetailQuery q = (ChangelistDetailQuery) query;
                    assertEquals(q.getChangelistId(), deletedChange.getChangelistId());
                    return new ChangelistDetailResult(sc, deletedChange);
                });
        runner.setResult(P4CommandRunner.ClientQueryCmd.DEFAULT_CHANGELIST_DETAIL,
                (ClientQueryAnswer) (cc, query) -> {
                    assertSame(config, cc);
                    return new DefaultChangelistDetailResult(config, defaultChange);
                });

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        sync.synchronizeWithServer(
                Collections.singleton(config),
                runner,
                fileMap,
                changelistMap,
                false
        );

        ExtAsserts.assertEmpty(fileMap.files);
        ExtAsserts.assertContainsAll(changelistMap.openChanges,
                defaultChange);
        ExtAsserts.assertContainsAll(changelistMap.closedChanges,
                deletedChange.getSummary());
    }

    @Test
    void synchronizeWithServer_newChange() {
        final MockIdeFileMap fileMap = new MockIdeFileMap();
        final MockIdeChangelistMap changelistMap = new MockIdeChangelistMap();
        final MockCommandRunner runner = new MockCommandRunner();
        final ClientConfig config = mkClientConfig();
        assertNotNull(config.getClientname());

        final MockP4RemoteChangelist defaultChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withDefaultChangelist();
        final MockLocalChangeList defaultLocal = new MockLocalChangeList()
                .withName("Default")
                .withIsDefault(true);
        changelistMap.linkedIdeChanges.put(defaultChange.getChangelistId(), defaultLocal);

        final MockP4RemoteChangelist newChange = new MockP4RemoteChangelist()
                .withConfig(config)
                .withChangelistId(2)
                .withComment("Old stuff");

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

        ChangelistFileMapSynchronizer sync = new ChangelistFileMapSynchronizer();
        sync.synchronizeWithServer(
                Collections.singleton(config),
                runner,
                fileMap,
                changelistMap,
                false
        );

        ExtAsserts.assertEmpty(fileMap.files);
        ExtAsserts.assertContainsAll(changelistMap.openChanges,
                defaultChange, newChange);
        ExtAsserts.assertEmpty(changelistMap.closedChanges);
    }


    private static ClientConfig mkClientConfig() {
        ConfigPart part = new MockConfigPart()
                .withServerName("a12:12")
                .withClientname(CLIENTNAME)
                .withUsername("my-user");
        return ClientConfig.createFrom(ServerConfig.createFrom(part), part);
    }


    static class MockIdeFileMap implements IdeFileMap {
        List<P4LocalFile> files;

        @Nullable
        @Override
        public P4LocalFile forIdeFile(VirtualFile file) {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @Nullable
        @Override
        public P4LocalFile forIdeFile(FilePath file) {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @Nullable
        @Override
        public P4LocalFile forDepotPath(P4RemoteFile file) {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @NotNull
        @Override
        public Stream<P4LocalFile> getLinkedFiles() {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @Override
        public void updateAllLinkedFiles(@NotNull Stream<P4LocalFile> files) {
            assertNull(this.files, "Must only be called once");
            this.files = files.collect(Collectors.toList());
        }
    }

    static class MockIdeChangelistMap implements IdeChangelistMap {
        Map<P4ChangelistId, LocalChangeList> linkedIdeChanges = new HashMap<>();
        List<P4RemoteChangelist> openChanges;
        List<P4ChangelistSummary> closedChanges;
        boolean expectedDeleteNotEmpty;

        @Nullable
        @Override
        public LocalChangeList getIdeChangeFor(@NotNull P4ChangelistId changelistId) {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @Nullable
        @Override
        public P4ChangelistId getP4ChangeFor(@NotNull LocalChangeList changeList) {
            fail("Should not be called");
            throw new IllegalArgumentException("");
        }

        @NotNull
        @Override
        public Map<P4ChangelistId, LocalChangeList> getLinkedIdeChanges() {
            return linkedIdeChanges;
        }

        @Override
        public void updateForOpenChanges(@NotNull IdeFileMap fileMap, @NotNull Stream<P4RemoteChangelist> openChanges) {
            assertNotNull(fileMap);
            assertNull(this.openChanges, "Must only be called once");
            this.openChanges = openChanges.collect(Collectors.toList());
        }

        @Override
        public void updateForDeletedSubmittedChanges(@NotNull Stream<P4ChangelistSummary> closedChanges,
                boolean deleteNotEmpty) {
            assertNull(this.closedChanges, "Must only be called once");
            assertEquals(expectedDeleteNotEmpty, deleteNotEmpty);
            this.closedChanges = closedChanges.collect(Collectors.toList());
        }
    }


}