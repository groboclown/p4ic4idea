package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

import java.util.List;

import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test getting user groups with '-g | -u | -o name' options.
 */

@Jobs({"job059617"})
@TestId("Dev132_GetUserGroupTest")
public class GetUserGroupTest extends P4JavaRshTestCase {

  @ClassRule
  public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetUserGroupTest.class.getSimpleName());

  private IOptionsServer superserver = null;
  private String superUserGroupName = "p4jtestsuper";
  private UserGroup userGroup;
  private String superUser = "testsuperuser";
  private String user = "testuser";

  @Before
  public void setUp() {
    try {
      setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
      client = getClient(server);
      userGroup = createSuperUserGroup(server, superUserGroupName, superUser, user);
      superserver = getSuperConnection(p4d.getRSHURL());
      IClient superClient = getDefaultClient(superserver);
      assertThat(superClient, notNullValue());
      superserver.setCurrentClient(superClient);
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getLocalizedMessage());
    }
  }

  @After
  public void tearDown() {
    afterEach(server, superserver);
  }

  /*
   * Test getting user groups with '-g | -u | -o name' options.
   */
  @Test
  public void testGetUserGroups() throws Exception {

    // p4 groups -o p4jtestsuper
    List<IUserGroup> userGroups = server.getUserGroups(superUser, new GetUserGroupsOptions().setOwnerName(true));
    assertThat("null user group list", userGroups, notNullValue());
    assertThat("too few user groups in list", userGroups.size() >= 1, is(true));

    // p4 groups -u p4jtestuser
    userGroups = server.getUserGroups(user, new GetUserGroupsOptions().setUserName(true));
    assertThat("null user group list", userGroups, notNullValue());
    assertThat("too few user groups in list", userGroups.size() >= 1);

    // p4 groups -g p4users
    userGroups = server.getUserGroups(superUserGroupName, new GetUserGroupsOptions().setGroupName(true));
    assertThat("null user group list", userGroups, notNullValue());
    assertThat(userGroups.size() == 0, is(true));

  }

}
