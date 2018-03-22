package com.perforce.p4java.tests.dev.unit.bug.r131;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.impl.mapbased.rpc.helper.RpcUserAuthCounter;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test concurrent user login/logout counter.
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job064015"})
@TestId("Dev131_ConcurrentRpcUserAuthCounterTest")
public class ConcurrentRpcUserAuthCounterTest extends P4JavaTestCase {

  private RpcUserAuthCounter authCounter = new RpcUserAuthCounter();

  private class Incrementer implements Runnable {
    private String authPrefix = null;

    Incrementer(String authPrefix) {
      this.authPrefix = authPrefix;
    }

    public void run() {
      authCounter.incrementAndGet(authPrefix);
    }
  }

  private class Decrementer implements Runnable {
    private String authPrefix = null;

    Decrementer(String authPrefix) {
      this.authPrefix = authPrefix;
    }

    public void run() {
      authCounter.decrementAndGet(authPrefix);
    }
  }

  /**
   * Test incrementing and decrementing the auth user counter.
   */
  @Test
  public void testConcurrentUserAuthCounter() {
    String authPrefix = "server:1666=testuser";
    String authPrefix2 = "server:1666=testuser2";

    // Run concurrent reads and writes
    ExecutorService executor = Executors.newFixedThreadPool(500);
    for (int i = 0; i < 1000; i++) {

      Runnable task = null;

      if ((i % 2) == 0) {
        task = new Incrementer(authPrefix);
      } else {
        task = new Incrementer(authPrefix2);
      }

      executor.execute(task);
    }

    for (int i = 0; i < 500; i++) {

      Runnable task;

      if ((i % 2) == 0) {
        task = new Decrementer(authPrefix);
      } else {
        task = new Decrementer(authPrefix2);
      }

      executor.execute(task);
    }

    executor.shutdown();

    while (!executor.isTerminated()) {
      //System.out.println("Threads are still running...");
    }

    System.out.println("Finished all threads");

    assertThat(authCounter.getCount(authPrefix), is(250));
    assertThat(authCounter.getCount(authPrefix2), is(250));

    System.out.println("RPC User Auth Counter...");
    System.out.println(authCounter.toString());
  }

}
