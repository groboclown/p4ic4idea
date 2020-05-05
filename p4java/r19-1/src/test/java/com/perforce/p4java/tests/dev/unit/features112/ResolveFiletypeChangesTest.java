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
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
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
 * Test Resolve files with filetype changes.
 * 
 * Setup:
 * 
 * Copy a file to the copy target <br>
 * Integrate the copy target file to the release target <br>
 * Edit the copy target file with change of filetype <br>
 * Integrate the edited copy target file to the release target <br>
 * Integrate a file with schedule 'branch resolves' (-Rb) <br>
 * Resolve files with 'resolve filetype changes' (-At)
 * 
 * Check:
 * 
 * Only the file with a filetype change will be attempted to be resolved <br>
 * The branching file will not be attempted to be resolved <br>
 * 
 * Verify:
 * 
 * Make sure the submitted files have revision histories with the correct
 * resolve actions
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveFiletypeChangesTest")
public class ResolveFiletypeChangesTest extends P4JavaTestCase {

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
     * Save new client line end in spec and return the setting that was replaced.
     *
     * @param client the client
     * @param newLineEnd the new line end
     * @return the i client summary. client line end
     * @throws ConnectionException the connection exception
     * @throws RequestException the request exception
     * @throws AccessException the access exception
     */
    private IClientSummary.ClientLineEnd saveNewClientLineEndInSpec(IClient client,
            IClientSummary.ClientLineEnd newLineEnd)
            throws ConnectionException, RequestException, AccessException {
        // get the old line ending
        assertNotNull("Client object is null.", client);
        IClientSummary.ClientLineEnd oldLineEnd = client.getLineEnd();
        // set the new line ending
        client.setLineEnd(newLineEnd);
        client.update();
        // check that the changes stuck
        IClientSummary.ClientLineEnd cLineEnd = client.getLineEnd();
        debugPrint("setLineEnd to: " + newLineEnd, "OLD: " + oldLineEnd, "NEW: " + cLineEnd);
        return oldLineEnd;
    }

