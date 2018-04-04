package com.perforce.p4java.tests.dev.unit.bug.r123;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test auth tickets handling while connected to a Perforce replica.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job062513"})
@TestId("Dev123_ReplicaAuthTicketsTest")
public class ReplicaAuthTicketsTest extends P4JavaTestCase {
  private IOptionsServer server = null;

  @BeforeEach
  public void setUp() throws Exception {
    // initialization code (before each test).
    Properties props = new Properties();

    // Tell the server to use memory to store auth tickets
    props.put("useAuthMemoryStore", "true");

    // Connect to a replica server
    server = ServerFactory.getOptionsServer(P4JTEST_REPLICA_SERVER_URL_DEFAULT, props);

    assertThat(server, notNullValue());

    // Register callback
    server.registerCallback(createCommandCallback());
    server.connect();
    setUtf8CharsetIfServerSupportUnicode(server);
    // Set the server user
    server.setUserName(getSuperUserName());

    // Login using the normal method
    server.login(getSuperUserPassword(), new LoginOptions());

    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
      endServerSession(server);
    }
  }

  /**
   * Test get changelists (read) while connected to a Perforce replica.
   */
  @Test
  public void testGetChangelists() throws Exception {
    Map<String, Object>[] result = server.execMapCmd(
        CmdSpec.INFO.toString(),
        new String[]{},
        null);
    assertThat(result, notNullValue());
    assertTrue(result.length > 0);

    // Expire the ticket by logging out
    server.logout();

    // Login using the normal method
    server.login(getSuperUserName(), new LoginOptions());

    result = server.execMapCmd(
        CmdSpec.CHANGES.toString(),
        new String[]{"-m2"},
        null);
    assertThat(result, notNullValue());
    assertTrue(result.length > 0);
  }

  /**
   * Test create/update/delete user (write) while connected to a Perforce replica.
   */
  @Test
  public void testCreateUpdateDeleteUser() throws Exception {
    final int maxTries = 5;
    IUser user = null;
    final String email = "p4jtest@invalid.invalid";
    final String fullName = "CreateUpdateDeleteUserTest test user";
    final String password = "password";
    final String jobView1 = "type=bug & ^status=closed";
    final String jobView2 = "priority<=b description=gui";

    String userName = null;
    String opResultStr;
    // Try to ensure it's not already in use:
    for (int i = maxTries; i > 0; i--) {
      userName = getRandomName(false, "user");
      assertThat(userName, notNullValue());
      user = server.getUser(userName);
      if (isNull(user)) {
        break;
      }
    }

    assertThat("Can't find unique user name after " + maxTries + " tries", user, nullValue());
    user = new User(userName, email, fullName, null, null, password, jobView1, null);
    opResultStr = server.createUser(user, new UpdateUserOptions().setForceUpdate(true));
    assertThat(opResultStr, notNullValue());
    assertThat(opResultStr, is("User " + userName + " saved."));    // This could break sometime...

    user = server.getUser(userName);
    assertThat(user, notNullValue());
    assertThat(user.getLoginName(), is(userName));
    assertThat(user.getEmail(), is(email));
    assertThat(user.getFullName(), is(fullName));
    assertThat(user.getJobView(), is(jobView1));
    assertThat(user.getUpdate(), notNullValue());

    user.setJobView(jobView2);
    server.updateUser(user, new UpdateUserOptions(true));
    assertThat(opResultStr, notNullValue());
    assertThat(opResultStr, is("User " + userName + " saved."));    // This could break sometime...
    user = server.getUser(userName);
    assertThat(opResultStr, notNullValue());
    assertThat(user.getLoginName(), is(userName));
    assertThat(user.getEmail(), is(email));
    assertThat(user.getFullName(), is(fullName));
    assertThat(user.getJobView(), is(jobView2));
    assertThat(user.getUpdate(), notNullValue());

    opResultStr = server.deleteUser(userName, new UpdateUserOptions(true));
    assertThat(opResultStr, notNullValue());
    assertThat(opResultStr, is("User " + userName + " deleted."));    // This could break sometime...
  }
}
