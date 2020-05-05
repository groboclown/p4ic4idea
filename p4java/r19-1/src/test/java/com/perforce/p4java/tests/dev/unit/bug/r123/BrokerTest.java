package com.perforce.p4java.tests.dev.unit.bug.r123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test broker
 */

@Jobs({"job059311"})
@TestId("Dev123_PasswordTest")
public class BrokerTest extends P4JavaRshTestCase {
    
  @ClassRule
  public static SimpleServerRule p4d = new SimpleServerRule("r16.1", BrokerTest.class.getSimpleName());

  @Before
  public void setUp() throws Exception {
    // P4Broker URL
    Properties props = new Properties();
    props.put("useAuthMemoryStore", "1");
    setupServer(p4d.getRSHURL(), userName, password, true, props);
    IClient client = server.getClient("p4TestUserWS20112");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
  }

  @After
  public void tearDown() {
    if (server != null) {
      endServerSession(server);
    }
  }

  /**
   * Test broker with "-o" command
   */
  @Test
  public void testBroker() throws Exception {
    // do a counter then a user -o
    String[] args1 = {"-u", "changes"};
    String[] args2 = {"-o", "tester1"};

    server.setUserName(getUserName());

    for (int i = 0; i < 10; i++) {
//    	System.out.println("Connect "+i);
//    	Thread.sleep(100);
      server.connect();
//  	System.out.println("Login "+i);
// 	Thread.sleep(100);
      server.login(getPassword());
      server.execMapCmd("counter", args1, null);
      server.logout();
//  	System.out.println("Login2 "+i);
//  	Thread.sleep(100);
      server.login(getPassword());
      server.execMapCmd("user", args2, null);
//  	System.out.println("Disconnect "+i);
//  	Thread.sleep(100);
      server.logout();
      server.disconnect();
    }
  }
}
