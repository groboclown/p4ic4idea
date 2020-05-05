/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * Test setting of the user's auth ticket when calling
 * Server.setUserName(String userName).
 */
@Jobs({ "job050343" })
@TestId("Dev123_UserAuthTicketTest")
public class UserAuthTicketTest extends P4JavaTestCase {

	IOptionsServer server = null;
	String serverMessage = null;
	String defaultTicketFile = null;

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
		defaultTicketFile = System.getProperty("user.dir") + File.separator
				+ ".p4tickets";
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
	}

	/**
	 * Test setting of the user's auth ticket when calling
	 * Server.setUserName(String userName).
	 */
	@Test
	public void testUserAuthTicket() {

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(server);
			server.setTicketsFilePath(defaultTicketFile);

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
			server.login(this.password, new LoginOptions());

			String authTicket = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			assertNotNull(authTicket);
			assertEquals(server.getAuthTicket(), authTicket);

			// Set a super user
			server.setUserName(this.superUserName);

			server.login(this.superUserPassword, new LoginOptions());

			String authTicket2 = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			assertNotNull(authTicket2);
			assertEquals(server.getAuthTicket(), authTicket2);
			
			// Now set a normal user again
			server.setUserName(this.userName);
			assertNotNull(server.getAuthTicket());
			assertNotNull(server.getAuthTicket(server.getUserName()));

			// The server auth ticket should equal to the normal user's
			assertEquals(server.getAuthTicket(), authTicket);
			
			// Logout the normal user			
			server.logout();
			assertEquals(null, server.getAuthTicket());
			assertEquals(null, server.getAuthTicket(server.getUserName()));
			
			// Set a super user
			server.setUserName(this.superUserName);
			assertNotNull(server.getAuthTicket());
			assertNotNull(server.getAuthTicket(server.getUserName()));

			// Logout the super user			
			server.logout();
			assertEquals(null, server.getAuthTicket());
			assertEquals(null, server.getAuthTicket(server.getUserName()));
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
