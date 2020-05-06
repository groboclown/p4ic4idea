package com.perforce.p4java.tests.dev.unit.bug.r123;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;

/**
 * Test auto load user's auth ticket from the tickets file.
 */

@Jobs({"job064875"})
@TestId("Dev123_AutoloadUserAuthTicketTest")
public class AutoloadUserAuthTicketTest extends P4JavaRshTestCase {

  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", AutoloadUserAuthTicketTest.class.getSimpleName());

  private static final String p4testclient = "p4TestUserWS20112";

  private IOptionsServer superserver = null;

  @After
  public void tearDown() {
    // cleanup code (after each test).
    if (superserver != null) {
      endServerSession(superserver);
    }
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test auto load user's auth ticket from the tickets file.
   */
  @Test
  public void testAutoLoadUserAuthTicket() throws Exception {
      // Using the super user to login the test user and save the test
      // user's auth ticket to file
      loginTestUser();

      //props.put("com.perforce.p4java.rpc.socketPoolSize", 100);
      setupServer(p4d.getRSHURL(), userName, password, true, props);

      // Get and set the test client
      IClient client = server.getClient(p4testclient);
      assertThat(client, notNullValue());
      server.setCurrentClient(client);

      // Test the 'changes' command
      List<IChangelistSummary> changelistSummaries = server
          .getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
      assertThat(changelistSummaries.size(), is(10));
  }

  /**
   * Use the super user to login the test user, so an auth ticket for the test
   * user will be written to file
   */
  private void loginTestUser() throws Exception{
      superserver = getSuperConnection(p4d.getRSHURL());
      assertThat(superserver, notNullValue());
      IUser anotherUser = superserver.getUser(userName);
      assertThat(anotherUser, notNullValue());

      StringBuffer authTicketFromMemory = new StringBuffer();

      // Login another user
      // The ticket should be written to the file
      // Also, the same ticket should be written to the StringBuffer
      superserver.login(anotherUser, authTicketFromMemory, new LoginOptions());
      assertThat(authTicketFromMemory, notNullValue());
      assertThat(authTicketFromMemory.length() > 0, is(true));
  }
}
