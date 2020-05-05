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
import com.perforce.p4java.option.client.MergeFilesOptions;
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
 * Test 'p4 merge'
 */
@Jobs({ "job046667" })
@TestId("Dev112_ResolveContentChangesTest")
public class MergeFilesTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	IChangelist changelist = null;

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
	 * Test merge files
	 */
	@Test
	public void testMergeFiles() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files for integrate with schedule 'branch resolves'
		String originalSpanish = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String ourBranchOfSpanish = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		// Source and target files for integrate with content changes
		String originalItalian = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
		String ourBranchOfItalian = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + "/MessagesBundle_it.properties";
		String ourItalianContentChange = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + "/contentchanges/MessagesBundle_it.properties";

		String testText = "///// added test text " + randNum + " /////";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		try {
			// Setup the server connection and client workspace
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);

			// Create our branch of the italian properties file
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest create an italian branch changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
					originalItalian), new FileSpec(ourBranchOfItalian), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertTrue("There should be 1 entries, for the new path", copyFiles.size() == 1);
			changelist.refresh();
			List<IFileSpec> submitItalianBranchOutput = changelist.submit(new SubmitOptions());
			assertTrue("There should be 5 entries, 1 path and 4 messages", submitItalianBranchOutput.size() == 5);

			// Integrate targetFile2 to targetFile3 for content changes setup
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest create contentchanges copy");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> integrateItalianContentChangesResponse = client
					.integrateFiles(new FileSpec(ourBranchOfItalian), FileSpecBuilder.makeFileSpecList(
							ourItalianContentChange), new IntegrateFilesOptions()
							.setChangelistId(changelist.getId()));
			assertTrue("There should be 1 entries, for the updated path", integrateItalianContentChangesResponse.size() == 1);
			changelist.refresh();
			List<IFileSpec> submitItalianContentChangeIntegrationResponse = changelist
					.submit(new SubmitOptions());
			assertTrue("There should be 5 messages in the submit response, 1 valid filepath and 4 infos",submitItalianContentChangeIntegrationResponse.size()==5);
			boolean foundValidFile = false;
			for (IFileSpec message: submitItalianContentChangeIntegrationResponse){
				if ( message.getOpStatus().equals(FileSpecOpStatus.VALID)) {
					foundValidFile = true;
				}
			}
			assertTrue("Output from submit should include a valid filepath. " + submitItalianContentChangeIntegrationResponse, foundValidFile);

			// Update targetFile2
			List<IFileSpec> syncFiles = client.sync(
					FileSpecBuilder.makeFileSpecList(ourBranchOfItalian),
					new SyncOptions().setForceUpdate(true));
			assertTrue("Sync output should contain one entry, the synced path.",syncFiles.size() == 1);

			// Edit targetFile2
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest edit files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> editFiles = client.editFiles(
					FileSpecBuilder.makeFileSpecList(ourBranchOfItalian),
					new EditFilesOptions().setChangelistId(changelist.getId()));
			assertTrue("The edit files output should contain 1 entry, the path being edited.", editFiles.size() == 1);

			// Append some text to targetFile2
			String clientPath = null;
			for(IFileSpec file: editFiles){
				if (file.getOpStatus().equals(FileSpecOpStatus.VALID)){
					clientPath = file.getClientPathString();
				}
			}
			assertNotNull("Could not find a valid client path in edit response" + editFiles, clientPath);
			writeFileBytes(clientPath, testText,
					true);

			// Submit the changes
			changelist.refresh();
			List<IFileSpec> editFiles2 = changelist.submit(new SubmitOptions());
			assertTrue("The edit files output should contain 5 entries, the path being edited and 4 messages.", editFiles2.size() == 5);

			// Create changelist merge files
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveContentChangesTest merge files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Run merge
			List<IFileSpec> mergeFiles = client
					.mergeFiles(new FileSpec(originalSpanish), FileSpecBuilder.makeFileSpecList(
							ourBranchOfSpanish), new MergeFilesOptions()
							.setChangelistId(changelist.getId()));

			// Check for null
			assertTrue("The merge files output should contain 1 entry, the path being merged.", mergeFiles.size() == 1);

			// Check for invalid filespecs
			List<IFileSpec> invalidFiles = FileSpecBuilder
					.getInvalidFileSpecs(mergeFiles);
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

			// Validate file action type.
			assertEquals(FileAction.DELETE, changelistFiles.get(0).getAction());
			
			List<IFileSpec> editFiles3 = changelist.submit(new SubmitOptions());
			assertNotNull(editFiles3);

			// Run merge with normal content changes
			List<IFileSpec> mergeFiles2 = client.integrateFiles(
					new FileSpec(ourBranchOfItalian),
					new FileSpec(ourItalianContentChange),
					null,
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setForceIntegration(true));

			// Check for null
			assertNotNull(mergeFiles2);
			// Expect can't integrate already open for delete, probably need to resolve
			// Check for invalid filespecs
			List<IFileSpec> invalidFiles2 = FileSpecBuilder
					.getInvalidFileSpecs(mergeFiles2);
			if (invalidFiles2.size() != 0) {
				fail(invalidFiles2.get(0).getOpStatus() + ": "
						+ invalidFiles2.get(0).getStatusMessage());
			}

			// Build a list of files that have an outstanding resolve
			List<IFileSpec> allFiles = new ArrayList<IFileSpec>();
			allFiles.addAll(FileSpecBuilder.makeFileSpecList(
					ourBranchOfSpanish, ourItalianContentChange));

			// Run resolve with 'resolve file content changes'
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(
					allFiles,
					new ResolveFilesAutoOptions().setChangelistId(
							changelist.getId()).setResolveFileContentChanges(
							true));

			// Check for the correct number of replies
			assertTrue("Response from resolve should have 5 entries", resolveFiles.size() == 4);

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
			List<IFileSpec> submitChangelistResponse = changelist
					.submit(new SubmitOptions());

			// Check for null, 2 entries ..must resolve.. and 
			assertTrue("Should be 2 messages; ...must resolve... and a cannot submit", submitChangelistResponse.size() == 2);
			// Run resolve with 'resolve file content changes'
			List<IFileSpec> resolveBranchedDeletionsResponse = client.resolveFilesAuto(
					allFiles,
					new ResolveFilesAutoOptions().setChangelistId(
							changelist.getId()).setResolveFileBranching(
							true));
			assertTrue(resolveBranchedDeletionsResponse.size() ==3); // should be 3 including valid resovetype=branch & valid how resolved branch from
			boolean foundBranchTo = false;
			boolean foundBranchFrom = false;
			for(IFileSpec response:resolveBranchedDeletionsResponse) {
				if ( FileSpecOpStatus.VALID.equals(response.getOpStatus())){
					if ( "branch".equals(response.getResolveType())) {
						foundBranchTo = true;
					} else if ( "branch from".equals(response.getHowResolved())){
						foundBranchFrom = true;
					}
				}
			}
			assertTrue("Resolve should have replied with branch to("+foundBranchTo+") and branch from("+foundBranchFrom+") messages", foundBranchTo && foundBranchFrom);
			submitChangelistResponse = changelist
					.submit(new SubmitOptions());
			// Check for correct number of responses including 2 valid filespecs and a submit message
			assertEquals(6, submitChangelistResponse.size());

			// Check the statuses and file actions of the two submitted files
			assertEquals(FileSpecOpStatus.VALID, submitChangelistResponse.get(3)
					.getOpStatus());
			assertEquals(FileAction.BRANCH, submitChangelistResponse.get(3).getAction());
			assertEquals(FileSpecOpStatus.VALID, submitChangelistResponse.get(4)
					.getOpStatus());
			assertEquals(FileAction.INTEGRATE, submitChangelistResponse.get(4).getAction());

			// Check for 'Submitted as change' in the info message
			assertTrue(submitChangelistResponse.get(5).getStatusMessage()
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
							FileSpecBuilder.makeFileSpecList(ourItalianContentChange),
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
			assertEquals(ourItalianContentChange, fileSpec.getDepotPathString());

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
			assertEquals(ourBranchOfItalian, revisionIntegrationData.getFromFile());
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
			assertEquals(ourBranchOfItalian, revisionIntegrationData2.getFromFile());
			assertNotNull(revisionIntegrationData2.getHowFrom());
			assertEquals("branch from", revisionIntegrationData2.getHowFrom());

			// Use fstat to verify the file action and file type on the revision
			// of the submitted resolved file
			List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(ourItalianContentChange),
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

				if (server != null) {
					try {
						// Delete submitted test files
						IChangelist deleteChangelist = getNewChangelist(server,
								client,
								"Dev112_IntegrateDeleteActionTest delete submitted test files changelist");
						deleteChangelist = client
								.createChangelist(deleteChangelist);
						client.deleteFiles(FileSpecBuilder
								.makeFileSpecList(new String[] { ourBranchOfSpanish,
										ourBranchOfItalian, ourItalianContentChange }),
								new DeleteFilesOptions()
										.setChangelistId(deleteChangelist
												.getId()));
						deleteChangelist.refresh();
						deleteChangelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
