package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test getting user groups with '-g | -u | -o name' options.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job059617"})
@TestId("Dev132_GetUserGroupTest")
public class GetUserGroupTest extends P4JavaTestCase {

  private IOptionsServer server = null;
  private IOptionsServer superserver = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

    superserver = getServerAsSuper();
    assertThat(superserver, notNullValue());
    IClient superClient = getDefaultClient(superserver);
    assertThat(superClient, notNullValue());
    superserver.setCurrentClient(superClient);
  }

  @AfterEach
  public void tearDown() {
    afterEach(server, superserver);
  }

  /*
   * Test getting user groups with '-g | -u | -o name' options.
   */
  @Test
  public void testGetUserGroups() throws Exception {

    // p4 groups -o p4jtestsuper
    List<IUserGroup> userGroups = server.getUserGroups("p4jtestsuper", new GetUserGroupsOptions().setOwnerName(true));
    assertThat("null user grpoup list", userGroups, notNullValue());
    assertThat("too few user groups in list", userGroups.size() >= 1, is(true));

    // p4 groups -u p4jtestuser
    userGroups = server.getUserGroups("p4jtestuser", new GetUserGroupsOptions().setUserName(true));
    assertThat("null user grpoup list", userGroups, notNullValue());
    assertThat("too few user groups in list", userGroups.size() >= 1);

    // p4 groups -g p4users
    userGroups = server.getUserGroups("p4users", new GetUserGroupsOptions().setGroupName(true));
    assertThat("null user grpoup list", userGroups, notNullValue());
    assertThat(userGroups.size() == 0, is(true));

  }

}
