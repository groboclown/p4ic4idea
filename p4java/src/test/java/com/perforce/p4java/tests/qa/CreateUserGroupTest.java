package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(JUnitPlatform.class)
public class CreateUserGroupTest {
    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private String groupName = "group1";

    @BeforeAll
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");
    }

    @DisplayName("attempt to create a group")
    @Test
    public void basicUsage() throws Exception {
        IUserGroup userGroup = createUserGroup(new UserGroup());
        verifyCreatedUserGroup(userGroup);
    }

    private void verifyCreatedUserGroup(IUserGroup firstUserGroup) {
        assertThat("wrong name", firstUserGroup.getName(), is(groupName));
        List<String> usersInGroup = firstUserGroup.getUsers();
        assertThat("wrong user list", usersInGroup.size(), is(1));
        assertThat("wrong user list", usersInGroup.get(0), is(ts.getUser()));
    }

    private IUserGroup createUserGroup(IUserGroup group) throws P4JavaException {
        ArrayList<String> users = newArrayList();
        users.add(ts.getUser());
        group.setUsers(users);
        group.setName(groupName);
        group.setOwners(users);

        String result = server.createUserGroup(group, null);
        assertThat(result, containsString("created"));
        List<IUserGroup> groups = server.getUserGroups(null, null);
        assertThat("wrong number of groups", groups.size(), is(1));
        return groups.get(0);
    }

    @DisplayName("verify that the password timeout is properly set")
    @Test
    public void passwordTimeout() throws Exception {
        IUserGroup userGroup = createUserGroup(1234);
        assertThat("wrong timeout", userGroup.getPasswordTimeout(), is(1234));
        verifyCreatedUserGroup(userGroup);
    }

    private IUserGroup createUserGroup(int passwordTimeOut) throws P4JavaException {
        IUserGroup group = new UserGroup();
        group.setPasswordTimeout(passwordTimeOut);
        return createUserGroup(group);
    }

    @AfterAll
    public static void afterClass() {
        h.after(ts);
    }

    @AfterEach
    public void reset() {
        try {
            IUserGroup group = new UserGroup();
            group.setName(groupName);
            server.deleteUserGroup(group);
        } catch (Exception ignored) {
        }
    }
}

