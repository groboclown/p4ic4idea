package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.LOGTAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.LogTailOptions;

/**
 * Tests the LogTailDelegator.
 */
public class LogTailDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The log tail delegator. */
    private LogTailDelegator logTailDelegator;
    /** Empty matcher. */
    private static final CommandLineArgumentMatcher EMPTY_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});
    /** Options matcher. */
    private static final CommandLineArgumentMatcher OPTION_MATCHER = new CommandLineArgumentMatcher(
            new String[] {"-b1", "-s2", "-m1"});
    
    /** Example value. */
    private static final String FILE = "/tmp/log.txt";
    
    /** Example value. */
    private static final String ERROR = "Simulated error";
    
    /** Example value. */
    private static final String DATA = "Perforce Server starting";
    
    /** Example value. */
    private static final String OFFSET = "1234";
    
    /** Example value. */
    private static final String LOW_OFFSET = "-1";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        logTailDelegator = new LogTailDelegator(server);
    }

    /**
     * Sets up the server with an exception.
     *
     * @param exceptionClass
     *            the new up
     * @throws ConnectionException
     *             the connection exception
     * @throws AccessException
     *             the access exception
     * @throws RequestException
     *             the request exception
     */
    private void setUpException(final Class<? extends P4JavaException> exceptionClass)
            throws ConnectionException, AccessException, RequestException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), any(String[].class), any(Map.class)))
                .thenThrow(exceptionClass);
    }
    
    /**
     * Builds the valid result map.
     *
     * @param offset the offset
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap(final String offset) {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("file", FILE);
        result.put("data", DATA);
        results.add(result);
        result = new HashMap<>();
        result.put("offset", offset);
        results.add(result);
        return results;
    }
    
    /**
     * Builds the error result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildErrorResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", ERROR);
        result.put("code0", "807672853");
        results.add(result);
        return results;
    }
    
    /**
     * Assert log tail.
     *
     * @param logTail the log tail
     */
    private void assertLogTail(final ILogTail logTail) {
        assertEquals(FILE, logTail.getLogFilePath());
        assertEquals(Long.valueOf(OFFSET).longValue(), logTail.getOffset());
        assertNotNull(logTail.getData());
        assertEquals(1, logTail.getData().size());
        assertEquals(DATA, logTail.getData().get(0));
    }

    /**
     * Test log tail connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testLogTailConnectionException() throws P4JavaException {
        setUpException(ConnectionException.class);
        logTailDelegator.getLogTail(new LogTailOptions());
    }

    /**
     * Test log tail access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testLogTailAccessException() throws P4JavaException {
        setUpException(AccessException.class);
        logTailDelegator.getLogTail(new LogTailOptions());
    }

    /**
     * Test log tail request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLogTailRequestException() throws P4JavaException {
        setUpException(RequestException.class);
        logTailDelegator.getLogTail(new LogTailOptions());
    }

    /**
     * Test log tail p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testLogTailP4JavaException() throws P4JavaException {
        setUpException(P4JavaException.class);
        logTailDelegator.getLogTail(new LogTailOptions());
    }

    /**
     * Test log tail.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testLogTail() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap(OFFSET));
        ILogTail logTail = logTailDelegator.getLogTail(new LogTailOptions());
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertLogTail(logTail);
    }
    
    /**
     * Test log tail options.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testLogTailOptions() throws P4JavaException {
        LogTailOptions options =
                new LogTailOptions().setBlockSize(1L).setMaxBlocks(1).setStartingOffset(2L);
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(OPTION_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap(OFFSET));
        ILogTail logTail = logTailDelegator.getLogTail(options);
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(OPTION_MATCHER), eq(null));
        assertLogTail(logTail);
    }
    
    /**
     * Test log tail error.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testLogTailError() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(buildErrorResultMap());
        ILogTail logTail = logTailDelegator.getLogTail(new LogTailOptions());
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertLogTail(logTail);
    }
    
    /**
     * Test log tail low offset. LogTail is not returned if offset < 0.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testLogTailLowOffset() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap(LOW_OFFSET));
        ILogTail logTail = logTailDelegator.getLogTail(new LogTailOptions());
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertNull(logTail);
    }
    
    /**
     * Test log tail null results.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testLogTailNullResults() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(null);
        ILogTail logTail = logTailDelegator.getLogTail(new LogTailOptions());
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertNull(logTail);
    }
    
    /**
     * Test log tail empty results.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testLogTailEmptyResults() throws P4JavaException {
        when(server.execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null)))
                .thenReturn(new ArrayList<>());
        ILogTail logTail = logTailDelegator.getLogTail(new LogTailOptions());
        verify(server).execMapCmdList(eq(LOGTAIL.toString()), argThat(EMPTY_MATCHER), eq(null));
        assertNull(logTail);
    }
}