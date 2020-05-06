package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test protocol 'app' tag.
 */

@Jobs({"job060798", "job061402"})
@TestId("Dev123_ProtocolAppTagTest")
public class ProtocolAppTagTest extends P4JavaRshTestCase {
    
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ProtocolAppTagTest.class.getSimpleName());

  private final Properties serverProps = new Properties();
  IClient client = null;
  
  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test protocol 'app' tag.
   */
  @Test
  public void testProtocolAppTag() throws Exception {
    // Set the protocol 'app' tag value to 'commons-1.0'
    serverProps.put("applicationName", "commons-1.0");
    setupServer(p4d.getRSHURL(), userName, password, true, serverProps);
    // Login using the normal method
    client = getClient(server);
    IServerInfo serverInfo = server.getServerInfo();
    assertThat(serverInfo, notNullValue());
  }
}
