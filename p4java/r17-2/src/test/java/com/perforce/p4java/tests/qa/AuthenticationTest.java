package com.perforce.p4java.tests.qa;

import com.google.common.io.Closeables;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
public class AuthenticationTest {
    private static final String SECOND_USER_NAME = "secondUser";
    private static final String SECOND_USER_PASSWORD = "thispasswordisoversixteencharacterslong";
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;


    // setup a server with one open and locked file with two clients and users
    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.setMonitor(2);
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();
        IUser user = helper.createUser(server, ts.getUser(), "banana");

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        File test2File = new File(client.getRoot(), "bar.txt");

        helper.addFile(server, user, client, testFile.getAbsolutePath(), "FileSpecTest", "text");
        helper.addFile(server, user, client, test2File.getAbsolutePath(), "FileSpecTest", "text");

        IUserGroup group = new UserGroup();
        List<String> users = newArrayList();
        // create a group and add some other user
        users.add(SECOND_USER_NAME);
        group.setName("timeoutnow");
        group.setTimeout(1);
        group.setUsers(users);

        UpdateUserGroupOptions opt = new UpdateUserGroupOptions();
        server.createUserGroup(group, opt);

        helper.createUser(server, SECOND_USER_NAME, SECOND_USER_PASSWORD);
        server.login(SECOND_USER_PASSWORD);
        server.logout();
        server.setUserName(ts.getUser());
    }

    @BeforeEach
    public void Setup() throws Exception {
        server.setUserName(ts.getUser());
        server.login("banana");
        server.logout();
    }

    // verify that we get an access exception if we log out or our ticket expires
    // streaming commands in particular displayed this issue
    // we should get an exception if everything is working correctly
    @Test
    public void exceptionAfterLoggingOut() throws Throwable {
        InputStream diffStream = null;
        try {
            diffStream = server.getServerFileDiffs(new FileSpec("//depot/foo.txt"), new FileSpec("//depot/bar.txt"),
                    null, null, false, false, false);
            assertThat(diffStream, notNullValue());
            // we shouldn't get here
            fail("Did not get access exception");
        } catch (AccessException a) {
            assertTrue("Should have been unset", a.getMessage().startsWith("Perforce password (P4PASSWD) invalid or unset."));
        } finally {
            Closeables.closeQuietly(diffStream);
        }
    }

    // make sure a timed out connection works
    @Test
    public void exceptionAfterTimeout() throws Throwable {
        InputStream diffStream = null;
        try {
            server.setUserName(SECOND_USER_NAME);
            server.login(SECOND_USER_PASSWORD);
            assertThat("Was not secondUser", server.getUserName(), is(SECOND_USER_NAME));
            TimeUnit.SECONDS.sleep(2);
            diffStream = server.getServerFileDiffs(new FileSpec("//depot/foo.txt"), new FileSpec("//depot/bar.txt"),
                    null, null, false, false, false);
            assertThat(diffStream, notNullValue());
            // we shouldn't get here
            fail("Did not get access exception");
        } catch (AccessException a) {
            assertTrue("Should have expired.", a.getMessage().startsWith("Your session has expired, please login again."));
        } finally {
            Closeables.closeQuietly(diffStream);
        }
    }

    // verify that we can get a valid ticket for all machines
    @Test
    public void globalLogin() throws Throwable {
        server.login("banana", true);

        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    // verify that we can have a password with a '#'
    @Test
    public void changePassword() throws Throwable {
        helper.createUser(server, "thirdUser", "foobar");
        server.setUserName("thirdUser");

        server.changePassword("foobar", "foo#bar", null);
        server.login("foo#bar", true);

        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    // create a password with the changePassword method
    @Test
    public void newPasswordWithChangePassword() throws Throwable {
        helper.createUser(server, "fourthUser", null);
        server.setUserName("fourthUser");

        server.changePassword("", "foo#bar", null);
        server.login("foo#bar", true);

        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    // change to an overly long password
    @Test
    public void changeToLongPassword() throws Throwable {
        helper.createUser(server, "fifthUser", "short");
        server.setUserName("fifthUser");

        server.changePassword("short", "verylongverylongverylong", null);
        server.login("verylongverylongverylong", true);

        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    // verify job049985: should get access exception from bad login
    @Test
    public void wrongPassword() throws Throwable {
        try {
            server.login("bad_password");
            fail("should have thrown exception");
        } catch (AccessException ae) {
            assertThat("incorrect error message", ae.getLocalizedMessage(), containsString("Password invalid."));
        }
    }


    // verify job047563: verify support of the -p flag
    @Test
    public void displayTicket() throws Throwable {
        LoginOptions opts = new LoginOptions().setDontWriteTicket(true);
        StringBuffer ticket = new StringBuffer();
        ticket.append("password=");
        server.login("banana", ticket, opts);

        assertTrue("option not set", opts.isDontWriteTicket());
        assertTrue("no ticket returned", ticket.length() > 10);
        assertTrue("was not appended", ticket.toString().startsWith("password="));

        // make sure we are still logged in
        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    // verify job046826: verify support for logging in others
    @Test
    public void loginOther() throws Throwable {
        // generate ticket first
        String sixthUser = "sixthUser";
        helper.createUser(server, sixthUser, SECOND_USER_PASSWORD);
        server.setUserName(sixthUser);
        server.login(SECOND_USER_PASSWORD);
        server.logout();
        // login
        server.setUserName(ts.getUser());
        server.login("banana");
        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertNotNull(changes);
        assertThat("wrong number of changes", changes.size(), is(2));

        IUser other = server.getUser(sixthUser);
        server.login(other, null, null);

        // make sure we are still logged in
        server.setUserName(sixthUser);
        String userServerAuthTicket = server.getAuthTicket(sixthUser);
        server.setAuthTicket(userServerAuthTicket);
        changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

