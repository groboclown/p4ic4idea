/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.LocalServerRule;
import com.perforce.p4java.tests.SSLServerRule;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import com.perforce.p4java.tests.dev.unit.features151.ReconcileUseModTimeTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
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

import java.nio.file.Paths;
import java.util.Properties;

/**
 * Test server info.
 */
@Jobs({"job049708", "job051803", "job053530"})
@TestId("Dev121_ServerInfoTest")
public class ServerInfoTest extends P4JavaLocalServerTestCase {

    @ClassRule
    public static SSLServerRule p4d = new SSLServerRule("r16.1", ServerInfoTest.class.getSimpleName(), "ssl:localhost:10667");

    @Before
    public void setUp() {
        try {
            setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);
            IClient client = server.getClient("p4TestUserWS20112");
            assertNotNull(client);
            server.setCurrentClient(client);
            IOptionsServer superServer = getSuperConnection(p4d.getP4JavaUri());
            superServer.setOrUnsetServerConfigurationValue("dm.integ.engine","2");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Test server info on ssl server.
     */
    @Test
    public void testServerInfoOnSSLServer() throws Exception {
        IServerInfo serverInfo = server.getServerInfo();
        assertNotNull(serverInfo);
        assertTrue(serverInfo.isCaseSensitive());
        assertTrue(serverInfo.isServerEncrypted());
    }

    /**
     * Test server info integEngine field.
     */
    @Test
    public void testServerInfoIntegEngineOnSSLServer() throws Exception {
        // Check server info
        IServerInfo serverInfo = server.getServerInfo();
        assertNotNull(serverInfo);
        assertEquals("2", serverInfo.getIntegEngine());
        assertTrue(serverInfo.isCaseSensitive());
        assertTrue(serverInfo.isServerEncrypted());
    }

    @After
    public void afterEach() throws Exception {
        afterEach(server);
    }
}
