/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test get clients with case-insensitive name filter.
 */
@Jobs({ "job046825" })
@TestId("Dev112_GetClientsTest")
public class GetClientsTest extends P4JavaTestCase {

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
			server = getServer();
			assertNotNull(server);
			client = getDefaultClient(server);
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
	 * Test get clients with case-insensitive name filter.
	 */
	@Test
	public void testGetClients() {
		int randNum = getRandomInt();
		String clientName = "Test-client-job046825-" + randNum;

		try {
			// Create a new client with a mixed of lower and upper case letters

			IClient newClient = new Client();
			newClient.setName(clientName);
			newClient.setOwnerName(client.getOwnerName());
			newClient.setDescription(newClient.getName() + " description.");
			newClient.setRoot(client.getRoot());
			ClientView clientView1 = new ClientView();
			String mapping1 = "//depot/112Dev/... //" + newClient.getName()
					+ "/112Dev/...";
			ClientViewMapping clientViewMapping1 = new ClientViewMapping(0,
					mapping1);
			clientView1.addEntry(clientViewMapping1);
			newClient.setClientView(clientView1);

			String message = server.createClient(newClient);
			assertNotNull(message);
			assertEquals("Client " + clientName + " saved.", message);

			// Setting a default case-sensitive name filter with lower case
			List<IClientSummary> clients = server
					.getClients(new GetClientsOptions()
							.setNameFilter("test-client-job046825-*"));
			assertNotNull(clients);

			// Should get an empty list, since the filter is case sensitive
			assertEquals(0, clients.size());

			// Setting a name filter with lower case
			clients = server.getClients(new GetClientsOptions()
					.setCaseInsensitiveNameFilter("test-client-job046825-*"));
			assertNotNull(clients);

			// Should get one in the list, since the filter is case sensitive
			assertEquals(1, clients.size());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test client
				server = getServerAsSuper();
				if (server != null) {
					String message = server.deleteClient(clientName, true);
					assertNotNull(message);
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			} catch (URISyntaxException e) {
				// Can't do much here...
			}
		}
	}
}
