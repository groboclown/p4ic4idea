package com.perforce.p4java.tests.dev.unit.features132;

import com.perforce.p4java.P4JavaUtil;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.StandardPerforceServers;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.MockCommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.test.ServerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test turning on/off journal-wait. The client application can specify the
 * "noWait" replication when using a forwarding replica or an edge server.
 */
@Jobs({ "job068493" })
@TestId("Dev132_JournalWaitTest")
public class JournalWaitTest {
    @Rule
    public ServerRule serverRule = StandardPerforceServers.createP4Java20132();

    @Rule
    public TemporaryFolder clientRoot = new TemporaryFolder();

    private IOptionsServer server = null;

    @Before
    public void setUp()
            throws Exception {
        // URI for instantiating a NTS server implementation.
        // String ntsServerURI = "p4jrpcnts://eng-p4java-vm.perforce.com:20132";
        server = ServerFactory
                .getOptionsServer(StandardPerforceServers.PORT_NTS_20132, StandardPerforceServers.getSuperUserProperties());
        assertThat(server, notNullValue());

        // Register callback
        server.registerCallback(new MockCommandCallback());
        // Connect to the server.
        server.connect();
        P4JavaUtil.setUtf8CharsetIfServerSupportUnicode(server);

        server.login(StandardPerforceServers.getSuperUserProperties().getProperty(PropertyDefs.PASSWORD_KEY),
                new LoginOptions());

        IClient client = P4JavaUtil.getDefaultClient(server, clientRoot.getRoot());
        assertThat(client, notNullValue());
        server.setCurrentClient(client);
    }

    @Test
    public void testJournalWait()
            throws Exception {
        // Turn off journal-wait
        server.journalWait(new JournalWaitOptions().setNoWait(true));

        // Set "undoc" counters
        for (int i = 0; i < 50; i++) {
            String result = server.setCounter("Xtestcounter" + i, "10" + i,
                    new CounterOptions().setUndocCounter(true));
            assertThat(result, notNullValue());
        }
        for (int i = 0; i < 50; i++) {
            String result = server.setCounter("Ytestcounter" + i, "10" + i,
                    new CounterOptions().setUndocCounter(true));
            assertThat(result, notNullValue());
        }
        for (int i = 0; i < 50; i++) {
            String result = server.setCounter("Ztestcounter" + i, "10" + i,
                    new CounterOptions().setUndocCounter(true));
            assertThat(result, notNullValue());
        }

        // Turn on journal-wait
        server.journalWait(new JournalWaitOptions());
    }
}
