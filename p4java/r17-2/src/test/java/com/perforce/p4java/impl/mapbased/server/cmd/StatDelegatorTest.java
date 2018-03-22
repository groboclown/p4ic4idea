package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.ISTAT;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Before;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;

/**
 * Tests StatDelegator.
 */
public class StatDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The stat delegator. */
    private StatDelegator iStatDelegator;
    /** Example stream. */
    private static final String STREAM = "//stream";
    /** Example stream. */
    private static final String PARENT_STREAM = "//parent_stream";

    /** Simple matcher. */
    private static final CommandLineArgumentMatcher STAT_MATCHER = new CommandLineArgumentMatcher(
            new String[] { STREAM });
    /** Options matcher. */
    private static final CommandLineArgumentMatcher STAT_OPT_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-a", "-c", "-s", STREAM });

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        iStatDelegator = new StatDelegator(server);
    }

    /**
     * Test stat connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testStatConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        iStatDelegator.getStreamIntegrationStatus(STREAM, new StreamIntegrationStatusOptions());
    }

    /**
     * Test stat access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testStatAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        iStatDelegator.getStreamIntegrationStatus(STREAM, new StreamIntegrationStatusOptions());
    }

    /**
     * Test stat request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testStatRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        iStatDelegator.getStreamIntegrationStatus(STREAM, new StreamIntegrationStatusOptions());
    }

    /**
     * Test stat request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testStatP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        iStatDelegator.getStreamIntegrationStatus(STREAM, new StreamIntegrationStatusOptions());
    }

    /**
     * Test stat no options.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testStatNoOptions() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        assertStatus(iStatDelegator.getStreamIntegrationStatus(STREAM,
                new StreamIntegrationStatusOptions()));
        verify(server).execMapCmdList(eq(ISTAT.toString()), argThat(STAT_MATCHER), eq(null));
    }

    /**
     * Test stat options.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testStatOptions() throws P4JavaException {
        when(server.execMapCmdList(eq(ISTAT.toString()), argThat(STAT_OPT_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        StreamIntegrationStatusOptions options = new StreamIntegrationStatusOptions();
        options.setNoRefresh(true);
        options.setBidirectional(true);
        options.setForceUpdate(true);
        assertStatus(iStatDelegator.getStreamIntegrationStatus(STREAM, options));
        verify(server).execMapCmdList(eq(ISTAT.toString()), argThat(STAT_OPT_MATCHER), eq(null));
    }

    /**
     * Assert status.
     *
     * @param status the status
     */
    private void assertStatus(final IStreamIntegrationStatus status) {
        assertEquals(STREAM, status.getStream());
        assertEquals(PARENT_STREAM, status.getParent());
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("stream", STREAM);
        result.put("parent", PARENT_STREAM);
        results.add(result);
        return results;
    }
}