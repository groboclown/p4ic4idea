package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.perforce.p4java.common.base.StringHelper.format;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test P4Java sync is around 50% slower than Process.exec("p4 sync")
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job038737" })
@TestId("Dev123_DescribeChangelistTest")
public class SyncPerformanceTest extends P4JavaTestCase {
    private IOptionsServer server = null;

    @BeforeEach
    public void setUp() throws Exception {
        Properties props = new Properties();

        props.put("sockPerfPrefs", "3, 2, 1");
        // props.put("tcpNoDelay", "false");
        // props.put("enableProgress", "true");
        // props.put("defByteRecvBufSize", "40960");

        server = ServerFactory.getOptionsServer(this.serverUrlString, props);
        assertThat(server, notNullValue());

        // Register callback
        server.registerCallback(createCommandCallback());
        // Connect to the server.
        server.connect();
        setUtf8CharsetIfServerSupportUnicode(server);

        // Set the server user
        server.setUserName(getUserName());

        // Login using the normal method
        server.login(getPassword(), new LoginOptions());

        IClient client = getDefaultClient(server);
        assertThat(client, notNullValue());
        server.setCurrentClient(client);
    }

    @AfterEach
    public void tearDown() {
        if (nonNull(server)) {
            endServerSession(server);
        }
    }

    /**
     * Test "p4 sync" using execMapCmd
     */
    @Test
    public void testExecMapCmdSync() throws Exception {
        List<Map<String, Object>> result = server.execMapCmdList(CmdSpec.SYNC.toString(),
                new String[] { "-f", "//depot/basic/..." }, null);
        assertThat(result, notNullValue());
        assertThat(result.size() > 0, is(true));
    }
}
