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
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the integrate files with schedules 'delete resolves' instead of
 * deleting target files automatically.
 */
@Jobs({ "job046102" })
@TestId("Dev112_IntegrateDeleteActionTest")
public class IntegrateDeleteActionTest extends P4JavaTestCase {

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

    @Test
    public void testIntegrateFilesWithScheduleDeleteResolve() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
                + dir + "/MessagesBundle_es.properties";

        String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
                + dir + "/Delete_MessagesBundle_es.properties";

        IChangelist changelist = null;

        try {
            // Create the copy changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateDeleteActionTest copy changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Make a new copy of a file
            List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
                    sourceFile), new FileSpec(targetFile), null,
                    new CopyFilesOptions().setChangelistId(changelist.getId()));
            assertNotNull(copyFiles);

            // Submit the file in the copy changelist
            changelist.refresh();
            List<IFileSpec> submitCopyList = changelist.submit(null);
            assertNotNull(submitCopyList);

            // Create the integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateDeleteActionTest integration changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Integrate the new copy file to the destination
            List<IFileSpec> integrateFiles = client.integrateFiles(
                    new FileSpec(targetFile),
                    new FileSpec(targetFile2),
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist.getId()).setForceIntegration(true));
            assertNotNull(integrateFiles);

            // Submit the file in the integrate changelist
            changelist.refresh();
            List<IFileSpec> submitIntegrationList = changelist.submit(null);
            assertNotNull(submitIntegrationList);

            // Create delete changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateDeleteActionTest delete changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Delete the newly copied file
            List<IFileSpec> deleteFiles = client
                    .deleteFiles(copyFiles, new DeleteFilesOptions()
                            .setChangelistId(changelist.getId()));
            assertNotNull(deleteFiles);

            // Submit the file in the delete changelist
            changelist.refresh();
            List<IFileSpec> submitDeleteList = changelist.submit(null);
            assertNotNull(submitDeleteList);

            // Create integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateDeleteActionTest schedule delete resolve changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Run integrate with 'delete resolve'
            List<IFileSpec> integrateFiles2 = client.integrateFiles(
                    new FileSpec(targetFile),
                    new FileSpec(targetFile2),
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist.getId()).setDeleteResolves(true));
            assertNotNull(integrateFiles2);

            // Check for invalid filespecs
            List<IFileSpec> invalidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(integrateFiles2);
            if (invalidFiles.size() != 0) {
                fail(invalidFiles.get(0).getOpStatus() + ": "
                        + invalidFiles.get(0).getStatusMessage());
            }

            // Check for correct number of filespecs
            assertEquals(1, integrateFiles2.size());

            // Validate the filespecs action types: integrate
            assertEquals(FileAction.INTEGRATE, integrateFiles2.get(0)
                    .getAction());

            // Run resolve with 'resolve file branching'
            List<IFileSpec> resolveFiles = client.resolveFilesAuto(
                    integrateFiles2, new ResolveFilesAutoOptions()
                            .setChangelistId(changelist.getId())
                            .setResolveFileDeletions(true));

            // Check for null
            assertNotNull(resolveFiles);

            // Check for correct number of filespecs
            assertEquals(2, resolveFiles.size());

            // Validate file actions
            // Check the "how" it is resolved
            assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(1)
                    .getOpStatus());
            assertTrue(resolveFiles.get(1).getHowResolved()
                    .contentEquals("delete from"));

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
                    // Delete the newly integrated copied file
                    IChangelist deleteChangelist = getNewChangelist(server,
                            client,
                            "Dev112_IntegrateDeleteActionTest delete submit test files changelist");
                    deleteChangelist = client
                            .createChangelist(deleteChangelist);
                    client.deleteFiles(FileSpecBuilder
                            .makeFileSpecList(new String[] { targetFile,
                                    targetFile2 }), new DeleteFilesOptions()
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
