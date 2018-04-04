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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
public class Authentication102Test {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
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
        users.add("secondUser");
        group.setName("timeoutnow");
        group.setTimeout(1);
        group.setUsers(users);

        UpdateUserGroupOptions opt = new UpdateUserGroupOptions();
        server.createUserGroup(group, opt);

        helper.createUser(server, "secondUser", "thispasswordisoversixteencharacterslong");

        server.setUserName(ts.getUser());
    }

    @BeforeEach
    public void Setup() throws Exception {
        server.setUserName(ts.getUser());
        server.setUserName(ts.getUser());
        server.login("banana");
    }

    // verify that we get an access exception if we log out or our ticket expires
    // streaming commands in particular displayed this issue
    // we should get an exception if everything is working correctly
    @Test
    public void exceptionAfterLoggingOut() throws Exception {
        InputStream diffStream = null;
        try {
            server.logout();

            diffStream = server.getServerFileDiffs(
                    new FileSpec("//depot/foo.txt"),
                    new FileSpec("//depot/bar.txt"),
                    null,
                    null,
                    false,
                    false,
                    false);
            assertThat(diffStream, notNullValue());

            // we shouldn't get here
            fail("Did not get access exception");
        } catch (AccessException a) {
            assertThat("Should have been unset", a.getMessage(), startsWith("Perforce password (P4PASSWD) invalid or unset."));
        } finally {
            Closeables.closeQuietly(diffStream);
        }
    }

    // make sure a timed out connection works
    @Test
    public void exceptionAfterTimeout() throws Exception {
        InputStream diffStream = null;
        try {
            server.setUserName("secondUser");
            server.login("thispasswordisoversixteencharacterslong");
            assertThat("Was not secondUser", server.getUserName(), is("secondUser"));
            TimeUnit.SECONDS.sleep(3);
            diffStream = server.getServerFileDiffs(
                    new FileSpec("//depot/foo.txt"),
                    new FileSpec("//depot/bar.txt"),
                    null,
                    null,
                    false,
                    false,
                    false);
            assertThat(diffStream, notNullValue());

            // we shouldn't get here
            fail("Did not get access exception");
        } catch (AccessException a) {
            assertThat("Should have expired.", a.getMessage(), startsWith("Your session has expired, please login again."));
        } finally {
            Closeables.closeQuietly(diffStream);
        }
    }

    // verify that we can still get a valid ticket for all machines
    // there's no great way to verify that it is a global ticket; I had to do that by hand
    @Test
    public void globalLogin() throws Throwable {
        server.logout();
        server.login("banana", true);
        List<IChangelistSummary> changes = server.getChangelists(FileSpecBuilder.makeFileSpecList("//depot/..."), null);
        assertThat(changes, notNullValue());
        assertThat("wrong number of changes", changes.size(), is(2));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

