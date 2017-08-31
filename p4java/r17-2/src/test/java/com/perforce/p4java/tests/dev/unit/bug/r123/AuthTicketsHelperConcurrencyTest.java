package com.perforce.p4java.tests.dev.unit.bug.r123;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.server.AuthTicket;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

@RunWith(JUnitPlatform.class)
@Jobs({"job059813"})
@TestId("Dev123_AuthTicketsHelperConcurrencyTest")
public class AuthTicketsHelperConcurrencyTest extends P4JavaTestCase {
  private class AuthTicketsWriter implements Runnable {
    private final String user;
    private final String address;
    private final String value;
    private final String ticketsFilePath;

    AuthTicketsWriter(
        final String user,
        final String address,
        final String value,
        final String ticketsFilePath) {

      this.user = user;
      this.address = address;
      this.value = value;
      this.ticketsFilePath = ticketsFilePath;
    }

    public void run() {
      try {
        AuthTicketsHelper.saveTicket(
            user,
            address,
            value,
            ticketsFilePath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private class AuthTicketsReader implements Runnable {
    private final String ticketsFilePath;

    AuthTicketsReader(final String ticketsFilePath) {
      this.ticketsFilePath = ticketsFilePath;
    }

    public void run() {
      try {
        AuthTicket[] tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
        for (AuthTicket ticket : tickets) {
          debugPrint(ticket.toString());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Test saving auth tickets
   */
  @Test
  public void testSaveAuthTicketsConcurrently() throws Exception {
    int randNo = getRandomInt();

    String address = "server:1666";
    String value = "ABCDEF123123";
    String user = "bruno";

    String ticketsFilePath = System.getProperty("user.dir");
    assertThat(ticketsFilePath, notNullValue());
    ticketsFilePath += File.separator + "realticketsfile" + randNo;

    try {
      // Create the first tickets file
      AuthTicketsHelper.saveTicket(user, address, value, ticketsFilePath);

      // Run concurrent reads and writes
      ExecutorService executor = Executors.newFixedThreadPool(10);
      for (int i = 0; i < 25; i++) {
        String addr = address + i;
        String val = value + i;
        String usr = user + i;

        Runnable task;

        if ((i % 2) == 0) {
          task = new AuthTicketsWriter(
              usr,
              addr,
              val,
              ticketsFilePath);
        } else {
          task = new AuthTicketsReader(ticketsFilePath);
        }

        executor.execute(task);
      }

      executor.shutdown();

      while (!executor.isTerminated()) {
        System.out.println("Threads are still running...");
        Thread.sleep(2000);
      }

      System.out.println("Finished all threads");

    } finally {
      File ticketsFile = new File(ticketsFilePath);
      boolean deleted = ticketsFile.delete();
      assertThat(deleted, is(true));
    }
  }
}
