/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 login -p'. The -p flag displays the ticket, but does not store it on
 * the client machine.
 */
@Jobs({ "job047563" })
@TestId("Dev112_LoginTest")
public class LoginTest extends P4JavaRshTestCase {

	static String defaultTicketFile = null;
	private static Properties serverProps;

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LoginAnotherUserTest.class.getSimpleName());

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	    try {
	        defaultTicketFile = Paths.get(System.getProperty("user.dir"), File.separator, ".p4tickets").toString();
            serverProps = new Properties();
            serverProps.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
    		setupServer(p4d.getRSHURL(),userName, password, true, serverProps);
    	} catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
         }
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

		try {
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

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
}
