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
package net.groboclown.p4.server.api;

import com.intellij.openapi.project.Project;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.messagebus.ClientConfigAddedMessage;
import net.groboclown.p4.server.api.messagebus.ClientConfigRemovedMessage;
import net.groboclown.p4.server.api.messagebus.MessageBusClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class AbstractProjectConfigRegistryTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getRegisteredClientConfig() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        idea.registerProjectComponent(AbstractProjectConfigRegistry.COMPONENT_NAME, registry);

        assertSame(AbstractProjectConfigRegistry.getInstance(idea.getMockProject()), registry);
    }

    @Test
    void addClientConfig_existing() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, removed::add);
        ClientConfig config = createClientConfig();

        registry.addClientConfig(config);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(0, removed.size());
        assertSame(config, registry.getRegisteredClientConfig(config.getClientServerRef()));

        added.clear();
        registry.addClientConfig(config);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
    }

    @Test
    void addClientConfig_new() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, clientConfig -> fail("incorrectly called remove"));
        ClientConfig config = createClientConfig();

        registry.addClientConfig(config);

        assertEquals(1, added.size());
        assertSame(config, added.get(0));
        assertSame(config, registry.getRegisteredClientConfig(config.getClientServerRef()));
    }

    @Test
    void removeClientConfig_notRegistered() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, clientConfig -> fail("should not have added anything"));
        ClientConfigRemovedMessage.addListener(client, removed::add);
        ClientConfig config = createClientConfig();

        registry.removeClientConfig(config.getClientServerRef());

        assertEquals(0, removed.size());
        assertNull(registry.getRegisteredClientConfig(config.getClientServerRef()));
    }

    @Test
    void removeClientConfig_registered() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, removed::add);
        ClientConfig config = createClientConfig();
        registry.addClientConfig(config);
        assertEquals(1, added.size());
        added.clear();

        registry.removeClientConfig(config.getClientServerRef());

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        assertNull(registry.getRegisteredClientConfig(config.getClientServerRef()));
    }

    // projectOpened - nothing to do
    // initComponent - nothing to do

    @Test
    void projectClosed() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, removed::add);
        ClientConfig config = createClientConfig();
        registry.addClientConfig(config);
        assertEquals(1, added.size());
        added.clear();

        registry.projectClosed();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        assertNull(registry.getRegisteredClientConfig(config.getClientServerRef()));

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfig(config.getClientServerRef()));
        assertEquals(0, removed.size());
    }

    @Test
    void disposeComponent() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        final List<ClientConfig> added = new ArrayList<>();
        final List<ClientConfig> removed = new ArrayList<>();
        MessageBusClient client = MessageBusClient.forProject(idea.getMockProject(), idea.getMockProject());
        ClientConfigAddedMessage.addListener(client, added::add);
        ClientConfigRemovedMessage.addListener(client, removed::add);
        ClientConfig config = createClientConfig();
        registry.addClientConfig(config);
        assertEquals(1, added.size());
        added.clear();

        registry.disposeComponent();

        assertEquals(0, added.size());
        assertEquals(1, removed.size());
        assertSame(config, removed.get(0));
        assertNull(registry.getRegisteredClientConfig(config.getClientServerRef()));

        // closing the project should turn off further registration
        removed.clear();
        assertThrows(Throwable.class, () -> registry.addClientConfig(config));
        assertEquals(0, added.size());

        assertThrows(Throwable.class, () -> registry.removeClientConfig(config.getClientServerRef()));
        assertEquals(0, removed.size());
    }

    @Test
    void getComponentName() {
        AbstractProjectConfigRegistry registry = new TestableAbstractProjectConfigRegistry(idea.getMockProject());
        assertSame(AbstractProjectConfigRegistry.COMPONENT_NAME, registry.getComponentName());
    }

    private ClientConfig createClientConfig() {
        MockConfigPart data = new MockConfigPart()
                .withServerName("1666")
                .withUsername("user")
                .withClientname("my-client");
        ServerConfig serverConfig = ServerConfig.createFrom(data);
        return ClientConfig.createFrom(serverConfig, data);
    }


    class TestableAbstractProjectConfigRegistry
            extends AbstractProjectConfigRegistry {
        Map<Project, ClientConfig> added = new HashMap<>();
        Map<Project, ClientConfig> removed = new HashMap<>();

        TestableAbstractProjectConfigRegistry(Project project) {
            super(project);
        }

        @Override
        protected void addConfigToApplication(@NotNull Project project, @NotNull ClientConfig clientConfig) {
            added.put(project, clientConfig);
        }

        @Override
        protected void removeConfigFromApplication(@NotNull Project project, @NotNull ClientConfig clientConfig) {
            removed.put(project, clientConfig);
        }
    }
}