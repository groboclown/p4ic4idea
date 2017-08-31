package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.VerifyFileSpec;

/**
 *
 * The IFileSpecE2ETest class exercises the FileSpec class as it affects files. The fileSpecs
 * are created with various fields set, and the files are then added to the depot using
 * client.addFiles(). The results are verified at the fileSpec field level, and at the depot
 * level (i.e. does the file exist).
 */


@TestId("IFileSpecE2ETest01")
public class IFileSpecE2ETest extends P4JavaTestCase {

	private static IClient client = null;
	private static String sourceFile;
	private static String clientDir;
	
	@BeforeClass
	public static void beforeAll() throws Exception {
		server = getServer();
		client = getDefaultClient(server);
		clientDir = defaultTestClientName + File.separator + testId;
		server.setCurrentClient(client);
		sourceFile = client.getRoot() + File.separator + textBaseFile;
		createTestSourceFile(sourceFile, false);
	}

	/**
	 * Add one text file into default changelist and verify the filespec info.
	 */
	@Test
	public void testFileSpecFromAddFilesText() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;
		try {
			
			debugPrintTestName();
			
			assertNotNull("client should not be Null.", client);
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			//verifyFileSpecMethods(buildFileSpecs.get(0), fSpec0);
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);

			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * Add one text file into numbered changelist. Verify the filespec info.
	 */
	@Test
	public void testFileSpecFromAddFilesTextNumberedChangelist() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;

		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			//add the files, submit them, reopen them
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			fSpec0.setExpChangelistId(changelist.getId());
			List<IFileSpec> buildSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildSpecs, "Built file Specs");
			dumpFileSpecMethods(buildSpecs.get(0), "Built file Specs");
			
			//verifyFileSpecMethods(buildFileSpecs.get(0), fSpec0);
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			changelist.refresh();
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			addedSpecs = changelist.getFiles(false);
			fSpec0.setExpOriginalPath(null);
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_EXTENDED);

			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * Add one text file into numbered changelist, don't set FileAction and verify the filespec info.
	 * This test exists to make sure bug job038193 did not occur due to FileAction being set before
	 * the action occurred on the filespec. The bug does not occur.
	 */
	@Test
	public void testFileSpecFromAddFilesTextSetNoAction() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;

		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			//add the files, submit them, reopen them
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			fSpec0.setExpChangelistId(changelist.getId());
			List<IFileSpec> buildSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildSpecs, "Built file Specs");
			dumpFileSpecMethods(buildSpecs.get(0), "Built file Specs");

			//verifyFileSpecMethods(buildFileSpecs.get(0), fSpec0);
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			//now set the exp FileAction for verification of final filespec
			fSpec0.setExpAction(FileAction.ADD);
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);

			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * Add one binary file into default changelist and verify the filespec info.
	 */
	@Test
	public void testFileSpecFromAddFileBinary() {
		int expNumValidFSpecs = 1;  // We create 3 files, so we expect three files to be returned.
		int expNumInvalidFSpecs = 0;

		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "bindetmi2.dll";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpChangelistId(IChangelist.DEFAULT);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_BINARY);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			//verifyFileSpecMethods(buildFileSpecs.get(0), fSpec0);
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_BINARY);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * Add one text file into default changelist and verify the filespec info.
	 */
	@Test
	public void testFileSpecFromReopenedFileText() {
		int expNumValidFSpecs = 1;  
		int expNumInvalidFSpecs = 0;

		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpChangelistId(Changelist.UNKNOWN);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");			
			assertEquals(expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals(expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			
			//create a new changelist 
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			//reopen the files in the new changelist
			List<IFileSpec> reopenedFileSpecs = client.reopenFiles(buildFileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
			
			//set the expected values
			fSpec0.setExpChangelistId(changelist.getId());
			fSpec0.setExpAction(FileAction.ADD);

			dumpFileSpecInfo(reopenedFileSpecs, "Reopened FileSpecs");
			dumpFileSpecMethods(reopenedFileSpecs.get(0), "Reopened file Specs");
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);

			List<IFileSpec> submittedSpecs = changelist.submit(false);
			dumpFileSpecInfo(submittedSpecs, "Submitted file Specs");
			fSpec0.setExpStatusMessage("Submitted as change");
			IFileSpec validSpec = submittedSpecs.get(submittedSpecs.size() - 1);
			fSpec0.setExpOriginalPath(null);
			verifyFileSpecMethods("Submitted fileSpecs - verify message: " + fSpec0.getExpStatusMessage(), 
					validSpec, fSpec0, P4JTEST_VERIFYTYPE_MESSAGE);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	
	/**
	 * Create one text file spec with the minimal info: originalPath; FileSpecOpStatus; 
	 * and changelist. This should be equivalent to using FileSpecBuilder.
	 */
	@Test
	public void testBuildFileSpecMinimal() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;
		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};
						
			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
			
			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.UNKNOWN, P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of valid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);

			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * Add one text file into default changelist and verify the filespec info.
	 */
	@Test
	public void testBuildFileSpecTextTypical() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;
		
		try {
			
			debugPrintTestName();
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpChangelistId(IChangelist.DEFAULT);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());

			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			//verifyFileSpecMethods(buildFileSpecs.get(0), fSpec0);
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added file Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of valid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			verifyFileSpecMethods("Added fileSpecs", addedSpecs.get(0), fSpec0, P4JTEST_VERIFYTYPE_BASIC);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}



	
	
	/**
	 * Add one text file into default changelist, set Action to DELETE (wrong setting) and verify the 
	 * added filespec has Action set to ADD.
	 */
	@Test
	public void testFileSpecSetActionToDeleteAddFiles() {
		IServer server = null;
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 0;

		try {
			
			debugPrintTestName();

			assertNotNull("client should not be Null.", client);
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpAction(FileAction.DELETE);
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);			
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());
			
			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_TEXT);
			
			fSpec0.setExpAction(FileAction.ADD);
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of valid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			
			verifyFileSpecMethods("Added fileSpecs: " + fSpec0.getExpStatusMessage(), addedSpecs.get(0), 
					fSpec0, P4JTEST_VERIFYTYPE_BASIC);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	
	/**
	 * Add one text file into default changelist, don't set the originalPath. Verify the 
	 * error in the filespec info.
	 */
	@Test
	public void testFileSpecNoOrigPathAddFiles() {
		int expNumValidFSpecs = 0; 
		int expNumInvalidFSpecs = 1;

		try {
			
			debugPrintTestName();
			
			assertNotNull("client should not be Null.", client);
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			
			//now build the spec without specifying the original filepath
			List<IFileSpec> buildFileSpecs = buildFileSpecs(filePaths, fSpec0);
			dumpFileSpecInfo(buildFileSpecs, "Built file Specs");
			dumpFileSpecMethods(buildFileSpecs.get(0), "Built file Specs");
			
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, buildFileSpecs, IChangelist.DEFAULT, P4JTEST_FILETYPE_TEXT);
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			assertEquals("Number of valid fileSpecs is incorrect.", expNumInvalidFSpecs, FileSpecBuilder.getInvalidFileSpecs(addedSpecs).size());
			
			fSpec0.setExpAction(null);
			fSpec0.setExpOpStatus(FileSpecOpStatus.ERROR);
			fSpec0.setExpStatusMessage("Usage: add/edit/delete [-c changelist#]");
			IFileSpec invalidSpec = FileSpecBuilder.getInvalidFileSpecs(addedSpecs).get(0);
			verifyFileSpecMethods("Added fileSpecs - verify message: " + fSpec0.getExpStatusMessage(), 
					invalidSpec, fSpec0, P4JTEST_VERIFYTYPE_MESSAGE);
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	

	/**
	 * Creates a file with 3 revs, and then a filespec that can be used
	 * to sync to an previous rev. Verify the sync works by checking the haveRev.
	 */
	@Test
	public void testFileSpecSyncToPrevRev() {
		int expNumValidFSpecs = 1; 
		int expNumInvalidFSpecs = 1; // Not one becuase there is a trigger in place
		int expNumInfoFSpecs = 1;

		try {
			
			debugPrintTestName();
			
			assertNotNull("client should not be Null.", client);
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileFSpec.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filePaths
			final String[] filePaths = {
					file1.getAbsolutePath(),
			};

			//set up the expected values
			VerifyFileSpec fSpec0 = new VerifyFileSpec();
			fSpec0.setExpClientName(defaultTestClientName);
			fSpec0.setExpUserName(userName);
			fSpec0.setExpFileType(P4JTEST_FILETYPE_TEXT);
			fSpec0.setExpAction(FileAction.ADD);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpChangelistId(IChangelist.UNKNOWN);			
			fSpec0.setExpOriginalPath(file1.getAbsolutePath());
			
			List<IFileSpec> builtFileSpecs = buildFileSpecs(filePaths, fSpec0);
			assertNotNull("builtFileSpecs should not be Null.");
			List<IFileSpec> addedSpecs = taskAddTestFiles(server, client, builtFileSpecs, IChangelist.UNKNOWN, P4JTEST_FILETYPE_TEXT);
			dumpFileSpecMethods(addedSpecs.get(0), "Added File Specs");
			
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(addedSpecs).size());
			
			//create a new changelist, reopen file and submit 
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			//reopen the files in the new changelist
			List<IFileSpec> reopenedFileSpecs = client.reopenFiles(builtFileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
			dumpFileSpecInfo(reopenedFileSpecs, "reopened FileSpecs");
			
			List<IFileSpec> submittedSpecs = changelist.submit(false);
			dumpFileSpecInfo(submittedSpecs, "Submitted FileSpecs - First Rev");
			dumpFileSpecMethods(submittedSpecs.get(0), "Submitted file Specs - First Rev");
			
			//add some revs to the file
			for(int i=0; i<=1; i++) {
				changelistImpl  = getNewChangelist(server, client, 
						"Changelist to submit files for " + getName());		
				changelist = client.createChangelist(changelistImpl);
				
				client.editFiles(builtFileSpecs, false, false, changelist.getId(), P4JTEST_FILETYPE_TEXT);
				submittedSpecs = changelist.submit(false);
				dumpFileSpecInfo(submittedSpecs, "Submitted FileSpecs - rev " + i);
			}
		
			fSpec0.setExpAction(FileAction.EDIT);
			fSpec0.setExpOpStatus(FileSpecOpStatus.VALID);
			fSpec0.setExpFileType(null);
			fSpec0.setExpOriginalPath(null);
			dumpFileSpecInfo(submittedSpecs, "Submitted FileSpecs");
			dumpFileSpecMethods(submittedSpecs.get(0), "Submitted file Specs");
			assertEquals("Number of valid fileSpecs is incorrect.", expNumValidFSpecs, FileSpecBuilder.getValidFileSpecs(submittedSpecs).size());
			assertEquals("Number of invalid fileSpecs is incorrect.", 0, FileSpecBuilder.getInvalidFileSpecs(submittedSpecs).size());
			verifyFileSpecMethods("Submitted fileSpecs", submittedSpecs.get(submittedSpecs.size() - 2), fSpec0, P4JTEST_VERIFYTYPE_BASIC);

			dumpFileSpecInfo(submittedSpecs, "Submitted file Specs");
			fSpec0.setExpStatusMessage("Submitted as change");
			
			//create the fileSpecs with the Rev we want to sync to
			final String[] filePaths2 = {
					file1.getAbsolutePath() + "#2",
			};

			fSpec0.setExpFileRev(2);
			String fileRev2 = filePaths2[0];
			fSpec0.setExpOriginalPath(fileRev2);
			fSpec0.setExpAction(FileAction.UPDATED);
			builtFileSpecs = buildFileSpecs(filePaths2, fSpec0);
			
			//now sync to rev2 and get the filespec.
			List<IFileSpec> syncFiles = client.sync(builtFileSpecs, true, false, false, false);
			dumpFileSpecInfo(syncFiles, "Sync'd to Rev 2");
			IFileSpec validSyncSpec = FileSpecBuilder.getValidFileSpecs(syncFiles).get(0);
			assertEquals("The FileSpec Revs should match.", fSpec0.getExpFileRev(), validSyncSpec.getEndRevision());
			assertEquals("The FileSpec Actions should match.", fSpec0.getExpAction(), validSyncSpec.getAction());
						
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	

	
	//*****************//
	//**** Helpers ****//
	//****************//
	

	private List<IFileSpec> buildFileSpecs(String[] filePaths) {
		
		FileSpec fileSpec = null;
		List<IFileSpec> fileSpecList = null;
		
		if(filePaths != null) {
			fileSpecList = new ArrayList<IFileSpec>();
			for(String filePath : filePaths) {
				if(filePath != null) {
					fileSpec = new FileSpec(filePath);
					fileSpec.setClientName(defaultTestClientName);
					fileSpec.setUserName(userName);
					fileSpecList.add(new FileSpec(fileSpec));
				}
			}
		}

		return fileSpecList;
	}

	
	/**
	 * Takes the verification spec and creates a filespec from it. 
	 */
	private List<IFileSpec> buildFileSpecs(String[] filePaths, VerifyFileSpec verifySpec) {
		
		FileSpec fileSpec = null;
		List<IFileSpec> fileSpecList = null;
		
		if(filePaths != null) {
			fileSpecList = new ArrayList<IFileSpec>();
			for(String filePath : filePaths) {
				debugPrint("Building FileSpec: " + filePath);
				if(filePath != null) {
					fileSpec = new FileSpec(filePath);
					fileSpec.setClientName(verifySpec.getExpClientName());
					fileSpec.setUserName(verifySpec.getExpUserName());
					fileSpec.setFileType(verifySpec.getExpFileType());
					fileSpec.setAction(verifySpec.getExpAction());
					fileSpec.setChangelistId(verifySpec.getExpChangelistId());
					fileSpec.setOriginalPath(verifySpec.getExpOriginalPath());
					fileSpec.setOpStatus(verifySpec.getExpOpStatus());

					fileSpecList.add(new FileSpec(fileSpec));
					
					debugPrint("New FileSpec SETTINGS");
					debugPrint("FileSpec: " + fileSpec);
					debugPrint("ClientName: " + fileSpec.getClientName());
					debugPrint("UserName: " + fileSpec.getUserName());
					debugPrint("FileType: " + fileSpec.getFileType());
					debugPrint("Action: " + fileSpec.getAction());
					debugPrint("ChangelistId: " + fileSpec.getChangelistId());
					debugPrint("OriginalPath: " + fileSpec.getOriginalPath());
					debugPrint("OpStatus: " + fileSpec.getOpStatus());
					
					dumpFileSpecInfo(fileSpecList, "Built file Specs");
				}	
			}
		}

		return fileSpecList;
	}


	/**
	 * This task adds files to named changelist and returns the added fileSpecs. No files are
	 * submitted, and wildcards are allowed.
	 */
	private List<IFileSpec> taskAddTestFiles(IServer server,  IClient client, List<IFileSpec> fileSpecs, int changelistId, String fileType) {
		List<IFileSpec> addedFileSpecs = null;
		
		try {
	
			assertNotNull("FileSpecs should not be Null.", fileSpecs);
			addedFileSpecs = client.addFiles(fileSpecs, false, changelistId, fileType, true);  //changed from wildcards false to true
			dumpFileSpecInfo(addedFileSpecs, "Added FileSpecs");
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	
		return addedFileSpecs;

	}
	
	public void verifyFileSpecMethods(String comments, IFileSpec fileSpec, VerifyFileSpec spec, int verifyType ) {
		
		debugPrint("** verifyFileSpecMethods **" + "\n" + comments);
		
		if(fileSpec != null) {
			dumpFileSpecMethods(fileSpec, "Dump before Verify");
			
			if(verifyType == P4JTEST_VERIFYTYPE_BASIC) {
				assertEquals("The fileSpec OpStatus does not match.", spec.expOpStatus, fileSpec.getOpStatus());
				assertEquals("The fileSpec Actions are not equal.", spec.expAction, fileSpec.getAction());
				assertEquals("The fileSpec fileTypes are not equal.", spec.expFileType, fileSpec.getFileType());
			}
			if(verifyType == P4JTEST_VERIFYTYPE_EXTENDED || verifyType == P4JTEST_VERIFYTYPE_ALL) {
				assertEquals("The fileSpec changelistIds are not equal.", spec.expChangelistId, fileSpec.getChangelistId());
			}
			if(verifyType == P4JTEST_VERIFYTYPE_ALL) {
				assertEquals("The fileSpec clientNames are not equal.", spec.expClientName, fileSpec.getClientName());
				assertEquals("The fileSpec userNames are not equal.", spec.expUserName, fileSpec.getUserName());
			}
			if(verifyType == P4JTEST_VERIFYTYPE_MESSAGE) {
				debugPrint("Verifying Message: " + spec.getExpStatusMessage());
				debugPrint("FileSpec StatusMsg: " +  fileSpec.getStatusMessage());
				assertTrue("The fileSpec messages are not equal.", fileSpec.getStatusMessage().contains(spec.getExpStatusMessage()));				
			}
			if(spec.getExpClientPath() != null) {
				if(fileSpec.getClientPath() != null) {
					assertEquals("The fileSpec preferredPaths are not equal.", spec.expClientPath, "" + fileSpec.getClientPath()); 
				} else {
					fail("The fileSpec preferredPaths are not equal. " + fileSpec.getClientPath());
				}
			}
			if(spec.getExpOriginalPath() != null) {
				if(fileSpec.getOriginalPath() != null) {
					assertTrue("The fileSpec originalPaths are not equal.", fileSpec.getOriginalPath().toString().contains(spec.expOriginalPath));
				} else {
					fail("The fileSpec originalPaths are not equal. " + fileSpec.getOriginalPath());
				}
			}
			if(spec.getExpPreferredPath() != null) {
				if(fileSpec.getPreferredPath() != null) {
					assertEquals("The fileSpec preferredPaths are not equal.", spec.expPreferredPath, "" + fileSpec.getPreferredPath()); 
				} else {
					fail("The fileSpec preferredPaths are not equal. " + fileSpec.getPreferredPath());
				}
			}
		}
	}
	
	private void dumpFileSpecMethods(IFileSpec fileSpec, String comments) {
		
		debugPrint("** verifyFileSpecMethods **" + "\n" + comments);
		debugPrint("Dump info on fileSpec: " + fileSpec);
		
		if(fileSpec != null) {
			debugPrint("The returned fileSpecOpStatus: " + fileSpec.getOpStatus());
			debugPrint("The returned fileSpec Action: " + fileSpec.getAction());
			debugPrint("The returned fileSpec fileType: " + fileSpec.getFileType());
			debugPrint("The returned fileSpec clientName: " + fileSpec.getClientName());
			debugPrint("The returned fileSpec userName: " + fileSpec.getUserName());
			debugPrint("The returned fileSpec changelistId: " + fileSpec.getChangelistId());
			if(fileSpec.getDepotPath() != null) {
				debugPrint("The returned fileSpec depotPath: " + fileSpec.getDepotPath().toString()); 
			}
			if(fileSpec.getClientPath() != null) {
				debugPrint("The returned fileSpec originalPath: " + fileSpec.getClientPath().toString()); 
			}
			if(fileSpec.getOriginalPath() != null) {
				debugPrint("The returned fileSpec originalPath: " + fileSpec.getOriginalPath().toString()); 
			}
			if(fileSpec.getPreferredPath() != null) {
				debugPrint("The returned fileSpec preferredPath: ", fileSpec.getPreferredPath().toString()); 
			}
		}
	}
	@AfterClass
	public static void afterAll() throws Exception {
		afterEach(server);
	}

}
