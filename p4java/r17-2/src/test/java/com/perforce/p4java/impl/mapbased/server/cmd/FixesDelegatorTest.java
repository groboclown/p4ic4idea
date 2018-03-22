package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.FIXES;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IFix;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetFixesOptions;

/**
 * Tests the FixesDelegator.
 */
public class FixesDelegatorTest extends AbstractP4JavaUnitTest {

    /** The fixes delegator. */
    private FixesDelegator fixesDelegator;
    /** Test path. */
    private static final String DEPOT_DEV_PATH = "//depot/dev/...";
    /** Example changelist. */
    private static final String TEST_CHANGELIST = "123";
    /** Example JobId. */
    private static final String TEST_JOB_123 = "job123";
    /** Matcher for call with path. */
    private static final CommandLineArgumentMatcher FIX_FS_MATCHER = new CommandLineArgumentMatcher(
            new String[] { DEPOT_DEV_PATH });

    /** Matcher for call with params. */
    private static final CommandLineArgumentMatcher FIX_FS_PARAMS_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-c" + TEST_CHANGELIST, "-j" + TEST_JOB_123,
                            "-i", DEPOT_DEV_PATH });

    /** Matcher for call with params and default changelist. */
    private static final CommandLineArgumentMatcher FIX_FS_PARAMS_DEFAULT_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-cdefault", "-j" + TEST_JOB_123, "-i", DEPOT_DEV_PATH });
    
    /** Matcher for call with params and default changelist to support legacy. */
    private static final CommandLineArgumentMatcher FIX_FS_PARAMS_UNKNOWN_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-j" + TEST_JOB_123, "-i", DEPOT_DEV_PATH });

    /**
     * Runs before each test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        fixesDelegator = new FixesDelegator(server);
    }

    /**
     * Test fixes with opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFixesJobsOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixes(specs, new GetFixesOptions());
    }

    /**
     * Test fixes connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFixesJobsConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST), TEST_JOB_123, true, 0);
    }

    /**
     * Test fixes with opt AccessException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFixesJobsOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixes(specs, new GetFixesOptions());
    }

    /**
     * Test fixes AccessException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFixesJobsAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST), TEST_JOB_123, true, 0);
    }

    /**
     * Test fixes with opt RequestException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFixesJobsOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixes(specs, new GetFixesOptions());
    }

    /**
     * Test fixes RequestException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFixesJobsRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST), TEST_JOB_123, true, 0);
    }

    /**
     * Test fixes with opt P4JavaException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testFixesJobsOptP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixes(specs, new GetFixesOptions());
    }

    /**
     * Test fixes P4JavaException.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testFixesJobsP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST), TEST_JOB_123, true, 0);
    }

    /**
     * Gets the fix list empty.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListEmpty() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildEmptyResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST),
                TEST_JOB_123, true, 0);
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER),
                eq(null));
        assertNotNull(fixes);
        assertEquals(0, fixes.size());
    }

    /**
     * Gets the fix list.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixList() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixList(specs, Integer.valueOf(TEST_CHANGELIST),
                TEST_JOB_123, true, 0);
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER),
                eq(null));
        assertFixes(fixes);
    }
    
    /**
     * Gets the fix list for a default.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListDefault() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()),
                argThat(FIX_FS_PARAMS_UNKNOWN_MATCHER), eq(null)))
                    .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixList(specs, Changelist.DEFAULT,
                TEST_JOB_123, true, 0);
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_UNKNOWN_MATCHER),
                eq(null));
        assertFixes(fixes);
    }

    /**
     * Gets the fix list with opt empty.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListOptEmpty() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenReturn(buildEmptyResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixes(specs, new GetFixesOptions());
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null));
        assertNotNull(fixes);
        assertEquals(0, fixes.size());
    }

    /**
     * Gets the fix list with opt.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixes(specs, new GetFixesOptions());
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_MATCHER), eq(null));
        assertFixes(fixes);
    }

    /**
     * Gets the fix list with specified opt. Use options to build parameters and verify
     * it is correct.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListSpecifiedOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        GetFixesOptions options = new GetFixesOptions();
        options.setJobId(TEST_JOB_123);
        options.setChangelistId(Integer.valueOf(TEST_CHANGELIST));
        options.setIncludeIntegrations(true);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixes(specs, options);
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_MATCHER),
                eq(null));
        assertFixes(fixes);
    }

    /**
     * Gets the fix list specified opt default list. Use options to build parameters and a
     * default change list and verify it is correct.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void getFixListSpecifiedOptDefaultList() throws P4JavaException {
        when(server.execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_DEFAULT_MATCHER),
                eq(null))).thenReturn(buildValidResultMap());
        GetFixesOptions options = new GetFixesOptions();
        options.setJobId(TEST_JOB_123);
        options.setChangelistId(Integer.valueOf(Changelist.DEFAULT));
        options.setIncludeIntegrations(true);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFix> fixes = fixesDelegator.getFixes(specs, options);
        verify(server).execMapCmdList(eq(FIXES.toString()), argThat(FIX_FS_PARAMS_DEFAULT_MATCHER),
                eq(null));
        assertFixes(fixes);
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
        return results;
    }

    /**
     * Assert fixes.
     *
     * @param fixes
     *            the fixes
     */
    private void assertFixes(final List<IFix> fixes) {
        assertNotNull(fixes);
        assertEquals(1, fixes.size());
        IFix fix = fixes.get(0);
        assertEquals(Integer.valueOf(TEST_CHANGELIST).intValue(), fix.getChangelistId());
        assertEquals(TEST_JOB_123, fix.getJobId());
        assertEquals("closed", fix.getStatus());
    }

    /**
     * Builds the empty result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildEmptyResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        return results;
    }
}