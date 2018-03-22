/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features152;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test create stream depot with stream depth.
 */
@Jobs({ "job083907" })
@TestId("Dev152_StreamDepotTest")
public class StreamDepotTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	IOptionsServer superserver = null;
	IClient superclient = null;

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
			serverUrlString = P4JTEST_UNICODE_SERVER_URL_DEFAULT;
			server = getServer();
			assertNotNull(server);
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);

			superserver = getServerAsSuper();
			assertNotNull(superserver);
			superclient = getDefaultClient(superserver);
			assertNotNull(superclient);
			superserver.setCurrentClient(superclient);

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
		final String depotMap = depotName + "/...";
		final String depotDescription = "temp stream depot for test " + this.testId;
		final String expectedCreationResultString = "Depot " + depotName + " saved.";
		final String expectedDeletionResultString = "Depot " + depotName + " deleted.";

		IDepot depot = null;

		try {
			depot = new Depot(
					depotName,
					superserver.getUserName(),
					null,
					depotDescription,
					DepotType.STREAM,
					null,
					null,
					streamDepth,
					depotMap
				);
			String resultStr = superserver.createDepot(depot);
			assertNotNull("null result string from createDepot()", resultStr);
			assertEquals(expectedCreationResultString, resultStr);

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
