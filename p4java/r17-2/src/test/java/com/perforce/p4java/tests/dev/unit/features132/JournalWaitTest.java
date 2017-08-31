package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test turning on/off journal-wait. The client application can specify the
 * "noWait" replication when using a forwarding replica or an edge server.
 */
@Jobs({"job068493"})
@TestId("Dev132_JournalWaitTest")
@RunWith(JUnitPlatform.class)
public class JournalWaitTest extends P4JavaTestCase {
  private IOptionsServer server = null;

  @BeforeEach
  public void setUp() throws Exception {
    // URI for instantiating a NTS server implementation.
    String ntsServerURI = "p4jrpcnts://eng-p4java-vm.perforce.com:20132";

    server = ServerFactory
        .getOptionsServer(ntsServerURI, null);
    assertThat(server, notNullValue());

    // Register callback
    server.registerCallback(createCommandCallback());
    // Connect to the server.
    server.connect();
    setUtf8CharsetIfServerSupportUnicode(server);

    // Set the server user
    server.setUserName(this.getSuperUserName());

    // Login using the normal method
    server.login(this.getSuperUserPassword(), new LoginOptions());

    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

  }

  @AfterEach
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
