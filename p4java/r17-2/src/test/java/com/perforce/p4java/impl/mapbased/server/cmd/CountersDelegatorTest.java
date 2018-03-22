package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.server.CmdSpec.COUNTERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Executable;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.UnitTestWhen;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.CounterOptions;
import com.perforce.p4java.option.server.GetCountersOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.UnitTestGiven;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
@RunWith(JUnitPlatform.class)
public class CountersDelegatorTest extends AbstractP4JavaUnitTest {
    private CountersDelegator countersDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private GetCountersOptions mockGetCountersOptions;
    private CounterOptions mockCounterOptions;
    private Method parseCounterCommandResultMaps = getPrivateMethod(
            CountersDelegator.class, "parseCounterCommandResultMaps",
            List.class, Function.class);
    private Function<Map, Boolean> errorOrInfoStringCheckFunc;

    @BeforeEach
    public void beforeEach() {
        server = mock(IOptionsServer.class);
        countersDelegator = new CountersDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        mockFileSpecs = newArrayList();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);

        mockGetCountersOptions = mock(GetCountersOptions.class);
        mockCounterOptions = mock(CounterOptions.class);

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
        Executable executable = () -> countersDelegator.getCounters(new GetCountersOptions());
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            when(server.execMapCmdList(eq(COUNTERS.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                            .thenThrow(originalException);
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
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
        Executable executable = () -> countersDelegator.getCounters();
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            when(server.execMapCmdList(eq(COUNTERS.toString()),
                    argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                            .thenThrow(originalException);
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
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
                argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                        .thenReturn(resultMaps);
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
                argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                        .thenReturn(resultMaps);
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
        assertThat(actual.size(), is(0));

        checkEmptyCounters(() -> {
        }, () -> (Map<String, String>) parseCounterCommandResultMaps.invoke(countersDelegator,
                null, errorOrInfoStringCheckFunc));
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
        checkEmptyCounters(() -> resultMaps = newArrayList(),
                () -> (Map<String, String>) parseCounterCommandResultMaps
                        .invoke(countersDelegator, resultMaps, errorOrInfoStringCheckFunc));
    }

    /**
     * Check that a result list from getCounters is empty
     * 
     * @param unitTestGiven
     *            The test to run
     * @param unitTestWhen
     *            The condition to intercept
     * @throws P4JavaException
     *             when there is a perforce error
     * @throws InvocationTargetException
     *             when there is a reflection error
     * @throws IllegalAccessException
     *             when there is a reflection access violation
     */
    private void checkEmptyCounters(UnitTestGiven unitTestGiven,
            UnitTestWhen<Map<String, String>> unitTestWhen)
            throws P4JavaException, InvocationTargetException, IllegalAccessException {
        unitTestGiven.given();
        Map<String, String> actual = unitTestWhen.when();
        // then
        assertThat(actual.size(), is(0));
    }
}