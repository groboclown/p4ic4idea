package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.IUserSummary.UserType;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(JUnitPlatform.class)
public class CreateUserTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static String testUser = "testUser";

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");
    }


    @BeforeEach
    public void reset() {
        try {
            server.setUserName(ts.getUser());
        } catch (Exception ignore) {
        }
    }

    // attempt to create a user
    @Test
    public void basicUsage() throws Exception {
        server.setUserName(testUser);
        IUser user = new User();
        user.setLoginName(testUser);
        user.setEmail(testUser + "@example.com");
        user.setFullName(testUser + " " + testUser);

        String result = server.createUser(user, false);

        assertThat(isNotBlank(result), is(true));
        assertThat(result, containsString("User " + testUser + " saved."));

        List<IUserSummary> users = server.getUsers(null, null);

        assertThat(users.size(), is(2));

        verifyCreatedUsers(users);
    }

    // attempt to create a user
    @Test
    public void forcedCreation() throws Exception {
        createAndVerifyCreatedUser(null);
        List<IUserSummary> users = server.getUsers(null, null);

        assertThat(users.size(), is(2));

        verifyCreatedUsers(users);
    }

    private void createAndVerifyCreatedUser(UserType userType) throws ConnectionException, RequestException, AccessException {
        IUser user = new User();
        if (nonNull(userType)) {
            user.setType(userType);
        }
        user.setLoginName(testUser);
        user.setEmail(testUser + "@example.com");
        user.setFullName(testUser + " " + testUser);

        String result = server.createUser(user, true);
        assertThat(isNotBlank(result), is(true));
        assertThat(result, containsString("User " + testUser + " saved."));
    }

    private void verifyCreatedUsers(List<IUserSummary> users) {
        for (IUserSummary u : users) {
            assertThat("wrong type", u.getType().toString(), is("STANDARD"));
            if (u.getLoginName().contains(testUser)) {
                assertThat("wrong full name", u.getFullName(), is(testUser + " " + testUser));
                assertThat("wrong email", u.getEmail(), is(testUser + "@example.com"));
            }
        }
    }

    // attempt to create a service user; currently we don't have a way to run
    // p4 users -a so we can't see the user that is created
    @Test
    public void userType() throws Exception {
        createAndVerifyCreatedUser(UserType.SERVICE);

        GetUsersOptions opts = new GetUsersOptions();
        opts.setIncludeServiceUsers(true);

        List<IUserSummary> users = server.getUsers(null, opts);

        assertThat(users.size(), is(2));
        boolean testUserSeen = false;

        for (IUserSummary u : users) {
            if (u.getLoginName().contains(testUser)) {
                testUserSeen = true;
                assertThat("wrong full name", u.getFullName(), is(testUser + " " + testUser));
                assertThat("wrong email", u.getEmail(), is(testUser + "@example.com"));
                assertThat("wrong type", u.getType().toString(), is("SERVICE"));
            }
        }

        assertThat("testUser not seen", testUserSeen, is(true));
    }

    @AfterEach
    public void afterEach() {
        try {
            server.deleteUser(testUser, true);
        } catch (Exception ignore) {
        }
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

