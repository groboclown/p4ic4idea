package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
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
public class UsersDelegator2Test {
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
    }

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Expected throws <code>AccessException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsAccessExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        // p4ic4idea: use a public, non-abstract class with default constructor
        executeAndExpectedThrowsException(
                AccessException.AccessExceptionForTests.class,
                AccessException.class);
    }

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsConnectionExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        executeAndExpectedThrowsException(
                ConnectionException.class,
                ConnectionException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsRequestExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        executeAndExpectedThrowsException(RequestException.class, RequestException.class);
    }

    /**
     * Expected throws <code>AccessException</code> when inner method
     * call throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
            throws Exception {

        executeAndExpectedThrowsException(P4JavaException.class, RequestException.class);
    }

    private void executeAndExpectedThrowsException(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        thrown.expect(expectedThrows);

        doThrow(thrownException).when(server).execMapCmdList(
                eq(USERS.toString()),
                eq(CMD_ARGUMENTS),
                eq(null));
        usersDelegator.getUsers(USER_LIST, MAX_USERS);
    }

    /**
     * Expected return non empty user list
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyUserList() throws Exception {
        //when
        List<IUserSummary> users = usersDelegator.getUsers(USER_LIST, MAX_USERS);

        //then
        assertThat(users.size(), is(2));
        assertThat(users.get(0).getLoginName(), is(TEST_USERS[0]));
        assertThat(users.get(1).getLoginName(), is(TEST_USERS[1]));
    }
}