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
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.config.part.MockConfigPart;
import net.groboclown.p4.server.api.messagebus.ServerConnectedMessage;
import net.groboclown.p4.server.api.messagebus.UserSelectedOfflineMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProjectConfigRegistryTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getClientFor() {
        TestableProjectConfigRegistry registry = new TestableProjectConfigRegistry(idea.getMockProject());
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "one/two/root.txt", "contents"
        );
        MockVirtualFile five = vfs.get("one/two/three/four/five.txt");
        MockVirtualFile two = vfs.get("one/two/root.txt");
        MockVirtualFile one = vfs.get("one/root.txt");
        MockVirtualFile zero = vfs.get("root.txt");

        MockConfigPart cp1 = new MockConfigPart()
                .withClientname("cn")
                .withUsername("u")
                .withServerName("s:123");
        ClientConfig cc1 = ClientConfig.createFrom(ServerConfig.createFrom(cp1), cp1);
        registry.addClientConfig(cc1, one.getParent());

        MockConfigPart cp2 = new MockConfigPart()
                .withClientname("cn0")
                .withUsername("u0")
                .withServerName("s:123");
        ClientConfig cc2 = ClientConfig.createFrom(ServerConfig.createFrom(cp2), cp2);
        registry.addClientConfig(cc2, two.getParent());


        assertNotNull(registry.getClientConfigFor(five));
        assertSame(cc2, registry.getClientConfigFor(five).getClientConfig());
        assertNotNull(registry.getClientConfigFor(five.asFilePath()));
        assertSame(cc2, registry.getClientConfigFor(five.asFilePath()).getClientConfig());

        assertNotNull(registry.getClientConfigFor(one.asFilePath()));
        assertSame(cc1, registry.getClientConfigFor(one.asFilePath()).getClientConfig());

        assertNull(registry.getClientConfigFor(zero));
        assertNull(registry.getClientConfigFor(zero.asFilePath()));
    }

    private static class TestableProjectConfigRegistry extends ProjectConfigRegistry {
        private final List<RootedClientConfig> states = new ArrayList<>();

        protected TestableProjectConfigRegistry(@NotNull Project project) {
            super(project);
        }

        @NotNull
        @Override
        public List<RootedClientConfig> getRootedClientConfigs() {
            return states;
        }

        @Override
        protected void onLoginExpired(@NotNull OptionalClientServerConfig config) {

        }

        @Override
        protected void updateClientConfigAt(@Nonnull VirtualFile vcsRoot, @Nonnull List<ConfigPart> parts) {
            throw new IllegalStateException();
        }

        @Override
        public void removeClientConfigAt(@NotNull VirtualFile ref) {
            throw new IllegalStateException();
        }

        @Override
        protected void initializeRoots() {
            throw new IllegalStateException();
        }

        @Override
        protected void onLoginError(@NotNull OptionalClientServerConfig config) {
            throw new IllegalStateException();
        }

        @Override
        protected void onPasswordInvalid(@NotNull OptionalClientServerConfig config) {

        }

        @Override
        protected void onPasswordUnnecessary(@NotNull OptionalClientServerConfig config) {

        }

        @Override
        protected void onHostConnectionError(@NotNull P4ServerName server) {
            throw new IllegalStateException();
        }

        @Override
        protected void onServerConnected(@NotNull ServerConnectedMessage.ServerConnectedEvent e) {
            throw new IllegalStateException();
        }

        @Override
        protected void onUserSelectedOffline(@NotNull UserSelectedOfflineMessage.OfflineEvent e) {
            throw new IllegalStateException();
        }

        @Override
        protected void onUserSelectedOnline(@NotNull ClientServerRef clientServerRef) {
            throw new IllegalStateException();
        }

        @Override
        protected void onUserSelectedAllOnline() {
            throw new IllegalStateException();
        }

        void addClientConfig(@NotNull ClientConfig cc, @NotNull VirtualFile root) {
            states.add(new MockRootedClientConfig(cc, root));
        }
    }


}