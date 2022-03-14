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

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
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

        assertSame(ProjectConfigRegistry.getInstance(idea.getMockProject()), registry);
    }

    @Test
    void addClientConfig_existing() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
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
        ClientConfig fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(config, fetchedState);

        // Add the same config again.  Because the same config in the same root
        // has already been added, no event should trigger.
        added.clear();
        registry.addClientConfig(config, root);

        assertEquals(0, added.size());
        assertEquals(0, removed.size());

        // Add an exact same config but with a different object to a different root
        MockVirtualFile root2 = root.addChildDir(this, "2");
        registry.addClientConfig(createClientConfig(), root2);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(0, removed.size());

        // Add a different config to the same root
        added.clear();
        ClientConfig second = createClientConfig("second");
        registry.addClientConfig(second, root);

        assertEquals(1, added.size());
        assertSame(second, added.get(0));
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        fetchedState = registry.getRegisteredClientConfigState(second.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(second, fetchedState);

        fetchedState = registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(config, fetchedState);
    }

    @Test
    void addClientConfig_new() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this, e -> added.add(e.getClientConfig()));
        ClientConfigRemovedMessage.addListener(client, this, (event) -> fail("incorrectly called remove"));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();

        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        ClientConfig fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(config, fetchedState);
    }

    @Test
    void removeClientConfig_notRegistered() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient.ProjectClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, this,
                e -> fail("should not have added anything"));
        ClientConfigRemovedMessage.addListener(client, this, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();

        registry.removeClientConfigAt(MockVirtualFileSystem.createRoot());

        assertEquals(0, removed.size());
        ClientConfig fetchedState = registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);
    }

    @Test
    void removeClientConfig_registered() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
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
        ClientConfig fetchedState = registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);
    }

    // projectOpened - nothing to do
    // initComponent - nothing to do

    @Test
    void projectClosed() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
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

        registry.projectClosed();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        ClientConfig fetchedState = registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config, root));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfigAt(root));
        assertEquals(0, removed.size());
    }

    @Test
    void disposeComponent() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
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

        registry.disposeComponent();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        ClientConfig fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config, root));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfigAt(root));
        assertEquals(0, removed.size());
    }

    @Test
    void getComponentName() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        assertSame(ProjectConfigRegistry.COMPONENT_NAME, registry.getComponentName());
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
}
