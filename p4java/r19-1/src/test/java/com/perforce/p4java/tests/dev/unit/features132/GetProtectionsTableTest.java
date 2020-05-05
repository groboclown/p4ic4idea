package com.perforce.p4java.tests.dev.unit.features132;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.InputStream;

import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * Test 'p4 protect -o'
 */

@Jobs({"job062361"})
@TestId("Dev132_GetProtectionsTableTest")
public class GetProtectionsTableTest extends P4JavaRshTestCase {

  @ClassRule
  public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", GetProtectionsTableTest.class.getSimpleName());

  private IOptionsServer superServer;

  @Before
  public void setUp() throws Exception {
    setupServer(p4d.getRSHURL(), userName, password, true, null);
    superServer = getSuperConnection(p4d.getRSHURL());
  }

  @After
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
