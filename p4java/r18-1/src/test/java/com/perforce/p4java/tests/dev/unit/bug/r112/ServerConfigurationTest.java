/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 configure' command for Perforce server 2011.2.
 */
@Jobs({ "job050801" })
@TestId("Dev112_GetServerConfigurationTest")
public class ServerConfigurationTest extends P4JavaTestCase {

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
		try {
			server = getServerAsSuper();
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
			assertTrue(retVal.equals("Configuration variable '" + CONFIG_NAME + "' did not have a value.\n") || retVal.equals(""));

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

}
