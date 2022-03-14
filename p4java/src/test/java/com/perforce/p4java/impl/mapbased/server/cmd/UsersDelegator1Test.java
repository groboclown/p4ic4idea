package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetUsersOptions;
import com.perforce.p4java.server.IOptionsServer;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.server.CmdSpec.USERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
public class UsersDelegator1Test {
    private static final int MAX_USERS = 20;
    private static final String[] CMD_OPTIONS = {"-m" + MAX_USERS};
    private static final String[] TEST_USERS = {"p4javaTest_win", "p4javaTest_unix"};
    private static final List<String> USER_LIST = Arrays.asList(TEST_USERS);
    private static final String[] CMD_ARGUMENTS = ArrayUtils.addAll(CMD_OPTIONS, TEST_USERS);

    private UsersDelegator usersDelegator;
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        usersDelegator = new UsersDelegator(server);
        Map<String, Object> resultMap = mock(Map.class);
        when(resultMap.get("User")).thenReturn(TEST_USERS[0]);
        Map<String, Object> resultMap2 = mock(Map.class);
        when(resultMap2.get("User")).thenReturn(TEST_USERS[1]);

        List<Map<String, Object>> resultMaps = List.of(resultMap, resultMap2);
        when(server.execMapCmdList(eq(USERS.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(resultMaps);

        opts = new GetUsersOptions(CMD_OPTIONS);
    }

    private GetUsersOptions opts;

    /**
     * Expected return empty user list when command result maps is null
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenResultMapsIsNull() throws Exception {
        //given
        when(server.execMapCmdList(eq(USERS.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(null);
        //when
        List<IUserSummary> users = usersDelegator.getUsers(USER_LIST, opts);

        //then
        assertThat(users.size(), is(0));
    }

    /**
     * Expected return non emtpy user list
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyUserList() throws Exception {
        //when
        List<IUserSummary> users = usersDelegator.getUsers(USER_LIST, opts);

        //then
        assertThat(users.size(), is(TEST_USERS.length));
        assertThat(users.get(0).getLoginName(), is(TEST_USERS[0]));
        assertThat(users.get(1).getLoginName(), is(TEST_USERS[1]));
    }
}