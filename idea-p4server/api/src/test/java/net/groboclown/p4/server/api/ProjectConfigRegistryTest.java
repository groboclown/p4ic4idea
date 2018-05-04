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
import net.groboclown.p4.server.api.cache.ClientConfigState;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
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


        assertNotNull(registry.getClientFor(five));
        assertSame(cc2, registry.getClientFor(five).getClientConfig());
        assertNotNull(registry.getClientFor(five.asFilePath()));
        assertSame(cc2, registry.getClientFor(five.asFilePath()).getClientConfig());

        assertNotNull(registry.getClientFor(one.asFilePath()));
        assertSame(cc1, registry.getClientFor(one.asFilePath()).getClientConfig());

        assertNull(registry.getClientFor(zero));
        assertNull(registry.getClientFor(zero.asFilePath()));
    }

    private static class TestableProjectConfigRegistry extends ProjectConfigRegistry {
        private final List<ClientConfigState> states = new ArrayList<>();

        protected TestableProjectConfigRegistry(@NotNull Project project) {
            super(project);
        }

        @Nullable
        @Override
        public ClientConfigState getRegisteredClientConfigState(@NotNull ClientServerRef ref) {
            return null;
        }

        @Override
        public void addClientConfig(@NotNull ClientConfig config, @NotNull VirtualFile vcsRootDir) {
            states.add(new TestableClientConfigState(config, vcsRootDir));
        }

        @Override
        public boolean removeClientConfig(@NotNull ClientServerRef ref) {
            throw new IllegalStateException();
        }

        @Nonnull
        @NotNull
        @Override
        protected Collection<ClientConfigState> getRegisteredStates() {
            return states;
        }

        @Override
        protected void onLoginError(@NotNull ServerConfig config) {
            throw new IllegalStateException();
        }

        @Override
        protected void onHostConnectionError(@NotNull P4ServerName server) {
            throw new IllegalStateException();
        }

        @Override
        protected void onServerConnected(@NotNull ServerConfig server) {
            throw new IllegalStateException();
        }

        @Override
        protected void onClientRemoved(@NotNull ClientConfig config, @Nullable VirtualFile vcsRootDir) {
            throw new IllegalStateException();
        }

        @Override
        protected void onUserSelectedOffline(@NotNull P4ServerName serverName) {
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
    }


    private static class TestableClientConfigState implements ClientConfigState {
        final ClientConfig config;
        final VirtualFile root;

        public TestableClientConfigState(ClientConfig config, VirtualFile vcsRootDir) {
            this.config = config;
            this.root = vcsRootDir;
        }

        @NotNull
        @Override
        public ClientConfig getClientConfig() {
            return config;
        }

        @Override
        public boolean isLoadedFromServer() {
            return false;
        }

        @Nullable
        @Override
        public Boolean isCaseSensitive() {
            return null;
        }

        @Nullable
        @Override
        public VirtualFile getClientRootDir() {
            return root;
        }

        @NotNull
        @Override
        public VirtualFile getProjectVcsRootDir() {
            return null;
        }

        @NotNull
        @Override
        public ServerConfig getServerConfig() {
            return config.getServerConfig();
        }

        @Override
        public boolean isOffline() {
            return false;
        }

        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public boolean isServerConnectionProblem() {
            return false;
        }

        @Override
        public boolean isUserWorkingOffline() {
            return false;
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public void dispose() {

        }
    }
}