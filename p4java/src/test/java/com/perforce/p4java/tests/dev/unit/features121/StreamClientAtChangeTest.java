/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.server.SwitchClientViewOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'AtChange' field with dynamically generated back-in-time stream client:
 * 'p4 client -f -s -S //p4java_stream/dev@43866'
 * <p>
 * 'p4 client -o'
 */
@Jobs({ "job052514" })
@TestId("Dev112_StreamClientAtChangeTest")
public class StreamClientAtChangeTest extends P4JavaRshTestCase {

	IOptionsServer superServer = null;
	IClient client = null;
	String message = null;
	private String streamsDepotName = "p4java_stream";
	private String streamDepth = "//" + streamsDepotName + "/1";
	private String testStreamMain = "//p4java_stream/main";
	private String testStreamDev = "//p4java_stream/dev";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", StreamClientAtChangeTest.class.getSimpleName());

	/**
	 * 
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
			client = createClient(server, "StreamClientAtChangeTestClient");
			assertNotNull(client);
			server.setCurrentClient(client);
			createStreamsDepot(streamsDepotName, server, streamDepth);
			IStream stream = Stream.newStream(server, testStreamMain, IStreamSummary.Type.MAINLINE.toString(), null, null, null, null, null, null, null);
			String retVal = server.createStream(stream);
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + testStreamMain + " saved.");
			IStream childStream = Stream.newStream(server, testStreamDev, IStreamSummary.Type.DEVELOPMENT.toString(), testStreamMain, null, null, null, null, null, null);
			retVal = server.createStream(childStream);
			assertNotNull(retVal);
			assertEquals(retVal, "Stream " + testStreamDev + " saved.");
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
	 * 'p4 client -f -s -S //p4java_stream/dev@43866'
	 * <p>
	 * 'p4 client -o'
	 */
	@Test
	public void testStreamClientAtChange() {

		int randNum = getRandomInt();
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
			message = server.switchStreamView(testStreamDev, testClient.getName(),
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
			message = server.switchStreamView(testStreamDev + "@" + atChange,
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
			    superServer = getServerAsSuper(p4d.getRSHURL());
				assertNotNull(superServer);
				message = superServer.deleteClient(testClient.getName(), true);
				assertNotNull(message);
			} catch (Exception ignore) {
				// Nothing much we can do here...
			}
		}
	}
}
