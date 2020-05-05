/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features131;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test in-memory IPv6 fingerprints handling.
 */
@Jobs({ "job061060", "job061048" })
@TestId("Dev131_IPv6InMemoryFingerprintsTest")
@Ignore("Until we have an ipv6 compliant network")
public class IPv6InMemoryFingerprintsTest extends P4JavaTestCase {

	Properties serverProps = new Properties();

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;
	long completedTime = 0;

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
	}

	/**
	 * Test in-memory IPv6 fingerprints - add trust
	 */
	@Test
	public void testInMemoryAddTrust() {

		String result = null;

		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			// Run remove trust first
			result = server.removeTrust();
			assertNotNull(result);

			// Should get 'new connection' trust exception
			try {
				// Connect to the server.
				server.connect();
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

			// Add the key again
			try {
				result = server.addTrust(new TrustOptions());
				assertNotNull(result);
				assertEquals(result, "Trust already established.");
			} catch (P4JavaException e) {
				assertNotNull(e);
			}

			// Add a specific fake fingerprint
			try {
				result = server
						.addTrust("B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2");
				assertNotNull(result);
				assertTrue(result.contains("IDENTIFICATION HAS CHANGED"));
			} catch (P4JavaException e) {
				assertNotNull(e);
			}

			// Add trust WITHOUT 'force' option
			// Should get 'new key' exception
			try {
				result = server.addTrust(new TrustOptions());
			} catch (P4JavaException e) {
				assertNotNull(e);
				assertTrue(e instanceof TrustException);
				assertTrue(((TrustException) e).getType() == TrustException.Type.NEW_KEY);
			}

			// Add trust WITH 'force' and 'autoAccept' options
			try {
				result = server.addTrust(new TrustOptions()
						.setForce(true).setAutoAccept(true));
				assertNotNull(result);
			} catch (P4JavaException e) {
				assertNotNull(e);
			}

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test in-memory fingerprints - remove trust
	 */
	@Test
	public void testInMemoryRemoveTrust() {
		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			// Remove trust
			String removeResult = server.removeTrust();
			assertNotNull(removeResult);
			assertTrue(removeResult.contains("Removed trust for Perforce server"));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test in-memory fingerprints - get fingerprint from Perforce SSL connection
	 */
	@Test
	public void testInMemoryGetTrust() {
		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			// Get fingerprint
			String fingerprint = server.getTrust();
			assertNotNull(fingerprint);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test in-memory fingerprints - get trusts from trust file
	 */
	@Test
	public void testInMemoryGetTrusts() {
		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			// Add a fingerprints
			String fp = "B3:C6:B3:C6:B3:C6:B3:C6:B3:C6:B3:C6:B3:C6:B3:C6:B3:C6:B3:C6";
			try {
				String result = server
						.addTrust(fp);
				assertNotNull(result);
			} catch (P4JavaException e) {
				assertNotNull(e);
			}

			// Get trusts
			List<Fingerprint> fingerprints = server.getTrusts();
			assertNotNull(fingerprints);
			assertTrue(fingerprints.size() > 0);
			
			boolean found = false;
			for(Fingerprint f : fingerprints) {
				if (f != null && f.getFingerprintValue() != null) {
					if (f.getFingerprintValue().equalsIgnoreCase(fp)) {
						found = true;
						break;
					}
				}
			}
			assertTrue(found);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test in-memory fingerprints - trust key is an IP address from trust file
	 */
	@Test
	public void testInMemoryTrustKey() {
		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			// Get fingerprint
			String fingerprint = server.getTrust();
			assertNotNull(fingerprint);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test in-memory fingerprints - connecting to the server.
	 */
	@Test
	public void testInMemoryConnection() {
		try {
			String serverUri = "p4javassl://[fc01:5034:a05:1e:ad94:403f:ae19:1aa9]:1702?socketPoolSize=10&testKey1=testVal1";
			server = ServerFactory.getOptionsServer(serverUri, serverProps);
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
					completedTime = millisecsTaken;
				}
			});

			try {
				// Connect to the server.
				server.connect();
			} catch (P4JavaException e) {
				assertNotNull(e);
				assertTrue(e.getCause() instanceof TrustException);
				if (((TrustException) e.getCause()).getType() == TrustException.Type.NEW_CONNECTION
						|| ((TrustException) e.getCause()).getType() == TrustException.Type.NEW_KEY) {
					// Add trust WITH 'force' option
					try {
						String result = server.addTrust(new TrustOptions()
								.setForce(true).setAutoAccept(true));
						assertNotNull(result);
					} catch (P4JavaException e2) {
						assertNotNull(e2);
					}
				}
			}
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}

			// Set the server user
			server.setUserName("mruan");

			// Login using the normal method
			server.login("mruan", new LoginOptions());

			client = server.getClient("mruan-mac");
			assertNotNull(client);
			server.setCurrentClient(client);

			// Check server info
			IServerInfo serverInfo = server.getServerInfo();
			assertNotNull(serverInfo);
			assertTrue(serverInfo.isCaseSensitive());
			assertTrue(serverInfo.isServerEncrypted());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
