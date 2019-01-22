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
package net.groboclown.p4.server.impl.cache;

import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.DeleteChangelistAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import net.groboclown.p4.server.impl.cache.store.ClientQueryCacheStore;
import net.groboclown.p4.server.impl.cache.store.ProjectCacheStore;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4LocalChangelistImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertContainsAll;
import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheQueryHandlerImplTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getCachedOpenedChangelists_noCacheForClient() {
        MockConfigPart configPart = createConfigPart();
        ServerConfig serverConfig = ServerConfig.createFrom(configPart);
        ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, configPart);
        ProjectCacheStore projectStore = new ProjectCacheStore();
        CacheQueryHandlerImpl query = new CacheQueryHandlerImpl(idea.getMockProject(), projectStore);

        Collection<P4LocalChangelist> changes =
                query.getCachedOpenedChangelists(clientConfig);

        assertEmpty(changes);
    }

    @Test
    void getCachedOpenedChangelists_noActions() {
        MockConfigPart configPart = createConfigPart();
        ServerConfig serverConfig = ServerConfig.createFrom(configPart);
        ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, configPart);
        ProjectCacheStore projectStore = new ProjectCacheStore();
        CacheQueryHandlerImpl query = new CacheQueryHandlerImpl(idea.getMockProject(), projectStore);
        ClientQueryCacheStore clientStore = new ClientQueryCacheStore(clientConfig.getClientServerRef());
        P4LocalChangelist cl1 = new P4LocalChangelistImpl.Builder()
                .withChangelistId(new P4ChangelistIdImpl(1, clientConfig.getClientServerRef()))
                .withComment("comment 1")
                .withClientname(clientConfig.getClientname())
                .withUsername(serverConfig.getUsername())
                .build();
        clientStore.setChangelists(cl1);
        projectStore.addCache(clientStore);

        Collection<P4LocalChangelist> changes = query.getCachedOpenedChangelists(clientConfig);

        assertSize(1, changes);
        assertEqualChangelists(cl1, changes.iterator().next());
    }

    @Test
    void getCachedOpenedChangelists_deleteAction() {
        MockConfigPart configPart = createConfigPart();
        ServerConfig serverConfig = ServerConfig.createFrom(configPart);
        ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, configPart);
        ProjectCacheStore projectStore = new ProjectCacheStore();
        CacheQueryHandlerImpl query = new CacheQueryHandlerImpl(idea.getMockProject(), projectStore);
        ClientQueryCacheStore clientStore = new ClientQueryCacheStore(clientConfig.getClientServerRef());
        P4LocalChangelist cl1 = new P4LocalChangelistImpl.Builder()
                .withChangelistId(new P4ChangelistIdImpl(1, clientConfig.getClientServerRef()))
                .withComment("comment 1")
                .withClientname(clientConfig.getClientname())
                .withUsername(serverConfig.getUsername())
                .build();

        projectStore.addPendingAction(ActionStore.createPendingAction(
                clientConfig.getClientServerRef(), new DeleteChangelistAction(cl1.getChangelistId())));
        clientStore.setChangelists(cl1);
        projectStore.addCache(clientStore);

        Collection<P4LocalChangelist> changes = query.getCachedOpenedChangelists(clientConfig);

        assertSize(0, changes);
    }

    @Test
    void getCachedOpenedChangelists_createAction()
            throws InterruptedException {
        MockConfigPart configPart = createConfigPart();
        ServerConfig serverConfig = ServerConfig.createFrom(configPart);
        ClientConfig clientConfig = ClientConfig.createFrom(serverConfig, configPart);
        ProjectCacheStore projectStore = new ProjectCacheStore();
        CacheQueryHandlerImpl query = new CacheQueryHandlerImpl(idea.getMockProject(), projectStore);
        ClientQueryCacheStore clientStore = new ClientQueryCacheStore(clientConfig.getClientServerRef());
        P4LocalChangelist cl1 = new P4LocalChangelistImpl.Builder()
                .withChangelistId(new P4ChangelistIdImpl(1, clientConfig.getClientServerRef()))
                .withComment("comment 1")
                .withClientname(clientConfig.getClientname())
                .withUsername(serverConfig.getUsername())
                .build();
        CreateChangelistAction addChangelistAction = new CreateChangelistAction(
                clientConfig.getClientServerRef(), "my comment", "local-id");

        // Simulated P4ChangeProvider action.
        MockLocalChangeList ideChangeList = new MockLocalChangeList();
        ideChangeList.setName("id:123");
        IdeChangelistMap ideChangelistMap = new IdeChangelistMapImpl(idea.getMockProject(),
                projectStore.getChangelistCacheStore());
        ideChangelistMap.setMapping(addChangelistAction, ideChangeList);

        projectStore.addPendingAction(ActionStore.createPendingAction(
                clientConfig.getClientServerRef(), addChangelistAction));
        clientStore.setChangelists(cl1);
        projectStore.addCache(clientStore);

        List<P4LocalChangelist> changes = new ArrayList<>(query.getCachedOpenedChangelists(clientConfig));

        assertSize(2, changes);
        P4LocalChangelist res1 = changes.get(0).getChangelistId().getChangelistId() == 1
                ? changes.get(0) : changes.get(1);
        P4LocalChangelist res2 = changes.get(0).getChangelistId().getChangelistId() == 1
                ? changes.get(1) : changes.get(0);

        assertEqualChangelists(cl1, res1);

        assertEquals(
                projectStore.getChangelistCacheStore()
                        .getPendingChangelist(addChangelistAction, false).getChangelistId().getChangelistId(),
                res2.getChangelistId().getChangelistId());
        assertEquals("my comment", res2.getComment());
        assertEquals(clientConfig.getClientname(), res2.getClientname());

        // Note: information about the user name just isn't available in the depths of the code,
        // and it doesn't add any real benefit to the UI display.
        assertEquals("(unknown)", res2.getUsername());

        assertEmpty(res2.getAttachedJobs());
        assertEmpty(res2.getShelvedFiles());
        assertEmpty(res2.getFiles());
    }

    @Test
    void getCachedOpenedChangelists_filesMovedAction() {
        // FIXME test
    }

    @Test
    void getCachedOpenedChangelists_editAction() {
        // FIXME test
    }

    @Test
    void getCachedOpenedChangelists_addJobAction() {
        // FIXME test
    }

    @Test
    void getCachedOpenedFiles() {
        // FIXME test
    }


    private static MockConfigPart createConfigPart() {
        return new MockConfigPart()
                // By using the RSH port, it means that the connection will be kept open
                // (NTS connection).  By keeping the connection open until explicitly
                // disconnected, this will indirectly be testing that the
                // SimpleConnectionManager closes the connection.
                .withServerName("1234")
                .withUsername("u")
                .withNoPassword()
                .withClientname("client1");
    }


    private static void assertEqualChangelists(P4LocalChangelist expected, P4LocalChangelist actual) {
        assertNotNull(actual);
        assertEquals(expected.getChangelistId(), actual.getChangelistId());
        assertEquals(expected.getClientname(), actual.getClientname());
        assertEquals(expected.getComment(), actual.getComment());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getChangelistType(), actual.getChangelistType());
        assertEquals(expected.isDeleted(), actual.isDeleted());
        assertEquals(expected.hasShelvedFiles(), actual.hasShelvedFiles());
        assertEquals(expected.isOnServer(), actual.isOnServer());

        assertContainsAll(actual.getAttachedJobs(), expected.getAttachedJobs());
        assertContainsAll(actual.getFiles(), expected.getFiles());
        assertContainsAll(actual.getShelvedFiles(), expected.getShelvedFiles());
    }

}