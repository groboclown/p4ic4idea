/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test automatic charset configuration when talking to an Unicode enabled Perforce server.
 */
@Jobs({ "job072273" })
@TestId("Dev141_AutoCharsetUnicodeServerTest")
public class AutoCharsetUnicodeServerTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", AutoCharsetUnicodeServerTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			Properties properties = new Properties();
			properties.put("relaxCmdNameChecks", "true");
			setupServer(p4d.getRSHURL(), userName, password, true, properties);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	/**
	 * Test automatic charset configuration when talking to an Unicode enabled Perforce server.
	 */
	@Test
	public void testAutoCharset() {
		// Unicode enabled Perforce server
		try {
			// Set the charset before connecting
			server.setCharsetName("iso8859-1");
			
			// Connect to the server.
			server.connect();

			// Check the charset after connecting
			assertNotNull(server.getCharsetName());
			assertEquals(server.getCharsetName(), "iso8859-1");
			System.out.println(server.getCharsetName());

			server.setUserName(this.getSuperUserName());
			server.login(this.getSuperUserPassword(), new LoginOptions());
			
			IClient testClient = getDefaultClient(server);
			assertNotNull(testClient);
			
			// Disconnect
			server.disconnect();

			// Set charset to "no charset"
			server.setCharsetName(null);
			
			// The charset should be null
			assertNull(server.getCharsetName());

			// Connect again
			server.connect();
			
			// The charset should be automatically set
			assertNotNull(server.getCharsetName());
			System.out.println(server.getCharsetName());
			
			// Set the server user
			server.setUserName(this.getSuperUserName());

			// Login using the normal method
			server.login(this.getSuperUserPassword(), new LoginOptions());

			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
