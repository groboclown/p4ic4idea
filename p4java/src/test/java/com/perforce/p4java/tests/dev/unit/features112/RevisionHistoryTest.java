/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.features121.GetStreamOptionsTest;

/**
 * Test for allow a revision range to be specified for 'p4 filelog'.
 */
@Jobs({ "job046612" })
@TestId("Dev112_RevisionHistoryTest")
public class RevisionHistoryTest extends P4JavaRshTestCase {

    IClient client = null;
    private int randNum = getRandomInt();
    private String dir = "main" + randNum;
    private String copyTargetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/" + dir + "/P4CmdLogListener.java";
    private String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java";

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", RevisionHistoryTest.class.getSimpleName());

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            setupServer(p4d.getRSHURL(), userName, password, true, props);
            client = getClient(server);
            createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java", "desc");

            copyFile(server, client, "desc", depotFile, copyTargetFile);
            editFile(server, client, "edit file", copyTargetFile);
            editFile(server, client, "edit file", copyTargetFile);
            editFile(server, client, "edit file", copyTargetFile);
            editFile(server, client, "edit file", copyTargetFile);
            editFile(server, client, "edit file", copyTargetFile);
        } catch (Exception e) {
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
        try {
            // Retrieve the revision history ('filelog') of a file spec with a
            // specified revision range
            Map<IFileSpec, List<IFileRevisionData>> fileRevisionHisotryMap = server
                    .getRevisionHistory(
                            FileSpecBuilder.makeFileSpecList(copyTargetFile
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
            assertEquals(copyTargetFile, fileSpec.getDepotPathString());

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
