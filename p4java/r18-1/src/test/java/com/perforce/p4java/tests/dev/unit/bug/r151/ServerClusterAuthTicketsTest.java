/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r151;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test auth tickets in a Perforce server cluster environment.
 */
@Jobs({ "job078145" })
@TestId("Dev151_ServerClusterAuthTicketsTest")
public class ServerClusterAuthTicketsTest extends P4JavaTestCase {

	static final String faketicket = "88888888888ZZZZZZZZZZ88888888888";
	
	static final String p4duri = "p4java://llam-ds1.das.perforce.com:1667";
	static final String p4testuser = "p4jtestuser";
	static final String p4testpasswd = "p4jtestuser";
	static final String p4testclient = "p4TestUserWS20112";

	IOptionsServer server = null;
	IClient client = null;

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
		if (server != null) {
			endServerSession(server);
		}
	}

	/**
	 * Test auth tickets connecting to a Perforce server cluster.
	 */
	@Test
	@Ignore("Tries to connect to llam-ds1.das.perforce.com that does not exist")
	public void testAuthTickets() {

		try {
			server = ServerFactory.getOptionsServer(p4duri, null);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
				}

				public void receivedServerInfoLine(int key, String infoLine) {
				}

				public void receivedServerErrorLine(int key, String errorLine) {
				}

				public void issuingServerCommand(int key, String command) {
				}

				public void completedServerCommand(int key, long millisecsTaken) {
				}
			});
			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the test user
			server.setUserName(p4testuser);

			// Login the user
			// The ticket is set in memory and save to ticket file
			server.login(p4testpasswd, new LoginOptions());

			// Check if the ticket is set in memory
			assertNotNull(server.getAuthTicket());
			assertTrue(server.getAuthTicket().trim().length() > 5);
			
			// Save the good ticket for later usage
			String goodticket = server.getAuthTicket();
			
			// Get and set the test client
			client = server.getClient(p4testclient);
			assertNotNull(client);
			server.setCurrentClient(client);

			// Run the 'changes' command
			List<IChangelistSummary> changelistSummaries = server
					.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(5));
			assertEquals(5, changelistSummaries.size());
			
			// Set a fake auth ticket
			server.setAuthTicket(faketicket);

			// Check if the fake ticket is set in memory
			assertNotNull(server.getAuthTicket());
			assertEquals(faketicket, server.getAuthTicket());
			
			// Command should fail
			P4JavaException exception = null;
			try {
				changelistSummaries = server
						.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
			} catch (P4JavaException e) {
				exception = e;
			}
			assertNotNull(exception);

			// Set the good auth ticket
			server.setAuthTicket(goodticket);
			
			changelistSummaries = server
					.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
			assertEquals(10, changelistSummaries.size());
			
			// Logout
			server.logout();

			// The ticket should be gone
			assertNull(server.getAuthTicket());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

}
