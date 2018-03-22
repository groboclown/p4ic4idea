package com.perforce.p4java.tests.dev.unit.bug.r162;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * @author Sean Shou
 * @since 20/01/2017
 */
@RunWith(JUnitPlatform.class)
public class Job089581Test extends P4JavaTestCase {
    /*
    * group      | max results | max scan rows | max lock time | login time out   | password time out|
    * -----------|-------------|---------------|---------------|------------------|------------------|
    * testgroup  |     100     |    unlimited  |    unset      | 0 days 12:00:00        unset
    *
    */
    private static final String EXIST_TEST_GROUP_NAME = "testgroup";
    private static final int EXIST_TEST_GROUP_LOGIN_TIME_OUT = 12 * 60 * 60; // seconds
    private static final int EXPECTED_MAX_RESULTS = 100;
    private static final int EXPECTED_MAX_SCAN_ROWS = IUserGroup.UNLIMITED;
    private static final int EXPECTED_MAX_LOCK_TIME = IUserGroup.UNSET;
    private static final int EXPECTED_LOGIN_TIME_OUT = IUserGroup.UNLIMITED;
    private static final int EXPECTED_PASSWORD_TIME_OUT = IUserGroup.UNSET;
    private static final String P4D_ADMIN_USER = "p4java";
    private static final String P4D_ADMIN_USER_PASSWD = "p4java";


    @BeforeAll
    public static void beforeAll() throws Exception {
        defaultBeforeAll();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        defaultAfterAll();
    }

    @Test
    public void testGetUserGroups() throws Exception {
        List<IUserGroup> userGroups = server.getUserGroups(EXIST_TEST_GROUP_NAME, new GetUserGroupsOptions().setDisplayValues(true));
        assertThat(userGroups.size(), is(1));
        IUserGroup userGroup = userGroups.get(0);

        verifyUserGroup(userGroup, EXIST_TEST_GROUP_NAME, EXIST_TEST_GROUP_LOGIN_TIME_OUT);
    }

    @Test
    public void testGetUserGroup() throws Exception {
        IUserGroup userGroup = server.getUserGroup(EXIST_TEST_GROUP_NAME);
        assertThat(userGroup, notNullValue());

        verifyUserGroup(userGroup, EXIST_TEST_GROUP_NAME, EXIST_TEST_GROUP_LOGIN_TIME_OUT);
    }

    @Test
    public void testCreateGroupGivenUnlimitedLoginTimeOut() throws Exception {
        testCreateGroup(EXPECTED_LOGIN_TIME_OUT);
    }

    @Test
    public void testCreateGroupGiven12HoursLoginTimeOut() throws Exception {
        testCreateGroup(EXIST_TEST_GROUP_LOGIN_TIME_OUT);
    }

    private void testCreateGroup(int loginTimeOut) throws Exception {
        loginAsAdminUser();

        String groupName = null;
        IUserGroup userGroup = null;
        try {
            String expectedGroupName = EXIST_TEST_GROUP_NAME + System.currentTimeMillis();
            groupName = server.createUserGroup(populateUserGroup(expectedGroupName, loginTimeOut));

            userGroup = server.getUserGroup(expectedGroupName);
            verifyUserGroup(userGroup, expectedGroupName, loginTimeOut);
        } finally {
            try {
                if (userGroup != null) {
                    server.deleteUserGroup(userGroup);
                } else {
                    if (isNotBlank(groupName)) {
                        UserGroup toDeleteUserGroup = new UserGroup();
                        toDeleteUserGroup.setName(groupName);
                        server.deleteUserGroup(toDeleteUserGroup);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    private void loginAsAdminUser() throws Exception {
        server.setUserName(P4D_ADMIN_USER);
        server.login(P4D_ADMIN_USER_PASSWD, new LoginOptions());
    }

    private void verifyUserGroup(IUserGroup userGroup, String expectedGroupName, int expectedLoginTimeOut) {
        assertThat(userGroup.getName(), is(expectedGroupName));
        assertThat(userGroup.getMaxResults(), is(EXPECTED_MAX_RESULTS));
        assertThat(userGroup.getMaxScanRows(), is(EXPECTED_MAX_SCAN_ROWS));
        assertThat(userGroup.getMaxLockTime(), is(EXPECTED_MAX_LOCK_TIME));
        assertThat(userGroup.getTimeout(), is(expectedLoginTimeOut));
        assertThat(userGroup.getPasswordTimeout(), is(EXPECTED_PASSWORD_TIME_OUT));
    }

    private IUserGroup populateUserGroup(String groupName, int loginTimeOut) throws Exception {
        List<String> groupOwners = new ArrayList<>();
        groupOwners.add(getUserName());
        groupOwners.add(getSuperUserName());

        List<String> groupUsers = new ArrayList<>();
        groupUsers.add(getUserName());
        groupUsers.add(getSuperUserName());
        groupUsers.add(getInvalidUserName());


        IUserGroup newUserGroup = new UserGroup();
        newUserGroup.setName(groupName);
        newUserGroup.setOwners(groupOwners);
        newUserGroup.setUsers(groupUsers);

        newUserGroup.setMaxResults(EXPECTED_MAX_RESULTS);
        newUserGroup.setMaxScanRows(IUserGroup.UNLIMITED);
        newUserGroup.setMaxLockTime(IUserGroup.UNSET);
        newUserGroup.setSubGroup(false);
        newUserGroup.setTimeout(loginTimeOut);
        newUserGroup.setServer(server);
        newUserGroup.setPasswordTimeout(IUserGroup.UNSET);

        return newUserGroup;
    }
}
