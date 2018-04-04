/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test setting and retrieving the auth tickets file.
 */
@Jobs({ "job064162" })
@TestId("Dev131_TicketsFileTest")
public class TicketsFileTest extends P4JavaTestCase {

	IOptionsServer server = null;
	String serverMessage = null;
	String ticketFile = null;

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
		ticketFile = System.getProperty("user.dir") + File.separator
				+ testId + getRandomInt() + ".p4tickets";
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
		File file = new File(ticketFile);
		file.delete();
	}

	/**
	 * Test setting and retrieving the auth tickets file.
	 */
	@Test
	public void testTicketsFile() {

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(server);

			assertFalse(ticketFile.contentEquals(server.getTicketsFilePath()));
			
			server.setTicketsFilePath(ticketFile);
			assertTrue(ticketFile.contentEquals(server.getTicketsFilePath()));
			
			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});

			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set a normal user
			server.setUserName(this.userName);

			// Login the normal user			
			server.login(this.password, new LoginOptions());

			String authTicket = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					ticketFile);

			assertNotNull(authTicket);
			assertEquals(server.getAuthTicket(), authTicket);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
