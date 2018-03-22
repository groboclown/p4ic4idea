package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.FILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetDepotFilesOptions;

/**
 * Tests the FilesDelegator.
 */
public class FilesDelegatorTest extends AbstractP4JavaUnitTest {
    /** The files delegator. */
    private FilesDelegator filesDelegator;
    /** Test path. */
    private static final String DEPOT_DEV_PATH = "//depot/dev/...";

    /** Matcher for call with path. */
    private static final CommandLineArgumentMatcher DEV_FS_MATCHER = new CommandLineArgumentMatcher(
            new String[] { DEPOT_DEV_PATH });

    /** Matcher for call with path for all revs. */
    private static final CommandLineArgumentMatcher DEV_ALL_FS_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-a", DEPOT_DEV_PATH });

    /**
     * Runs before each test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        filesDelegator = new FilesDelegator(server);
    }

    /**
     * Test get files with GetDepotFilesOptions.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetFilesOpt() throws P4JavaException {
        List<Map<String, Object>> serverResults = buildDevPathMap();
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenReturn(serverResults);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFileSpec> files = filesDelegator.getDepotFiles(specs, new GetDepotFilesOptions());
        assertFileSpecs(files);
    }

    /**
     * Test get files with parameters.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetFiles() throws P4JavaException {
        List<Map<String, Object>> serverResults = buildDevPathMap();
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_ALL_FS_MATCHER), eq(null)))
                .thenReturn(serverResults);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFileSpec> files = filesDelegator.getDepotFiles(specs, true);
        assertFileSpecs(files);
    }
    
    /**
     * Test get files with parameters - not all.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetFilesNotAll() throws P4JavaException {
        List<Map<String, Object>> serverResults = buildDevPathMap();
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenReturn(serverResults);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        List<IFileSpec> files = filesDelegator.getDepotFiles(specs, false);
        assertFileSpecs(files);
    }

    /**
     * Test get files connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetFilesOptConnectionException() throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        filesDelegator.getDepotFiles(specs, new GetDepotFilesOptions());
    }

    /**
     * Test get files connection exception for parameters.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetFilesConnectionException() throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_ALL_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        filesDelegator.getDepotFiles(specs, true);
    }

    /**
     * Test get files Access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetFilesOptAccessException() throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        filesDelegator.getDepotFiles(specs, new GetDepotFilesOptions());
    }

    /**
     * Test get files Access exception for parameters.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetFilesAccessException() throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_ALL_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        filesDelegator.getDepotFiles(specs, true);
    }

    /**
     * Test get files Request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGetFilesOptRequestException() throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        filesDelegator.getDepotFiles(specs, new GetDepotFilesOptions());
    }

    /**
     * Test get files Request exception for parameters.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetFilesRequestException() throws P4JavaException {
        // TODO Why does RequestException get handled differently depending
        // on the call?
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_ALL_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> results = filesDelegator.getDepotFiles(specs, true);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    /**
     * Test get directories with GetDirectoriesOptions error condition.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesOptError() throws P4JavaException {
        List<Map<String, Object>> serverResults = buildDevPathMapError();
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_FS_MATCHER), eq(null)))
                .thenReturn(serverResults);
        List<IFileSpec> dirs = filesDelegator.getDepotFiles(specs, new GetDepotFilesOptions());
        assertFileSpecsError(dirs);
    }
    
    /**
     * Test get directories with parameters error condition.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesError() throws P4JavaException {
        List<Map<String, Object>> serverResults = buildDevPathMapError();
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH);
        when(server.execMapCmdList(eq(FILES.toString()), argThat(DEV_ALL_FS_MATCHER), eq(null)))
                .thenReturn(serverResults);
        List<IFileSpec> dirs = filesDelegator.getDepotFiles(specs, true);
        assertFileSpecsError(dirs);
    }

    /**
     * Assert that the file specs built are as expected.
     *
     * @param specs
     *            the specs
     */
    private void assertFileSpecs(final List<IFileSpec> specs) {
        assertNotNull(specs);
        assertEquals(1, specs.size());
        IFileSpec fs = specs.get(0);
        assertNotNull(specs.get(0));
        assertEquals(FileSpecOpStatus.VALID, fs.getOpStatus());
    }

    /**
     * Assert file specs contain the correct error.
     *
     * @param specs
     *            the specs
     */
    private void assertFileSpecsError(final List<IFileSpec> specs) {
        assertNotNull(specs);
        assertEquals(1, specs.size());
        IFileSpec fs = specs.get(0);
        assertNotNull(specs.get(0));
        assertEquals("... - must create client 'testclient' to access local files.",
                fs.getStatusMessage());
        assertEquals(FileSpecOpStatus.ERROR, fs.getOpStatus());
    }

    /**
     * Mock return from the server.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDevPathMap() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<>();
        map.put("rev", "1");
        map.put("change", "891");
        map.put("depotFile", DEPOT_DEV_PATH);
        map.put("action", "add");
        map.put("type", "ubinary");
        results.add(map);
        return results;
    }

    /**
     * Mock error from the server.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildDevPathMapError() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<>();
        map.put("fmt0", "%arg% - must create client '%client%' to access local files.");
        map.put("arg", "...");
        map.put("code0", "841226339");
        map.put("client", "testclient");
        results.add(map);
        return results;
    }
}