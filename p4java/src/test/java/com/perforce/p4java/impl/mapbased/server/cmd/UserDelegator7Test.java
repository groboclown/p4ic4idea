package com.perforce.p4java.impl.mapbased.server.cmd;

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

import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.UpdateUserOptions;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
public class UserDelegator7Test {
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

        when(server.execMapCmdList(eq(USER.toString()), eq(getCmdArguments), any(Map.class)))
                .thenReturn(resultMaps);
    }

    private final String[] getCmdArguments = {"-o", USER_NAME};


    /**
     * Expected return null when command result maps is null
     * @throws Exception
     */
    @Test
    public void shouldReturnNullWhenResultMapsIsNull() throws Exception {
        //when
        IUser user = userDelegator.getUser(USER_NAME);

        //then
        assertThat(user, nullValue());
    }

    /**
     * Expected return null when It isn't a exist client or label or user
     * @throws Exception
     */
    @Test
    public void shouldReturnNullWhenIsExistClientOrLabelOrUserIsFalse() throws Exception {
        //given
        givenNonInfoMessageCode(resultMap);
        givenNotExistClientOrLabelOrUser(resultMap);

        //when
        IUser user = userDelegator.getUser(USER_NAME);

        //then
        assertThat(user, nullValue());
    }

    /**
     * Expected return non null user
     * @throws Exception
     */
    @Test
    public void shouldReturnNonNullUser() throws Exception {
        //given
        givenNonInfoMessageCode(resultMap);
        givenExistClientOrLabelOrUser(resultMap);

        //when
        IUser user = userDelegator.getUser(USER_NAME);

        //then
        assertThat(user, notNullValue());
        assertThat(user.getLoginName(), is(USER_NAME));
    }
}