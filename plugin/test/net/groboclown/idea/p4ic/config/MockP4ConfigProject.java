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

package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerAddress.Protocol;
import net.groboclown.idea.p4ic.config.P4Config.ConnectionMethod;
import net.groboclown.idea.p4ic.server.connection.TestConnectionHandler;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

public class MockP4ConfigProject extends P4ConfigProject {
    private final Setup[] setups;


    public static class Setup {
        /** Allows for simulating clients on the same or different servers */
        @NotNull public String serverName = "test:1";
        @NotNull public final VirtualFile[] roots;
        @NotNull public final IOptionsServer mockServer;
        @NotNull public ConnectionMethod connectionMethod;

        public boolean autoOffline;
        private int index;

        public Setup(@NotNull final VirtualFile[] roots) {
            this.roots = roots;
            this.mockServer = mock(IOptionsServer.class);
            if (roots.length <= 1) {
                connectionMethod = ConnectionMethod.UNIT_TEST_SINGLE;
            } else {
                connectionMethod = ConnectionMethod.UNIT_TEST_MULTIPLE;
            }
        }
    }


    public static Setup mkSetup(@NotNull String serverName, @NotNull VirtualFile... roots) {
        Setup ret = new Setup(roots);
        ret.serverName = serverName;
        return ret;
    }


    public static Setup mkSetup(@NotNull VirtualFile... roots) {
        return new Setup(roots);
    }


    public MockP4ConfigProject(@NotNull final Project project, @NotNull Setup... setups) {
        super(project);
        this.setups = setups;
        for (int i = 0; i < setups.length; i++) {
            setups[i].index = i;
        }
    }


    @Override
    @NotNull
    public List<Client> loadClients(@NotNull Project project) throws P4InvalidConfigException, P4InvalidClientException {
        List<Client> ret = new ArrayList<Client>();
        for (Setup setup: setups) {
            ret.add(new P4ConfigProject.ClientImpl(project, new TestP4Config(setup),
                    Arrays.asList(setup.roots)));
        }
        return ret;
    }




    static class TestP4Config implements P4Config {
        private final Setup setup;

        TestP4Config(final Setup setup) {
            this.setup = setup;
            // Make sure the mock server is registered
            TestConnectionHandler.registerServer(setup.serverName, setup.mockServer);
        }

        @Override
        public boolean hasIsAutoOfflineSet() {
            return true;
        }

        @Override
        public boolean isAutoOffline() {
            return setup.autoOffline;
        }

        @Override
        public void reload() {

        }

        @Override
        public boolean hasPortSet() {
            return true;
        }

        @Nullable
        @Override
        public String getPort() {
            return setup.serverName;
        }

        @Override
        public boolean hasProtocolSet() {
            return true;
        }

        @Nullable
        @Override
        public Protocol getProtocol() {
            return Protocol.P4JAVA;
        }

        @Override
        public boolean hasClientnameSet() {
            return true;
        }

        @Nullable
        @Override
        public String getClientname() {
            return "test_" + setup.index;
        }

        @Override
        public boolean hasUsernameSet() {
            return true;
        }

        @Nullable
        @Override
        public String getUsername() {
            return "user";
        }

        @NotNull
        @Override
        public ConnectionMethod getConnectionMethod() {
            return setup.connectionMethod;
        }

        @Nullable
        @Override
        public String getPassword() {
            return null;
        }

        @Nullable
        @Override
        public String getAuthTicketPath() {
            return null;
        }

        @Override
        public boolean hasTrustTicketPathSet() {
            return false;
        }

        @Nullable
        @Override
        public String getTrustTicketPath() {
            return null;
        }

        @Override
        public boolean hasServerFingerprintSet() {
            return false;
        }

        @Nullable
        @Override
        public String getServerFingerprint() {
            return null;
        }

        @Nullable
        @Override
        public String getConfigFile() {
            return null;
        }

        @Override
        public boolean isPasswordStoredLocally() {
            return false;
        }
    }


}
