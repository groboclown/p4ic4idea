/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.server.AuthTicket;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test in-memory concurrent IPv6 auth tickets handling.
 */
@Jobs({ "job061060", "job061048" })
@TestId("Dev131_IPv6InMemoryAuthTicketsHelperConcurrencyTest")
@Ignore("Until we have an ipv6 compliant network")
public class IPv6InMemoryAuthTicketsHelperConcurrencyTest extends P4JavaTestCase {

	class AuthTicketsWriter implements Runnable {

		private String user = null;
		private String address = null;
		private String value = null;
		private String ticketsFilePath = null;

		AuthTicketsWriter(String user, String address, String value,
				String ticketsFilePath) {
			this.user = user;
			this.address = address;
			this.value = value;
			this.ticketsFilePath = ticketsFilePath;
		}

		public void run() {

			try {
				AuthTicketsHelper.saveTicket(this.user, this.address,
						this.value, this.ticketsFilePath);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	class AuthTicketsReader implements Runnable {

		private String ticketsFilePath = null;

		AuthTicketsReader(String ticketsFilePath) {
			this.ticketsFilePath = ticketsFilePath;
		}

		public void run() {

			try {
				AuthTicket[] tickets = AuthTicketsHelper
						.getTickets(this.ticketsFilePath);
				for (AuthTicket ticket : tickets) {
				    debugPrint(ticket.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
	}

	/**
	 * Test in-memory tickets - saving auth tickets
	 */
	@Test
	public void testInMemorySaveAuthTicketsConcurrently() {
		String address = "[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1666";
		String value = "ABCDEF123123";
		String user = "bruno";

		String ticketsFilePath = null;

		try {
			// Create the first ticket
			AuthTicketsHelper.saveTicket(user, address, value, ticketsFilePath);

			// Run concurrent reads and writes
			ExecutorService executor = Executors.newFixedThreadPool(10);
			for (int i = 0; i < 25; i++) {
				String addr = address + i;
				String val = value + i;
				String usr = user + i;

				Runnable task = null;

				if ((i % 2) == 0) {
					task = new AuthTicketsWriter(usr, addr, val,
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

			// Check the size of the in-memory map - 501
			AuthTicket[] tickets = AuthTicketsHelper.getTickets(ticketsFilePath);
			assertNotNull(tickets);
			assertTrue(tickets.length > 10);
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

}
