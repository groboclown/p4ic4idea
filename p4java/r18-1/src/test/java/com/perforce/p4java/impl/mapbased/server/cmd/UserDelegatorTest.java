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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.UpdateUserOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
@RunWith(NestedRunner.class)
public class UserDelegatorTest extends AbstractP4JavaUnitTest {
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
    public void beforeEach() {
        server = mock(Server.class);
        userDelegator = new UserDelegator(server);

        resultMap = mock(Map.class);
        when(resultMap.get("User")).thenReturn(USER_NAME);
        resultMaps = Lists.newArrayList(resultMap);
        user = mock(IUser.class);
        when(user.getLoginName()).thenReturn(USER_NAME);
        opts = new UpdateUserOptions(FORCE);
    }

    /**
     * Test createUser()
     */
    public class TestCreateUser {
        private final String[] createCmdOptions = {FORCE};
        private final String[] createCmdArguments = ArrayUtils.add(createCmdOptions, "-i");

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            when(server.execMapCmdList(
                    eq(USER.toString()),
                    eq(createCmdArguments),
                    any(Map.class))).thenReturn(resultMaps);
        }

        /**
         * Test createUser(user, force)
         */
        public class WhenUserForceGiven {
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            /**
             * Expected throws <code>NullPointerException</code> when user is null
             *
             * @throws Exception
             */
            @Test
            public void shouldThrowsNullPointerExceptionWhenUserIsNull() throws Exception {
                //then
                thrown.expect(NullPointerException.class);
                //given
                user = null;
                //then
                userDelegator.createUser(user, false);
            }

            /**
             * Expected return not blank created user name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankCreatedUserName() throws Exception {
                //given
                givenInfoMessageCode(resultMap, "createUser", USER_NAME);
                //when
                String userName = userDelegator.createUser(user, force);
                //then
                assertThat(userName, is(USER_NAME));
            }
        }

        /**
         * Test createUser(user, updateUserOptions)
         */
        public class WhenUserUpdateUserOptionsGiven {
            /**
             * Expected return not blank created user name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankCreatedUserName() throws Exception {
                //given
                givenInfoMessageCode(resultMap, "createUser", USER_NAME);
                //when
                String userName = userDelegator.createUser(user, opts);
                //then
                assertThat(userName, is(USER_NAME));
            }
        }
    }

    /**
     * Test updateUser()
     */
    public class TestUpdateUser {
        private final String[] updateCmdOptions = {FORCE};
        private final String[] updateCmdArguments = ArrayUtils.add(updateCmdOptions, "-i");

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            when(server.execMapCmdList(
                    eq(USER.toString()),
                    eq(updateCmdArguments),
                    any(Map.class))).thenReturn(resultMaps);
        }

        /**
         * Test updateUser(user, force)
         */
        public class WhenUserForceGiven {
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

                updateUserExpectedThrowsException(
                        AccessException.class,
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

        /**
         * Test updateUser(user, updateUserOptions)
         */
        public class WhenUserUpdateUserOptionsGiven {
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            /**
             * Expected throws <code>NullPointerException</code> when user is null
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownNullPointerExceptionWhenUserIsNull() throws Exception {
                //given
                user = null;
                thrown.expect(NullPointerException.class);

                //when
                userDelegator.updateUser(user, opts);
            }

            /**
             * Expected throws <code>IllegalArgumentException</code> when user login name is blank
             *
             * @throws Exception
             */
            @Test
            public void shouldThrowsIllegalArgumentExceptionWhenUserLoginNameIsBlank() throws Exception {
                thrown.expect(IllegalArgumentException.class);
                //given
                when(user.getLoginName()).thenReturn(EMPTY);

                //when
                userDelegator.updateUser(user, opts);
            }

            /**
             * Expected return non blank updated user name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankUpdatedUserName() throws Exception {
                //given
                givenInfoMessageCode(resultMap, "updateUser", USER_NAME);
                //when
                String updateUser = userDelegator.updateUser(user, opts);
                //then
                assertThat(updateUser, is(USER_NAME));
            }
        }
    }

    /**
     * Test deleteUser()
     */
    public class TestDeleteUser {
        private final String[] deleteCmdOptions = {FORCE};
        private final String[] deleteCmdArguments = ArrayUtils.addAll(
                deleteCmdOptions,
                "-d",
                USER_NAME);

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            when(server.execMapCmdList(eq(USER.toString()), eq(deleteCmdArguments), any(Map.class)))
                    .thenReturn(resultMaps);
        }

        /**
         * Test deleteUser(userName, force)
         */
        public class WhenUserNameUpdateForceGiven {
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
            public void shouldThrownConnectionExceptionWhenInnerMethodCallThrowsIt()
                    throws Exception {

                deleteUserExpectedThrowsException(
                        ConnectionException.class,
                        ConnectionException.class);
            }

            /**
             * Expected throws <code>AccessException</code> when inner method call throws it
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownAccessExceptionWhenInnerMethodCallThrowsIt()
                    throws Exception {

                deleteUserExpectedThrowsException(
                        AccessException.class,
                        AccessException.class);
            }

            /**
             * Expected throws <code>RequestException</code> when inner method call throws it
             *
             * @throws Exception
             */
            @Test
            public void shouldThrownRequestExceptionWhenInnerMethodCallThrowsIt()
                    throws Exception {

                deleteUserExpectedThrowsException(
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
            public void shouldThrownRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
                    throws Exception {

                deleteUserExpectedThrowsException(
                        P4JavaException.class,
                        RequestException.class);
            }

            /**
             * Expected return non blank deleted user name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankDeletedUserName() throws Exception {
                //given
                givenInfoMessageCode(resultMap, "deleteUser", USER_NAME);
                //when
                String deleteUser = userDelegator.deleteUser(USER_NAME, force);

                //then
                assertThat(deleteUser, is(USER_NAME));
            }

            private void deleteUserExpectedThrowsException(
                    Class<? extends P4JavaException> thrownException,
                    Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

                thrown.expect(expectedThrows);

                doThrow(thrownException).when(server).execMapCmdList(
                        eq(USER.toString()),
                        eq(deleteCmdArguments),
                        any(Map.class));
                userDelegator.deleteUser(USER_NAME, force);
            }
        }

        /**
         * Test deleteUser(userName, updateUserOptions)
         */
        public class WhenUserNameUpdateUserOptionsGiven {
            /**
             * Rule for expected exception verification
             */
            @Rule
            public ExpectedException thrown = ExpectedException.none();


            /**
             * Expected throws <code>IllegalArgumentException</code> when user name is blank
             * @throws Exception
             */
            @Test
            public void shouldThrownIllegalArgumentExceptionAsUserNameIsBlank()
                    throws Exception {

                thrown.expect(IllegalArgumentException.class);
                //when
                userDelegator.deleteUser(EMPTY, opts);
            }

            /**
             * Expected return non blank deleted user name
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonBlankDeletedUserName() throws Exception {
                //given
                givenInfoMessageCode(resultMap, "deleteUser", USER_NAME);
                //when
                String deleteUser = userDelegator.deleteUser(USER_NAME, opts);

                //then
                assertThat(deleteUser, is(USER_NAME));
            }
        }
    }

    /**
     * Test getUser()
     */
    public class TestGetUser {
        private final String[] getCmdArguments = {"-o", USER_NAME};

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            when(server.execMapCmdList(eq(USER.toString()), eq(getCmdArguments), any(Map.class)))
                    .thenReturn(resultMaps);
        }

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
}