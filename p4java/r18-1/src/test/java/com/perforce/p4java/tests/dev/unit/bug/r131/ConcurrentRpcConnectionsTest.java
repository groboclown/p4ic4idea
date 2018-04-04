package com.perforce.p4java.tests.dev.unit.bug.r131;

import static com.perforce.p4java.common.base.StringHelper.format;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AbstractAuthHelper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
/**
 * Test concurrent RPC connections with the same user and frequent login/logout.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job064015"})
@TestId("Dev131_ConcurrentRpcConnectionsTest")
public class ConcurrentRpcConnectionsTest extends P4JavaTestCase {
  private class GetChangelistsRunner implements Runnable {
    private final IOptionsServer server;
    GetChangelistsRunner(final IOptionsServer optsServer) {
      this.server = optsServer;
    }

    public void run() {
      try {
        server.login("p4jtestuser", new LoginOptions());
        IClient client = server.getClient("p4TestUserWS20112");
        assertThat(client, notNullValue());
        server.setCurrentClient(client);
        List<IChangelistSummary> changelistSummaries = server.getChangelists(
            null,
            new GetChangelistsOptions().setMaxMostRecent(10));
        server.logout();
        assertThat(changelistSummaries.size(), is(10));
        endServerSession(server);
      } catch (Exception e) {
        String threadName = Thread.currentThread().getName();
        String localizedMessage = e.getLocalizedMessage();
        String ignoreExceptionMessage = format(
            "Error creating new auth lock file \"%s.lck\" after retries: %s",
            server.getTicketsFilePath(),
            AbstractAuthHelper.DEFAULT_LOCK_TRY);
        if (!contains(localizedMessage, ignoreExceptionMessage)) {
          fail("Unexpected exception: " + localizedMessage);
        } else {
          Log.info("--> %s : %s", threadName, ignoreExceptionMessage);
        }
      }
    }
  }

  /**
   * Test concurrent RPC connections with the same user and frequent login/logout.
   */
  @Test
  public void testConcurrentRpcConnections() throws Exception {
    // Run concurrent reads and writes
    ExecutorService executor = Executors.newFixedThreadPool(50);
    for (int i = 0; i < 100; i++) {
      Runnable task = new GetChangelistsRunner(getIOptionsServer());
      executor.execute(task);
    }

    executor.shutdown();

    while (!executor.isTerminated()) {
      //System.out.println("Threads are still running...");
    }

    System.out.println("Finished all threads");
  }

  private IOptionsServer getIOptionsServer() throws Exception {
    IOptionsServer server;
    IClient client;

    Properties props = new Properties();
    String defaultTicketFile = System.getProperty("user.dir") + File.separator + ".p4tickets";
    props.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
    props.put("com.perforce.p4java.rpc.socketPoolSize", 100);

    server = ServerFactory.getOptionsServer("p4jrpc://eng-p4java-vm.perforce.com:20131", props);
    assertThat(server, notNullValue());

    // Register callback
    server.registerCallback(createCommandCallback());
    // Connect to the server.
    server.connect();
    setUtf8CharsetIfServerSupportUnicode(server);

    // Set the server user
    server.setUserName(getUserName());
    return server;
  }
}
