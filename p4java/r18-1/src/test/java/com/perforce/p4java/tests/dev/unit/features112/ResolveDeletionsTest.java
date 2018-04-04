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
 * Test Resolve files with deletions.
 * 
 * Setup:
 * 
 * Copy a file to the copy target <br>
 * Integrate the copy target file to the delete target <br>
 * Delete the delete target file <br>
 * Integrate a file with schedule 'branch resolves' (-Rb) <br>
 * Resolve files with 'resolve file deletions' (-Ad)
 * 
 * Check:
 * 
 * Only the file with a deleted target will be attempted to be resolved <br>
 * The branching file will not be attempted to be resolved
 * 
 * Verify:
 * 
 * Make sure the submitted files have revision histories with the correct
 * resolve actions
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveFilesAutoOptionsTest")
public class ResolveDeletionsTest extends P4JavaTestCase {

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
	 * Test resolve file deletions (-Ad)
	 */
	@Test
	public void testResolveFileDeletions() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files for copy
		String copySourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String copyTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/Copy_MessagesBundle_es.properties";

		// Target file for deletion
		String deleteTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/Delete_MessagesBundle_es.properties";

		// Source and target files for integrate with schedule 'branch resolves'
		String branchSourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_cs.properties";
		String branchTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_cs.properties";

		// Error message indicating merges still pending
		String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		IChangelist changelist = null;

		try {
			// Create the copy changelist
			changelist = getNewChangelist(server, client,
					"Dev112_IntegrateDeleteActionTest copy changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Make a new copy of a file
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
					copySourceFile), new FileSpec(copyTargetFile), null,
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
					new FileSpec(copyTargetFile),
					new FileSpec(deleteTargetFile),
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

			// Run the integrate files with schedule delete resolves
			List<IFileSpec> integrateFiles2 = client.integrateFiles(
					new FileSpec(copyTargetFile),
					new FileSpec(deleteTargetFile),
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

			// Check for the correct number of filespecs with 'valid' op status
			changelist.refresh();
			List<IFileSpec> integrateFilesList = changelist.getFiles(true);
			assertEquals(1,
					FileSpecBuilder.getValidFileSpecs(integrateFilesList)
							.size());

			// Validate the filespecs action types: integrate
			assertEquals(FileAction.INTEGRATE, integrateFiles2.get(0)
					.getAction());

			// Run integrate files with schedule branch resolves
			List<IFileSpec> integrateFiles3 = client.integrateFiles(
					new FileSpec(branchSourceFile),
					new FileSpec(branchTargetFile),
					null,
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setBranchResolves(true));
			assertNotNull(integrateFiles3);
			List<IFileSpec> invalidFiles2 = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles3);
			if (invalidFiles2.size() != 0) {
				fail(invalidFiles2.get(0).getOpStatus() + ": "
						+ invalidFiles2.get(0).getStatusMessage());
			}

			// Combine the files
			List<IFileSpec> allFiles = new ArrayList<IFileSpec>();
			allFiles.addAll(integrateFiles2);
			allFiles.addAll(integrateFiles3);

			// Run resolve with 'resolve file deletions'
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(
					allFiles,
					new ResolveFilesAutoOptions().setChangelistId(
							changelist.getId()).setResolveFileDeletions(true));

			// Check for null
			assertNotNull(resolveFiles);

			// Check for correct number of filespecs
			assertEquals(3, resolveFiles.size());

			// Validate file actions
			// Check "how" it is resolved
			assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(1)
					.getOpStatus());
			assertTrue(resolveFiles.get(1).getHowResolved()
					.contentEquals("delete from"));

			// Refresh after resolve
			changelist.refresh();

			// Submit should fail, since one file is not resolved
			List<IFileSpec> submitFiles = changelist
					.submit(new SubmitOptions());
			assertNotNull(submitFiles);
			assertEquals(2, submitFiles.size());

			// Check for info and error messages about 'must resolve' in submit
			assertEquals(FileSpecOpStatus.INFO, submitFiles.get(0)
					.getOpStatus());
			assertTrue(submitFiles.get(0).getStatusMessage()
					.contains(" - must resolve " + branchSourceFile));
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
			assertEquals(FileAction.DELETE, submitFiles2.get(3).getAction());
			assertEquals(FileSpecOpStatus.VALID, submitFiles2.get(4)
					.getOpStatus());
			assertEquals(FileAction.BRANCH, submitFiles2.get(4).getAction());

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
							FileSpecBuilder.makeFileSpecList(deleteTargetFile),
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
			assertEquals(deleteTargetFile, fileSpec.getDepotPathString());

			// Make sure we have the revision data
			List<IFileRevisionData> fileRevisionDataList = entry.getValue();
			assertNotNull(fileRevisionDataList);

			// There should be two revision data
			assertEquals(2, fileRevisionDataList.size());

			// LATEST REVISION DATA

			// Verify the revision actually did the correct resolve action.
			IFileRevisionData fileRevisionData = fileRevisionDataList.get(0);
			assertNotNull(fileRevisionData);
			assertNotNull(fileRevisionData.getAction());
			assertEquals(FileAction.DELETE, fileRevisionData.getAction());

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
			assertEquals(copyTargetFile, revisionIntegrationData.getFromFile());
			assertNotNull(revisionIntegrationData.getHowFrom());
			assertEquals("delete from", revisionIntegrationData.getHowFrom());

			// PREVIOUS REVISION DATA

			// Verify this revision actually did the correct resolve action.
			IFileRevisionData fileRevisionData2 = fileRevisionDataList.get(1);
			assertNotNull(fileRevisionData2);
			assertNotNull(fileRevisionData2.getAction());
			assertEquals(FileAction.BRANCH, fileRevisionData2.getAction());

			// There should be one revision integration data for this revision
			List<IRevisionIntegrationData> revisionIntegrationDataList2 = fileRevisionData2
					.getRevisionIntegrationDataList();
			assertNotNull(revisionIntegrationDataList2);
			assertEquals(1, revisionIntegrationDataList2.size());

			// Verify the revision integration data contains correct resolve
			// action info for this revision
			IRevisionIntegrationData revisionIntegrationData2 = revisionIntegrationDataList2
					.get(0);
			assertNotNull(revisionIntegrationData2);
			assertNotNull(revisionIntegrationData2.getFromFile());
			assertEquals(copyTargetFile, revisionIntegrationData2.getFromFile());
			assertNotNull(revisionIntegrationData2.getHowFrom());
			assertEquals("branch from", revisionIntegrationData2.getHowFrom());

			// Use fstat to verify the file action and file type on the revision
			// of the submitted resolved file
			List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(deleteTargetFile),
					new GetExtendedFilesOptions().setSinceChangelist(changelist
							.getId()));
			assertNotNull(extendedFiles);
			assertTrue(FileAction.DELETE == extendedFiles.get(0)
					.getHeadAction());
			assertTrue(2 == extendedFiles.get(0).getHeadRev());
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
							.makeFileSpecList(new String[] { copyTargetFile,
									deleteTargetFile, branchTargetFile }),
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
