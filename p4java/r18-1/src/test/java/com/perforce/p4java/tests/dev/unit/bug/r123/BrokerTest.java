package com.perforce.p4java.tests.dev.unit.bug.r123;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.Properties;

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
 * Test broker
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job059311"})
@TestId("Dev123_PasswordTest")
public class BrokerTest extends P4JavaTestCase {

  private IOptionsServer server = null;

  @BeforeEach
  public void setUp() throws Exception {
    // P4Broker URL
    String serverUrl = "p4jrpcnts://eng-p4java-vm.perforce.com:50121";
    Properties props = new Properties();
    props.put("useAuthMemoryStore", "1");
    server = getServer(serverUrl,
        props,
        getUserName(),
        getPassword());
    assertThat(server, notNullValue());
    IClient client = server.getClient("p4TestUserWS20112");
    assertThat(client, notNullValue());
    server.setCurrentClient(client);
    // Register callback
    server.registerCallback(createCommandCallback());
  }

  @AfterEach
  public void tearDown() {
    if (nonNull(server)) {
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
