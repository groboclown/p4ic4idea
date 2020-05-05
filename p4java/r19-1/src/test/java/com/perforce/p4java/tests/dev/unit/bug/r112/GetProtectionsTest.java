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
import org.junit.ClassRule;
import org.junit.Test;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features112.RevisionHistoryTest;

/**
 * Test for Perforce protections functionality. Requires super user login.
 */
@Jobs({ "job049974" })
@TestId("Dev112_GetProtectionsTest")
public class GetProtectionsTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetProtectionsTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
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
