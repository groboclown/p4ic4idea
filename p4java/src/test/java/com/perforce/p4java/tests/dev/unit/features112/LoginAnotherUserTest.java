/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 login [-h<host>] user'.
 */
@Jobs({ "job046826" })
@TestId("Dev112_LoginAnotherUserTests")
public class LoginAnotherUserTest extends P4JavaRshTestCase {

	static String defaultTicketFile = null;
	private static Properties serverProps;

	IUser anotherUser = null;
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", LoginAnotherUserTest.class.getSimpleName());

	/**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            defaultTicketFile = Paths.get(System.getProperty("user.dir"), File.separator, ".p4tickets").toString();
            serverProps = new Properties();
            serverProps.put(PropertyDefs.TICKET_PATH_KEY_SHORT_FORM, defaultTicketFile);
            setupServer(p4d.getRSHURL(),superUserName, superUserPassword, false, serverProps);
            createUser(server, "p4jtestuser2", "p4jtestuser2");
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
	 * Test login another user: 'p4 login [-h<host>] user'.
	 */
	@Test
	public void testLoginAnotherUser() {
		try {
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

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} 
	}
}
