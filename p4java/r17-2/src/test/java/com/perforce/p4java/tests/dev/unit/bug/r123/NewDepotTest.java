package com.perforce.p4java.tests.dev.unit.bug.r123;

import static com.perforce.p4java.common.base.StringHelper.format;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for unable to refresh server connection after creating a new depot.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job057913"})
@TestId("Dev123_NewDepotTest")
public class NewDepotTest extends P4JavaTestCase {
  private static final String TEST_102_SERVER_URL = "p4jrpcnts://eng-p4java-vm.perforce.com:20152";
  private static final String TEST_102_CLIENT_NAME = "p4TestUserWS";
  private IOptionsServer superserver = null;

  @BeforeEach
  public void setUp() throws Exception {
    superserver = getServer(
        TEST_102_SERVER_URL,
        null,
        superUserName,
        superUserPassword);
    assertThat(superserver, notNullValue());
    IClient superClient = superserver.getClient(TEST_102_CLIENT_NAME);
    assertThat(superClient, notNullValue());
    superserver.setCurrentClient(superClient);
  }

  @AfterEach
  public void tearDown() {
    if (nonNull(superserver)) {
      endServerSession(superserver);
    }
  }

  /**
   * Test for unable to refresh server connection after creating a new depot.
   */
  @Test
  public void testNewDepot() throws Exception {
    final String depotName = this.getRandomName(false, "test-new-depot");
    final String depotMap = depotName + "/...";
    final String depotDescription = "temp depot for test " + this.testId;

    IDepot tempDepot = null;
    IClient tempClient = null;

    try {
      tempDepot = new Depot(
          depotName,
          superserver.getUserName(),
          null,
          depotDescription,
          DepotType.LOCAL,
          null,
          null,
          depotMap
      );
      String resultStr = superserver.createDepot(tempDepot);
      assertThat(resultStr, notNullValue());

      IDepot newDepot = superserver.getDepot(depotName);
      assertThat("null depot returned from getDepot method", newDepot, notNullValue());

      // Create a new client with a view mapping of the new depot
      String tempClientName = "testclient-" + getRandomName(testId);
      ClientView clientView = new ClientView();
      String mapping1 = format("//%s/... //%s/...", tempDepot.getName(), tempClientName);
      ClientViewMapping clientViewMapping = new ClientViewMapping(0,
          mapping1);
      clientView.addEntry(clientViewMapping);
      tempClient = new Client(
          tempClientName,
          null,    // accessed
          null,    // updated
          testId + " temporary test client",
          null,
          getUserName(),
          getTempDirName() + "/" + testId,
          ClientLineEnd.LOCAL,
          null,    // client options
          null,    // submit options
          null,    // alt roots
          superserver,
          clientView
      );
      assertThat("Null client", tempClient, notNullValue());

      try {
        resultStr = superserver.createClient(tempClient);
        assertThat(resultStr, notNullValue());
        tempClient = superserver.getClient(tempClient.getName());
        assertThat("couldn't retrieve new client", tempClient, notNullValue());
      } catch (P4JavaException e) {
        assertThat(e, notNullValue());
        String message = e.getMessage();
        assertThat(message, notNullValue());
        assertThat(message, containsString("Error in client specification"));
        assertThat(message, containsString(format("Mapping '//%s/...' is not under", tempDepot.getName())));
      }
    } finally {
      if (nonNull(superserver)) {
        try {
          if (nonNull(tempClient)) {
            String resultStr = superserver.deleteClient(tempClient.getName(), false);
            assertThat(resultStr, notNullValue());
          }

          if (nonNull(tempDepot)) {
            String resultStr = superserver.deleteDepot(depotName);
            assertThat(resultStr, notNullValue());
          }
        } catch (P4JavaException e) {
          // Can't do much here...
        }
      }
    }
  }
}
