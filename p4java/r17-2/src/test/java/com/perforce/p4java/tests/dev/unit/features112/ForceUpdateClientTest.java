/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import com.perforce.p4java.client.IClientSummary.IClientOptions;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test force update of locked client
 */
@Jobs({ "job044623" })
@TestId("Dev112_ForceUpdateClientTest")
public class ForceUpdateClientTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient testClient = null;
	String callbackMessage = null;
	String message = null;

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
	 * Switch an existing client spec's view.
	 */
	@Test
	public void testSwitchClientView() {
		int randNum = getRandomInt();

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					callbackMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					callbackMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					callbackMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					callbackMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					callbackMessage = String.valueOf(millisecsTaken);
				}
			});
			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the server user to a super user ("p4jtestsuper")
			server.setUserName("p4jtestsuper");

			IClient p4TestUserWS = server.getClient("p4TestUserWS");

			testClient = new Client();
			IClientOptions clientOptions = new ClientOptions();
			clientOptions.setLocked(true);
			testClient.setOptions(clientOptions);
			testClient.setName("testClient" + randNum);
			testClient.setOwnerName(p4TestUserWS.getOwnerName());
			testClient.setDescription("Test description.");
			testClient.setRoot(p4TestUserWS.getRoot());
			ClientView clientView = new ClientView();
			String mapping1 = "//depot/101Bugs/... //" + testClient.getName()
					+ "/101Bugs/...";
			ClientViewMapping clientViewMapping = new ClientViewMapping(0,
					mapping1);
			clientView.addEntry(clientViewMapping);
			testClient.setClientView(clientView);
			testClient.setServer(server);
			server.setCurrentClient(testClient);
			message = server.createClient(testClient);
			assertNotNull(message);
			assertTrue(message.contentEquals("Client " + testClient.getName()
					+ " saved."));

			// Make sure the client is locked
			testClient = server.getClient(testClient.getName());
			assertTrue(testClient.getOptions().isLocked() == true);

			// Change the 'locked' status to false
			clientOptions = testClient.getOptions();
			clientOptions.setLocked(false);
			testClient.setOptions(clientOptions);
			// Change the description
			testClient.setDescription("I have changed!!!");

			// Since the client is locked we will get an error if we use the
			// standard update method.
			try {
				testClient.update();
			} catch (P4JavaException e) {
				assertTrue(e.getLocalizedMessage().contains(
						"Locked client '" + testClient.getName()
								+ "' owned by '" + testClient.getOwnerName()
								+ "'; use -f to force update."));
			}

			testClient.refresh();
			// The fields should have the old values
			assertTrue(testClient.getOptions().isLocked() == true);
			assertTrue(testClient.getDescription().contentEquals("Test description."));

			// Change the 'locked' status to false
			clientOptions = testClient.getOptions();
			clientOptions.setLocked(false);
			testClient.setOptions(clientOptions);
			// Change the description
			testClient.setDescription("I have changed!!!");
			
			// This time it will succeed since we will use the force update
			// to do the update.
			try {
				testClient.update(true);
			} catch (P4JavaException e) {
				fail("Unable to update client: " + e.getLocalizedMessage());
			}

			testClient.refresh();
			// The fields should have the new values
			assertTrue(testClient.getOptions().isLocked() == false);
			assertTrue(testClient.getDescription().contentEquals("I have changed!!!"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			try {
				// Delete the test clients
				server = getServerAsSuper();
				if (server != null) {
					if (testClient != null) {
						server.deleteClient(testClient.getName(), true);
					}
				}
			} catch (P4JavaException e) {
				// Can't do much here...
			} catch (URISyntaxException e) {
				// Can't do much here...
			}
		}
	}
}
