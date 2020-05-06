/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.perforce.p4java.tests.ServerMessageMatcher.doesMessageEqualIgnoreCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for info lines output from p4 grep a la job039472.
 */
@TestId("Features111_GetMatchingLinesInfoOutputTest")
public class GetMatchingLinesInfoOutputTest extends P4JavaRshTestCase {

	public GetMatchingLinesInfoOutputTest() {
	}

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetMatchingLinesInfoOutputTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties properties = new Properties();
            setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
            assertNotNull(server);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }
    
	@Test
	public void testGetMatchingLinesInfoOutput() {
		final String searchPattern = "Return a plausibly-random number string in hex form"; // i.e. not random...
		final List<IFileSpec> searchFiles =
			FileSpecBuilder.makeFileSpecList("//depot/client/ShelveUnshelveTest/p4java/...");
		MatchingLinesOptions opts = new MatchingLinesOptions();
		final String knownInfoLine =
							"//depot/client/ShelveUnshelveTest/p4java/overview-tree.html#1"
							+ " - line 232: maximum line length of 4096 exceeded\n";
		
		try {
			List<IFileLineMatch> matches = server.getMatchingLines(searchFiles,
												searchPattern, opts);
			assertNotNull("null matches list returned", matches);
			assertEquals("too few matches returned", 1, matches.size());
			matches = server.getMatchingLines(searchFiles,
												searchPattern, null, opts);
			assertNotNull("null matches list returned", matches);
			assertEquals("too few matches returned", 1, matches.size());
			
			List<IServerMessage> infoLines = new ArrayList<>();
			matches = server.getMatchingLines(searchFiles,
					searchPattern, infoLines, opts);
			assertNotNull("null matches list returned", matches);
			assertEquals("too few matches returned", 1, matches.size());
			assertNotNull("how could this happen?!", infoLines);
			assertEquals("too few info lines returned", 1, infoLines.size());
			boolean found = false;
			for (IServerMessage line : infoLines) {
				assertNotNull("null line in infoLines", line);
				if (doesMessageEqualIgnoreCase(line, knownInfoLine)) {
					found = true;
				}
			}
			assertTrue("known info line not found", found);
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
