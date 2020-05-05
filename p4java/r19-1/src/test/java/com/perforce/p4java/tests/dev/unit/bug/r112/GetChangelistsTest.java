/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IChangelistSummary.Visibility;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test correctness of changelist visibility (access type) for the
 * server.getChangelists() method.
 */
@Jobs({ "job046271" })
@TestId("Dev112_GetChangelistsTest")
public class GetChangelistsTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetChangelistsTest.class.getSimpleName());

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
	 * Test correctness of changelist visibility (access type) for the
	 * server.getChangelists() method.
	 */
	@Test
	public void testGetChangelists() {

		int randNum = getRandomInt();

		IClient testClient = null;
		String message = null;

		try {
			IClient p4jTestClient = server.getClient("p4TestUserWS");
			// Create a test client and set it to the server
			testClient = new Client();
			testClient.setName("testClient" + randNum);
			testClient.setOwnerName(p4jTestClient.getOwnerName());
			testClient.setDescription(testClient.getName() + " description.");
			testClient.setRoot(p4jTestClient.getRoot());
			ClientView clientView1 = new ClientView();
			String mapping1 = "//depot/101Bugs/... //" + testClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping1 = new ClientViewMapping(0,
					mapping1);
			clientView1.addEntry(clientViewMapping1);
			testClient.setClientView(clientView1);
			testClient.setServer(server);
			server.setCurrentClient(testClient);
			message = server.createClient(testClient);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient.getName()
					+ " saved."));

			// Create a changelist with public visibility
			IChangelist changelist = getNewChangelist(server, testClient,
					"Dev112_GetChangelistsTest public visibility");
			assertNotNull(changelist);
			changelist.setVisibility(Visibility.PUBLIC);
			changelist = testClient.createChangelist(changelist);
			assertNotNull(changelist);
			changelist.refresh();
			assertTrue(changelist.getVisibility() == Visibility.PUBLIC);

			// Create a changelist with restricted visibility
			changelist = getNewChangelist(server, testClient,
					"Dev112_GetChangelistsTest restricted visibility");
			assertNotNull(changelist);
			changelist.setVisibility(Visibility.RESTRICTED);
			changelist = testClient.createChangelist(changelist);
			assertNotNull(changelist);
			changelist.refresh();
			assertTrue(changelist.getVisibility() == Visibility.RESTRICTED);

			// Use getChangelists method to check if changelists have the
			// correct visibility
			List<IChangelistSummary> changelistSummaries = server
					.getChangelists(null, new GetChangelistsOptions().setClientName(testClient.getName()));
			assertTrue(changelistSummaries.size() == 2);
			assertEquals(Visibility.RESTRICTED, changelistSummaries.get(0)
					.getVisibility());
			assertEquals(Visibility.PUBLIC, changelistSummaries.get(1)
					.getVisibility());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test client
				server = getSuperConnection(p4d.getRSHURL());
				if (server != null) {
					if (testClient != null) {
						server.deleteClient(testClient.getName(), true);
					}
				}
			} catch (Exception e) {
				// Can't do much here...
			} 
		}
	}
}
