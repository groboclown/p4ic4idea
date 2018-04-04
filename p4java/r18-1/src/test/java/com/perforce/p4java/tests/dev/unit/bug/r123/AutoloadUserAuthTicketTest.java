package com.perforce.p4java.tests.dev.unit.bug.r123;


import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test auto load user's auth ticket from the tickets file.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job064875"})
@TestId("Dev123_AutoloadUserAuthTicketTest")
public class AutoloadUserAuthTicketTest extends P4JavaTestCase {

  private static final String p4superuser = "p4jtestsuper";
  private static final String p4superpasswd = "p4jtestsuper";

  private static final String p4testuser = "p4jtestuser";
  private static final String p4testclient = "p4TestUserWS20112";

  private IOptionsServer superserver = null;
  private IOptionsServer server = null;

  @AfterEach
  public void tearDown() {
    // cleanup code (after each test).
    if (nonNull(superserver)) {
      endServerSession(superserver);
    }
    if (nonNull(server)) {
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

      Properties props = new Properties();
      //props.put("com.perforce.p4java.rpc.socketPoolSize", 100);
      server = ServerFactory.getOptionsServer(serverUrlString, props);
      assertThat(server, notNullValue());

      // Register callback
      server.registerCallback(createCommandCallback());
      // Connect to the server.
      server.connect();
      setUtf8CharsetIfServerSupportUnicode(server);
      // Set the test user
      server.setUserName(p4testuser);

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
      superserver = ServerFactory.getOptionsServer(serverUrlString, null);
      assertThat(superserver, notNullValue());

      // Register callback
      superserver.registerCallback(createCommandCallback());

      // Connect to the server.
      superserver.connect();
      setUtf8CharsetIfServerSupportUnicode(superserver);

      // Set the super user to the server
      superserver.setUserName(p4superuser);

      // Login the super user
      superserver.login(p4superpasswd, new LoginOptions());

      IUser anotherUser = superserver.getUser(p4testuser);
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
