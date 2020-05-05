/**
 *
 */
package com.perforce.p4java.tests.dev.unit.features112;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test resolve file -dw: Ignore whitespace altogether (for instance, deletion
 * of tabs or other whitespace)
 */
@Jobs({ "job046102" })
@TestId("Dev112_ResolveIgnoreWhitespaceTest")
public class ResolveForceTextualMergeTest extends P4JavaRshTestCase {

	public static final String LINE_SEPARATOR = System.getProperty(
			"line.separator", "\n");
	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ResolveForceTextualMergeTest.class.getSimpleName());

     /**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);;
			createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties", "desc");
		} catch (Exception e) {
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
	 * Test resolve file -dw: Ignore whitespace altogether (for instance,
	 * deletion of tabs or other whitespace)
	 */
	@Test
	public void testResolveIgnoreWhitespace() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files for integrate with content changes
		String sourceFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_it.properties";
		String targetFile = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + "/MessagesBundle_it.properties";
		String targetFile2 = "//depot/112Dev/GetOpenedFilesTest/src/gnu/getopt/"
				+ dir + randNum + "/MessagesBundle_it.properties";

		String testText = "///// added test text 1 ///// - " + randNum
				+ LINE_SEPARATOR;
		String testText2 = "///// added test text 2 ///// - " + randNum
				+ LINE_SEPARATOR;

		// Error message indicating merges still pending
		String mergesPending = "Merges still pending -- use 'resolve' to merge files.";

		// Info message indicating submitted change
		String submittedChange = "Submitted as change";

		try {
			// Copy a file to be used as a target for integrate content changes
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveIgnoreWhitespaceTest copy files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);
			List<IFileSpec> files = client.copyFiles(new FileSpec(sourceFile),
					new FileSpec(targetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Integrate targetFile to targetFile2
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveIgnoreWhitespaceTest integrate files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);
			List<IFileSpec> integrateFiles = client.integrateFiles(
					new FileSpec(targetFile), new FileSpec(targetFile2), null,
					new IntegrateFilesOptions().setChangelistId(changelist
							.getId()));
			assertNotNull(integrateFiles);
			changelist.refresh();
			integrateFiles = changelist.submit(new SubmitOptions());
			assertNotNull(integrateFiles);

			// Edit targetFile (change type to binary)
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveIgnoreWhitespaceTest edit files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);
			files = client.editFiles(FileSpecBuilder
					.makeFileSpecList(targetFile), new EditFilesOptions()
					.setChangelistId(changelist.getId()).setFileType("binary"));
			assertNotNull(files);

			// Append some text (tab between the brackets) to targetFile
			assertNotNull(files.get(0).getClientPathString());
			writeFileBytes(files.get(0).getClientPathString(), testText, true);

			// Submit the changes
			changelist.refresh();
			List<IFileSpec> submittedFiles = changelist
					.submit(new SubmitOptions());
			assertNotNull(submittedFiles);

			// Edit targetFile2
			changelist = getNewChangelist(server, client,
					"Dev112_ResolveIgnoreWhitespaceTest edit files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);
			files = client.editFiles(
					FileSpecBuilder.makeFileSpecList(targetFile2),
					new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);

			// Append some text (spaces between the brackets) to targetFile2
			assertNotNull(files.get(0).getClientPathString());
			writeFileBytes(files.get(0).getClientPathString(), testText2, true);

			// Submit the changes
			changelist.refresh();
			submittedFiles = changelist.submit(new SubmitOptions());
			assertNotNull(submittedFiles);

			changelist = getNewChangelist(server, client,
					"Dev112_ResolveIgnoreWhitespaceTest integrate files changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Run integrate
			integrateFiles = client.integrateFiles(new FileSpec(targetFile),
					new FileSpec(targetFile2), null,
					new IntegrateFilesOptions().setChangelistId(changelist
							.getId()));

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

			// Validate file action type
			assertEquals(FileAction.INTEGRATE, changelistFiles.get(0)
					.getAction());

			// Run resolve with 'resolve file content changes'
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(
					integrateFiles, new ResolveFilesAutoOptions()
							.setChangelistId(changelist.getId()));

			// Check for null
			assertNotNull(resolveFiles);

			// Check for correct number of filespecs
			assertEquals(5, resolveFiles.size());

			// Check file operation status info message
			assertEquals(FileSpecOpStatus.INFO, resolveFiles.get(3)
					.getOpStatus());
			assertTrue(resolveFiles
					.get(3)
					.getStatusMessage()
					.contains(
							"Diff chunks: 0 yours + 0 theirs + 0 both + 1 conflicting"));

			// Refresh changelist
			changelist.refresh();

			// Submit should fail, since the file with branching is not resolved
			List<IFileSpec> submitFiles = changelist
					.submit(new SubmitOptions());

			// Check for null
			assertNotNull(submitFiles);

			// Check for correct number of filespecs
			assertEquals(3, submitFiles.size());

			// Check for 'must resolve' and 'Merges still pending' in info and
			// error messages
			assertEquals(FileSpecOpStatus.INFO, submitFiles.get(0)
					.getOpStatus());
			assertTrue(submitFiles.get(0).getStatusMessage()
					.contains(" - must resolve " + targetFile));
			assertEquals(FileSpecOpStatus.ERROR, submitFiles.get(2)
					.getOpStatus());
			assertTrue(submitFiles.get(2).getStatusMessage()
					.contains(mergesPending));

			// Resolving the file with the forceTextualMerge option
			resolveFiles = client.resolveFilesAuto(
					files,
					new ResolveFilesAutoOptions()
							.setChangelistId(changelist.getId())
							.setAcceptTheirs(true).setForceTextualMerge(true));
			assertNotNull(resolveFiles);
			changelist.refresh();
			submitFiles = changelist.submit(new SubmitOptions());
			assertNotNull(submitFiles);

			// There should be 2 filespecs 
			assertEquals(2, submitFiles.size());

			// Check the status and file action of the submitted file
			assertEquals(FileSpecOpStatus.VALID, submitFiles.get(0)
					.getOpStatus());
			assertEquals(FileAction.INTEGRATE, submitFiles.get(0).getAction());

			// Check for 'Submitted as change' in the info message
			assertTrue(submitFiles.get(1).getStatusMessage()
					.contains(submittedChange + " " + changelist.getId()));

			// Make sure the changelist is submitted
			changelist.refresh();
			assertTrue(changelist.getStatus() == ChangelistStatus.SUBMITTED);

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
							"Dev112_ResolveIgnoreWhitespaceTest delete submitted test files changelist");
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
