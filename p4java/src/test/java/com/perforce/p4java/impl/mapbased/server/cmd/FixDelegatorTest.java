package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.FIX;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.FixJobsOptions;

/**
 * Tests FixDelegator.
 */
public class FixDelegatorTest extends AbstractP4JavaUnitTest {
    
    /** The fix delegator. */
    private FixDelegator fixDelegator;
    
    /** Example JobId. */
    private static final String TEST_JOB_123 = "job123";
    
    /** Example JobId. */
    private static final String TEST_JOB_456 = "job456";
    
    /** Example list of jobs. */
    private static final List<String> JOB_LIST = Arrays
            .asList(new String[] { TEST_JOB_123, TEST_JOB_456 });
    
    /** Example changelist. */
    private static final String TEST_CHANGELIST = "123";
    /** Matcher for call for fixes. */
    private static final CommandLineArgumentMatcher FIX_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-c" + TEST_CHANGELIST, TEST_JOB_123, TEST_JOB_456 });
    
    /** Matcher for call for fixes with delete. */
    private static final CommandLineArgumentMatcher FIX_DELETE_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-d", "-c" + TEST_CHANGELIST, TEST_JOB_123, TEST_JOB_456 });

    /**
     * Runs before each test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        fixDelegator = new FixDelegator(server);
    }

    /**
     * Test fix jobs with opt.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testFixJobsOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFix> fixes = fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST),
                new FixJobsOptions());
        assertFixes(fixes);
    }

    /**
     * Test fix jobs.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testFixJobs() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFix> fixes = fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), null,
                false);
        assertFixes(fixes);
    }

    /**
     * Assert fixes.
     *
     * @param fixes the fixes
     */
    private void assertFixes(final List<IFix> fixes) {
        assertNotNull(fixes);
        assertEquals(2, fixes.size());
        IFix fix = fixes.get(0);
        assertEquals(Integer.valueOf(TEST_CHANGELIST).intValue(), fix.getChangelistId());
        assertEquals(TEST_JOB_123, fix.getJobId());
        assertEquals("closed", fix.getStatus());
        fix = fixes.get(1);
        assertEquals(Integer.valueOf(TEST_CHANGELIST).intValue(), fix.getChangelistId());
        assertEquals(TEST_JOB_456, fix.getJobId());
        assertEquals("closed", fix.getStatus());
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("change", TEST_CHANGELIST);
        result.put("job", TEST_JOB_123);
        result.put("status", "closed");
        results.add(result);
        result = new HashMap<>();
        result.put("change", TEST_CHANGELIST);
        result.put("job", TEST_JOB_456);
        result.put("status", "closed");
        results.add(result);
        return results;
    }

    /**
     * Test fix jobs with opt connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFixJobsOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), new FixJobsOptions());
    }

    /**
     * Test fix jobs connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFixJobsConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_DELETE_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), null, true);
    }

    /**
     * Test fix jobs with opt access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFixJobsOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), new FixJobsOptions());
    }

    /**
     * Test fix jobs access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFixJobsAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_DELETE_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), null, true);
    }

    /**
     * Test fix jobs with opt request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFixJobsOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), new FixJobsOptions());
    }

    /**
     * Test fix jobs request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFixJobsRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_DELETE_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), null, true);
    }

    /**
     * Test fix jobs with opt p4javaexception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testFixJobsOptP4JavaException() throws P4JavaException {
        // TODO Why are P4JavaException and RequestException handled
        // differently for each method?
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), new FixJobsOptions());
    }

    /**
     * Test fix jobs p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFixJobsP4JavaException() throws P4JavaException {
        // TODO Why are P4JavaException and RequestException handled
        // differently for each method?
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_DELETE_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST), null, true);
    }

    /**
     * Test fix jobs with Sopt null result map fix to string.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFixJobsOptNullResultMapFixToString() throws Exception {
        when(server.execMapCmdList(eq(FIX.toString()), argThat(FIX_MATCHER), eq(null)))
                .thenReturn(null);
        List<IFix> fixes = fixDelegator.fixJobs(JOB_LIST, Integer.valueOf(TEST_CHANGELIST),
                new FixJobsOptions());
        assertNotNull(fixes);
        assertEquals(0, fixes.size());
    }
}