package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test setting password and clearing secret key.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job065140"})
@TestId("Dev123_PasswordSecretKeyTest")
public class PasswordSecretKeyTest extends P4JavaTestCase {
  private IOptionsServer server = null;
  private IOptionsServer superServer = null;
  private IUser newUser = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer(
        getServerUrlString(),
        props, getUserName(),
        getPassword());
    assertThat(server, notNullValue());
    IClient client = server.getClient("p4TestUserWS20112");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
    // Register callback
    server.registerCallback(createCommandCallback());

    superServer = getServer(
        getServerUrlString(),
        props,
        getSuperUserName(),
        getSuperUserPassword());
    assertThat(superServer, notNullValue());
    IClient superClient = superServer.getClient("p4TestSuperWS20112");
    superServer.setCurrentClient(superClient);
    // Register callback
    superServer.registerCallback(createCommandCallback());
  }

  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
      endServerSession(server);
    }
    if (nonNull(superServer)) {
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
      if (nonNull(superServer) && nonNull(newUser)) {
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
    Map<String, Object> map = newHashMap();
    map.put("oldPassword", oldPwd);
    map.put("newPassword", newPwd);
    map.put("newPassword2", newPwd);
    Map<String, Object>[] retMap = server.execMapCmd("passwd", new String[0], map);
    System.out.println(retMap[0].get("code0"));
  }
}
