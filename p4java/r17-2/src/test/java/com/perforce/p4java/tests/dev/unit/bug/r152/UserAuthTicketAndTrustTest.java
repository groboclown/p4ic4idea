/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static com.perforce.p4java.exception.TrustException.Type.NEW_CONNECTION;
import static com.perforce.p4java.exception.TrustException.Type.NEW_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Test;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test user's auth tickets and trust files.
 */
@Jobs({"job083624"})
@TestId("Dev152_UserAuthTicketAndTrustTest")
public class UserAuthTicketAndTrustTest extends P4JavaTestCase {
    private static final String SSL_SERVER_URL = "p4javassl://eng-p4java-vm.perforce.com:30121";
    private static final String p4testclient = "p4TestUserWS20112";
    /**
     * Test user's auth tickets and trust files.
     */
    @Test
    public void testUserAuthTicketAndTrust() throws Exception {
        Properties props = new Properties();
        //props.put(PropertyDefs.AUTH_FILE_LOCK_TRY_KEY_SHORT_FORM, 200);
        //props.put(PropertyDefs.AUTH_FILE_LOCK_DELAY_KEY_SHORT_FORM, 200000);
        //props.put(PropertyDefs.AUTH_FILE_LOCK_WAIT_KEY_SHORT_FORM, 2);

        props.put(PropertyDefs.AUTH_FILE_LOCK_TRY_KEY, 200);
        props.put(PropertyDefs.AUTH_FILE_LOCK_DELAY_KEY, 200000);
        props.put(PropertyDefs.AUTH_FILE_LOCK_WAIT_KEY, 2);

        //props.put("authFileLockTry", 200);
        //props.put("authFileLockDelay", 200000);
        //props.put("authFileLockWait", 2);

        //props.put("com.perforce.p4java.authFileLockTry", 200);
        //props.put("com.perforce.p4java.authFileLockDelay", 200000);
        //props.put("com.perforce.p4java.authFileLockWait", 2);

        server = getServer(SSL_SERVER_URL, props);
        assertThat(server, notNullValue());

        server.registerCallback(createCommandCallback());
        // Connect to the server.
        try {
            server.connect();
        } catch (P4JavaException e) {
            assertNotNull(e);
            assertTrue(e.getCause() instanceof TrustException);
            TrustException cause = (TrustException) e.getCause();
            if (cause.getType() == NEW_CONNECTION || cause.getType() == NEW_KEY) {
                // Add trust WITH 'force' option
                try {
                    String result = server.addTrust(new TrustOptions().setForce(true).setAutoAccept(true));
                    assertNotNull(result);
                } catch (P4JavaException e2) {
                    assertNotNull(e2);
                }
            }
        }
        setUtf8CharsetIfServerSupportUnicode(server);

        // Set the test user
        server.setUserName(getUserName());

        // Logout
        server.logout();

        // Login
        server.login(getPassword(), new LoginOptions());

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
