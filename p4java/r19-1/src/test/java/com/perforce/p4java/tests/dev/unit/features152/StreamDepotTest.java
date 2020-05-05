/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features152;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
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
 * Test create stream depot with stream depth.
 */
@Jobs({ "job083907" })
@TestId("Dev152_StreamDepotTest")
public class StreamDepotTest extends P4JavaRshTestCase {

	@ClassRule
	public static UnicodeServerRule p4d = new UnicodeServerRule("r16.1", StreamDepotTest.class.getSimpleName());

	IOptionsServer superserver = null;
	IClient superclient = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, props);
			superserver = getSuperConnection(p4d.getRSHURL());
			assertNotNull(superserver);
			superclient = getDefaultClient(superserver);
			assertNotNull(superclient);
			superserver.setCurrentClient(superclient);
		} catch ( Exception e) {
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
		if (superserver != null) {
			this.endServerSession(superserver);
		}
	}

	/**
	 * Test create stream depot with stream depth.
	 */
	@Test
	public void testCreateStreamDepot() {
		final String depotName = this.getRandomName(false, "test-stream-depot");
		final String streamDepth = "//" + depotName + "/1";
		final String expectedDeletionResultString = "Depot " + depotName + " deleted.";
		IDepot depot = null;

		try {
			depot = createStreamsDepot(depotName, superserver, streamDepth);
			IDepot newDepot = superserver.getDepot(depotName);
			assertNotNull("null depot returned from getDepot method", newDepot);
			assertEquals("depot address mismatch", depot.getAddress(), newDepot.getAddress());
			assertEquals("depot name mismatch", depot.getName(), newDepot.getName());
			assertEquals("depot type mismatch", depot.getDepotType(), newDepot.getDepotType());
			assertEquals("depot description mismatch", depot.getDescription(), newDepot.getDescription());
			assertEquals("depot map mismatch", depot.getMap(), newDepot.getMap());
			assertEquals("depot owner mismatch", depot.getOwnerName(), newDepot.getOwnerName());
			assertEquals("depot suffix mismatch", depot.getSuffix(), newDepot.getSuffix());
			assertEquals("stream depth mismatch", depot.getStreamDepth(), newDepot.getStreamDepth());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (depot != null && superserver != null) {
				try {
					String resultStr = superserver.deleteDepot(depotName);
					assertNotNull("null result string returned by deleteDepot", resultStr);
					assertEquals(expectedDeletionResultString, resultStr);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
