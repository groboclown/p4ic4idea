/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test Resolve files with new "contentResolveType" tagged field.
 *
 * Changes to tagged "p4 resolve" output to support P4V's resolve dialog:
 *
 * 1) The server-provided portion of the output is now always tagged, even in
 * interactive mode (2011.1+ protocolApi required).
 *
 * 2) There is a new "contentResolveType" tagged field indicating whether we're
 * doing a 3-way merge of text files, a 3-way merge of binary files, or a 2-way
 * binary resolve. In non-tagged mode the client can get this information by
 * seeing which message comes back, but prior to this change you couldn't tell
 * from the tagged output whether you were dealing with a binary resolve.
 * Possible values of this field are "3waytext", "3wayraw", and "2wayraw".
 */
@Jobs({ "job046677" })
@TestId("Dev112_ContentResolveTypeTest")
public class ContentResolveTypeTest extends P4JavaTestCase {

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
			server = getServer();
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
     * Test content resolve type field
     */
    @Test
    public void testResolveFileContentChanges() {
		int randNum = getRandomInt();
        String lineSep = System.getProperty("line.separator", "\n");
        String testdir = "testdir" + randNum;
        String testfile = "testfile.txt";

        String depotBaseDir = "//depot/112Dev/" + testdir;
        String mainDepotFile = depotBaseDir + "/main/" + testfile;
        String releaseDepotFile = depotBaseDir + "/release/" + testfile;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";

        IChangelist changelist = null;
        List<IFileSpec> fileSpecs = null;

        try {
            // Copy a file to the main
            changelist = getNewChangelist(server, client,
                    "Dev112_ContentResolveTypeTest: copy a file to main.");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            fileSpecs = client.copyFiles(new FileSpec(sourceFile),
                    new FileSpec(mainDepotFile), null,
                    new CopyFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            fileSpecs = changelist.submit(new SubmitOptions());
            assertNotNull(fileSpecs);

            // Integrate the file from main to release
            changelist = getNewChangelist(server, client,
                    "Dev112_ContentResolveTypeTest: integrate test file from main to release.");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            fileSpecs = client.integrateFiles(new FileSpec(mainDepotFile),
                    new FileSpec(releaseDepotFile), null,
                    new IntegrateFilesOptions().setChangelistId(changelist
                            .getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            fileSpecs = changelist.submit(new SubmitOptions());
            assertNotNull(fileSpecs);

            // Make changes to the file in main
            changelist = getNewChangelist(server, client,
                    "Dev112_ContentResolveTypeTest: edit test file in main.");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            fileSpecs = client.editFiles(
                    FileSpecBuilder.makeFileSpecList(mainDepotFile),
                    new EditFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            writeFileBytes(fileSpecs.get(0).getClientPathString(),
                    "// Make change #1 to test file." + lineSep, true);

            fileSpecs = changelist.submit(new SubmitOptions());
            assertNotNull(fileSpecs);

            // Make changes to the file in release
            changelist = getNewChangelist(server, client,
                    "Dev112_ContentResolveTypeTest: edit test file in release.");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            fileSpecs = client.editFiles(
                    FileSpecBuilder.makeFileSpecList(releaseDepotFile),
                    new EditFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            writeFileBytes(fileSpecs.get(0).getClientPathString(),
                    "// Make change #1 to test file." + lineSep, true);

            writeFileBytes(fileSpecs.get(0).getClientPathString(),
                    "// Make change #2 to test file." + lineSep, true);

            writeFileBytes(fileSpecs.get(0).getClientPathString(),
                    "// Make change #3 to test file." + lineSep, true);

            fileSpecs = changelist.submit(new SubmitOptions());
            assertNotNull(fileSpecs);

            // Integrate main to release
            changelist = getNewChangelist(server, client,
                    "Dev112_ContentResolveTypeTest: integrate main to release.");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            fileSpecs = client.integrateFiles(new FileSpec(depotBaseDir
                    + "/main/..."),
                    new FileSpec(depotBaseDir + "/release/..."), null,
                    new IntegrateFilesOptions().setChangelistId(changelist
                            .getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            // Resolve the integrated file
            fileSpecs = client.resolveFilesAuto(FileSpecBuilder
                    .makeFileSpecList(depotBaseDir + "/release/..."),
                    new ResolveFilesAutoOptions().setChangelistId(changelist
                            .getId()));
            assertNotNull(fileSpecs);
            changelist.refresh();

            // Check for filespec "contentResolveType" field value: "3waytext"
            assertTrue(fileSpecs.get(0).getOpStatus() == FileSpecOpStatus.VALID);
            assertEquals(fileSpecs.get(0).getContentResolveType(), "3waytext");

        } catch (Exception exc) {
            fail("Unexpected exception: " + exc.getLocalizedMessage());
        } finally {
            if (client != null) {
                if (changelist != null) {
                    if (changelist.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(
                                    changelist.getFiles(true),
                                    new RevertFilesOptions()
                                            .setChangelistId(changelist.getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
            if (client != null && server != null) {
                try {
                    // Delete submitted test files
                    IChangelist deleteChangelist = getNewChangelist(server,
                            client,
                            "Dev112_ContentResolveTypeTest: delete submitted test files.");
                    deleteChangelist = client
                            .createChangelist(deleteChangelist);
                    client.deleteFiles(FileSpecBuilder
                            .makeFileSpecList(depotBaseDir + "/..."),
                            new DeleteFilesOptions()
                                    .setChangelistId(deleteChangelist.getId()));
                    deleteChangelist.refresh();
                    deleteChangelist.submit(null);
                } catch (P4JavaException e) {
                    // Can't do much here...
                }
            }
        }
    }
}
