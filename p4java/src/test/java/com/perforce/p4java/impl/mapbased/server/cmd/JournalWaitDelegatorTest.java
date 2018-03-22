package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.JOURNALWAIT;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.JournalWaitOptions;

/**
 * Tests the JournalWaitDelegator.
 */
public class JournalWaitDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The journal wait delegator. */
    private JournalWaitDelegator journalWaitDelegator;
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher WAIT_MATCHER = new CommandLineArgumentMatcher(
            new String[] {});
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher NO_WAIT_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-i" });
    
    /** Test error. */
    private static final String ERROR = "Simulated error";

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        journalWaitDelegator = new JournalWaitDelegator(server);
    }

    /**
     * Test wait connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testWaitConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        journalWaitDelegator.journalWait(new JournalWaitOptions());
    }

    /**
     * Test wait access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testWaitAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        journalWaitDelegator.journalWait(new JournalWaitOptions());
    }

    /**
     * Test wait request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testWaitRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        journalWaitDelegator.journalWait(new JournalWaitOptions());
    }

    /**
     * Test wait p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testWaitP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        journalWaitDelegator.journalWait(new JournalWaitOptions());
    }

    /**
     * Test wait.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testWait() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenReturn(new ArrayList<>());
        journalWaitDelegator.journalWait(new JournalWaitOptions());
        verify(server).execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null));
    }

    /**
     * Test no wait.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testNoWait() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(NO_WAIT_MATCHER), eq(null)))
                .thenReturn(new ArrayList<>());
        journalWaitDelegator.journalWait(new JournalWaitOptions().setNoWait(true));
        verify(server).execMapCmdList(eq(JOURNALWAIT.toString()), argThat(NO_WAIT_MATCHER),
                eq(null));
    }

    /**
     * Test null options.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testNullOptions() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenReturn(new ArrayList<>());
        journalWaitDelegator.journalWait(null);
        verify(server).execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null));
    }
    
    /**
     * Test null options, null return from server.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testNullOptionsNullReturn() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenReturn(null);
        journalWaitDelegator.journalWait(null);
        verify(server).execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null));
    }

    /**
     * Test error.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testError() throws P4JavaException {
        when(server.execMapCmdList(eq(JOURNALWAIT.toString()), argThat(WAIT_MATCHER), eq(null)))
                .thenReturn(buildErrorResultMap());
        try {
            journalWaitDelegator.journalWait(null);
        } catch (RequestException e) {
            assertTrue(e.getMessage().startsWith(ERROR));
        }
    }

    /**
     * Builds the error result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildErrorResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("fmt0", "%error%");
        result.put("code0", "841226339");
        result.put("error", ERROR);
        results.add(result);
        return results;
    }
}