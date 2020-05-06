/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.tests.SSLServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaLocalServerTestCase;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test user's auth tickets and trust files.
 */
@Jobs({"job083624"})
@TestId("Dev152_UserAuthTicketAndTrustTest")
public class UserAuthTicketAndTrustTest extends P4JavaLocalServerTestCase {

    @ClassRule
    public static SSLServerRule p4d = new SSLServerRule("r16.1", UserAuthTicketAndTrustTest.class.getSimpleName(), "ssl:localhost:10672");

    private static final String p4testclient = "p4TestUserWS20112";
    /**
     * Test user's auth tickets and trust files.
     */
    @Test
    public void testUserAuthTicketAndTrust() throws Exception {
        Properties props = new Properties();
        props.put(PropertyDefs.AUTH_FILE_LOCK_TRY_KEY, 200);
        props.put(PropertyDefs.AUTH_FILE_LOCK_DELAY_KEY, 200000);
        props.put(PropertyDefs.AUTH_FILE_LOCK_WAIT_KEY, 2);

        setupSSLServer(p4d.getP4JavaUri(), userName, password, true, props);

        // Get and set the test client
        IClient client = server.getClient(p4testclient);
        assertNotNull(client);
        server.setCurrentClient(client);

        // Test the 'changes' command
        List<IChangelistSummary> changelistSummaries = server.getChangelists(null, new GetChangelistsOptions().setMaxMostRecent(10));
        assertEquals(10, changelistSummaries.size());
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }
}
