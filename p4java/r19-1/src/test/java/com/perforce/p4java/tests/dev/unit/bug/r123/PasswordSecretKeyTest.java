package com.perforce.p4java.tests.dev.unit.bug.r123;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.P4JavaException;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Test setting password and clearing secret key.
 */

@Jobs({"job065140"})
@TestId("Dev123_PasswordSecretKeyTest")
public class PasswordSecretKeyTest extends P4JavaRshTestCase {
  
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", PasswordSecretKeyTest.class.getSimpleName());

  private IOptionsServer superServer = null;
  private IUser newUser = null;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    IClient client = server.getClient("p4TestUserWS20112");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);    
    superServer = getSuperConnection(p4d.getRSHURL());
    IClient superClient = superServer.getClient("p4TestSuperWS20112");
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
   * Test setting password and clearing secret key.
   */
  @Test
  public void testPasswordSecretKey() throws Exception {
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
      message = server.changePassword(null, "simon", null);
      assertThat(message, notNullValue());
      assertThat(message, containsString("Password updated."));

      List<IDepot> depots;

      // Should get an error message
      try {
        server.getDepots();
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("Perforce password (P4PASSWD) invalid or unset."));
      }

      // Login with the new password
      server.login("simon");
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsString("User " + newUserName + " logged in."));

      // Should get a list of depots
      depots = server.getDepots();
      assertThat(depots, notNullValue());
      assertThat(depots.size() > 0, is(true));

      // Change the password
      setPassword("simon", "simon");
      // Delete the password
      setPassword("simon", "");
      // Login with a empty password.
      try {
        server.login("");
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("'login' not necessary, no password set for this user."));
      }
      // Change the password
      setPassword("garbage", "simon");
      // Login with the new password
      server.login("simon");
      assertThat(serverMessage, notNullValue());
      assertThat(serverMessage, containsString("User " + newUserName + " logged in."));

      // Should get a list of depots
      depots = server.getDepots();
      assertThat(depots, notNullValue());
      assertThat(depots.size() > 0, is(true));
    } finally {
      if (superServer != null && newUser != null) {
        String message = superServer.deleteUser(newUser.getLoginName(), true);
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
  }

  private void setPassword(String oldPwd, String newPwd) throws P4JavaException {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("oldPassword", oldPwd);
    map.put("newPassword", newPwd);
    map.put("newPassword2", newPwd);
    Map<String, Object>[] retMap = server.execMapCmd("passwd", new String[0], map);
    System.out.println(retMap[0].get("code0"));
  }
}
