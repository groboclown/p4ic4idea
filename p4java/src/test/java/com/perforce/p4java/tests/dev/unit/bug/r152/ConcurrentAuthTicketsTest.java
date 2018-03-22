package com.perforce.p4java.tests.dev.unit.bug.r152;

import com.perforce.p4java.Log;
import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.server.AuthTicket;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test save auth tickets with multiple threads.
 */
@Jobs({"job083624"})
@TestId("Dev152_ConcurrentAuthTicketsTest")
public class ConcurrentAuthTicketsTest extends P4JavaRshTestCase {
    /**
     * The files helper.
     */
    private static SymbolicLinkHelper filesHelper;

    /**
     * Before all.
     */
    @BeforeClass
    public static void beforeAll() {
        filesHelper = OSUtils.isWindows()
                ? new WindowsRpcSystemFileCommandsHelper() : new RpcSystemFileCommandsHelper();
    }

    private class AuthTicketsWriter implements Runnable {
        private final String user;
        private final String address;
        private final String value;
        private final String ticketsFilePath;
        private int totalSuccessSaveTicket = 0;

        AuthTicketsWriter(final String user, final String address, final String value,
                          final String ticketsFilePath) {

            this.user = user;
            this.address = address;
            this.value = value;
            this.ticketsFilePath = ticketsFilePath;
        }

        public void run() {
            try {
                AuthTicketsHelper.saveTicket(user, address, value, ticketsFilePath);
                totalSuccessSaveTicket++;
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.info("-----" + Thread.currentThread().getName() + " writing is complete!\r\n\r\n");
        }

        public int getTotalSuccessSaveTicket() {
            return totalSuccessSaveTicket;
        }
    }

    private class AuthTicketsReader implements Runnable {
        private final String ticketsFilePath;

        AuthTicketsReader(final String ticketsFilePath) {
            this.ticketsFilePath = ticketsFilePath;
        }

        public void run() {
            try {
                Thread.sleep(100);
                AuthTicket[] tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
                for (AuthTicket ticket : tickets) {
                    debugPrint(ticket.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.info("-----" + Thread.currentThread().getName() + " reading is complete!\r\n\r\n");
        }
    }

    /**
     * Test save auth tickets with multiple threads.
     */
    @Test
    public void testSaveAuthTicketsConcurrently() throws Exception {
        String address = "server:1666";
        String value = "ABCDEF1231234";
        String user = "bruno2";

        String ticketsFilePath = System.getProperty("user.dir");
        assertThat(ticketsFilePath, notNullValue());
        ticketsFilePath += File.separator + "realticketsfile4" + System.currentTimeMillis();

        List<AuthTicketsWriter> ticketsWriters = newArrayList();
        try {
            // Run concurrent reads and writes
            int x=0;
            ExecutorService executor = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 20; i++) {
                String addr = address + i;
                String val = value + i;
                String usr = user + i;
                
                Runnable task;
                if ((i % 2) == 0) {
                    task = new AuthTicketsWriter(usr, addr, val, ticketsFilePath);
                    ticketsWriters.add((AuthTicketsWriter) task);
                } else {
                    task = new AuthTicketsReader(ticketsFilePath);
                }
                
                executor.execute(task);
            }

            executor.shutdown();

            while (!executor.isTerminated()) {
                // System.out.println("Threads are still running..." +
                // executor.toString());
            }

            System.out.println("Finished all threads");

            // Check the number of tickets in the file
            AuthTicket[] tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
            assertThat(tickets, notNullValue());

            // If some thread after try no more than 'max lockTry', it will give
            // up
            int numTickets = ticketsWriters.stream()
                    .mapToInt(AuthTicketsWriter::getTotalSuccessSaveTicket).sum();
            assertThat(tickets.length, is(numTickets));
        } finally {
            filesHelper.setWritable(ticketsFilePath, true);
            boolean deleted = Files.deleteIfExists(Paths.get(ticketsFilePath + ".lck"));
            assertThat(deleted, is(false));
            deleted = Files.deleteIfExists(Paths.get(ticketsFilePath));
            assertThat(deleted, is(true));
        }
    }
}
