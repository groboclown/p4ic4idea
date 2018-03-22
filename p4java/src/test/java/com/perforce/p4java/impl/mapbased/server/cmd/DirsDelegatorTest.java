package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.DIRS;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

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
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IServer;

/**
 * Tests the DirsDelegator.
 */
public class DirsDelegatorTest extends AbstractP4JavaUnitTest {

    /** The dirs delegator. */
    private DirsDelegator dirsDelegator;

    /** Test path. */
    private static final String DEPOT_DEV_PATH = "//depot/dev/...";

    /** Matcher for call with path. */
    private static final CommandLineArgumentMatcher DEV_FS_MATCHER = new CommandLineArgumentMatcher(
            new String[] { DEPOT_DEV_PATH });

    /** Map for server call. */
    private static final HashMap<String, Object> IN_MAP = new HashMap<>();

    static {
        IN_MAP.put(IServer.IN_MAP_USE_TAGS_KEY, "no");
    }

    /**
     * Runs before each test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        dirsDelegator = new DirsDelegator(server);
    }

    /**
     * Test get directories with GetDirectoriesOptions.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenReturn(buildDevPathMap());
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), new GetDirectoriesOptions());
        assertFileSpecs(dirs);
    }

    /**
     * Test get directories opt with GetDirectoriesOptions if the return from
     * the server is 'dir' rather than 'dirName'.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesOptWithDir() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenReturn(buildDevPathMap("dir"));
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), new GetDirectoriesOptions());
        assertFileSpecs(dirs);
    }

    /**
     * Test get directories with GetDirectoriesOptions error condition.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesOptError() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenReturn(buildDevPathMapError());
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), new GetDirectoriesOptions());
        assertFileSpecsError(dirs);
    }

    /**
     * Test get directories error condition.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesError() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenReturn(buildDevPathMapError());
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), false, false, false);
        assertFileSpecsError(dirs);
    }

    /**
     * Test get directories.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectories() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenReturn(buildDevPathMap());
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), false, false, false);
        assertFileSpecs(dirs);
    }

    /**
     * Test get directories connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetDirectoriesConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(ConnectionException.class);
        dirsDelegator.getDirectories(FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), false, false,
                false);
    }

    /**
     * Test get directories with GetDirectoriesOptions connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGetDirectoriesOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(ConnectionException.class);
        dirsDelegator.getDirectories(FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH),
                new GetDirectoriesOptions());
    }

    /**
     * Test get directories access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetDirectoriesAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(AccessException.class);
        dirsDelegator.getDirectories(FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), false, false,
                false);
    }

    /**
     * Test get directories with GetDirectoriesOptions access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGetDirectoriesOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(AccessException.class);
        dirsDelegator.getDirectories(FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH),
                new GetDirectoriesOptions());
    }

    /**
     * Test get directories with GetDirectoriesOptions request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGetDirectoriesOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(RequestException.class);
        dirsDelegator.getDirectories(FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH),
                new GetDirectoriesOptions());
    }

    /**
     * Test get directories request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGetDirectoriesRequestException() throws P4JavaException {
        // TODO Why should RequestException behave differently depending
        // on the getDirectories method called?
        when(server.execMapCmdList(eq(DIRS.toString()), argThat(DEV_FS_MATCHER), eq(IN_MAP)))
                .thenThrow(RequestException.class);
        List<IFileSpec> dirs = dirsDelegator.getDirectories(
                FileSpecBuilder.makeFileSpecList(DEPOT_DEV_PATH), false, false, false);
        assertNotNull(dirs);
        assertEquals(0, dirs.size());
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
        assertNotNull(fs.getOriginalPath());
        assertEquals(DEPOT_DEV_PATH, fs.getOriginalPath().getPathString());
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
        assertEquals("//depot/dev/... - must refer to client 'testclient'.", fs.getStatusMessage());
        assertEquals(FileSpecOpStatus.ERROR, fs.getOpStatus());
    }

    /**
     * Mock return from the server.
     *
     * @param dirParam
     *            the dir param
     * @return the list
     */
    private List<Map<String, Object>> buildDevPathMap(final String... dirParam) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<>();
        map.put("fmt0", "%dirName%");
        map.put("code0", "285219025");
        if (dirParam != null && dirParam.length > 0) {
            map.put(dirParam[0], DEPOT_DEV_PATH);
        } else {
            map.put("dirName", DEPOT_DEV_PATH);
        }
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
        map.put("fmt0", "%path% - must refer to client '%client%'.");
        map.put("code0", "838998116");
        map.put("client", "testclient");
        map.put("path", DEPOT_DEV_PATH);
        results.add(map);
        return results;
    }
}