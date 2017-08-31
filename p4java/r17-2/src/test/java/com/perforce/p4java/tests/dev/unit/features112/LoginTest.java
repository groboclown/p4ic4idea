/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
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
 * Test 'p4 login -p'. The -p flag displays the ticket, but does not store it on
 * the client machine.
 */
@Jobs({ "job047563" })
@TestId("Dev112_LoginTest")
public class LoginTest extends P4JavaTestCase {

	IOptionsServer server = null;
	String serverMessage = null;
	static String defaultTicketFile = null;
	private static Properties serverProps;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
		defaultTicketFile = Paths.get(System.getProperty("user.dir"),
				File.separator, ".p4tickets").toString();
		serverProps = new Properties();
		serverProps.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
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
			this.endServerSession(server);
		}
	}

	/**
	 * Test 'p4 login -p'. The -p flag displays the ticket, but does not store
	 * it on the client machine.
	 */
	@Test
	public void testLoginShowAuthTicketOnly() {

		String user = "p4jtestuser";
		String password = "p4jtestuser";

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, serverProps);
			assertNotNull(server);

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

			// Set the server user
			server.setUserName(user);

			// Login using the normal method (the auth ticket will be written to
			// file)
			server.login(password, new LoginOptions());

			// Get the auth ticket after the login
			String authTicket = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			// We should have a ticket
			assertNotNull(authTicket);

			// Logout will remove the ticket from file
			server.logout();

			// Get the auth tickets after the logout
			String authTicket2 = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			// It should be null
			assertNull(authTicket2);

			// Login only display the ticket and not storing the ticket to file
			StringBuffer authTicket3 = new StringBuffer();
			server.login(password, authTicket3, new LoginOptions().setDontWriteTicket(true));
			assertNotNull(authTicket3);

			// The original ticket and this ticket should be the same
			assertEquals(authTicket, authTicket3.toString());

			// Get the auth tickets after the login
			String authTicket4 = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			// It should be null since we are using "login -p"
			assertNull(authTicket4);

			try {
				// Login with the wrong password
				server.login("wrong123456789", authTicket3,
						new LoginOptions());
			} catch (P4JavaException e) {
				assertTrue(e.getLocalizedMessage()
						.contains("Password invalid."));
			}

			// Restore the ticket
			// Login using the normal method (the auth ticket will be written to
			// file)
			server.login(password, new LoginOptions());

			// Get the auth tickets after the login
			String authTicket5 = AuthTicketsHelper.getTicketValue(server
					.getUserName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);
			assertNotNull(authTicket5);

			// The original ticket and this ticket should be the same
			assertEquals(authTicket, authTicket5);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
