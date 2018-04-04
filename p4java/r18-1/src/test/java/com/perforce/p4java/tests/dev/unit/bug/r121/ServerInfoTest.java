/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test server info.
 */
@Jobs({"job049708", "job051803", "job053530"})
@TestId("Dev121_ServerInfoTest")
public class ServerInfoTest extends P4JavaTestCase {
    /**
     * The client.
     */
    private IClient client = null;

    /**
     * Test server info.
     */
    @Test
    public void testServerInfo() throws Exception {
        String serverUri =
                "p4javassl://eng-p4java-vm.perforce.com:30121?socketPoolSize=10&testKey1=testVal1";
        server = ServerFactory.getOptionsServer(serverUri, props);

        server.removeTrust();
        // assume a new first time connection
        server.addTrust(new TrustOptions().setAutoAccept(true));
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());
        // Connect to the server.
        server.connect();
        setUtf8CharsetIfServerSupportUnicode(server);

        // Set the server user
        server.setUserName(userName);

        // Login using the normal method
        server.login(password, new LoginOptions());

        client = server.getClient(getPlatformClientName("p4TestUserWS20112"));
        assertNotNull(client);
        server.setCurrentClient(client);

        // Check server info
        IServerInfo serverInfo = server.getServerInfo();
        assertNotNull(serverInfo);
        assertTrue(serverInfo.isCaseSensitive());
        assertTrue(serverInfo.isServerEncrypted());
    }

    /**
     * Test server info integEngine field.
     */
    @Test
    @Ignore("Tries to connect to play.perforce.com")
    public void testServerInfoIntegEngine() throws Exception {
        String serverUri = "p4java://play.perforce.com:20111";
        server = ServerFactory.getOptionsServer(serverUri, props);
        assertNotNull(server);

        // Register callback
        server.registerCallback(createCommandCallback());
        // Connect to the server.
        server.connect();
        setUtf8CharsetIfServerSupportUnicode(server);

        // Check server info
        IServerInfo serverInfo = server.getServerInfo();
        assertNotNull(serverInfo);
        assertNotNull(serverInfo.getIntegEngine());
    }

    @After
    public void afterEach() throws Exception {
        afterEach(server);
    }
}
