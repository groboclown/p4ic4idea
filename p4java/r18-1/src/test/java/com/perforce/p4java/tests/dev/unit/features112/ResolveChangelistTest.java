/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
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
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test resolve files in a changelist.
 */
@Jobs({ "job046062" })
@TestId("Dev112_ResolveChangelistTest")
public class ResolveChangelistTest extends P4JavaTestCase {

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
     * Test resolve files in a changelist (-c)
     */
    @Test
    public void testResolveFilesInChangelist() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
                + dir + "/MessagesBundle_it.properties";

        String sourceFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_ro.properties";
        String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
                + dir + "/MessagesBundle_ro.properties";

        IChangelist changelist = null;
        IChangelist changelist2 = null;

        try {
            // Create a integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_ResolveChangelistTest integration changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Run integrate files
            List<IFileSpec> integrateFiles = client.integrateFiles(
                    new FileSpec(sourceFile),
                    new FileSpec(targetFile),
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist.getId()).setBranchResolves(true));

            // Check for null
            assertNotNull(integrateFiles);

            // Check for invalid files
            List<IFileSpec> invalidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(integrateFiles);
            if (invalidFiles.size() != 0) {
                fail(invalidFiles.get(0).getOpStatus() + ": "
                        + invalidFiles.get(0).getStatusMessage());
            }

            // Refresh changelist
            changelist.refresh();

            // Should only contain one file
            List<IFileSpec> integrateFilesList = changelist.getFiles(true);
            assertNotNull(integrateFilesList);
            assertEquals(1, integrateFilesList.size());

            // Create a integrate changelist
            changelist2 = getNewChangelist(server, client,
                    "Dev112_ResolveChangelistTest integration changelist");
            assertNotNull(changelist2);
            changelist2 = client.createChangelist(changelist2);

            // Run integrate files
            List<IFileSpec> integrateFiles2 = client.integrateFiles(
                    new FileSpec(sourceFile2),
                    new FileSpec(targetFile2),
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist2.getId()).setBranchResolves(true));

            // Check for null
            assertNotNull(integrateFiles2);

            // Check for invalid files
            List<IFileSpec> invalidFiles2 = FileSpecBuilder
                    .getInvalidFileSpecs(integrateFiles2);
            if (invalidFiles2.size() != 0) {
                fail(invalidFiles2.get(0).getOpStatus() + ": "
                        + invalidFiles2.get(0).getStatusMessage());
            }

            // Refresh changelist
            changelist2.refresh();

            // Should only contain one file
            List<IFileSpec> integrateFilesList2 = changelist2.getFiles(true);
            assertNotNull(integrateFilesList2);
            assertEquals(1, integrateFilesList2.size());

            // Combine the files
            List<IFileSpec> allFiles = new ArrayList<IFileSpec>();
            allFiles.addAll(integrateFiles);
            allFiles.addAll(integrateFiles2);

            // Create a resolve files auto options and set the changelist id
            ResolveFilesAutoOptions resolveOpts = new ResolveFilesAutoOptions();
            resolveOpts.setChangelistId(changelist.getId());
            resolveOpts.setAcceptTheirs(true);

            // Run the resolve files with the first changelist
            List<IFileSpec> resolveFiles = client.resolveFilesAuto(allFiles,
                    new ResolveFilesAutoOptions().setChangelistId(changelist
                            .getId()));

            // Check for null
            assertNotNull(resolveFiles);

    	    // Check for correct number of filespecs
            assertEquals(3, resolveFiles.size());

            // Validate file actions
    	    // Check the "how" it is resolved
    	    assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(1)
    		    .getOpStatus());
    	    assertTrue(resolveFiles.get(1).getHowResolved()
    		    .contentEquals("branch from"));

            // There should be an error since the "MessagesBundle_ro.properties"
            // is in the second changelist
            assertEquals(FileSpecOpStatus.ERROR, resolveFiles.get(2)
                    .getOpStatus());
            assertTrue(resolveFiles.get(2).getStatusMessage()
                    .contentEquals(targetFile2 + " - no file(s) to resolve."));

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
                if (changelist2 != null) {
                    if (changelist2.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(changelist2.getFiles(true),
                                    new RevertFilesOptions()
                                            .setChangelistId(changelist2
                                                    .getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
        }
    }
}
