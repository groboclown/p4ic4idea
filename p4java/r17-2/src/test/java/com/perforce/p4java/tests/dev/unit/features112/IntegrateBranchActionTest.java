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
@TestId("Dev112_IntegrateBranchActionTest")
public class IntegrateBranchActionTest extends P4JavaTestCase {

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
    public void testIntegrateFilesWithScheduleBranchResolve() {
		int randNum = getRandomInt();
        String dir = "branch" + randNum;

        String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
        String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
                + dir + "/MessagesBundle_es.properties";

        IChangelist changelist = null;

        try {
            // Create the integrate changelist
            changelist = getNewChangelist(server, client,
                    "Dev112_IntegrateBranchActionTest schedule branch resolve changelist");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);

            // Run integrate with schedule 'branch resolves' ('-Rb' option)
            List<IFileSpec> integrateFiles = client.integrateFiles(
                    new FileSpec(sourceFile),
                    new FileSpec(targetFile),
                    null,
                    new IntegrateFilesOptions()
                            .setChangelistId(changelist.getId())
                            .setForceIntegration(true).setBranchResolves(true));

            // Check for invalid filespecs
            List<IFileSpec> invalidFiles = FileSpecBuilder
                    .getInvalidFileSpecs(integrateFiles);
            if (invalidFiles.size() != 0) {
                fail(invalidFiles.get(0).getOpStatus() + ": "
                        + invalidFiles.get(0).getStatusMessage());
            }

            // Check for null
            assertNotNull(integrateFiles);

            // Check for correct number of valid filespecs in changelist
            changelist.refresh();
            List<IFileSpec> integrateFilesList = changelist.getFiles(true);
            assertEquals("Wrong number of filespecs in changelist", 1,
                    FileSpecBuilder.getValidFileSpecs(integrateFilesList)
                            .size());

            // Validate file action type. It might seem odd that we have a
            // 'delete' file action, but it is correct.
            assertEquals(FileAction.DELETE, integrateFilesList.get(0)
                    .getAction());

            // Validate file actions in the 'resolve' info messages
            List<IFileSpec> resolveFiles = client.resolveFilesAuto(
                    integrateFiles, new ResolveFilesAutoOptions()
                            .setChangelistId(changelist.getId())
                            .setResolveFileBranching(true)
                            .setForceResolve(true));

            // Check for null
            assertNotNull(resolveFiles);

            // Check for correct number of filespecs
            assertEquals(2, resolveFiles.size());

            // Validate file actions
            // Check the "how" it is resolved
            assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(1)
                    .getOpStatus());
            assertTrue(resolveFiles.get(1).getHowResolved().contentEquals("branch from"));

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
        }
    }
}
