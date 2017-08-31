/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static com.perforce.p4java.exception.TrustException.Type.NEW_CONNECTION;
import static com.perforce.p4java.exception.TrustException.Type.NEW_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test fingerprint and 'p4 trust'
 */
@Jobs({"job053040"})
@TestId("Dev121_FingerprintTest")
public class FingerprintTest extends P4JavaTestCase {
    private static String SSL_ENABLED_P4D_SERVER = "p4javassl://eng-p4java-vm.perforce.com:30121";
    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @After
    public void tearDown() {
        afterEach(server);
    }

    /**
     * Test add trust
     */
    @Test
    public void testAddTrust() throws Exception {
        server = ServerFactory.getOptionsServer(SSL_ENABLED_P4D_SERVER, props);
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
            assertTrue(e.getMessage().contains("IDENTIFICATION HAS CHANGED"));
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
     * Test remove trust
     */
    @Test
    public void testRemoveTrust() throws Exception {
        server = ServerFactory.getOptionsServer(SSL_ENABLED_P4D_SERVER, props);
        assertNotNull(server);
        // Register callback
        server.registerCallback(createCommandCallback());

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
        server = ServerFactory.getOptionsServer(SSL_ENABLED_P4D_SERVER, props);
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
        server = ServerFactory.getOptionsServer(SSL_ENABLED_P4D_SERVER, props);
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
            assertTrue(result.contains("Added trust for Perforce server"));
        } catch (P4JavaException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test trust key is an IP address from trust file
     */
    @Test
    public void testTrustKey() throws Exception {
        server = ServerFactory.getOptionsServer(SSL_ENABLED_P4D_SERVER, props);
        assertNotNull(server);

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
        String serverUri = SSL_ENABLED_P4D_SERVER + "?socketPoolSize=10&testKey1=testVal1";
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
