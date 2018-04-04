/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 client -S' and 'p4 clients -S" commands:
 * <p>
 * Create stream clients
 * <p>
 * Get a list of stream client summaries
 * <p>
 * Get a full stream client
 * <p>
 * Switch a classic client's view to a stream view
 * <p>
 */
@Jobs({ "job046690", "job046693" })
@TestId("Dev112_StreamClientsTest")
public class StreamClientsTest extends P4JavaTestCase {

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
		if (superServer != null) {
			this.endServerSession(superServer);
		}
	}

	/**
	 * Test create stream clients.
	 */
	@Test
	public void testStreamClients() {
		int randNum = getRandomInt();
		String streamName = "testmain" + randNum;
		String streamPath = "//p4java_stream/" + streamName;
		String streamName2 = "testdev" + randNum;
		String streamPath2 = "//p4java_stream/" + streamName2;

		IClient streamClient = null;
		IClient streamClient2 = null;
		IClient classicClient = null;

		try {
			// Create a mainline stream
			IStream stream = Stream.newStream(server, streamPath, "mainline",
					null, null, null, null, null, null, null);
			String retVal = server.createStream(stream);
			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + streamPath + " saved.");

			// Create a developement stream
			String options = "locked ownersubmit notoparent nofromparent";
			String[] viewPaths = new String[] {
					"share ...",
					"share core/GetOpenedFilesTest/src/gnu/getopt/...",
					"isolate readonly/sync/p4cmd/*",
					"import core/GetOpenedFilesTest/bin/gnu/... //p4java_stream/main/core/GetOpenedFilesTest/bin/gnu/...",
					"exclude core/GetOpenedFilesTest/src/com/perforce/p4cmd/..." };
			String[] remappedPaths = new String[] {
					"core/GetOpenedFilesTest/... core/GetOpenedFilesTest/src/...",
					"core/GetOpenedFilesTest/src/... core/GetOpenedFilesTest/src/gnu/..." };
			String[] ignoredPaths = new String[] { "/temp", "/temp/...",
					".tmp", ".class" };
			IStream stream2 = Stream.newStream(server, streamPath2,
					"development", streamPath, "Development stream",
					"The development stream of " + streamPath, options,
					viewPaths, remappedPaths, ignoredPaths);
			retVal = server.createStream(stream2);
			// The stream should be created
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + streamPath2 + " saved.");

			// Create a stream client dedicated to the development stream
			streamClient = new Client();
			streamClient.setName("testStreamClient1" + randNum);
			streamClient.setOwnerName(client.getOwnerName());
			streamClient.setDescription(streamClient.getName()
					+ " description.");
			streamClient.setRoot(client.getRoot());
			ClientView clientView1 = new ClientView();
			String mapping1 = "//depot/101Bugs/... //" + streamClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping1 = new ClientViewMapping(0,
					mapping1);
			clientView1.addEntry(clientViewMapping1);

			// Set the stream's path to the client
			streamClient.setStream(stream2.getStream());

			streamClient.setClientView(clientView1);
			streamClient.setServer(server);
			server.setCurrentClient(streamClient);

			// Create the stream client
			message = server.createClient(streamClient);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + streamClient.getName()
					+ " saved."));

			// Validate the created client is a stream client
			streamClient = server.getClient(streamClient.getName());
			assertNotNull(streamClient);
			assertTrue(streamClient.isStream());

			// Create a second stream client dedicated to the development stream
			streamClient2 = new Client();
			streamClient2.setName("testStreamClient2" + randNum);
			streamClient2.setOwnerName(client.getOwnerName());
			streamClient2.setDescription(streamClient2.getName()
					+ " description.");
			streamClient2.setRoot(client.getRoot());
			ClientView clientView2 = new ClientView();
			String mapping2 = "//depot/101Bugs/... //" + streamClient2.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping2 = new ClientViewMapping(0,
					mapping2);
			clientView2.addEntry(clientViewMapping2);

			// Set the stream's path to the client
			streamClient2.setStream(stream2.getStream());

			streamClient2.setClientView(clientView2);
			streamClient2.setServer(server);
			server.setCurrentClient(streamClient2);

			// Create the stream client
			message = server.createClient(streamClient2);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + streamClient2.getName()
					+ " saved."));

			// Validate the created client is a stream client
			streamClient2 = server.getClient(streamClient2.getName());
			assertNotNull(streamClient2);
			assertTrue(streamClient2.isStream());

			// Get a list of all clients
			// There should be more than two clients return
			List<IClientSummary> streamClients = server.getClients(new GetClientsOptions());
			assertNotNull(streamClients);
			assertTrue(streamClients.size() > 2);
			
			// Get a list of stream clients dedicated to streamPath2
			// There should be only two stream clients returned
			streamClients = server.getClients(new GetClientsOptions().setStream(streamPath2));
			assertNotNull(streamClients);
			assertTrue(streamClients.size() == 2);

			// Create a classic client
			classicClient = new Client();
			classicClient.setName("testClassicClient" + randNum);
			classicClient.setOwnerName(client.getOwnerName());
			classicClient.setDescription(classicClient.getName()
					+ " description.");
			classicClient.setRoot(client.getRoot());
			ClientView clientView3 = new ClientView();
			String mapping3 = "//depot/101Bugs/... //" + classicClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping3 = new ClientViewMapping(0,
					mapping3);
			clientView3.addEntry(clientViewMapping3);

			classicClient.setClientView(clientView3);
			classicClient.setServer(server);
			server.setCurrentClient(classicClient);

			// Create the classic client
			message = server.createClient(classicClient);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + classicClient.getName()
					+ " saved."));

			// Validate the created client is classic (non stream)
			classicClient = server.getClient(classicClient.getName());
			assertNotNull(classicClient);
			assertFalse(classicClient.isStream());

			// Switch the classic client's to a stream view
			message = server.switchStreamView(streamPath2, classicClient.getName(), new SwitchClientViewOptions().setForce(true));
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + classicClient.getName() +  " switched."));
			
			// Get a list of stream clients dedicated to streamPath2
			// There should be three stream clients returned
			// Including the classic client, who had turned into a stream client
			streamClients = server.getClients(new GetClientsOptions().setStream(streamPath2));
			assertNotNull(streamClients);
			assertTrue(streamClients.size() == 3);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				superServer = getServerAsSuper();
				assertNotNull(superServer);
				message = superServer.deleteClient(
						streamClient.getName(), true);
				assertNotNull(message);
				message = superServer.deleteClient(
						streamClient2.getName(), true);
				assertNotNull(message);
				message = superServer.deleteClient(
						classicClient.getName(), true);
				assertNotNull(message);
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
