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
import com.perforce.p4java.core.IFileDiff;
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
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test Resolve files with content changes.
 * 
 * Setup:
 * 
 * Integrate a file with schedules 'branch resolves' (-Rb) <br>
 * Integrate a file with content changes <br>
 * Resolve files with 'resolve file content changes' (-Ac)
 * 
 * Check:
 * 
 * Only the file with content changes will be attempted to be resolved <br>
 * The branching file will not be attempted to be resolved
 * 
 * Verify:
 * 
 * Make sure the submitted files have revision histories with the correct
 * resolve actions
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveContentChangesTest")
public class ResolveContentChangesTest extends P4JavaTestCase {

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
	 * Test resolve file content changes (-Ac)
	 */
	@Test
	public void testResolveFileContentChanges() {
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
		String targetFile3 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + "/contentchanges/MessagesBundle_it.properties";

		String testText = "///// added test text " + randNum + " /////";

		// Error message indicating merges still pending
		String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		IChangelist changelist = null;

		try {
			// Copy a file to be used as a target for integrate content changes
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest copy files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
					sourceFile2), new FileSpec(targetFile2), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(copyFiles);
			changelist.refresh();
			List<IFileSpec> copyFiles2 = changelist.submit(new SubmitOptions());
			assertNotNull(copyFiles2);

			// Integrate targetFile2 to targetFile3 for content changes setup
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest integrate files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> contentFiles = client.integrateFiles(new FileSpec(
					targetFile2), new FileSpec(targetFile3), null,
					new IntegrateFilesOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(contentFiles);
			changelist.refresh();
			List<IFileSpec> contentFiles2 = changelist
					.submit(new SubmitOptions());
			assertNotNull(contentFiles2);

			// Update targetFile2
			List<IFileSpec> syncFiles = client.sync(
					FileSpecBuilder.makeFileSpecList(targetFile2),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles);

			// Edit targetFile2
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest edit files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> editFiles = client.editFiles(
					FileSpecBuilder.makeFileSpecList(targetFile2),
					new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editFiles);

			// Append some text to targetFile2
			assertNotNull(editFiles.get(0).getClientPathString());
			writeFileBytes(editFiles.get(0).getClientPathString(), testText,
					true);

			// Submit the changes
			changelist.refresh();
			List<IFileSpec> editFiles2 = changelist.submit(new SubmitOptions());
			assertNotNull(editFiles2);

			// Create changelist for 'integrate -Rb'
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest 'integrate -Rb' changelist");
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
					new FileSpec(targetFile2),
					new FileSpec(targetFile3),
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

			// Run resolve with 'resolve file content changes'
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(
					allFiles,
					new ResolveFilesAutoOptions().setChangelistId(
							changelist.getId()).setResolveFileContentChanges(
							true));

			// Check for null
			assertNotNull(resolveFiles);
			assertTrue("Automatic resolve failed to return any filespec data.",
			        resolveFiles != null && resolveFiles.size() > 0);

			// Check for correct number of filespecs
			List<String> errorMessages = P4JavaTestCase.getErrorsFromFileSpecList(resolveFiles);
			// Expect 1 error, the branch of messages_es will not need to be resolved
			assertTrue("Automatic resolve returned more than 1 error, " + errorMessages, errorMessages.size() <= 1);
			assertTrue("Automatic resolve should have returned 4 filespecs, it returned "
			        + resolveFiles.size() + ", " + resolveFiles, 4 == resolveFiles.size());

			// Validate file actions
			// Check "how" it is resolved
			assertEquals(FileSpecOpStatus.INFO, resolveFiles.get(2)
					.getOpStatus());
			assertTrue(resolveFiles
					.get(2)
					.getStatusMessage()
					.contains(
							"Diff chunks: 0 yours + 1 theirs + 0 both + 0 conflicting"));
			assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(3)
					.getOpStatus());
			assertTrue(resolveFiles.get(3).getHowResolved()
					.contentEquals("copy from"));

			// Refresh changelist
			changelist.refresh();

			// Submit should fail, since the file with branching is not resolved
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
					.contains(" - must resolve " + sourceFile));
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
							FileSpecBuilder.makeFileSpecList(targetFile3),
							new GetRevisionHistoryOptions().setChangelistId(
									changelist.getId()).setMaxRevs(10));

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
			assertEquals(targetFile3, fileSpec.getDepotPathString());

			// Make sure we have the revision data
			List<IFileRevisionData> fileRevisionDataList = entry.getValue();
			assertNotNull(fileRevisionDataList);

			// There should be two revision data
			assertEquals(2, fileRevisionDataList.size());

			// LATEST REVISION DATA

			// Verify this revision actually did the correct resolve action
			IFileRevisionData fileRevisionData = fileRevisionDataList.get(0);
			assertNotNull(fileRevisionData);
			assertNotNull(fileRevisionData.getAction());
			assertEquals(FileAction.INTEGRATE, fileRevisionData.getAction());

			// There should be one revision integration data for this revision
			List<IRevisionIntegrationData> revisionIntegrationDataList = fileRevisionData
					.getRevisionIntegrationDataList();
			assertNotNull(revisionIntegrationDataList);
			assertEquals(1, revisionIntegrationDataList.size());

			// Verify this revision integration data contains correct resolve
			// action info for this revision
			IRevisionIntegrationData revisionIntegrationData = revisionIntegrationDataList
					.get(0);
			assertNotNull(revisionIntegrationData);
			assertNotNull(revisionIntegrationData.getFromFile());
			assertEquals(targetFile2, revisionIntegrationData.getFromFile());
			assertNotNull(revisionIntegrationData.getHowFrom());
			assertEquals("copy from", revisionIntegrationData.getHowFrom());

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
			assertEquals(targetFile2, revisionIntegrationData2.getFromFile());
			assertNotNull(revisionIntegrationData2.getHowFrom());
			assertEquals("branch from", revisionIntegrationData2.getHowFrom());

			// Use fstat to verify the file action and file type on the revision
			// of the submitted resolved file
			List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(targetFile3),
					new GetExtendedFilesOptions().setSinceChangelist(changelist
							.getId()));
			assertNotNull(extendedFiles);
			assertTrue(FileAction.INTEGRATE == extendedFiles.get(0)
					.getHeadAction());
			assertTrue(2 == extendedFiles.get(0).getHeadRev());
			assertTrue(extendedFiles.get(0).getHeadType().contentEquals("text"));

			// Use diff2 to verify the revision is a content change
			FileSpec currentRev = new FileSpec(extendedFiles.get(0)
					.getDepotPathString()
					+ "#"
					+ extendedFiles.get(0).getHeadRev());
			FileSpec previousRev = new FileSpec(extendedFiles.get(0)
					.getDepotPathString()
					+ "#"
					+ (extendedFiles.get(0).getHeadRev() - 1));
			List<IFileDiff> fileDiffs = server.getFileDiffs(previousRev,
					currentRev, null, new GetFileDiffsOptions());
			assertNotNull(fileDiffs);
			assertTrue(IFileDiff.Status.CONTENT == fileDiffs.get(0).getStatus());

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
									targetFile2, targetFile3 }),
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
