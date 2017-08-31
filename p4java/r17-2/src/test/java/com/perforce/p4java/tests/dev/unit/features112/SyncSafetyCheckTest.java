/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the sync with safety check.
 * 
 * This flag prevents the user from overwriting changes when sync'ing down the
 * latest version.
 * 
 * Setup:
 * 
 * Sync a file from depot //depot/file#1 <br>
 * Make some edits to the sync'd file <br>
 * Leave permission of the file readonly <br>
 * Add new revision of //depot/file#2 to repository from another client <br>
 * Run sync with the '-s' flag
 * 
 * Result:
 * 
 * Message: "//depot/file#2 - can't update modified file /tmp/depot/file"
 */
@Jobs({ "job046091" })
@TestId("Dev112_SyncSafetyCheckTest")
public class SyncSafetyCheckTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	IOptionsServer server2 = null;
	IClient client2 = null;

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

			server2 = getServer(this.serverUrlString, null, "p4jtestuser2",
					"p4jtestuser2");
			assertNotNull(server2);
			client2 = server2.getClient("p4TestUserWS2");
			assertNotNull(client2);
			server2.setCurrentClient(client2);
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
		if (server2 != null) {
			this.endServerSession(server2);
		}
	}

	/**
	 * Test sync of files with safety check (-s)
	 */
	@Test
	public void testSyncFilesWithSafetyCheck() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files for integrate with schedule 'branch resolves'
		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		IChangelist changelist = null;
		IChangelist changelist2 = null;

		try {
			// Copy a file to be used as a target for integrate content changes
			changelist = getNewChangelist(server, client,
					"Dev112_SyncSafetyCheckTest copy files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> copyFiles = client.copyFiles(new FileSpec(
					sourceFile), new FileSpec(targetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(copyFiles);
			changelist.refresh();
			List<IFileSpec> copyFiles2 = changelist.submit(new SubmitOptions());
			assertNotNull(copyFiles2);

			// Sync the test file
			List<IFileSpec> syncFiles = client.sync(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles);

			List<IFileSpec> invalidFiles = FileSpecBuilder
					.getInvalidFileSpecs(syncFiles);
			if (invalidFiles.size() != 0) {
				fail(invalidFiles.get(0).getOpStatus() + ": "
						+ invalidFiles.get(0).getStatusMessage());
			}
			assertEquals("Wrong number of valid filespecs after sync", 1,
					FileSpecBuilder.getValidFileSpecs(syncFiles).size());

			File file = new File(syncFiles.get(0).getClientPathString());
			assertNotNull(file.exists());

			// Modifying the file outside of Perforce: set file to writable
			file.setWritable(true);
			writeFileBytes(file.getCanonicalPath(), "// test text", true);

			// Use client2 to sync the test file to client2's workspace
			List<IFileSpec> syncFiles2 = client2.sync(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles2);

			List<IFileSpec> invalidFiles2 = FileSpecBuilder
					.getInvalidFileSpecs(syncFiles2);
			if (invalidFiles2.size() != 0) {
				fail(invalidFiles2.get(0).getOpStatus() + ": "
						+ invalidFiles2.get(0).getStatusMessage());
			}
			assertEquals("Wrong number of valid filespecs after sync", 1,
					FileSpecBuilder.getValidFileSpecs(syncFiles2).size());

			File file2 = new File(syncFiles2.get(0).getClientPathString());
			assertNotNull(file2.exists());

			// Use client2 to open the test file for edit
			changelist2 = getNewChangelist(server2, client2,
					server2.getUser("p4jtestuser2"),
					"Dev112_SyncSafetyCheckTest edit files changelist2");
			assertNotNull(changelist2);
			changelist2 = client2.createChangelist(changelist2);
			List<IFileSpec> editFiles = client2
					.editFiles(FileSpecBuilder.makeFileSpecList(targetFile),
							new EditFilesOptions().setChangelistId(changelist2
									.getId()));
			assertNotNull(editFiles);

			// Use client2 to modify the test file and submit the changes as a
			// new revision
			assertNotNull(editFiles.get(0).getClientPathString());
			writeFileBytes(editFiles.get(0).getClientPathString(),
					"// test text", true);
			changelist2.refresh();
			List<IFileSpec> submitFiles = changelist2
					.submit(new SubmitOptions());
			assertNotNull(submitFiles);

			Map<String, Boolean> resultMap = new HashMap<String, Boolean>();
			// Check for 'Submitted as change' in the info message
			for (IFileSpec fileSpec : submitFiles){
				if (fileSpec.getOpStatus() == FileSpecOpStatus.INFO &&
						fileSpec.getStatusMessage().contains(submittedChange + " " + changelist2.getId())){
					resultMap.put("submitted", true);
				}
			}
			assertTrue(resultMap.get("submitted"));

			// Make sure the changelist2 is submitted
			changelist2.refresh();
			assertTrue(changelist2.getStatus() == ChangelistStatus.SUBMITTED);

			// Use the first client to do a safe (-s) sync of the test file
			syncFiles = client.sync(
					FileSpecBuilder.makeFileSpecList(targetFile),
					new SyncOptions().setSafetyCheck(true));
			assertNotNull(syncFiles);
			
			String expectedMessage = " - can't update modified file " + file.getPath();
			for (IFileSpec fileSpec : syncFiles){
				if (fileSpec.getOpStatus() == FileSpecOpStatus.INFO &&
						fileSpec.getStatusMessage().contains(expectedMessage)){
					resultMap.put("cantUpdate", true);
				}
			}
			// Check for an info message " - can't update modified file "
			assertTrue(resultMap.get("cantUpdate"));

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
							client2.revertFiles(changelist2.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist2
													.getId()));
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
							"Dev112_SyncSafetyCheckTest delete submitted test files changelist");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { targetFile,
									targetFile }), new DeleteFilesOptions()
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
