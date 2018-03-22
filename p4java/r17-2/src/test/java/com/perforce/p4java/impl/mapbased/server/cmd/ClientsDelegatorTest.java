package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.MapKeys.CLIENT_KEY;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.CORE_AUTH_FAIL_STRING_1;
import static com.perforce.p4java.server.CmdSpec.CLIENTS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetClientsOptions;

/**
 * @author Sean Shou
 * @since 15/09/2016
 */
@RunWith(NestedRunner.class)
public class ClientsDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private ClientsDelegator clientsDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        clientsDelegator = new ClientsDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);
    }

    /**
     * Test getClients()
     */
    public class TestGetClients {
        /**
         * Test getClients() by <code>GetClientsOptions</code>.
         */
        public class WhenGetClientsOptionsGiven {
            private final String[] cmdArguments = {"-esvr-dev-rel*"};
            private GetClientsOptions clientsOptions;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() throws Exception {
                clientsOptions = new GetClientsOptions(cmdArguments);
                when(server.execMapCmdList(eq(CLIENTS.toString()), eq(cmdArguments), any()))
                        .thenReturn(resultMaps);
            }

            /**
             * Test get clients and it expected thrown  <code>P4JavaError</code> as command return empty result maps.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = P4JavaError.class)
            public void shouldThrowP4JavaErrorWhenResultMapsIsNull() throws Exception {
                when(server.execMapCmdList(eq(CLIENTS.toString()), eq(cmdArguments), any()))
                        .thenReturn(null);

                clientsDelegator.getClients(clientsOptions);
            }

            /**
             * Test get clients and it expected thrown  <code>RequestException</code> as error message is found in command result maps.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenErrorMessageResultMapGiven() throws Exception {
                givenErrorMessageCode();

                clientsDelegator.getClients(clientsOptions);
            }

            private void givenErrorMessageCode() {
                when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
                when(resultMap.get(FMT0)).thenReturn(CORE_AUTH_FAIL_STRING_1);
            }

            /**
             * Test get clients and it expected non-empty client list.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test
            public void shouldReturnNonEmptyClients() throws Exception {
                List<IClientSummary> clients = clientsDelegator.getClients(clientsOptions);
                assertThat(clients.size(), is(1));
            }
        }

        /**
         * Test getClients() by 'userName, nameFilter, maxResults' arguments.
         */
        public class WhenUserNameNameFilterAndMaxResultsGiven {
            private String userName;
            private String nameFilter;
            private int maxResults;
            private int serverVersion;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() {
                userName = EMPTY;
                nameFilter = "Date_Modified>453470485";
                maxResults = 10;
                serverVersion = 20161;
            }

            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected thrown <code>RequestException</code> as 'user restrictions' argument is not support in server version less than 20062.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenUserRestrictionsIsNotSupportedByServerVersionLessThan20062() throws Exception {
                userName = "sean";
                serverVersion = 20051;
                checkSupportFunctionDependOnTheServerVersion(
                        serverVersion,
                        userName,
                        nameFilter,
                        maxResults);
            }

            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected thrown <code>RequestException</code> as 'max limit' argument is not support in server version less than 20061.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenMaxLimitIsNotSupportedByServerVersionLessThan20061() throws Exception {
                serverVersion = 20051;
                checkSupportFunctionDependOnTheServerVersion(
                        serverVersion,
                        userName,
                        nameFilter,
                        maxResults);
            }


            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected thrown <code>RequestException</code> as 'name filter' argument is not support in server version less than 20081.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenNameFilterIsNotSupportedByServerVersionLessThan20081() throws Exception {
                maxResults = -1;
                serverVersion = 20071;

                checkSupportFunctionDependOnTheServerVersion(
                        serverVersion,
                        userName,
                        nameFilter,
                        maxResults);
            }


            private void checkSupportFunctionDependOnTheServerVersion(
                    int serverVersion,
                    String userName,
                    String nameFilter,
                    int maxResults) throws Exception {

                when(server.getServerVersion()).thenReturn(serverVersion);
                clientsDelegator.getClients(userName, nameFilter, maxResults);

                verify(server, never()).execMapCmdList(
                        eq(CLIENTS.toString()),
                        any(String[].class),
                        eq(null));
            }

            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected thrown <code>ConnectionException</code> as it will thrown inner method call
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrowConnectionExceptionWhenInnerGetClientsThrowsIt()
                    throws Exception {
                throwExpectedException(ConnectionException.class);
            }

            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected thrown <code>RequestException</code> as it will thrown inner method call
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenInnerGetClientsThrowP4JavaException()
                    throws Exception {
                throwExpectedException(P4JavaException.class);
            }

            private void throwExpectedException(Class<? extends Throwable> toBeThrown)
                    throws Exception {
                userName = "sean";
                when(server.getServerVersion()).thenReturn(serverVersion);
                String[] expectedCmdArgs = {"-m" + maxResults, "-u" + userName, "-e" + nameFilter};
                doThrow(toBeThrown)
                        .when(server)
                        .execMapCmdList(eq(CLIENTS.toString()), eq(expectedCmdArgs), eq(null));

                clientsDelegator.getClients(userName, nameFilter, maxResults);
            }


            /**
             * Test get clients by userName, nameFilter, maxResults command arguments.
             * It's expected return non-null client list.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test
            public void shouldReturnNonEmptyList() throws P4JavaException {
                //given
                String clientSummaryName = "myClient";
                userName = "sean";
                Map<String, Object> resultMap2 = mock(Map.class);
                when(resultMap2.get(CLIENT_KEY)).thenReturn(clientSummaryName);
                resultMaps.add(resultMap2);
                when(server.getServerVersion()).thenReturn(serverVersion);
                String[] expectedCmdArgs = {"-m" + maxResults, "-u" + userName, "-e" + nameFilter};
                when(server.execMapCmdList(
                        eq(CLIENTS.toString()),
                        eq(expectedCmdArgs),
                        eq(null)))
                        .thenReturn(resultMaps);

                //when
                List<IClientSummary> clientSummaries = clientsDelegator.getClients(
                        userName,
                        nameFilter,
                        maxResults);

                //then
                assertThat(clientSummaries.size(), is(2));
                assertThat(clientSummaries.get(1).getName(), is(clientSummaryName));
            }
        }

    }
}