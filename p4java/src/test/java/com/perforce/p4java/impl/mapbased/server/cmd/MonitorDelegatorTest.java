package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.MONITOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetServerProcessesOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 4/10/2016
 */
public class MonitorDelegatorTest {
    private static final String PROCESS_USER_NAME = "Tim";
    private static final String DEFAULT_GET_PROCESS_ARGUMENT = "show";
    private MonitorDelegator monitorDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IOptionsServer server;

    private final String[] getCmdArguments = {DEFAULT_GET_PROCESS_ARGUMENT};

    private String[] expectedOptionSpecs = {"-a", "-e", "-l"};
    private final List<String> optionList = Arrays.asList(expectedOptionSpecs);
    private final String[] procCmdArguments = ArrayUtils.add(expectedOptionSpecs, 0, DEFAULT_GET_PROCESS_ARGUMENT);
    private GetServerProcessesOptions options;

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        monitorDelegator = new MonitorDelegator(server);

        resultMap = mock(Map.class);
        when(resultMap.get(MapKeys.USER_LC_KEY)).thenReturn(PROCESS_USER_NAME);

        resultMaps = List.of(resultMap);

        options = mock(GetServerProcessesOptions.class);
        when(options.isImmutable()).thenReturn(true);
        when(options.getOptions()).thenReturn(optionList);
    }


    /**
     * Expected thrown exception (eg. <code>ConnectionException</code>) when any code throws it.
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsConnectionExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        String errorMessage = "connection to P4 server failure";
        ConnectionException toBeThrown = new ConnectionException(errorMessage);
        thrown.expect(ConnectionException.class);
        thrown.expectMessage(errorMessage);

        doThrow(toBeThrown).when(server)
                .execMapCmdList(eq(MONITOR.toString()), any(String[].class), eq(null));
        monitorDelegator.getServerProcesses();
    }

    /**
     * Expected <code>RequestException</code>
     * when any code throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownRequestExceptionWhenInnerMethodCallThrowsP4JavaException()
            throws Exception {

        thrown.expect(RequestException.class);

        doThrow(P4JavaException.class).when(server)
                .execMapCmdList(eq(MONITOR.toString()), any(String[].class), eq(null));
        monitorDelegator.getServerProcesses();
    }

    /**
     * Expected return non empty server process list
     *
     * @throws Exception
     */
    @Test
    public void getShouldReturnNonEmptyServerProcessList() throws Exception {
        //given
        when(server.execMapCmdList(eq(MONITOR.toString()), eq(getCmdArguments), eq(null))).thenReturn(resultMaps);
        //when
        List<IServerProcess> serverProcesses = monitorDelegator.getServerProcesses();
        //then
        assertThat(serverProcesses.size(), is(1));
        assertThat(serverProcesses.get(0).getUserName(), is(PROCESS_USER_NAME));
    }

    /**
     * Expected return non empty server process list
     *
     * @throws Exception
     */
    @Test
    public void procShouldReturnNonEmptyServerProcessList() throws Exception {
        //given
        when(server.execMapCmdList(eq(MONITOR.toString()), eq(procCmdArguments), eq(null)))
                .thenReturn(resultMaps);
        //when
        List<IServerProcess> serverProcesses = monitorDelegator.getServerProcesses(options);
        //then
        assertThat(serverProcesses.size(), is(1));
        assertThat(serverProcesses.get(0).getUserName(), is(PROCESS_USER_NAME));
        verify(resultMap, times(1)).get(MapKeys.USER_LC_KEY);
        verify(resultMap, times(1)).get(MapKeys.COMMAND_LC_KEY);
    }
}