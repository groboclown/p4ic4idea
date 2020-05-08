/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 configure' command for Perforce server 2011.2.
 */
@Jobs({ "job050801" })
@TestId("Dev112_GetServerConfigurationTest")
public class ServerConfigurationTest extends P4JavaRshTestCase {

	IClient client = null;

	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ServerConfigurationTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getSuperConnection(p4d.getRSHURL());
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
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
	 * Test 'p4 configure' command for Perforce server 2011.2.
	 */
	@Test
	public void testServerConfguration() {
		final String SERVER_NAME = "xyz";
		final String CONFIG_NAME = "minClientMessage";
		final String CONFIG_VALUE = "test value "
				+ this.getRandomName(" message ");

		try {
			// Set variable
			String retVal = server.setOrUnsetServerConfigurationValue(SERVER_NAME
					+ "#" + CONFIG_NAME, CONFIG_VALUE);
			assertNotNull(retVal);
			assertEquals("For server '" + SERVER_NAME + "', configuration variable '" + CONFIG_NAME + "' set to '" + CONFIG_VALUE + "'\n", retVal);

			// Unset variable
			retVal = server.setOrUnsetServerConfigurationValue(SERVER_NAME + "#"
					+ CONFIG_NAME, null);
			assertEquals("For server '" + SERVER_NAME + "', configuration variable '" + CONFIG_NAME + "' removed.\n", retVal);

			// Get an error message about the variable did not have a value
			retVal = server.setOrUnsetServerConfigurationValue(SERVER_NAME + "#"
					+ CONFIG_NAME, null);
			assertTrue(retVal.equals("Configuration variable '" + CONFIG_NAME + "' did not have a value.\n") || retVal.equals("")
					// p4ic4idea: it may not have an EOL
					|| retVal.equals("Configuration variable '" + CONFIG_NAME + "' did not have a value.")
			);

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

}
