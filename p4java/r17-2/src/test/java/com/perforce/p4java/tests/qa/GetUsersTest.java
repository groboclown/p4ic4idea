package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetUsersTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;


    // setup a server with a couple users
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
        user = h.createUser(server, ts.getUser(), "banana");

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");

        h.addFile(server, user, client, testFile.getAbsolutePath(), "GetUsersTest", "text");

        h.createUser(server, "secondUser", "thispasswordisoversixteencharacterslong");

        server.setUserName(ts.getUser());
    }

    @Before
    public void Setup() {
        server.setUserName(ts.getUser());
        try {

            server.setUserName(ts.getUser());
            server.login("banana");

        } catch (Throwable t) {

        }
    }

    // verify that -l works
    @Test
    public void userDetail() throws Throwable {
        GetUsersOptions opts = new GetUsersOptions();
        opts.setExtendedOutput(true);

        List<IUserSummary> users = server.getUsers(null, opts);

        for (IUserSummary u : users) {

            if (u.getLoginName().contains("secondUser")) {

                assertEquals("wrong full name", "secondUser", u.getFullName());
                assertEquals("wrong email", "secondUser@email.com", u.getEmail());
                assertNotNull("no password change date", u.getPasswordChange());
                assertNotNull("no expiration", u.getTicketExpiration());

            }
        }

        opts.setExtendedOutput(false);
        users = server.getUsers(null, opts);

        for (IUserSummary u : users) {

            if (u.getLoginName().contains("secondUser")) {

                assertEquals("wrong full name", "secondUser", u.getFullName());
                assertEquals("wrong email", "secondUser@email.com", u.getEmail());
                assertNull("should be null", u.getPasswordChange());
                assertNull("should be null", u.getTicketExpiration());

            }
        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
