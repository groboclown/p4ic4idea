/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features151;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.LoginOptions;
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

import static com.perforce.p4java.exception.TrustException.Type.NEW_CONNECTION;
import static com.perforce.p4java.exception.TrustException.Type.NEW_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test fingerprint and replacement ('p4 trust -r')
 */
@Jobs({"job076540"})
@TestId("Dev151_FingerprintReplacementTest")
public class FingerprintReplacementTest extends P4JavaLocalServerTestCase {

    @ClassRule
    public static SSLServerRule p4d = new SSLServerRule("r16.1", FingerprintReplacementTest.class.getSimpleName(), "ssl:localhost:10668");


    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @After
    public void tearDown() {
        afterEach(server);
    }

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void beforeEach() throws Exception {
        initialP4JavaTestCase();
    }

    /**
     * Test add and remove replacement fingerprints
     */
    @Test
    public void testAddRemoveReplacementTrust() throws Exception {
        server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), props);

        // Register callback
        server.registerCallback(createCommandCallback());

        // Run remove trust first
        String result = server.removeTrust();
        assertNotNull(result);

        result = server.removeTrust(new TrustOptions().setReplacement(true));
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
        } catch (P4JavaException e) {
            assertNotNull(e);
        }

        // Add a replacement fingerprint
        try {
            //result = server.addTrust("F2:5D:2F:FF:2F:C4:DD:F1:C0:B6:A1:11:AA:0F:F5:2B:11:54:39:86", new TrustOptions().setReplacement(true));
            result = server.addTrust(new TrustOptions().setReplacement(true).setForce(true).setAutoAccept(true));
            assertNotNull(result);
        } catch (P4JavaException e) {
            assertNotNull(e);
        }

        // The replacement should work
        try {
            result = server.addTrust(new TrustOptions());
            assertNotNull(result);
            assertEquals(result, "Trust already established.");
        } catch (P4JavaException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test remove trust
     */
    @Test
    public void testRemoveTrust() throws Exception {
        setupSSLServer(p4d.getP4JavaUri(),userName, password, true, props);

        // Remove trust
        String removeResult = server.removeTrust();
        assertNotNull(removeResult);
        assertTrue(removeResult.contains("Removed trust for Perforce server"));
    }

    /**
     * Test get fingerprint from Perforce SSL connection
     */
    @Test
    public void testGetTrust() throws Exception {
        server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), props);
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());

        // Get fingerprint
        String fingerprint = server.getTrust();
        assertNotNull(fingerprint);
    }

    /**
     * Test get trusts from trust file
     */
    @Test
    public void testGetTrusts() throws Exception {
        server = ServerFactory.getOptionsServer(p4d.getP4JavaUri(), props);
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());

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

        // Add trust WITH 'force' and 'autoAccept' options
        try {
            String result = server.addTrust(new TrustOptions()
                    .setForce(true).setAutoAccept(true));
            assertNotNull(result);
            assertTrue("Expected result to contain 'Added trust', but was " + result,
                    result.contains("Added trust for Perforce server"));
        } catch (P4JavaException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test trust key is an IP address from trust file
     */
    @Test
    public void testTrustKey() throws Exception {
        setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);

        // Register callback
        server.registerCallback(createCommandCallback());

        // Get fingerprint
        String fingerprint = server.getTrust();
        assertNotNull(fingerprint);
    }

    /**
     * Test connecting to the server
     */
    @Test
    public void testConnection() throws Exception {
        String serverUri = p4d.getP4JavaUri() + "?socketPoolSize=10&testKey1=testVal1";
        server = ServerFactory.getOptionsServer(serverUri, props);
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());

        try {
            // Connect to the server.
            server.connect();
        } catch (P4JavaException e) {
            assertNotNull(e);
            assertTrue(e.getCause() instanceof TrustException);
            TrustException cause = (TrustException) e.getCause();
            if (cause.getType() == NEW_CONNECTION
                    || cause.getType() == NEW_KEY) {
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

        setUtf8CharsetIfServerSupportUnicode(server);

        // Set the server user
        server.setUserName(getUserName());

        // Login using the normal method
        server.login(getPassword(), new LoginOptions());

        client = server.getClient("p4TestUserWS20112");
        assertNotNull(client);
        server.setCurrentClient(client);

        // Check server info
        IServerInfo serverInfo = server.getServerInfo();
        assertNotNull(serverInfo);
        assertTrue(serverInfo.isCaseSensitive());
        assertTrue(serverInfo.isServerEncrypted());
    }
}
