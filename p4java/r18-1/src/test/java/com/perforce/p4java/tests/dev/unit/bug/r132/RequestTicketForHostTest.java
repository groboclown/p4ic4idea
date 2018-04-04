package com.perforce.p4java.tests.dev.unit.bug.r132;



import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.bug.r161.SubmitAndSyncUtf8FileTypeTest;

/**
 * Test 'p4 login -h'. Request a ticket that is valid for the specified host.
 */

@Jobs({"job067176"})
@TestId("Dev132_RequestTicketForHostTest")
public class RequestTicketForHostTest extends P4JavaRshTestCase {
  
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", RequestTicketForHostTest.class.getSimpleName());

  @BeforeClass
  public static void beforeAll() throws Exception {
	String defaultTicketFile = System.getProperty("user.dir") + File.separator + ".p4tickets";
	Properties props = new Properties();
	props.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
  	setupServer(p4d.getRSHURL(), null, null, true, null);
  }

  @Before
  public void setUp() {
    String defaultTicketFile = System.getProperty("user.dir") + File.separator + ".p4tickets";
    props.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
  }

  @After
  public void tearDown() {
    afterEach(server);
  }

  /**
   * Test 'p4 login -h'. Request a ticket that is valid for the specified host.
   */
  @Test
  public void testLoginForHost() throws Exception {
    Assert.assertNotNull(server);
    StringBuffer hostTicket = new StringBuffer();
    server.login(password, hostTicket, new LoginOptions().setDontWriteTicket(true).setHost("localhost"));
    Assert.assertNotNull(hostTicket);

    // according changelist #1455646
    Assert.assertEquals(hostTicket.toString(), server.getAuthTicket());
  }
}
