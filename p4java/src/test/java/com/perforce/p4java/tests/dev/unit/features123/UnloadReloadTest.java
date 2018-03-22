/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.option.server.ReloadOptions;
import com.perforce.p4java.option.server.UnloadOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test unload clients and labels and list unloaded clients and labels:
 * 'p4 unload', 'p4 clients -U' and 'p4 labels -U'
 */
@Jobs({ "job056531" })
@TestId("Dev123_UnloadTest")
public class UnloadReloadTest extends P4JavaTestCase {

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
	 * Test create unload depot.
	 */
	//@Test
	public void testCreateUnloadDepot() {

		final String depotName = this.getRandomName(false, "test-unload-depot");
		final String depotMap = depotName + "/...";
		final String depotDescription = "temp depot for test " + this.testId;
		final String expectedCreationResultString = "Depot " + depotName + " saved.";
		final String expectedDeletionResultString = "Depot " + depotName + " deleted.";

		IDepot depot = null;

		try {
			depot = new Depot(
					depotName,
					superserver.getUserName(),
					null,
					depotDescription,
					DepotType.UNLOAD,
					null,
					null,
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

	/**
	 * Test unload and reload client
	 */
	@Test
	public void testUnloadReloadClient() {

		IClient tempClient = null;
		ILabel tempLabel = null;
		
		try {
			// Create temp client
			String tempClientName = "testclient-" + getRandomName(testId);
			tempClient = new Client(
					tempClientName,
                    null,	// accessed
                    null,	// updated
                    testId + " temporary test client",
                    null,
                    getUserName(),
                    getTempDirName() + "/" + testId,
                    ClientLineEnd.LOCAL,
                    null,	// client options
                    null,	// submit options
                    null,	// alt roots
                    server,
                    null
				);
			assertNotNull("Null client", tempClient);
			String resultStr = server.createClient(tempClient);
			assertNotNull(resultStr);
			tempClient = server.getClient(tempClient.getName());
			assertNotNull("couldn't retrieve new client", tempClient);

			// Create temp label
			String tempLabelName = "testlabel-" + getRandomName(testId);
			tempLabel = new Label(
					tempLabelName,
					getUserName(),
					null,	// lastAccess
					null,	// lastUpdate
					"Temporary label created for test " + testId,
					null,	// revisionSpec
					false,	// locked
					new ViewMap<ILabelMapping>()
				);
			assertNotNull("Null label", tempLabel);
			resultStr = server.createLabel(tempLabel);
			assertNotNull(resultStr);
			tempLabel = server.getLabel(tempLabel.getName());
			assertNotNull("couldn't retrieve new label", tempLabel);

			// unload client and label
			resultStr = server.unload(new UnloadOptions().setClient(tempClient.getName()).setLabel(tempLabel.getName()));
			assertNotNull(resultStr);

			// Check temp client has been unloaded
			boolean found = false;
			List<IClientSummary> unloadedClients = server.getClients(new GetClientsOptions().setUnloaded(true));
			assertNotNull(unloadedClients);
			for (IClientSummary cs : unloadedClients) {
				if (cs.getName().equalsIgnoreCase(tempClientName)) {
					found = true;
					break;
				}
			}
			assertTrue(found);

			// Check temp label has been unloaded
			found = false;
			List<ILabelSummary> unloadedLabels = server.getLabels(null, new GetLabelsOptions().setUnloaded(true));
			assertNotNull(unloadedLabels);
			for (ILabelSummary ls : unloadedLabels) {
				if (ls.getName().equalsIgnoreCase(tempLabelName)) {
					found = true;
					break;
				}
			}
			assertTrue(found);
			
			// reload client and label
			resultStr = server.reload(new ReloadOptions().setClient(tempClient.getName()).setLabel(tempLabel.getName()));
			assertNotNull(resultStr);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (tempClient != null) {
					try {
						String resultStr = server.deleteClient(tempClient.getName(), false);
						assertNotNull(resultStr);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
				if (tempLabel != null) {
					try {
						String resultStr = server.deleteLabel(tempLabel.getName(), false);
						assertNotNull(resultStr);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