	/**
	 * Test resolve filetype changes (-At)
	 */
	@Test
	public void testResolveFiletypeChanges() {
		int randNum = getRandomInt();
		String dir = "main" + randNum;
		String dir2 = "release" + randNum;

		// Source and target files and directories for copy
		String copySourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String copyTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";
		String copyTargetDir = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/...";
		String releaseTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir2 + "/MessagesBundle_es.properties";
		String releaseTargetDir = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir2 + "/...";

		// Source and target files for integrate with schedule 'branch resolves'
		String sourceFile2 = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_cs.properties";
		String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_cs.properties";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		IChangelist changelist = null;
		IClientSummary.ClientLineEnd oldLineEnd = null;
		try {
		 // Set to shared so that there is no auto conversion of lineendings when trying to 
            // resolve otherwise CRLF vs LF change on the same line in Windows will not be
            // seen as a conflict. Keep the old line end so we can restore it at the end of
            // the test.
            oldLineEnd =
                    saveNewClientLineEndInSpec(client, IClientSummary.ClientLineEnd.SHARE);
			// Create a changelist for copy
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveFiletypeChangesTest copy changelist");
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

			// Create a changelist for integrate
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveFiletypeChangesTest integrate changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Run integrate files method
			List<IFileSpec> integrateFiles = client.integrateFiles(
					new FileSpec(copyTargetDir),
					new FileSpec(releaseTargetDir), null,
					new IntegrateFilesOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(integrateFiles);

			// Submit the file in the integrate changelist
			changelist.refresh();
			List<IFileSpec> integrateFilesList = changelist.submit(null);
			assertNotNull(integrateFilesList);

			// Create a changelist for edit
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveFiletypeChangesTest changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Open file for edit
			List<IFileSpec> editFiles = client.editFiles(FileSpecBuilder
					.makeFileSpecList(copyTargetFile), new EditFilesOptions()
					.setChangelistId(changelist.getId()).setFileType("binary"));
			assertNotNull(editFiles);

			// Submit the file in the edit changelist
			changelist.refresh();
			List<IFileSpec> editFilesList = changelist.submit(null);
			assertNotNull(editFilesList);

			// Create a changelist for integrate
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveFiletypeChangesTest changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Run integrate files method
			List<IFileSpec> integrateFiles2 = client.integrateFiles(
					new FileSpec(copyTargetDir),
					new FileSpec(releaseTargetDir), null,
					new IntegrateFilesOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(integrateFiles2);
			List<IFileSpec> invalidFiles2 = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles2);
			if (invalidFiles2.size() != 0) {
				fail(invalidFiles2.get(0).getOpStatus() + ": "
						+ invalidFiles2.get(0).getStatusMessage());
			}

			// Run integrate files method
			List<IFileSpec> integrateFiles3 = client.integrateFiles(
					new FileSpec(sourceFile2),
					new FileSpec(targetFile2),
					null,
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setBranchResolves(true));
			assertNotNull(integrateFiles3);
			List<IFileSpec> invalidFiles3 = FileSpecBuilder
					.getInvalidFileSpecs(integrateFiles3);
			if (invalidFiles3.size() != 0) {
				fail(invalidFiles3.get(0).getOpStatus() + ": "
						+ invalidFiles3.get(0).getStatusMessage());
			}

			// Run resolve with 'resolve filetype changes'
			List<IFileSpec> resolveFiles = client
					.resolveFilesAuto(
							FileSpecBuilder.makeFileSpecList(new String[] {
									releaseTargetDir, targetFile2 }),
							new ResolveFilesAutoOptions().setChangelistId(
									changelist.getId())
									.setResolveFiletypeChanges(true));
			assertNotNull(resolveFiles);

			// Check for correct number of filespecs
			assertEquals(3, resolveFiles.size());

			// Validate file actions
			// Check "how" it is resolved
			assertEquals(FileSpecOpStatus.VALID, resolveFiles.get(1)
					.getOpStatus());
			assertTrue(resolveFiles.get(1).getHowResolved()
					.contentEquals("copy from"));

			// Refresh after resolve
			changelist.refresh();

			// Submit should fail, since one file is not resolved
			List<IFileSpec> submitFiles = changelist
					.submit(new SubmitOptions());
			assertNotNull(submitFiles);

			// Check for the "must resolve" message with 'branch' file
			boolean mustResolve = false;
			for (IFileSpec file : submitFiles) {
				if (file.getOpStatus() == FileSpecOpStatus.INFO) {
					if (file.getStatusMessage() != null) {
						if (file.getStatusMessage().contains(
								" - must resolve " + sourceFile2)) {
							mustResolve = true;
							break;
						}
					}
				}
			}
			assertTrue(mustResolve);

			// Finally, try resolving all the files and do a successful submit
			List<IFileSpec> resolveFiles2 = client.resolveFilesAuto(
					FileSpecBuilder.makeFileSpecList(new String[] {
							releaseTargetDir, targetFile2 }),
					new ResolveFilesAutoOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(resolveFiles2);
			changelist.refresh();
			List<IFileSpec> submitFiles2 = changelist
					.submit(new SubmitOptions());
			assertNotNull(submitFiles2);

			// There should be three filespecs (triggers)
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
							FileSpecBuilder.makeFileSpecList(releaseTargetFile),
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
			assertEquals(releaseTargetFile, fileSpec.getDepotPathString());

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
			assertEquals(FileAction.INTEGRATE, fileRevisionData.getAction());

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
			assertEquals(copyTargetFile, revisionIntegrationData2.getFromFile());
			assertNotNull(revisionIntegrationData2.getHowFrom());
			assertEquals("branch from", revisionIntegrationData2.getHowFrom());

			// Use fstat to verify the file action and file type on the revision
			// of the submitted resolved file
			List<IExtendedFileSpec> extendedFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(releaseTargetFile),
					new GetExtendedFilesOptions().setSinceChangelist(changelist
							.getId()));
			assertNotNull(extendedFiles);
			assertTrue(FileAction.INTEGRATE == extendedFiles.get(0)
					.getHeadAction());
			assertTrue(2 == extendedFiles.get(0).getHeadRev());
			assertTrue(extendedFiles.get(0).getHeadType()
					.contentEquals("binary"));

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
							.makeFileSpecList(new String[] { copyTargetDir,
									releaseTargetDir, targetFile2 }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
					if (oldLineEnd != null) {
                        saveNewClientLineEndInSpec(client, oldLineEnd);
                    }
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
