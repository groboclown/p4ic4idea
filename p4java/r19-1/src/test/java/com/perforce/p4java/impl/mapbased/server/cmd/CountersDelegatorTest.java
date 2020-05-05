package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.server.CmdSpec.COUNTERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.server.IOptionsServer;

public class CountersDelegatorTest extends AbstractP4JavaUnitTest {
    private CountersDelegator countersDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private Method parseCounterCommandResultMaps = getPrivateMethod(CountersDelegator.class,
            "parseCounterCommandResultMaps", List.class, Function.class);
    @SuppressWarnings("rawtypes")
    private Function<Map, Boolean> errorOrInfoStringCheckFunc;

    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(IOptionsServer.class);
        countersDelegator = new CountersDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = new ArrayList<Map<String, Object>>();
        resultMaps.add(resultMap);

        mockFileSpecs = new ArrayList<IFileSpec>();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);


        errorOrInfoStringCheckFunc = mock(Function.class);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementaion is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testConnectionException() throws P4JavaException {
        propagateGetCountersException(ConnectionException.class, ConnectionException.class);
        propagateGetCountersExceptionWithOptions(ConnectionException.class,
                ConnectionException.class);
    }

    /**
     * Test that an access exception thrown by the underlying server
     * implementation is correctly propagated as an access exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testAccessException() throws P4JavaException {
        propagateGetCountersException(AccessException.class, AccessException.class);
        propagateGetCountersExceptionWithOptions(AccessException.class, AccessException.class);
    }

    /**
     * Test that a request exception thrown by the underlying server
     * implementation is correctly propagated as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testRequestException() throws P4JavaException {
        propagateGetCountersException(RequestException.class, RequestException.class);
        propagateGetCountersExceptionWithOptions(RequestException.class, RequestException.class);
    }

    /**
     * Test that a P4JavaException thrown by the underlying server
     * implementation is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testP4JavaException() throws Exception {
        propagateGetCountersException(P4JavaException.class, RequestException.class);
        propagateGetCountersExceptionWithOptions(P4JavaException.class, P4JavaException.class);
    }

    /**
     * Simulate an exception being thrown from the underlying server back to the
     * counters delegator and verify that it propagates the
     * appropriateexception.
     * 
     * @param thrownException
     *            the exception thrown by the server
     * @param expectedThrows
     *            the exception expected from the delegator
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    private void propagateGetCountersExceptionWithOptions(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        

        try {
            when(server.execMapCmdList(eq(COUNTERS.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] {})), eq((Map<String,Object>)null)))
                            .thenThrow(thrownException);
            countersDelegator.getCounters(new GetCountersOptions());
            fail("Should have thown");
        } catch (Throwable e) {
            assertEquals(expectedThrows, e.getClass());
        }
    }

    /**
     * Simulate an exception being thrown from the underlying server back to the
     * counters delegator and verify that it propagates the
     * appropriateexception.
     * 
     * @param thrownException
     *            the exception thrown by the server
     * @param expectedThrows
     *            the exception expected from the delegator
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    private void propagateGetCountersException(Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        try {
            when(server.execMapCmdList(eq(COUNTERS.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] {})), eq((Map<String,Object>)null)))
                            .thenThrow(thrownException);
            countersDelegator.getCounters();
            fail("Should have thown");
        } catch (Throwable e) {
            assertEquals(expectedThrows, e.getClass());
        }
    }

    /**
     * Test that a known counter in the server response is returned in the list
     * of counters when a getCounters request is made with a default option set.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testGetCountersWithCounterOptions() throws P4JavaException {
        // given
        String mockValue = "mockValue";
        String mockName = "counter";
        when(server.execMapCmdList(eq(COUNTERS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] {})),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get(VALUE)).thenReturn(mockValue);
        when(resultMap.get("counter")).thenReturn(mockName);
        // when
        Map<String, String> counters = countersDelegator.getCounters(new CounterOptions());
        // then
        assertThat(counters.size(), is(1));
        assertThat(counters.get(mockName), is(mockValue));
    }

    /**
     * Test that a known counter in the server response is returned in the list
     * of counters when a simple getCounters request is made.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testGetCounters() throws P4JavaException {
        // given
        String mockValue = "mockValue";
        String mockName = "counter";
        when(server.execMapCmdList(eq(COUNTERS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] {})),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get(VALUE)).thenReturn(mockValue);
        when(resultMap.get("counter")).thenReturn(mockName);
        // when
        Map<String, String> counters = countersDelegator.getCounters();
        // then
        assertThat(counters.size(), is(1));
        assertThat(counters.get(mockName), is(mockValue));
    }

    /**
     * Test that a null result map from the server leads to an empty counter
     * list.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNullResultMaps() throws P4JavaException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        // given
        Map<String, String> actual = (Map<String, String>) parseCounterCommandResultMaps
                .invoke(countersDelegator, null, errorOrInfoStringCheckFunc);
        // then
        assertEquals(0, actual.size());
    }

    /**
     * Test that an empty result set from the server leads to an empty counters
     * list.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyResultMaps()
            throws P4JavaException, InvocationTargetException, IllegalAccessException {
        resultMaps = new ArrayList<Map<String, Object>>();
        Map<String, String> actual = (Map<String, String>) parseCounterCommandResultMaps
                        .invoke(countersDelegator, resultMaps, errorOrInfoStringCheckFunc);

        assertEquals(0, actual.size());
    }
}
