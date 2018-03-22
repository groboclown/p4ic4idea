package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.server.CmdSpec.OPENED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the OpenedDelegator.
 */
public class OpenedDelegatorTest extends AbstractP4JavaUnitTest {
    /** The opened delegator. */
    private OpenedDelegator openedDelegator;
    /** Example value. */
    private static final String DEPOT_FILE = "//depot/main/revisions.h";
    /** Example value. */
    private static final String CLIENT_FILE = "/tmp/revisions.h";
    /** Example value. */
    private static final String CLIENT = "test";
    /** Example value. */
    private static final String REV = "1";
    /** Example value. */
    private static final String CHANGE = "1234";
    /** Example value. */
    private static final String ACTION = "add";
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher SIMPLE_MATCHER = new CommandLineArgumentMatcher(
            new String[] { DEPOT_FILE });
    /** Option matcher. */
    private static final CommandLineArgumentMatcher OPTION_MATCHER = new CommandLineArgumentMatcher(
            new String[] { "-a", "-c" + CHANGE, "-C" + CLIENT, "-m1",
                    DEPOT_FILE });

    /**
     * Before each.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        openedDelegator = new OpenedDelegator(server);
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
        when(server.execMapCmdList(eq(OPENED.toString()), any(String[].class), any(Map.class)))
                .thenThrow(exceptionClass);
    }

    /**
     * Test opened opt connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testOpenedOptConnectionException() throws P4JavaException {
        setUpException(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, new OpenedFilesOptions());
    }

    /**
     * Test opened opt access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testOpenedOptAccessException() throws P4JavaException {
        setUpException(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, new OpenedFilesOptions());
    }

    /**
     * Test opened opt request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testOpenedOptRequestException() throws P4JavaException {
        setUpException(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, new OpenedFilesOptions());
    }

    /**
     * Test opened opt p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testOpenedOptP4JavaException() throws P4JavaException {
        setUpException(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, new OpenedFilesOptions());
    }

    /**
     * Test opened connection exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testOpenedConnectionException() throws P4JavaException {
        setUpException(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, false, "client", 1, 1);
    }

    /**
     * Test opened access exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testOpenedAccessException() throws P4JavaException {
        setUpException(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        openedDelegator.getOpenedFiles(specs, false, "client", 1, 1);
    }

    /**
     * Test opened request exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testOpenedRequestException() throws P4JavaException {
        // TODO Why does this exception behave differently to return as
        // empty list?
        setUpException(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        List<IFileSpec> opened = openedDelegator.getOpenedFiles(specs, false, "client", 1, 1);
        assertNotNull(opened);
        assertEquals(0, opened.size());
    }

    /**
     * Test opened p4 java exception.
     *
     * @throws P4JavaException the p4 java exception
     */
    @Test
    public void testOpenedP4JavaException() throws P4JavaException {
        // TODO Why does this exception behave differently to return as
        // empty list?
        setUpException(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        List<IFileSpec> opened = openedDelegator.getOpenedFiles(specs, false, "client", 1, 1);
        assertNotNull(opened);
        assertEquals(0, opened.size());
    }

    /**
     * Test opened.
     *
     * @throws P4JavaException the p4 java exception
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void testOpened() throws P4JavaException {
        when(server.execMapCmdList(eq(OPENED.toString()), argThat(SIMPLE_MATCHER), eq(null)))
                .thenReturn(buildResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        List<IFileSpec> opened = openedDelegator.getOpenedFiles(specs, new OpenedFilesOptions());
        verify(server).execMapCmdList(eq(OPENED.toString()), argThat(SIMPLE_MATCHER), eq(null));
        assertFileSpecs(opened);
    }

    /**
     * Test opened set options.
     *
     * @throws P4JavaException the p4 java exception
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void testOpenedSetOptions() throws P4JavaException {
        OpenedFilesOptions options = new OpenedFilesOptions()
                .setChangelistId(Integer.valueOf(CHANGE).intValue()).setAllClients(true)
                .setClientName(CLIENT).setMaxFiles(1);
        when(server.execMapCmdList(eq(OPENED.toString()), argThat(OPTION_MATCHER), eq(null)))
                .thenReturn(buildResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        List<IFileSpec> opened = openedDelegator.getOpenedFiles(specs, options);
        verify(server).execMapCmdList(eq(OPENED.toString()), argThat(OPTION_MATCHER), eq(null));
        assertFileSpecs(opened);
    }

    /**
     * Test opened parameters.
     *
     * @throws P4JavaException the p4 java exception
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void testOpenedParameters() throws P4JavaException {
        when(server.execMapCmdList(eq(OPENED.toString()), argThat(OPTION_MATCHER), eq(null)))
                .thenReturn(buildResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(DEPOT_FILE);
        List<IFileSpec> opened = openedDelegator.getOpenedFiles(specs, true, CLIENT, 1,
                Integer.valueOf(CHANGE).intValue());
        verify(server).execMapCmdList(eq(OPENED.toString()), argThat(OPTION_MATCHER), eq(null));
        assertFileSpecs(opened);
    }

    /**
     * Assert file specs.
     *
     * @param specs the specs
     */
    private void assertFileSpecs(final List<IFileSpec> specs) {
        assertNotNull(specs);
        assertEquals(1, specs.size());
        IFileSpec fs = specs.get(0);
        assertNotNull(specs.get(0));
        assertNotNull(fs.getOriginalPath());
        assertEquals(CLIENT_FILE, fs.getOriginalPath().getPathString());
        assertEquals(CLIENT_FILE, fs.getClientPath().getPathString());
        assertEquals(CLIENT, fs.getClientName());
        assertEquals(Integer.valueOf(CHANGE).intValue(), fs.getChangelistId());
        assertEquals(FileAction.ADD, fs.getAction());
        assertEquals(FileSpecOpStatus.VALID, fs.getOpStatus());
    }

    /**
     * Builds the result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildResultMap() {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<>();
        map.put("rev", REV);
        map.put("change", CHANGE);
        map.put("depotFile", DEPOT_FILE);
        map.put("clientFile", CLIENT_FILE);
        map.put("action", ACTION);
        map.put("client", CLIENT);
        results.add(map);
        return results;
    }
}