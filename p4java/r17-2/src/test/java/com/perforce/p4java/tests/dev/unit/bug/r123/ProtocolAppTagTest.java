package com.perforce.p4java.tests.dev.unit.bug.r123;


import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test protocol 'app' tag.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job060798", "job061402"})
@TestId("Dev123_ProtocolAppTagTest")
public class ProtocolAppTagTest extends P4JavaTestCase {
  private final Properties serverProps = new Properties();

  private IOptionsServer server = null;

  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
      endServerSession(server);
    }
  }

  /**
   * Test protocol 'app' tag.
   */
  @Test
  public void testProtocolAppTag() throws Exception {
    String serverUri = P4JTEST_REPLICA_SERVER_URL_DEFAULT;

    // Set the protocol 'app' tag value to 'commons-1.0'
    serverProps.put("applicationName", "commons-1.0");

    server = ServerFactory.getOptionsServer(serverUri, serverProps);
    assertThat(server, notNullValue());
    // Register callback
    server.registerCallback(createCommandCallback());

    // Connect to the server.
    server.connect();
    setUtf8CharsetIfServerSupportUnicode(server);

    // Set the server user
    server.setUserName(getSuperUserName());

    // Login using the normal method
    server.login(superUserPassword);
    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

    IServerInfo serverInfo = server.getServerInfo();
    assertThat(serverInfo, notNullValue());
  }
}
