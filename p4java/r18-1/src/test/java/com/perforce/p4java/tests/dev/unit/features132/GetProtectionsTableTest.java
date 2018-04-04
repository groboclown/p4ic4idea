package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 protect -o'
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job062361"})
@TestId("Dev132_GetProtectionsTableTest")
public class GetProtectionsTableTest extends P4JavaTestCase {

  private IOptionsServer server = null;
  private IOptionsServer superServer = null;

  @BeforeEach
  public void setUp() throws Exception {
    server = getServer();
    assertThat(server, notNullValue());
    IClient client = getDefaultClient(server);
    assertThat(client, notNullValue());
    server.setCurrentClient(client);

    superServer = getServerAsSuper();
    assertThat(superServer, notNullValue());
    IClient superclient = getDefaultClient(superServer);
    assertThat(superclient, notNullValue());
    superServer.setCurrentClient(superclient);
  }

  @AfterEach
  public void tearDown() {
    afterEach(server, superServer);
  }

  /**
   * Test 'p4 protect -o'
   */
  @Test
  public void testGetProtectionsTable() throws Exception {
    try (InputStream is = superServer.getProtectionsTable()) {
      assertThat(is, notNullValue());
      int c;
      while ((c = is.read()) != -1) {
        System.out.print((char) c);
      }
    }
  }
}
