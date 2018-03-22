/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r151;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;

import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test replacement fingerprint ('p4 trust -r')
 */
@Jobs({"job076501"})
@TestId("Dev151_ReplacementFingerprintTest")
public class ReplacementFingerprintTest extends P4JavaTestCase {
    /**
     * Test replacement fingerprint.
     */
    @Test
    public void testAddRemoveReplacementTrust() throws Exception {
        String serverUri = "p4javassl://eng-p4java-vm.perforce.com:30121";
        server = ServerFactory.getOptionsServer(serverUri, props);
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());

        // Remove the normal fingerprint
        String result = server.removeTrust();
        assertNotNull(result);
        assertTrue(result.contains("Removed trust for Perforce server"));

        // Remove the replacement fingerprint
        result = server.removeTrust(new TrustOptions().setReplacement(true));
        assertNotNull(result);
        assertTrue(result.contains("Removed trust for Perforce server"));

        // Add a wrong fingerprint
        result = server.addTrust("B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2:B1:C2");
        assertNotNull(result);
        assertTrue(result.contains("Added trust for Perforce server"));

        // Add a correct replacement fingerprint
        //result = server.addTrust("F2:5D:2F:FF:2F:C4:DD:F1:C0:B6:A1:11:AA:0F:F5:2B:11:54:39:86", new TrustOptions().setReplacement(true));
        result = server.addTrust(new TrustOptions().setReplacement(true).setForce(true).setAutoAccept(true));
        assertNotNull(result);
        assertTrue(result.contains("Added trust for Perforce server"));

        // Should be able to connect to the server.
        server.connect();

        // Run 'p4 info' command
        IServerInfo info = server.getServerInfo();
        assertNotNull(info);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
