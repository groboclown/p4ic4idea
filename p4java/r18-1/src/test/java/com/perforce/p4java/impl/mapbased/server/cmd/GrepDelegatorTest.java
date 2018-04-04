package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.GREP;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.MatchingLinesOptions;

/**
 * Tests the GrepDelegator.
 */
public class GrepDelegatorTest extends AbstractP4JavaUnitTest {

    /** The grep delegator. */
    private GrepDelegator grepDelegator;
    /** Example client file. */
    private static final String CLIENT_FILE = "/tmp/foo.txt";
    /** Example depot file. */
    private static final String DEPOT_FILE = "//depot/foo.txt";

    /** Example pattern. */
    private static final String PATTERN = "^a";
    /** Simple matcher. */
    private static final CommandLineArgumentMatcher GREP_FS_MATCHER =
            new CommandLineArgumentMatcher(
                    new String[] { "-e" + PATTERN, CLIENT_FILE });

    /**
     * Runs before each test.
     */
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        grepDelegator = new GrepDelegator(server);
    }

    /**
     * Test grep opt connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGrepOptConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, new MatchingLinesOptions());
    }

    /**
     * Test grep opt info connection exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = ConnectionException.class)
    public void testGrepOptInfoConnectionException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(ConnectionException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, null, new MatchingLinesOptions());
    }

    /**
     * Test grep opt access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGrepOptAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, new MatchingLinesOptions());
    }

    /**
     * Test grep opt info access exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = AccessException.class)
    public void testGrepOptInfoAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(AccessException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, null, new MatchingLinesOptions());
    }

    /**
     * Test grep opt request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGrepOptRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, new MatchingLinesOptions());
    }

    /**
     * Test grep opt info request exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = RequestException.class)
    public void testGrepOptInfoRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(RequestException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, null, new MatchingLinesOptions());
    }

    /**
     * Test grep opt p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testGrepOptP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, new MatchingLinesOptions());
    }

    /**
     * Test grep opt info p4 java exception.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test(expected = P4JavaException.class)
    public void testGrepOptInfoP4JavaException() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenThrow(P4JavaException.class);
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        grepDelegator.getMatchingLines(specs, PATTERN, null, new MatchingLinesOptions());
    }

    /**
     * Test grep opt.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGrepOpt() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMap());
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IFileLineMatch> matches = grepDelegator.getMatchingLines(specs, PATTERN,
                new MatchingLinesOptions());
        assertMatches(matches);
    }

    /**
     * Test grep opt info to simulate an error.
     *
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Test
    public void testGrepOptInfo() throws P4JavaException {
        when(server.execMapCmdList(eq(GREP.toString()), argThat(GREP_FS_MATCHER), eq(null)))
                .thenReturn(buildValidResultMapWithInfo());
        List<String> infoLines = new ArrayList<>();
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(CLIENT_FILE);
        List<IFileLineMatch> matches = grepDelegator.getMatchingLines(specs, PATTERN, infoLines,
                new MatchingLinesOptions());
        assertMatches(matches);
        assertEquals(1, infoLines.size());
        assertTrue(infoLines.get(0)
                .startsWith("//depot/foo.txt#abc - line 1: maximum line length of 4096 exceeded"));
    }

    /**
     * Assert matches.
     *
     * @param matches
     *            the matches
     */
    private void assertMatches(final List<IFileLineMatch> matches) {
        final int rev = 9;
        assertNotNull(matches);
        assertEquals(1, matches.size());
        IFileLineMatch match = matches.get(0);
        assertEquals(DEPOT_FILE, match.getDepotFile());
        assertEquals("abc", match.getLine());
        assertEquals(1, match.getLineNumber());
        assertEquals(rev, match.getRevision());
        assertEquals(IFileLineMatch.MatchType.MATCH, match.getType());
    }

    /**
     * Builds the valid result map.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMap() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("rev", "9");
        result.put("line", "1");
        result.put("matchEnd0", "3");
        result.put("depotFile", DEPOT_FILE);
        result.put("matchedLine", "abc");
        result.put("matchBegin0", "0");
        results.add(result);
        return results;
    }

    /**
     * Builds the valid result map with info error.
     *
     * @return the list
     */
    private List<Map<String, Object>> buildValidResultMapWithInfo() {
        List<Map<String, Object>> results = buildValidResultMap();
        Map<String, Object> info = new HashMap<>();
        info.put("maxlinelength", "4096");
        info.put("linenumber", "1");
        info.put("fmt0",
                "%depotFile%#%depotRev% - line %linenumber%: maximum line length of "
              + "%maxlinelength% exceeded");
        info.put("code0", "603986454");
        info.put("depotRev", "abc");
        info.put("depotFile", DEPOT_FILE);
        results.add(info);
        return results;
    }
}