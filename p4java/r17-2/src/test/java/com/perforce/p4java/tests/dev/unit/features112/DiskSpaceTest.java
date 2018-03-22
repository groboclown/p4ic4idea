/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 diskspace' command. Shows summary information about the current
 * availability of disk space on the server.
 * 
 * If no arguments are specified, disk space information for all relevant file
 * systems is displayed; otherwise the output is restricted to the named
 * filesystem(s).
 */
@Jobs({ "job046668" })
@TestId("Dev112_DiskSpaceTest")
public class DiskSpaceTest extends P4JavaTestCase {

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
	 * Shows summary information about the current availability of disk space on
	 * the server.
	 */
	@Test
	public void testDiskSpace() {

		try {
			List<IDiskSpace> diskSpaceList = server.getDiskSpace(null);
			assertNotNull(diskSpaceList);

			boolean foundRoot = false;
			boolean foundJournal = false;
			boolean foundLog = false;
			boolean foundTemp = false;
			boolean foundDepot = false;

			for (IDiskSpace diskSpace : diskSpaceList) {
				if (diskSpace != null) {
					if (diskSpace.getLocation().contentEquals("P4ROOT")) {
						foundRoot = true;
					} else if (diskSpace.getLocation().contentEquals(
							"P4JOURNAL")) {
						foundJournal = true;
					} else if (diskSpace.getLocation().contentEquals("P4LOG")) {
						foundLog = true;
					} else if (diskSpace.getLocation().contentEquals("TEMP")) {
						foundTemp = true;
					} else { // depot name
						foundDepot = true;
					}
				}
			}

			assertTrue(foundRoot);
			assertTrue(foundJournal);
			assertTrue(foundLog);
			assertTrue(foundTemp);
			assertTrue(foundDepot);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Shows disk space info restricted to the named filesystem(s).
	 */
	@Test
	public void testDiskSpaceNamedFilesystems() {

		try {
			List<IDiskSpace> diskSpaceList = server.getDiskSpace(Arrays.asList(new String[] {"P4ROOT", "P4LOG"}));
			assertNotNull(diskSpaceList);

			boolean foundRoot = false;
			boolean foundJournal = false;
			boolean foundLog = false;
			boolean foundTemp = false;
			boolean foundDepot = false;

			for (IDiskSpace diskSpace : diskSpaceList) {
				if (diskSpace != null) {
					if (diskSpace.getLocation().contentEquals("P4ROOT")) {
						foundRoot = true;
					} else if (diskSpace.getLocation().contentEquals(
							"P4JOURNAL")) {
						foundJournal = true;
					} else if (diskSpace.getLocation().contentEquals("P4LOG")) {
						foundLog = true;
					} else if (diskSpace.getLocation().contentEquals("TEMP")) {
						foundTemp = true;
					} else { // depot name
						foundDepot = true;
					}
				}
			}

			assertTrue(foundRoot);
			assertFalse(foundJournal);
			assertTrue(foundLog);
			assertFalse(foundTemp);
			assertFalse(foundDepot);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
