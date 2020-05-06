package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test P4Java sync is around 50% slower than Process.exec("p4 sync")
 */

@Jobs({ "job038737" })
@TestId("Dev123_DescribeChangelistTest")
public class SyncPerformanceTest extends P4JavaRshTestCase {
    
    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncPerformanceTest.class.getSimpleName());

    IClient client = null;
    
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
