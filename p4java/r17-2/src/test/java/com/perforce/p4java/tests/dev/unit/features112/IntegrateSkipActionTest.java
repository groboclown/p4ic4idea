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
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test for the integrate files with schedules 'branch resolves' instead of
 * branching new target files automatically.
 */
@Jobs({ "job046102" })
@TestId("Dev112_IntegrateSkipActionTest")
public class IntegrateSkipActionTest extends P4JavaTestCase {

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
    public void testIntegrateFilesWithSkipIntegrated() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        // The source file has 6 revisions (#6)
        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java";

        // The target file has revision #1 as original branch, and integrated a
        // cherry-picked revision #4,#4
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/"
                + dir + "/P4CmdLogListener.java";

        IChangelist changelist = null;

        try {
            // Create the integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateSkipActionTest schedule resolve changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Integrate revision #1 of a file to the destination
            IFileSpec sourceFileSpec = new FileSpec(sourceFile);
            sourceFileSpec.setStartRevision(1);
            sourceFileSpec.setEndRevision(1);
            IFileSpec targetFileSpec = new FileSpec(targetFile);
            List<IFileSpec> integrateFiles = client.integrateFiles(
                    sourceFileSpec,
                    targetFileSpec,
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist.getId()).setForceIntegration(true));
            assertNotNull(integrateFiles);

            // Submit the file in the integrate changelist
            changelist.refresh();
            List<IFileSpec> submitIntegrationList = changelist.submit(null);
            assertNotNull(submitIntegrationList);

            // Create the integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateSkipActionTest schedule resolve changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Integrate cherry-picked revision #4,#4 of a file to the
            // destination
            sourceFileSpec = new FileSpec(sourceFile);
            sourceFileSpec.setStartRevision(4);
            sourceFileSpec.setEndRevision(4);
            targetFileSpec = new FileSpec(targetFile);
            integrateFiles = client.integrateFiles(sourceFileSpec,
                    targetFileSpec, null, new IntegrateFilesOptions()
                            .setChangelistId(changelist.getId())
                            .setForceIntegration(true));
            assertNotNull(integrateFiles);

            // Resolve files
            List<IFileSpec> resolveFiles = client.resolveFilesAuto(
                    null,
                    new ResolveFilesAutoOptions().setChangelistId(
                            changelist.getId()).setForceResolve(true));
            assertNotNull(resolveFiles);

            // Submit the file in the integrate changelist
            changelist.refresh();
            submitIntegrationList = changelist.submit(null);
            assertNotNull(submitIntegrationList);

            // Create the integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateSkipActionTest schedule resolve changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Run integrate with 'skip cherry-picked revisions already
            // integrated'
            List<IFileSpec> integrateFiles2 = client.integrateFiles(
                    new FileSpec(sourceFile),
                    new FileSpec(targetFile),
                    null,
                    new IntegrateFilesOptions().setChangelistId(
                            changelist.getId()).setSkipIntegratedRevs(true));
            assertNotNull(integrateFiles2);

            // Check for invalid filespecs
            List<IFileSpec> invalidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(integrateFiles2);
            if (invalidFiles.size() != 0) {
                fail(invalidFiles.get(0).getOpStatus() + ": "
                        + invalidFiles.get(0).getStatusMessage());
            }

            // Check for the correct number of filespecs with 'valid' op status
            changelist.refresh();
            List<IFileSpec> integrateFilesList = changelist.getFiles(true);
            assertEquals(1,
                    FileSpecBuilder.getValidFileSpecs(integrateFilesList)
                            .size());

            // Validate the filespecs action types: integrate
            assertEquals(FileAction.INTEGRATE, integrateFilesList.get(0)
                    .getAction());

            // Validate the filespecs integrate revisions
            // The source file has 6 revisions (#6)
            // The target file has #1 as original branch, and #4,#4
            // cherry-picked integrate

            // There should be two filespecs in integrate files
            assertEquals(2, integrateFiles2.size());

            // Check the first integrate info
            assertEquals(FileSpecOpStatus.VALID, integrateFiles2.get(0)
                    .getOpStatus());
            assertTrue(integrateFiles2.get(0).getFromFile()
                    .contentEquals(sourceFile));

            // The second integrate: startFromRev 1 and endFromRev 3
            assertEquals(1, integrateFiles2.get(0).getStartFromRev());
            assertEquals(3, integrateFiles2.get(0).getEndFromRev());

            // Check the second integrate info
            assertEquals(FileSpecOpStatus.VALID, integrateFiles2.get(0)
                    .getOpStatus());
            assertTrue(integrateFiles2.get(1).getFromFile()
                    .contentEquals(sourceFile));

            // The first file integrate: startFromRev 4 and endFromRev 6
            // Note: revision #4 is skipped as expected
            assertEquals(4, integrateFiles2.get(1).getStartFromRev());
            assertEquals(6, integrateFiles2.get(1).getEndFromRev());

            // Validate using the 'resolve' command
            // Check the "how" it is resolved
            ResolveFilesAutoOptions resolveFilesAutoOptions = new ResolveFilesAutoOptions();
            resolveFilesAutoOptions.setChangelistId(changelist.getId());
            List<IFileSpec> resolveFiles2 = client.resolveFilesAuto(null,
                    resolveFilesAutoOptions);
            assertNotNull(resolveFiles2);
            assertTrue(resolveFiles2.size() > 0);
            IFileSpec lastFileSpec = resolveFiles2.get(resolveFiles2.size() - 1);
            assertNotNull(lastFileSpec);

            assertTrue(lastFileSpec.getOpStatus() == FileSpecOpStatus.VALID);
            assertTrue(lastFileSpec.getHowResolved().contentEquals("merge from"));

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
                            .makeFileSpecList(new String[] { targetFile }),
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
