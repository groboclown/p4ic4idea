package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static com.perforce.p4java.tests.ServerMessageMatcher.containsText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Test password with high-ascii characters.
 */

@Jobs({"job065872"})
@TestId("Dev131_HighASCIIPasswordTest")
public class HighASCIIPasswordTest extends P4JavaRshTestCase {

  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", HighASCIIPasswordTest.class.getSimpleName());

  private IOptionsServer superServer = null;
  private IClient superClient = null;
  private IUser newUser = null;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    IClient client = server.getClient("p4TestUserWS20112");
    assertThat(client,notNullValue());
    server.setCurrentClient(client);
    
    superServer = getSuperConnection(p4d.getRSHURL());
    superClient = superServer.getClient("p4TestSuperWS20112");
    superServer.setCurrentClient(superClient);
  }

  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
    if (superServer != null) {
      endServerSession(superServer);
    }
  }

  /**
   * Test change password with high-ascii characters.
   */
  @Test
  public void testChangePassword() throws Exception{
    try {
      // Create a new user, password not set.
      int randNum = getRandomInt();
      String newUserName = "testuser" + randNum;
      String email = newUserName + "@localhost.localdomain";
      String fullName = "New P4Java Test User " + randNum;
      newUser = new User(newUserName, email, fullName, null, null, null,
          null, UserType.STANDARD, null);
      String message = superServer.createUser(newUser, true);
      assertThat(message, notNullValue());
      assertThat(message.contentEquals("User " + newUserName + " saved."), is(true));

      newUser = server.getUser(newUserName);
      assertThat(newUser, notNullValue());

      // Set the user
      server.setUserName(newUserName);

      // Add the user in the p4users group
      IUserGroup userGroup = superServer.getUserGroup("p4users");
      assertThat(userGroup, notNullValue());
      userGroup.getUsers().add(newUserName);
      message = superServer.updateUserGroup(userGroup, new UpdateUserGroupOptions());
      assertThat(message, notNullValue());
      assertThat(message.contentEquals("Group p4users updated."), is(true));

      // Login with a empty password.
      try {
        server.login("");
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("'login' not necessary, no password set for this user."));
      }

      // Change password
      String password1 = "\u53f0\u5317 T\u00e1ib\u011bi rick\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00e4\u00c0\u00c0\u00c0\u00c0\u00c0\u00c0\u00c0\u00c0\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9\u00f9";
      message = server.changePassword(null, password1, null);
      assertThat(message, notNullValue());
      assertThat(message, containsString("Password updated."));

      List<IDepot> depots = null;

      // Should get an error message
      try {
         server.getDepots();
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("Perforce password (P4PASSWD) invalid or unset."));
      }

      // Login using a partial password
      // Should get an error message
      try {
        server.login("rick");
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("Password invalid."));
      }

      // Login with the new password
      server.login(password1);
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsText("User " + newUserName + " logged in."));

      // Should get a list of depots
      depots = server.getDepots();
      assertThat(depots, notNullValue());
      assertThat(depots.size() > 0, is(true));

      // Set another password
      String password2 = "abc123";
      message = server.changePassword(password1, password2, "");
      assertThat(message, notNullValue());

      // Login again
      server.login(password2);
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsText("User " + newUserName + " logged in."));

      // Delete the password
      String password3 = null;
      message = server.changePassword(password2, password3, "");
      assertThat(message, notNullValue());
      assertThat(message, containsString("Password deleted."));

      // Login again
      server.login(password3);
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsText("'login' not necessary, no password set for this user."));

      // Use the super user to change the password to something else
      String password4 = "abcd1234";
      message = superServer.changePassword(null, password4, newUserName);
      assertThat(message, notNullValue());
      assertThat(message, containsString("Password updated."));

      // Login using the old password
      // Should get an error message
      try {
        server.login(password2);
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("Password invalid."));
      }

      // Login using the new password
      server.login(password4);
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsText("User " + newUserName + " logged in."));

      // Get a list of depots
      depots = server.getDepots();
      assertThat(depots, notNullValue());
      assertThat(depots.size() > 0, is(true));
    } finally {
      try {
        if (superServer != null) {
          if (newUser != null) {
            String message = superServer.deleteUser(
                newUser.getLoginName(), true);
            assertThat(message, notNullValue());
            // Remove the user in the p4users group
            IUserGroup userGroup = superServer.getUserGroup("p4users");
            assertThat(userGroup, notNullValue());
            for (Iterator<String> it = userGroup.getUsers().iterator(); it.hasNext(); ) {
              String s = it.next();
              if (s.contentEquals(newUser.getLoginName())) {
                it.remove();
              }
            }
            message = superServer.updateUserGroup(userGroup, new UpdateUserGroupOptions());
            assertThat(message, notNullValue());
          }
        }
      } catch (Exception ignore) {
        // Nothing much we can do here...
      }
    }
  }
}