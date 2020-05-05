/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test IPv6 server address connections, both plain and SSL.
 */
@Jobs({ "job061060", "job061048" })
@TestId("Dev131_IPv6AddressConnectionTest")
@Ignore("Until we have an ipv6 compliant network")
public class IPv6AddressConnectionTest extends P4JavaTestCase {

	IOptionsServer server = null;
	String serverMessage = null;

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
	 * Test plain connection to IPv6 server address
	 */
	
	@Test
	public void testIPv6ServerPlainConnection() {

		String plainIPv6Uri = "p4java://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1602";

		try {
			server = ServerFactory.getOptionsServer(plainIPv6Uri, null);
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

			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Test SSL connection to IPv6 server address
	 */
	@Test
	public void testIPv6ServerSSLConnection() {

		String sslIPv6URI = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";

		try {
			server = ServerFactory.getOptionsServer(sslIPv6URI, null);
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

			// Run remove trust first
			String result = server.removeTrust();
			assertNotNull(result);

			// Should get 'new connection' trust exception
			try {
				// Connect to the server.
				server.connect();
				if (server.isConnected()) {
					if (server.supportsUnicode()) {
						server.setCharsetName("utf8");
					}
				}
			} catch (P4JavaException e) {
				assertNotNull(e);
				assertTrue(e.getCause() instanceof TrustException);
				assertTrue(((TrustException) e.getCause()).getType() == TrustException.Type.NEW_CONNECTION);
				assertNotNull(((TrustException) e.getCause()).getFingerprint());
				
				// Add the key (new connection)
				try {
					result = server.addTrust(((TrustException) e.getCause()).getFingerprint());
					assertNotNull(result);
					assertTrue(result.contains("first attempt to connect"));
					assertTrue(result.contains("Added trust for Perforce server"));
				} catch (P4JavaException e2) {
					assertNotNull(e2);
				}
			}
			
			// Set the user to the server
			server.setUserName("mruan");

			// Login the user
			server.login("mruan", new LoginOptions());

			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
