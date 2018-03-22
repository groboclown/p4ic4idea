/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'AtChange' field with dynamically generated back-in-time stream client:
 * 'p4 client -f -s -S //p4java_stream/dev@43866'
 * <p>
 * 'p4 client -o'
 */
@Jobs({ "job052514" })
@TestId("Dev112_StreamClientAtChangeTest")
public class StreamClientAtChangeTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IOptionsServer superServer = null;
	IClient client = null;
	String message = null;
	IChangelist changelist = null;

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
			client = server.getClient("p4TestUserWS20112");
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
	 * 'p4 client -f -s -S //p4java_stream/dev@43866'
	 * <p>
	 * 'p4 client -o'
	 */
	@Test
	public void testStreamClientAtChange() {

		int randNum = getRandomInt();
		String testStream = "//p4java_stream/dev";
		int atChange = 43866;

		IClient testClient = null;

		try {
			// Create a test client
			testClient = new Client();
			testClient.setName("testClient" + randNum);
			testClient.setOwnerName(client.getOwnerName());
			testClient.setDescription(testClient.getName() + " description.");
			testClient.setRoot(client.getRoot());
			ClientView clientView3 = new ClientView();
			String mapping3 = "//depot/101Bugs/... //" + testClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping3 = new ClientViewMapping(0,
					mapping3);
			clientView3.addEntry(clientViewMapping3);

			testClient.setClientView(clientView3);
			testClient.setServer(server);
			server.setCurrentClient(testClient);

			// Create the classic client
			message = server.createClient(testClient);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient.getName()
					+ " saved."));

			// Validate the created client is classic (non stream)
			testClient = server.getClient(testClient.getName());
			assertNotNull(testClient);
			assertFalse(testClient.isStream());

			// Set the test client to the server
			server.setCurrentClient(testClient);

			// Switch the test client's to a stream view
			message = server.switchStreamView(testStream, testClient.getName(),
					new SwitchClientViewOptions().setForce(true));
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient.getName()
					+ " switched."));
			
			// Get the test client
			testClient = server.getClient(testClient.getName());
			assertNotNull(testClient);
			assertNotNull(testClient.getClientView());
			assertNotNull(testClient.getClientView().getSize() > 0);

			// It should not have an atChange
			assertEquals(IChangelist.UNKNOWN, testClient.getStreamAtChange());
			
			// Switch the test client's to a stream view with at change
			message = server.switchStreamView(testStream + "@" + atChange,
					null, new SwitchClientViewOptions().setForce(true));
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient.getName()
					+ " switched."));

			// Get the test client
			testClient = server.getClient(testClient.getName());
			assertNotNull(testClient);
			assertNotNull(testClient.getClientView());
			assertNotNull(testClient.getClientView().getSize() > 0);

			// It should have a valid atChange
			assertEquals(atChange, testClient.getStreamAtChange());
			

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				superServer = getServerAsSuper();
				assertNotNull(superServer);
				message = superServer.deleteClient(testClient.getName(), true);
				assertNotNull(message);
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
