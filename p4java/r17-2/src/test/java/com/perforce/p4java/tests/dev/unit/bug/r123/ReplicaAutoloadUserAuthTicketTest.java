package com.perforce.p4java.tests.dev.unit.bug.r123;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test replica auto load user's auth ticket from the tickets file.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job064875"})
@TestId("Dev123_ReplicaAutoloadUserAuthTicketTest")
public class ReplicaAutoloadUserAuthTicketTest extends P4JavaTestCase {
  private IOptionsServer server = null;

  /**
   * Test replica auto load user's auth ticket from the tickets file.
   */
  @Test
  public void testReplicaAutoLoadUserAuthTicket() throws Exception{

    try {
      server = ServerFactory.getOptionsServer(P4JTEST_REPLICA_SERVER_URL_DEFAULT, props);
      assertThat(server, notNullValue());

      // Register callback
      server.registerCallback(createCommandCallback());
      // Connect to the server.
      server.connect();
      setUtf8CharsetIfServerSupportUnicode(server);
      // Set the test user
      server.setUserName(getSuperUserName());
      // Login the user, write auth ticket to file
      server.login(getSuperUserPassword(), new LoginOptions());

      // Remove cached auth ticket
      // Forcing reload from file
      server.setAuthTicket(null);

      // Get and set the test client
      IClient client = getDefaultClient(server);
      assertThat(client, notNullValue());
      server.setCurrentClient(client);

      // Test the 'changes' command
      List<IChangelistSummary> changelistSummaries = server.getChangelists(
          null,
          new GetChangelistsOptions().setMaxMostRecent(10));
      assertThat(changelistSummaries.size(), is(10));

      // Logout
      server.logout();
    } finally {
      if (nonNull(server)) {
        endServerSession(server);
      }
    }
  }
}
