package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("We no longer support 2010.1.")
@RunWith(JUnitPlatform.class)
public class CreateUser101Test {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static String testUser = "testUser";
    private static IClient client = null;
    private static File testFile = null;

    // server setup, nothing fancy
    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline("p10.1");
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + Helper.FILE_SEP + "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");
    }


    @BeforeEach
    public void reset() {
        try {
            server.deleteUser(testUser, true);
            server.setUserName(ts.getUser());
        } catch (Throwable ignore) {
        }
    }

    // attempt to create a user
    @Test
    public void basicUsage() throws Throwable {

        String result = "";
        server.setUserName(testUser);
        IUser user = new User();
        user.setLoginName(testUser);
        user.setEmail(testUser + "@example.com");
        user.setFullName(testUser + " " + testUser);

        result = server.createUser(user, false);

        assertThat(result, notNullValue());
        assertThat(result, containsString("User " + testUser + " saved."));

        List<IUserSummary> users = server.getUsers(null, null);

        assertThat(users.size(), is(2));

        for (IUserSummary u : users) {
            assertThat("type incorrectly set", u.getType(), nullValue());
            if (u.getLoginName().contains(testUser)) {
                assertThat("wrong full name", u.getFullName(), is(testUser + " " + testUser));
                assertThat("wrong email", u.getEmail(), is(testUser + "@example.com"));
            }
        }
    }

    // attempt to create a user
    @Test
    public void forcedCreation() throws Throwable {

        String result = "";
        IUser user = new User();
        user.setLoginName(testUser);
        user.setEmail(testUser + "@example.com");
        user.setFullName(testUser + " " + testUser);

        result = server.createUser(user, true);

        assertThat(result, notNullValue());
        assertThat(result, containsString("User " + testUser + " saved."));

        List<IUserSummary> users = server.getUsers(null, null);

        assertThat(users.size(), is(2));

        for (IUserSummary u : users) {
            assertThat("incorrectly set", u.getType(), nullValue());
            if (u.getLoginName().contains(testUser)) {
                assertThat("wrong full name", u.getFullName(), is(testUser + " " + testUser));
                assertThat("wrong email", u.getEmail(), is(testUser + "@example.com"));
            }
        }
    }

    // attempt to create a service user; currently we don't have a way to run
    // p4 users -a so we can't see the user that is created
    @Test
    public void userType() throws Throwable {
        try {
            IUser user = new User();
            user.setLoginName(testUser);
            user.setType(UserType.SERVICE);
            user.setEmail(testUser + "@example.com");
            user.setFullName(testUser + " " + testUser);
            server.createUser(user, true);
            fail("we should not get here");
        } catch (RequestException e) {
            assertThat(e.getLocalizedMessage(), containsString("Unknown field name 'Type'"));
        }
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

