package com.perforce.p4java.tests.qa;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetLoginStatusTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IClient client = null;


    // setup a server with one open and locked file with two clients and users
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.setMonitor(2);
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();
        h.createUser(server, ts.getUser(), "banana");

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);
    }

    @Before
    public void Setup() {
        try {

            server.login("banana");

        } catch (Throwable t) {

        }
    }

    // verify we get a proper expiry message, the current 10.2 p4d build causes this test to fail
    @Test
    public void loggedIn() throws Throwable {
        String result = server.getLoginStatus();
        assertNotNull("Login status should never be null", result);
        assertTrue("10.2 p4d is still sending out bad information", result.contains("expires"));
    }

    // verify we get a non-null response from login status
    @Test
    public void loggedOut() throws Throwable {
        server.logout();

        String result = server.getLoginStatus();
        assertNotNull(result);
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	

