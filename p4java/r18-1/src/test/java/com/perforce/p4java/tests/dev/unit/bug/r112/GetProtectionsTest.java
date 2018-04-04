/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for Perforce protections functionality. Requires super user login.
 */
@Jobs({ "job049974" })
@TestId("Dev112_GetProtectionsTest")
public class GetProtectionsTest extends P4JavaTestCase {

	IOptionsServer server = null;

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
			// Requires super user
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(server);

			// Connect to the server.
			server.connect();

            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                	server.setCharsetName("utf8");
                }
            }
			
			// Set the server user
			server.setUserName("p4jtestdummy");

			// Login
			//server.login("p4jtestsuper");
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
	 * Get default protections from a new server.
	 */
	@Test
	public void testGetDefaultProtections() {
		List<IProtectionEntry> protectsList = null;

		try {
			protectsList = server.getProtectionEntries(false, null, null, null, null);
			assertNotNull(protectsList);
			assertTrue(protectsList.size() > 0);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
