/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
public class DiskSpaceTest extends P4JavaRshTestCase {

	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", DiskSpaceTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getSuperConnection(p4d.getRSHURL());
			assertNotNull(server);
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
