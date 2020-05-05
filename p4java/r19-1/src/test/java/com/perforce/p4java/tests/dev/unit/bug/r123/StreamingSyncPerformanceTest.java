package com.perforce.p4java.tests.dev.unit.bug.r123;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

/**
 * Test P4Java sync is around 50% slower than Process.exec("p4 sync").
 * <p>
 *
 * When sync large number of files the JVM can run out of memory. The best way
 * to handle this issue is to use IStreamingCallback and process each result map
 * streaming back from the server.
 */

@Jobs({ "job038737" })
@TestId("Dev123_DescribeChangelistTest")
public class StreamingSyncPerformanceTest extends P4JavaRshTestCase {
    
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", StreamingSyncPerformanceTest.class.getSimpleName());

    private IClient client = null;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("sockPerfPrefs", "3, 2, 1");
        setupServer(p4d.getRSHURL(), userName, password, true, props);
        client = getClient(server);
    }

    @After
    public void tearDown() {
        if (server != null) {
            endServerSession(server);
        }
    }

    /**
     * Test "p4 sync" using execStreamingMapCommand
     */
    @Test
    public void testExecStreamingMapCmdSync() throws Exception {
        int key = this.getRandomInt();
        SimpleCallbackHandler handler = new SimpleCallbackHandler(key);
        server.execStreamingMapCommand(CmdSpec.SYNC.toString(),
                new String[] { "-f", "//depot/asic/..." }, null, handler, key);
    }

    private static class SimpleCallbackHandler implements IStreamingCallback {
        private final int expectedKey;

        public SimpleCallbackHandler(int key) {
            this.expectedKey = key;
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
            failIfConditionFails(resultMap != null, "null result map in handleResult");
            return true;
        }
    }
}
