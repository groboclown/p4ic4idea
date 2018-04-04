package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test P4Java sync is around 50% slower than Process.exec("p4 sync").
 * <p>
 *
 * When sync large number of files the JVM can run out of memory. The best way
 * to handle this issue is to use IStreamingCallback and process each result map
 * streaming back from the server.
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job038737" })
@TestId("Dev123_DescribeChangelistTest")
public class StreamingSyncPerformanceTest extends P4JavaTestCase {
    private IOptionsServer server = null;
    private IClient client = null;

    @BeforeEach
    public void setUp() throws Exception {
        Properties props = new Properties();

        props.put("sockPerfPrefs", "3, 2, 1");
        // props.put("tcpNoDelay", "false");
        // props.put("enableProgress", "true");
        // props.put("defByteRecvBufSize", "40960");

        server = ServerFactory.getOptionsServer(this.serverUrlString, null);
        assertThat(server, notNullValue());

        // Register callback
        server.registerCallback(createCommandCallback());
        // Connect to the server.
        server.connect();
        setUtf8CharsetIfServerSupportUnicode(server);

        // Set the server user
        server.setUserName(this.userName);

        // Login using the normal method
        server.login(this.password, new LoginOptions());

        client = getDefaultClient(server);
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
     * Test "p4 sync" using execStreamingMapCommand
     */
    @Test
    public void testExecStreamingMapCmdSync() throws Exception {
        int key = this.getRandomInt();
        SimpleCallbackHandler handler = new SimpleCallbackHandler(this, key);
        server.execStreamingMapCommand(CmdSpec.SYNC.toString(),
                new String[] { "-f", "//depot/asic/..." }, null, handler, key);
    }

    private static class SimpleCallbackHandler implements IStreamingCallback {
        private final int expectedKey;
        private final StreamingSyncPerformanceTest testCase;

        public SimpleCallbackHandler(StreamingSyncPerformanceTest testCase, int key) {
            checkNotNull(testCase);
            this.expectedKey = key;
            this.testCase = testCase;
        }

        public boolean startResults(final int key) throws P4JavaException {
            failIfKeyNotEqualsExpected(key, expectedKey);
            return true;
        }

        public boolean endResults(final int key) throws P4JavaException {
            failIfKeyNotEqualsExpected(key, expectedKey);
            return true;
        }

        public boolean handleResult(final Map<String, Object> resultMap, final int key)
                throws P4JavaException {

            failIfKeyNotEqualsExpected(key, expectedKey);
            failIfConditionFails(nonNull(resultMap), "null result map in handleResult");
            return true;
        }
    }
}
