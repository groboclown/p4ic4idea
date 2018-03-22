package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.IUserSummary.UserType.SERVICE;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

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
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class UpdateUserTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IUser testUser = null;
    private static IClient client = null;
    private static File testFile = null;

    // server setup, nothing fancy
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");

        testUser = h.createUser(server, "testUser");
        server.setUserName(ts.getUser());
    }


    @Before
    public void reset() {

        try {

            h.createUser(server, "testUser");
            server.setUserName(ts.getUser());

        } catch (Throwable t) {

        }

    }

    // attempt to create a user
    @Test
    public void basicUsage() {

        String result = "";
        try {

            server.setUserName(testUser.getLoginName());
            IUser lUser = server.getUser(testUser.getLoginName());

            lUser.setEmail("teststring");

            result = server.updateUser(lUser, false);

            assertNotNull(result);
            assertThat(result, containsString("User " + testUser.getLoginName() + " saved."));

            List<IUserSummary> users = server.getUsers(null, null);

            assertEquals(2, users.size());

            for (IUserSummary u : users) {
                assertEquals("wrong type", "STANDARD", u.getType().toString());

                if (u.getLoginName().contains(testUser.getLoginName())) {

                    assertEquals("wrong full name", testUser.getLoginName(), u.getFullName());
                    assertEquals("wrong email", "teststring", u.getEmail());

                }
            }
        } catch (Throwable t) {

            h.error(t);

        }
    }

    // attempt to create a user
    @Test
    public void forcedUpdate() {

        String result = "";
        try {

            IUser tUser = server.getUser(testUser.getLoginName());
            tUser.setEmail("forcedCreation");

            result = server.createUser(tUser, true);

            assertNotNull(result);
            assertThat(result, containsString("User " + testUser.getLoginName() + " saved."));

            List<IUserSummary> users = server.getUsers(null, null);

            assertEquals(2, users.size());

            for (IUserSummary u : users) {
                assertEquals("wrong type", "STANDARD", u.getType().toString());

                if (u.getLoginName().contains(testUser.getLoginName())) {
                    assertEquals("wrong full name", testUser.getLoginName(), u.getFullName());
                    assertEquals("wrong email", "forcedCreation", u.getEmail());
                }
            }
        } catch (Throwable t) {

            h.error(t);

        }
    }

    // attempt to create a user
    @Test(expected = RequestException.class)
    public void forcedUpdateFail() throws Throwable {
        IUser tUser = server.getUser(testUser.getLoginName());
        tUser.setEmail("forcedCreation");

        server.updateUser(tUser, false);
    }

    // attempt to create a service user; currently we don't have a way to run
    // p4 users -a so we can't see the user that is created
    @Test(expected = RequestException.class)
    public void userType() throws Throwable {
        IUser tUser = server.getUser(testUser.getLoginName());
        tUser.setType(SERVICE);

        server.updateUser(tUser, true);
        fail("we should never get here");
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
