/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.IFileLineMatch.MatchType;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests IOptionsServer.getMatchingLines as best it can...<p>
 * 
 * Relies <i>heavily</i> on no one changing what's in DEPOT_PATH_ROOT
 * files and file tree; all bets are off if these change.
 */

@TestId("Features101_GetMatchingLinesTest")
public class GetMatchingLinesTest extends P4JavaTestCase {
	
	public static final String DEPOT_PATH_ROOT = "//depot/basic/readonly/grep/...";
	public static final String DEPOT_PATH_ROOT_PREFIX = "//depot/basic/readonly/grep";

	public GetMatchingLinesTest() {
	}

	@Test
	public void testGetMatchingLinesBasics() {
		IOptionsServer server = null;
		
		try {
			server = getServer();
			List<IFileLineMatch> matches = server.getMatchingLines(
						FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
						"P4Java",
						null);
			checkBasics(matches, 8, false);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setIncludeLineNumbers(true));
			checkBasics(matches, 8, true);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions("-n"));
			checkBasics(matches, 8, true);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions(
							false,
							false,
							true,
							true,
							false,
							0,
							0,
							0,
							false));
			checkBasics(matches, 5331, true);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setCaseInsensitive(true));
			checkBasics(matches, 117, false);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setCaseInsensitive(true).setNonMatchingLines(true));
			checkBasics(matches, 5222, false);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}

	@Test
	public void testContexts() {
		IOptionsServer server = null;
		
		try {
			server = getServer();
			
			List<IFileLineMatch> matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setOutputContext(2).setIncludeLineNumbers(true));
			checkContexts(matches, 0, true, true, true);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setLeadingContext(2).setIncludeLineNumbers(false));
			checkContexts(matches, 0, false, true, false);
			matches = server.getMatchingLines(
					FileSpecBuilder.makeFileSpecList(DEPOT_PATH_ROOT),
					"P4Java",
					new MatchingLinesOptions().setTrailingContext(2));
			checkContexts(matches, 0, false, false, true);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		} 
	}
	
	private void checkBasics(List<IFileLineMatch> matches, int expectedSize,
								boolean checkLineNos) {
		assertNotNull(matches);
		if (expectedSize >= 0) {
			assertEquals(expectedSize, matches.size());
		}
		for (IFileLineMatch match : matches) {
			assertNotNull(match);
			checkMatchTypes(match.getType(), true, false, false);
			assertEquals(MatchType.MATCH, match.getType());
			assertNotNull(match.getDepotFile());
			assertTrue(match.getDepotFile().startsWith(DEPOT_PATH_ROOT_PREFIX));
			if (checkLineNos) {
				assertTrue("line number set with no -n option", match.getLineNumber() > 0);
			} else {
				assertEquals("line number not set with -n option", -1, match.getLineNumber());
			}
			assertTrue(match.getRevision() > 0);
		}
	}
	
	private void checkContexts(List<IFileLineMatch> matches, int expectedSize, boolean checkLineNos,
								boolean checkBefore, boolean checkAfter) {
		assertNotNull(matches);
		for (IFileLineMatch match : matches) {
			assertNotNull(match);
			checkMatchTypes(match.getType(), true, checkBefore, checkAfter);
			assertNotNull(match.getDepotFile());
			assertTrue(match.getDepotFile().startsWith(DEPOT_PATH_ROOT_PREFIX));
			if (checkLineNos) {
				assertTrue("line number set with no -n option", match.getLineNumber() > 0);
			} else {
				assertEquals("line number not set with -n option", -1, match.getLineNumber());
			}
			assertTrue(match.getRevision() > 0);
		}
	}
	
	private void checkMatchTypes(MatchType matchType, boolean match, boolean before, boolean after) {
		assertNotNull(matchType);
		
		if (match && before && after) {
			assertTrue((matchType == MatchType.MATCH)
					|| (matchType == MatchType.BEFORE)
					|| (matchType == MatchType.AFTER));
		} else if (match && before) {
			assertTrue((matchType == MatchType.MATCH)
					|| (matchType == MatchType.BEFORE));
		} else if (match && after) {
			assertTrue((matchType == MatchType.MATCH)
					|| (matchType == MatchType.AFTER));
		} else if (before && after) {
			assertTrue((matchType == MatchType.BEFORE)
					|| (matchType == MatchType.AFTER));
		} else if (match) {
			assertEquals(MatchType.MATCH, matchType);
		} else if (before) {
			assertEquals(MatchType.BEFORE, matchType);
		} else if (after) {
			assertEquals(MatchType.AFTER, matchType);
		} else {
			fail("?");
		}
	}
}
