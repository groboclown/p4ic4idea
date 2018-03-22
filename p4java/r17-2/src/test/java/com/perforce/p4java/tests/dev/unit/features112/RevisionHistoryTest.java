/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for allow a revision range to be specified for 'p4 filelog'.
 */
@Jobs({ "job046612" })
@TestId("Dev112_RevisionHistoryTest")
public class RevisionHistoryTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getServerAsSuper();
			assertNotNull(server);
            client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

    /**
     * Test for allow a revision range to be specified for 'p4 filelog'.
     */
    @Test
    public void testRevisionHistoryWithRevisionRange() {

        // This test file has contiguous revisions from #1 to #6
        String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java";

        try {
            // Retrieve the revision history ('filelog') of a file spec with a
            // specified revision range
            Map<IFileSpec, List<IFileRevisionData>> fileRevisionHisotryMap = server
                    .getRevisionHistory(
                            FileSpecBuilder.makeFileSpecList(depotFile
                                    + "#2,#5"), new GetRevisionHistoryOptions());

            // Check for null
            assertNotNull(fileRevisionHisotryMap);

            // There should be only one entry
            assertEquals(1, fileRevisionHisotryMap.size());

            // Get the filespec and revision data
            Map.Entry<IFileSpec, List<IFileRevisionData>> entry = fileRevisionHisotryMap
                    .entrySet().iterator().next();

            // Check for null
            assertNotNull(entry);

            // Make sure we have the correct filespec
            IFileSpec fileSpec = entry.getKey();
            assertNotNull(fileSpec);
            assertNotNull(fileSpec.getDepotPathString());
            assertEquals(depotFile, fileSpec.getDepotPathString());

            // Make sure we have the revision data
            List<IFileRevisionData> fileRevisionDataList = entry.getValue();
            assertNotNull(fileRevisionDataList);

            // There should be four revision data (assuming there is no gap
            // between #2 to #5
            assertEquals(4, fileRevisionDataList.size());

            // Verify the revisions (assuming ordered list)
            assertEquals(5, fileRevisionDataList.get(0).getRevision());
            assertEquals(4, fileRevisionDataList.get(1).getRevision());
            assertEquals(3, fileRevisionDataList.get(2).getRevision());
            assertEquals(2, fileRevisionDataList.get(3).getRevision());

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        }
    }
}
