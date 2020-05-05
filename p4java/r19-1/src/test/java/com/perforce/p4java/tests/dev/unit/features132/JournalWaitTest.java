package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test turning on/off journal-wait. The client application can specify the
 * "noWait" replication when using a forwarding replica or an edge server.
 */
@Jobs({ "job068493" })
@TestId("Dev132_JournalWaitTest")

public class JournalWaitTest extends P4JavaRshTestCase {

    @ClassRule
    public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", JournalWaitTest.class.getSimpleName());

    @Before
    public void setUp() throws Exception {
        setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
        client = getClient(server);
    }

    @After
    public void tearDown() {
        afterEach(server);
    }

    @Test
    public void testJournalWait() throws Exception {
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
