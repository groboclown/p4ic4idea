/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test in-memory IPv6 tickets handling.
 */
@Jobs({ "job061060", "job061048" })
@TestId("Dev131_IPv6InMemoryAuthTicketsTest")
@Ignore("Until we have an ipv6 compliant network")
public class IPv6InMemoryAuthTicketsTest extends P4JavaTestCase {

	Properties serverProps = new Properties();
	
	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;

	IOptionsServer superServer = null;
	IClient superClient = null;
	String superServerMessage = null;

	IUser newUser = null;

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

		// Tell the server to use memory to store auth tickets
		serverProps.put("useAuthMemoryStore", "true");
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
	 * Test in-memory IPv6 server - login
	 */
	@Test
	public void testInMemoryIPv6ServerLogin() {

		String plainIPv6Uri = "p4java://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1602";

		try {
			server = ServerFactory.getOptionsServer(plainIPv6Uri, serverProps);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});

			// Connect to the server.
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
			
			// Set the user to the server
			server.setUserName("mruan");

			// Login the user
			server.login("mruan", new LoginOptions());

			client = server.getClient("mruan-mac");
			assertNotNull(client);
			server.setCurrentClient(client);

			// Check server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
