package com.perforce.p4java.tests.dev.unit.bug.r131;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.server.AuthTicket;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test auth tickets file
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job065305"})
@TestId("Dev131_AuthTicketsFileTest")
public class AuthTicketsFileTest extends P4JavaTestCase {
  /**
   * Test auth tickets file
   */
  @Test
  public void testAuthTicketsFile() throws Exception {
    int randNo = getRandomInt();
    String ticketsFilePath = System.getProperty("user.dir");
    ticketsFilePath += File.separator + "realticketsfile" + randNo;
    String address = "server:1666";
    String value = "ABCDEF123123";
    String user = "bruno";

    try {
      // Get the tickets from a non-existing auth tickets file
      AuthTicket[] tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
      assertThat(tickets, notNullValue());
      assertThat(tickets.length, is(0));

      // Save some tickets to the non-existing auth tickets file
      // The auth tickets file will be created automatically
      // Write 20 tickets
      for (int i = 0; i < 20; i++) {
        address += i;
        value += i;
        user += i;
        AuthTicketsHelper.saveTicket(user, address, value, ticketsFilePath);
      }

      // Get the tickets from the newly created and populated auth tickets file
      tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
      assertThat(tickets, notNullValue());
      assertThat(tickets.length, is(20));
    } finally {
      File ticketsFile = new File(ticketsFilePath);
      boolean deleted = ticketsFile.delete();
      assertThat(deleted, is(true));
    }
  }
}
