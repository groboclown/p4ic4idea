package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.FSTAT;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;

/**
 * Tests for FstatDelegator.
 */
public class FstatDelegatorTest extends AbstractP4JavaUnitTest {

    /** The fstat delegator. */
    private FstatDelegator fstatDelegator;

    /** Example client file. */
    private static final String CLIENT_FILE = "/tmp/foo.txt";

    /** Example depot file. */
    private static final String DEPOT_FILE = "//depot/foo.txt";

    /** Simple matcher. */
    private static final CommandLineArgumentMatcher FIX_FS_MATCHER = new CommandLineArgumentMatcher(
            new String[] { CLIENT_FILE });

    /** Matcher with params. */
    private static final CommandLineArgumentMatcher FIX_FS_PARAM_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-m1", "-c1", "-edefault", CLIENT_FILE });

    /** Matcher for options. */
    private static final CommandLineArgumentMatcher FIX_FS_OPT_SPEC_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-m1", "-c1", CLIENT_FILE });

    /**
     * Runs before every test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        fstatDelegator = new FstatDelegator(server);
    }

    /**
     * Test Fstat with opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFstatJobsOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        fstatDelegator.getExtendedFiles(specs, new GetExtendedFilesOptions());
    }

    /**
     * Test Fstat connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testFstatJobsConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        fstatDelegator.getExtendedFiles(specs, 1, 1, 0, null, null);
    }

    /**
     * Test fixes jobs opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFstatJobsOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        fstatDelegator.getExtendedFiles(specs, new GetExtendedFilesOptions());
    }

    /**
     * Test Fstat access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testFstatJobsAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        fstatDelegator.getExtendedFiles(specs, 1, 1, 0, null, null);
    }

    /**
     * Test Fstat opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testFstatJobsOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        fstatDelegator.getExtendedFiles(specs, new GetExtendedFilesOptions());
    }

    /**
     * Test Fstat request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testFstatJobsRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs, 1, 1, 0, null,
                null);
        assertNotNull(exSpecs);
        assertEquals(0, exSpecs.size());
    }

    /**
     * Test Fstat opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testFstatJobsOptP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs,
                new GetExtendedFilesOptions());
        assertNotNull(exSpecs);
        assertEquals(0, exSpecs.size());
    }

    /**
     * Test Fstat p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testFstatJobsP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs, 1, 1, 0, null,
                null);
        assertNotNull(exSpecs);
        assertEquals(0, exSpecs.size());

    }

    /**
     * Test Fstat opt.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testFstatJobsOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs,
                new GetExtendedFilesOptions());
        verify(server).execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_MATCHER), eq(null));
        assertFileSpecs(exSpecs);
    }

    /**
     * Test Fstat.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testFstatJobs() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs, 1, 1, 0, null,
                null);
        verify(server).execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_PARAM_MATCHER),
                eq(null));
        assertFileSpecs(exSpecs);
    }

    /**
     * Test Fstat opt specific.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testFstatJobsOptSpecific() throws P4JavaException {
        when(server.execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_OPT_SPEC_MATCHER),
                eq(null))).thenReturn(buildValidResultMap());
        GetExtendedFilesOptions options = new GetExtendedFilesOptions();
        options.setMaxResults(1);
        options.setSinceChangelist(1);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IExtendedFileSpec> exSpecs = fstatDelegator.getExtendedFiles(specs, options);
        verify(server).execMapCmdList(eq(FSTAT.toString()), argThat(FIX_FS_OPT_SPEC_MATCHER),
                eq(null));
        assertFileSpecs(exSpecs);
    }

    /**
     * Assert that the file specs built are as expected.
     *
     * @param specs
     *            the specs
     */
    private void assertFileSpecs(final List<IExtendedFileSpec> specs) {
        final int rev3 = 3;
        final int rev9 = 9;
        assertNotNull(specs);
        assertEquals(1, specs.size());
        IExtendedFileSpec fs = specs.get(0);
        assertNotNull(specs.get(0));
        assertNotNull(fs.getOriginalPath());
        assertEquals(CLIENT_FILE, fs.getOriginalPath().getPathString());
        assertEquals(FileSpecOpStatus.VALID, fs.getOpStatus());
        assertNotNull(fs.getDepotPath());
        assertEquals(DEPOT_FILE, fs.getDepotPath().getPathString());
        assertEquals(rev3, fs.getHaveRev());
        assertEquals(rev3, fs.getHeadRev());
        assertEquals(rev3, fs.getEndRevision());
        assertEquals(rev9, fs.getHeadChange());
        assertEquals("text+w", fs.getHeadType());
        assertEquals(FileAction.EDIT, fs.getHeadAction());
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("headChange", "9");
        result.put("headType", "text+w");
        result.put("headRev", "3");
        result.put("isMapped", "");
        result.put("haveRev", "3");
        result.put("clientFile", CLIENT_FILE);
        result.put("depotFile", DEPOT_FILE);
        result.put("headAction", "edit");
        results.add(result);
        return results;
    }
}