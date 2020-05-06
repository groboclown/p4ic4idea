/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.SSLServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static com.perforce.p4java.exception.TrustException.Type.NEW_CONNECTION;
import static com.perforce.p4java.exception.TrustException.Type.NEW_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test in-memory fingerprints.
 */
@Jobs({"job059814"})
@TestId("Dev123_InMemoryFingerprintsTest")
public class InMemoryFingerprintsTest extends P4JavaLocalServerTestCase {

	@ClassRule
	public static SSLServerRule p4d = new SSLServerRule("r16.1", InMemoryFingerprintsTest.class.getSimpleName(), "ssl:localhost:10671");

	Properties serverProps = new Properties();
	IClient client = null;

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
		afterEach(server);
	}

	/**
	 * Test in-memory fingerprints - add trust
	 */
	@Test
	public void testInMemoryAddTrust() throws Exception {
		server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), serverProps);
		assertNotNull(server);

		// Register callback
		server.registerCallback(createCommandCallback());
		// Run remove trust first
		String result = server.removeTrust();
		assertNotNull(result);

		// Should get 'new connection' trust exception
		try {
			// Connect to the server.
			server.connect();
		} catch (P4JavaException e) {
			assertNotNull(e);
			assertTrue(e.getCause() instanceof TrustException);
			TrustException cause = (TrustException) e.getCause();
			assertTrue(cause.getType() == NEW_CONNECTION);
			assertNotNull(cause.getFingerprint());

			// Add the key (new connection)
			try {
				result = server.addTrust(cause.getFingerprint());
				assertNotNull(result);
				assertTrue(result.contains("not known"));
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
			assertTrue(result.contains("Added trust for Perforce server"));
		} catch (P4JavaException e) {
			assertNotNull(e);
		}

		// Add trust WITHOUT 'force' option
		// Should get 'new key' exception
		try {
			server.addTrust(new TrustOptions());
		} catch (P4JavaException e) {
			assertNotNull(e);
			assertTrue(e instanceof TrustException);
			assertTrue(((TrustException) e).getType() == NEW_KEY);
			assertTrue(e.getLocalizedMessage().contains("IDENTIFICATION HAS CHANGED"));
		}

		// Add trust WITH 'force' and 'autoAccept' options
		try {
			result = server.addTrust(new TrustOptions()
					.setForce(true).setAutoAccept(true));
			assertNotNull(result);
			assertTrue(result.contains("Added trust for Perforce server"));
		} catch (P4JavaException e) {
			assertNotNull(e);
		}
	}

	/**
	 * Test in-memory fingerprints - remove trust
	 */
	@Test
	public void testInMemoryRemoveTrust() throws Exception {
		setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);

		// Remove trust
		String removeResult = server.removeTrust();
		assertNotNull(removeResult);
		assertTrue(removeResult.contains("Removed trust for Perforce server"));
	}

	/**
	 * Test in-memory fingerprints - get fingerprint from Perforce SSL connection
	 */
	@Test
	public void testInMemoryGetTrust() throws Exception {
		server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), serverProps);
		assertNotNull(server);

		// Register callback
		server.registerCallback(createCommandCallback());

		// Get fingerprint
		String fingerprint = server.getTrust();
		assertNotNull(fingerprint);
	}

	/**
	 * Test in-memory fingerprints - get trusts from trust file
	 */
	@Test
	public void testInMemoryGetTrusts() throws Exception {
		setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);

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
		for (Fingerprint f : fingerprints) {
			if (f != null && f.getFingerprintValue() != null) {
				if (f.getFingerprintValue().equalsIgnoreCase(fp)) {
					found = true;
					break;
				}
			}
		}
		assertTrue(found);
	}

	/**
	 * Test in-memory fingerprints - trust key is an IP address from trust file
	 */
	@Test
	public void testInMemoryTrustKey() throws Exception {
		server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), serverProps);
		assertNotNull(server);

		// Register callback
		server.registerCallback(createCommandCallback());

		// Get fingerprint
		String fingerprint = server.getTrust();
		assertNotNull(fingerprint);
	}

	/**
	 * Test in-memory fingerprints - connecting to the server.
	 */
	@Test
	public void testInMemoryConnection() throws Exception {
		setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);

		// Check server info
		IServerInfo serverInfo = server.getServerInfo();
		assertNotNull(serverInfo);
		assertTrue(serverInfo.isCaseSensitive());
		assertTrue(serverInfo.isServerEncrypted());
	}
}
