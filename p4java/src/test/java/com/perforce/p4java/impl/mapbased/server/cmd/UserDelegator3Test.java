package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.UpdateUserOptions;
import com.perforce.p4java.server.IOptionsServer;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.USER;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
public class UserDelegator3Test {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";

    private static final String FORCE = "-f";
    private static final String USER_NAME = "Sean";
    private UserDelegator userDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IUser user;
    private UpdateUserOptions opts;
    private boolean force = true;
    private IOptionsServer server;

    private static void givenInfoMessageCode(
            final Map<String, Object> map, String mapKey,
            final String expectedValue) {

        when(map.get(FMT0)).thenReturn("%" + mapKey + "%");
        when(map.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(map.get(mapKey)).thenReturn(expectedValue);
    }

    private static void givenNonInfoMessageCode(final Map<String, Object> map) {
        when(map.get(E_FAILED)).thenReturn(EMPTY);
        when(map.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
    }

    private static void givenExistClientOrLabelOrUser(final Map<String, Object> map) {
        when(map.containsKey(MapKeys.UPDATE_KEY)).thenReturn(true);
        when(map.containsKey(MapKeys.ACCESS_KEY)).thenReturn(true);
    }

    private static void givenNotExistClientOrLabelOrUser(final Map<String, Object> map) {
        when(map.containsKey(MapKeys.UPDATE_KEY)).thenReturn(false);
        when(map.containsKey(MapKeys.ACCESS_KEY)).thenReturn(false);
    }

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        userDelegator = new UserDelegator(server);

        resultMap = mock(Map.class);
        when(resultMap.get("User")).thenReturn(USER_NAME);
        resultMaps = List.of(resultMap);
        user = mock(IUser.class);
        when(user.getLoginName()).thenReturn(USER_NAME);
        opts = new UpdateUserOptions(FORCE);

        when(server.execMapCmdList(
                eq(USER.toString()),
                eq(updateCmdArguments),
                any(Map.class))).thenReturn(resultMaps);
    }


    private final String[] updateCmdOptions = {FORCE};
    private final String[] updateCmdArguments = ArrayUtils.add(updateCmdOptions, "-i");

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsConnectionExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        updateUserExpectedThrowsException(
                ConnectionException.class,
                ConnectionException.class);
    }

    /**
     * Expected throws <code>AccessException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsAccessExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        // p4ic4idea: use a public, non-abstract class with default constructor
        updateUserExpectedThrowsException(
                AccessException.AccessExceptionForTests.class,
                AccessException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsRequestExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        updateUserExpectedThrowsException(
                RequestException.class,
                RequestException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws
     * <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
            throws Exception {

        updateUserExpectedThrowsException(
                P4JavaException.class,
                RequestException.class);
    }

    private void updateUserExpectedThrowsException(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        //then
        thrown.expect(expectedThrows);
        //given
        doThrow(thrownException).when(server).execMapCmdList(
                eq(USER.toString()),
                eq(updateCmdArguments),
                any(Map.class));

        //when
        userDelegator.updateUser(user, force);
    }

    /**
     * Expected return not blank updated user name
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonBlankUpdatedUserName() throws Exception {
        //given
        givenInfoMessageCode(resultMap, "updateUser", USER_NAME);
        //when
        String updateUser = userDelegator.updateUser(user, force);
        //then
        assertThat(updateUser, is(USER_NAME));
    }
}