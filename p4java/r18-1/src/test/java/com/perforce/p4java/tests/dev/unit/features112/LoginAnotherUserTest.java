/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IUser;
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
 * Test 'p4 login [-h<host>] user'.
 */
@Jobs({ "job046826" })
@TestId("Dev112_LoginAnotherUserTests")
public class LoginAnotherUserTest extends P4JavaTestCase {

	IOptionsServer server = null;
	String serverMessage = null;
	static String defaultTicketFile = null;
	private static Properties serverProps;

	IUser anotherUser = null;

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
	 * Test login another user: 'p4 login [-h<host>] user'.
	 */
	@Test
	public void testLoginAnotherUser() {

		String user = "p4jtestsuper";
		String password = "p4jtestsuper";

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

			// Set the super user to the server
			server.setUserName(user);

			// Login the super user
			server.login(password, new LoginOptions());

			anotherUser = server.getUser("p4jtestuser2");

			assertNotNull(anotherUser);

			StringBuffer authTicketFromMemory = new StringBuffer();
			
			// Login the specified "p4jtestuser2" user
			// The ticket should be written to the file
			// Also, the same ticket should be written to the StringBuffer
			server.login(anotherUser, authTicketFromMemory, new LoginOptions().setHost(InetAddress
					.getLocalHost().getHostName()));

			assertNotNull(authTicketFromMemory);
			assertTrue(authTicketFromMemory.length() > 0);
			
			// Get the auth ticket after the login of the specified "p4jtestuser2" user
			String authTicketFromFile = AuthTicketsHelper.getTicketValue(anotherUser
					.getLoginName(), server.getServerInfo().getServerAddress(),
					defaultTicketFile);

			// We should have a ticket for the specified "p4jtestuser2" user
			assertNotNull(authTicketFromFile);

			// The ticket in the StringBuffer should be the same as the ticket in the file
			assertEquals(authTicketFromMemory.toString(), authTicketFromFile);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
