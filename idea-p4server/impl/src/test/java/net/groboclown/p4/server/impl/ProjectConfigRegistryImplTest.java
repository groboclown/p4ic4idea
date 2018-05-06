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
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.MockConfigPart;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class ProjectConfigRegistryImplTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getRegisteredClientConfig() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        idea.registerProjectComponent(ProjectConfigRegistry.COMPONENT_NAME, registry);

        assertSame(ProjectConfigRegistry.getInstance(idea.getMockProject()), registry);
    }

    @Test
    void addClientConfig_existing() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();

        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(0, removed.size());
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(config, fetchedState.getClientConfig());

        added.clear();
        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
    }

    @Test
    void addClientConfig_new() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, (event) -> fail("incorrectly called remove"));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();

        registry.addClientConfig(config, root);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNotNull(fetchedState);
        assertSame(config, fetchedState.getClientConfig());
    }

    @Test
    void removeClientConfig_notRegistered() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, clientConfig -> fail("should not have added anything"));
        ClientConfigRemovedMessage.addListener(client, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();

        registry.removeClientConfig(config.getClientServerRef());

        assertEquals(0, removed.size());
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);
    }

    @Test
    void removeClientConfig_registered() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();
        registry.addClientConfig(config, root);
        assertEquals(1, added.size());
        added.clear();

        registry.removeClientConfig(config.getClientServerRef());

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);
    }

    // projectOpened - nothing to do
    // initComponent - nothing to do

    @Test
    void projectClosed() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();
        registry.addClientConfig(config, root);
        assertEquals(1, added.size());
        added.clear();

        registry.projectClosed();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config, root));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfig(config.getClientServerRef()));
        assertEquals(0, removed.size());
    }

    @Test
    void disposeComponent() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, (e) -> removed.add(e.getClientConfig()));
        ClientConfig config = createClientConfig();
        VirtualFile root = MockVirtualFileSystem.createRoot();
        registry.addClientConfig(config, root);
        assertEquals(1, added.size());
        added.clear();

        registry.disposeComponent();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        ClientConfigRoot fetchedState =
                registry.getRegisteredClientConfigState(config.getClientServerRef());
        assertNull(fetchedState);

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config, root));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfig(config.getClientServerRef()));
        assertEquals(0, removed.size());
    }

    @Test
    void getComponentName() {
        ProjectConfigRegistry registry = new ProjectConfigRegistryImpl(idea.getMockProject());
        assertSame(ProjectConfigRegistry.COMPONENT_NAME, registry.getComponentName());
    }

    private ClientConfig createClientConfig() {
        MockConfigPart data = new MockConfigPart()
                .withServerName("1666")
                .withUsername("user")
                .withClientname("my-client");
        ServerConfig serverConfig = ServerConfig.createFrom(data);
        return ClientConfig.createFrom(serverConfig, data);
    }
}
