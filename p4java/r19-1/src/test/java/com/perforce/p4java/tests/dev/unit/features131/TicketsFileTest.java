/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test setting and retrieving the auth tickets file.
 */
@Jobs({ "job064162" })
@TestId("Dev131_TicketsFileTest")
public class TicketsFileTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", TicketsFileTest.class.getSimpleName());

	String ticketFile = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			ticketFile = System.getProperty("user.dir") + File.separator + testId + getRandomInt() + ".p4tickets";
			setupServer(p4d.getRSHURL(), userName, password, true, props);
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
		File file = new File(ticketFile);
		file.delete();
	}

	/**
	 * Test setting and retrieving the auth tickets file.
	 */
	@Test
	public void testTicketsFile() {

		try {
//			server.logout();
			assertFalse(ticketFile.contentEquals(server.getTicketsFilePath()));
			
			server.setTicketsFilePath(ticketFile);
			assertTrue(ticketFile.contentEquals(server.getTicketsFilePath()));

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
			
		} catch (P4JavaException | IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
