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
import java.util.Map;

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
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test Resolve files with branching.
 * 
 * Setup:
 * 
 * Integrate a file with schedules 'branch resolves' (-Rb) <br>
 * Integrate a file with content changes <br>
 * Resolve files with 'resolve file branching' (-Ab)
 * 
 * Check:
 * 
 * Only the branching file will be attempted to be resolved <br>
 * The file with content changes will not be attempted to be resolved
 * 
 * Verify:
 * 
 * Make sure the submitted files have revision histories with the correct
 * resolve actions
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveBranchingTest")
public class ResolveBranchingTest extends P4JavaTestCase {

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
	 * Test resolve file branching (-Ab)
	 */
	@Test
	public void testResolveFileBranching() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files for integrate with schedule 'branch resolves'
		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		// Source and target files for integrate with content changes
		String sourceFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
		String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + "/MessagesBundle_it.properties";

		// Error message indicating merges still pending
		String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		IChangelist changelist = null;

		try {
			// Copy a file to be used as a target for integrate content changes
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveBranchingTest copy files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
					sourceFile2), new FileSpec(targetFile2), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(copyFiles);
			changelist.refresh();
			List<IFileSpec> copyFiles2 = changelist.submit(new SubmitOptions());
			assertNotNull(copyFiles2);

			// Create changelist for 'integrate -Rb'
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveBranchingTest 'integrate -Rb' changelist");
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

			// Check for null
			assertNotNull(integrateFiles);

