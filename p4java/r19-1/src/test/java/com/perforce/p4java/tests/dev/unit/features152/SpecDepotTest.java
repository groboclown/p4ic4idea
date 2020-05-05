/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features152;

import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IMapEntry;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.MapEntry;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test spec depot with spec maps.
 */
@Jobs({ "job083909" })
@TestId("Dev152_SpecDepotTest")
public class SpecDepotTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", SpecDepotTest.class.getSimpleName());

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			setupServer(p4d.getRSHURL(), superUserName, superUserPassword, true, null);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		if (server != null) {
			this.endServerSession(server);
		}
	}
	
	/**
	 * Test spec depot with spec maps.
	 */
	@Test
	public void testSpecDepot() {

		final String depotName = "spec-test";
		final String suffix = ".p4s";
		final String depotMap = depotName + "/...";
		final String depotDescription = "spec depot for test " + this.testId;
		final String expectedDeletionResultString = "Depot " + depotName + " deleted.";

		ViewMap<IMapEntry> specMap = new ViewMap<IMapEntry>();
		specMap.getEntryList().add(new MapEntry(0, "/spec/..."));
		specMap.getEntryList().add(new MapEntry(1, "-//spec/client/*_tmp*"));
		
		IDepot depot = null;

		try {
			depot = createDepot(depotName, server, DepotType.SPEC, null, depotMap, depotDescription, suffix, null, null, specMap);
			IDepot newDepot = server.getDepot(depotName);
			assertNotNull("null depot returned from getDepot method", newDepot);
			assertEquals("depot address mismatch", depot.getAddress(), newDepot.getAddress());
			assertEquals("depot name mismatch", depot.getName(), newDepot.getName());
			assertEquals("depot type mismatch", depot.getDepotType(), newDepot.getDepotType());
			assertEquals("depot description mismatch", depot.getDescription(), newDepot.getDescription());
			assertEquals("depot map mismatch", depot.getMap(), newDepot.getMap());
			assertEquals("depot owner mismatch", depot.getOwnerName(), newDepot.getOwnerName());
			assertEquals("depot suffix mismatch", depot.getSuffix(), newDepot.getSuffix());
			assertEquals("spec map size mismatch", depot.getSpecMap().getSize(), newDepot.getSpecMap().getSize());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (depot != null && server != null) {
				try {
					String resultStr = server.deleteDepot(depotName);
					assertNotNull("null result string returned by deleteDepot", resultStr);
					assertEquals(expectedDeletionResultString, resultStr);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
