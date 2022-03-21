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
package net.groboclown.p4plugin.modules.connection;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigRegistryImplTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getRegisteredClientConfig() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        idea.registerProjectComponent(ProjectConfigRegistry.COMPONENT_CLASS, registry);

        assertSame(registry, ProjectConfigRegistry.getInstance(idea.getMockProject()));
    }

    @Test
    void addClientConfig_existing() {
        ProjectConfigRegistryImpl registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this, e -> added.add(e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(client, this, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        MockVirtualFile root = MockVirtualFileSystem.createRoot();

        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(0, removed.size());
        assertRegisteredConfigEquals(registry, config, root);


        // Add the same config again.  Because the same config in the same root
        // has already been added, no event should trigger.
        added.clear();
        registry.addClientConfig(config, root);

        assertEquals(0, added.size());
        assertEquals(0, removed.size());

        // Add an exact same config but with a different object to a different root
        MockVirtualFile root2 = root.addChildDir(this, "2");
        registry.addClientConfig(createClientConfig(), root2);

        // Same config, so no new item added.
        assertEquals(0, added.size());
        assertEquals(0, removed.size());
        // But ensure that the new rooted result is valid for the new root.
        RootedClientConfig root2Client = registry.getClientConfigFor(root2);
        assertNotNull(root2Client);
        assertTrue(root2Client.getProjectVcsRootDirs().contains(root));
        assertTrue(root2Client.getProjectVcsRootDirs().contains(root2));

        // Add a different config to the same root
        added.clear();
        ClientConfig second = createClientConfig("second");
        registry.addClientConfig(second, root);
        // The config wasn't removed, because there's still another root using it.

        assertEquals(1, added.size());
        assertSame(second, added.get(0));
        assertEquals(0, removed.size());

        // And make sure the second one is still reported...
        assertRegisteredConfigEquals(registry, second, root);
        // And the first one is moved...
        assertRegisteredConfigEquals(registry, config, root2);

        // Now try removing the second root, just to make sure.
        added.clear();
        registry.removeClientConfigAt(root2);
        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));

        // And the second one still exists...
        assertRegisteredConfigEquals(registry, second, root);
    }

    @Test
    void addClientConfig_new() {
        ProjectConfigRegistryImpl registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this, e -> added.add(e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(client, this, (event) -> fail("incorrectly called remove"));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();

        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertRegisteredConfigEquals(registry, config, root);
    }

    @Test
    void removeClientConfig_notRegistered() {
        ProjectConfigRegistryImpl registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this,
                e -> fail("should not have added anything"));
        ClientConfigRemovedMessage.addListener(client, this, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();

        registry.removeClientConfigAt(MockVirtualFileSystem.createRoot());

        assertEquals(0, removed.size());
        List<RootedClientConfig> fetchedClients = registry.getClientConfigsForRef(config.getClientServerRef());
        assertEquals(0, fetchedClients.size());
    }

    @Test
    void removeClientConfig_registered() {
        ProjectConfigRegistryImpl registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this, e -> added.add(e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(client, this, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();
        registry.addClientConfig(config, root);
        assertEquals(1, added.size());
        added.clear();

        registry.removeClientConfigAt(root);

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        List<RootedClientConfig> fetchedClients = registry.getClientConfigsForRef(config.getClientServerRef());
        assertEquals(0, fetchedClients.size());
    }

    // initService - nothing to do

    @Test
    void dispose() {
        ProjectConfigRegistryImpl registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this, e -> added.add(e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(client, this, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();
        registry.addClientConfig(config, root);
        assertEquals(1, added.size());
        added.clear();

        registry.dispose();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        List<RootedClientConfig> fetchedClients = registry.getClientConfigsForRef(config.getClientServerRef());
        assertEquals(0, fetchedClients.size());

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config, root));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfigAt(root));
        assertEquals(0, removed.size());
    }

    private ClientConfig createClientConfig() {
        return createClientConfig("my-client");
    }

    private ClientConfig createClientConfig(String clientName) {
        MockConfigPart data = new MockConfigPart()
                .withServerName("1666")
                .withUsername("user")
                .withClientname(clientName);
        ServerConfig serverConfig = ServerConfig.createFrom(data);
        return ClientConfig.createFrom(serverConfig, data);
    }

    private static void assertRegisteredConfigEquals(
            ProjectConfigRegistry registry, ClientConfig config, @Nullable VirtualFile root) {
        List<RootedClientConfig> fetchedConfigs =
                registry.getClientConfigsForRef(config.getClientServerRef());
        assertEquals(1, fetchedConfigs.size());
        assertSame(config, fetchedConfigs.get(0).getClientConfig());
        if (root != null) {
            assertEquals(1, fetchedConfigs.get(0).getProjectVcsRootDirs().size());
            assertSame(root, fetchedConfigs.get(0).getProjectVcsRootDirs().get(0));
        }
    }
}
