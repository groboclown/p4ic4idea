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
package net.groboclown.idea.p4ic.v2.server.connection;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerAddress.Protocol;
import net.groboclown.idea.p4ic.ProjectRule;
import net.groboclown.idea.p4ic.config.ClientConfig;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config.ConnectionMethod;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import net.groboclown.idea.p4ic.mock.MockDataPart;
import net.groboclown.idea.p4ic.mock.MockOptionsServer;
import net.groboclown.idea.p4ic.mock.MockServerStatusController;
import net.groboclown.idea.p4ic.mock.P4Request;
import net.groboclown.idea.p4ic.mock.P4Response;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.connection.ClientExec.ServerCount;
import net.groboclown.idea.p4ic.v2.server.connection.ClientExec.WithClient;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

/**
 * This requires getting a valid Project instance.  Once that part of the ProjectRule is working
 * correctly, this this can be renamed to "ClientExecTest".
 */
public class ClientExecTestNotWorking {
    @Rule
    public ProjectRule project = new ProjectRule("test");


    private MockOptionsServer server;
    private MockServerStatusController controller;
    private ServerConfig serverConfig;
    private ClientExec exec;

    @Test
    public void testRunWithClient() throws Exception {
        // Required for login
        server.add(new P4Request(CmdSpec.INFO.toString()),
                P4Response.serverInfo(serverConfig, true, false));

        // Required for get client
        server.add(new P4Request(CmdSpec.CLIENT.toString(), "-o", "test-client"),
                P4Response.client("test-client", project.getVcsRoot(), null));


        exec.runWithClient(project.getProject(), new WithClient<Void>() {
            @Override
            public Void run(@NotNull final IOptionsServer server, @NotNull final IClient client,
                    @NotNull final ServerCount count)
                    throws P4JavaException, IOException, InterruptedException, TimeoutException, URISyntaxException,
                    P4Exception {

                // the test is just to get to this point.

                return null;
            }
        });
    }


    @Before
    public void setup() throws P4InvalidConfigException, ConnectionException, ConfigException {
        server = new MockOptionsServer("p4java://mock-options-server:1");
        controller = new MockServerStatusController();
        MockDataPart p4Config = new MockDataPart(FilePathUtil.getFilePath(".").getVirtualFile());
        p4Config.setUsername("test-user");
        // p4Config.setConnectionMethod(ConnectionMethod.UNIT_TEST_SINGLE);
        p4Config.setServerName("mock://mock-options-server:1");
        this.serverConfig = ServerConfig.createFrom(p4Config);
        ClientConfig clientConfig = ClientConfig.createFrom(null, serverConfig, p4Config,
                Collections.singleton(p4Config.getRootPath()));
        server.simulateSetup(serverConfig);
        exec = ClientExec.createFor(clientConfig, controller);
    }

    @After
    public void tearDown() {
        server.close();
        server = null;
        exec = null;
    }
}