			// Check for invalid filespecs
			List<IFileSpec> invalidFiles = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles);
			if (invalidFiles.size() != 0) {
				fail(invalidFiles.get(0).getOpStatus() + ": "
						+ invalidFiles.get(0).getStatusMessage());
			}

			// Refresh changelist
			changelist.refresh();

			// Check for correct number of valid filespecs in changelist
			List<IFileSpec> changelistFiles = changelist.getFiles(true);
			assertEquals("Wrong number of filespecs in changelist", 1,
					FileSpecBuilder.getValidFileSpecs(changelistFiles).size());

			// Validate file action type. It might seem odd that we have a
			// 'delete' file action, but it is correct.
			assertEquals(FileAction.DELETE, changelistFiles.get(0).getAction());

			// Run integrate with normal content changes
			List<IFileSpec> integrateFiles2 = client.integrateFiles(
					new FileSpec(sourceFile2),
					new FileSpec(targetFile2),
					null,
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setForceIntegration(true));

			// Check for null
			assertNotNull(integrateFiles2);

			// Check for invalid filespecs
			List<IFileSpec> invalidFiles2 = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles2);
			if (invalidFiles2.size() != 0) {
				fail(invalidFiles2.get(0).getOpStatus() + ": "
						+ invalidFiles2.get(0).getStatusMessage());
			}

			// Combine the filespecs
			List<IFileSpec> allFiles = new ArrayList<IFileSpec>();
			allFiles.addAll(integrateFiles);
			allFiles.addAll(integrateFiles2);

			// Run resolve with 'resolve file branching'
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(
					allFiles,
					new ResolveFilesAutoOptions().setChangelistId(
							changelist.getId()).setResolveFileBranching(true));

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

			// Refresh changelist
			changelist.refresh();

			// Submit should fail, since the file with content changes is not
			// resolved
			List<IFileSpec> submitFiles = changelist
					.submit(new SubmitOptions());

			// Check for null
			assertNotNull(submitFiles);

			// Check for correct number of filespecs
			assertEquals(2, submitFiles.size());

			// Check for 'must resolve' and 'Merges still pending' in info and
			// error messages
			assertEquals(FileSpecOpStatus.INFO, submitFiles.get(0)
					.getOpStatus());
			assertTrue(submitFiles.get(0).getStatusMessage()
					.contains(" - must resolve " + sourceFile2));
			assertEquals(FileSpecOpStatus.ERROR, submitFiles.get(1)
					.getOpStatus());
			assertTrue(submitFiles.get(1).getStatusMessage()
					.contains(mergesPending));

			// Finally, try resolving all the files and do a successful submit
			List<IFileSpec> resolveFiles2 = client.resolveFilesAuto(allFiles,
					new ResolveFilesAutoOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(resolveFiles2);
			changelist.refresh();
			List<IFileSpec> submitFiles2 = changelist
					.submit(new SubmitOptions());
			assertNotNull(submitFiles2);

			// There should be 6 filespecs (triggers)
			assertEquals(6, submitFiles2.size());

			// Check the statuses and file actions of the two submitted files
			assertEquals(FileSpecOpStatus.VALID, submitFiles2.get(3)
					.getOpStatus());
			assertEquals(FileAction.BRANCH, submitFiles2.get(3).getAction());
			assertEquals(FileSpecOpStatus.VALID, submitFiles2.get(4)
					.getOpStatus());
			assertEquals(FileAction.INTEGRATE, submitFiles2.get(4).getAction());

			// Check for 'Submitted as change' in the info message
			assertTrue(submitFiles2.get(5).getStatusMessage()
					.contains(submittedChange + " " + changelist.getId()));

			// Make sure the changelist is submitted
			changelist.refresh();
			assertTrue(changelist.getStatus() == ChangelistStatus.SUBMITTED);

			// The following validates the submitted resolved file using the
			// info provided by the file's revision history ('filelog')

			// Retrieve the revision history ('filelog') of the submitted
			// resolved file
			Map<IFileSpec, List<IFileRevisionData>> fileRevisionHisotryMap = server
					.getRevisionHistory(
							FileSpecBuilder.makeFileSpecList(targetFile),
							new GetRevisionHistoryOptions().setChangelistId(
									changelist.getId()).setMaxRevs(10));

			// Check for null
			assertNotNull(fileRevisionHisotryMap);

			// There should be one entry
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
			assertEquals(targetFile, fileSpec.getDepotPathString());

			// Make sure we have the revision data
			List<IFileRevisionData> fileRevisionDataList = entry.getValue();
			assertNotNull(fileRevisionDataList);

			// There should be one revision data
			assertEquals(1, fileRevisionDataList.size());

			// Verify the revision actually did the correct resolve action.
			IFileRevisionData fileRevisionData = fileRevisionDataList.get(0);
			assertNotNull(fileRevisionData);
			assertNotNull(fileRevisionData.getAction());
			assertEquals(FileAction.BRANCH, fileRevisionData.getAction());

			// There should be one revision integration data
			List<IRevisionIntegrationData> revisionIntegrationDataList = fileRevisionData
					.getRevisionIntegrationDataList();
			assertNotNull(revisionIntegrationDataList);
			assertEquals(1, revisionIntegrationDataList.size());

			// Verify the revision integration data contains correct resolve
			// action info
			IRevisionIntegrationData revisionIntegrationData = revisionIntegrationDataList
					.get(0);
			assertNotNull(revisionIntegrationData);
			assertNotNull(revisionIntegrationData.getFromFile());
			assertEquals(sourceFile, revisionIntegrationData.getFromFile());
			assertNotNull(revisionIntegrationData.getHowFrom());
			assertEquals("branch from", revisionIntegrationData.getHowFrom());

			// Use fstat to verify the file action and file type on the revision
			// of the submitted resolved file
			List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new GetExtendedFilesOptions().setSinceChangelist(changelist
							.getId()));
			assertNotNull(extendedFiles);
			assertTrue(FileAction.BRANCH == extendedFiles.get(0)
					.getHeadAction());
			assertTrue(1 == extendedFiles.get(0).getHeadRev());
			assertTrue(extendedFiles.get(0).getHeadType().contentEquals("text"));

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
							"Dev112_IntegrateDeleteActionTest delete submitted test files changelist");
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
