package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.apache.commons.lang3.StringUtils;


/**
 * 
 * This test class exercises the IClient interface's addFiles method with both positive and 
 * negative test data. It then attempts to submit the changelist to see that we get the correct
 * error or results.
 * 	
 * 	addFiles(java.util.List<IFileSpec> fileSpecs, boolean noUpdate, int changeListId, 
 * 					java.lang.String fileType, boolean useWildcards) 
 */
@TestId("ClientAddFilesE2ETest01")
public class ClientAddFilesE2ETest extends P4JavaTestCase {
	private static IClient client = null;
	private static String clientDir;
	private static String sourceFile;
	private static String binarySourceFile;
	
	@BeforeClass
	public static void beforeAll() throws Exception {
		server = getServer();
		client = getDefaultClient(server);
		clientDir = defaultTestClientName + File.separator + testId;
		server.setCurrentClient(client);
		sourceFile = client.getRoot() + File.separator + textBaseFile;
		binarySourceFile = client.getRoot() + File.separator + "bindetmi2.dll";
		createTestSourceFile(sourceFile, false);
		createTestSourceFile(binarySourceFile, true);
	}
	
	public void showServerInfo(IServer server) throws Exception {
		
		try {
			IClient client = getDefaultClient(server);
			debugPrint("serverUrlString: " + serverUrlString, "defaultTestClientName: " + defaultTestClientName);	
			debugPrint(true, "ClientRoot: " + client.getRoot(), "HostName: " + client.getHostName(), "userName: " + userName);
			
		} catch (Exception exc){
			debugPrint("showServerInfo - Unexpected exception: " + exc.getLocalizedMessage());
			fail("showServerInfo - Unexpected exception: " + exc.getLocalizedMessage());		
		}
		
	}
	

	/**
	 * This test adds a new file to the depot and submits it.
	 * No of Files: 1
	 * Path syntax: WS path
	 * File Type: text
	 */
	@Test
	public void testAddOneFileClientPathSyntaxE2E() throws Exception {
		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			
			final String[] filePaths = {
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)) //'/' is for ClientPath syntax 
			};
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);
			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	
	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 1
	 * Path syntax: local path
	 * File Type: text
	 */
	@Test
	public void testAddOneFileLocalPathSyntaxE2E() throws Exception {
		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();
			showServerInfo(server);
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			
			final String[] filePaths = {
				new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
			};
					
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 1
	 * Path syntax: depot path
	 * File Type: text
	 */
	@Test
	public void testAddOneFileDepotPathSyntaxE2E() throws Exception {		

		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			
			final String[] filePaths = {
				createDepotPathSyntax(testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
			};		
			
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}
	

	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 1
	 * Path syntax: depot path
	 * File Type: binary
	 */
	@Test
	public void testAddOneBinaryFileTypeE2E() throws Exception {
			
		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "binaryTestFile.dll";
			
			final String[] filePaths = {
					createDepotPathSyntax(testId + File.separator + prepareTestFile(binarySourceFile, newFile, true)),
			};
					
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_BINARY, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 3
	 * Path syntax: client path
	 * File Type: text
	 */
	@Test
	public void testAddMultipleFilesSameTypeE2E() throws Exception {

		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			
			//String newBaseFile = prepareTestFile(sourceFile, newFile, true);	
			final String[] filePaths = {
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
			};
		
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 3
	 * Path syntax: client path
	 * File Type: 1 binary, 2 text
	 */
	@Test
	public void testAddMultipleFilesDiffTypesE2E() throws Exception {
		
		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			String newFiledll = clientRoot + File.separator + testId + File.separator + "binaryTestFile.dll";
			
			final String[] filePaths = {
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
				createDepotPathSyntax(testId + File.separator + prepareTestFile(binarySourceFile, newFiledll, true)),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true))
			};
		
 			submittedFiles = taskAddSubmitTestFiles(server, filePaths, null, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}
	
	/**
	 * This test adds a new file to the depot and submits it. Verifies it was added and submitted
	 * with correct file type and revision.
	 * No of Files: 3
	 * Path syntax: client path
	 * File Type: text
	 */

	@Test
	public void testAddFilesNoExistE2E() throws Exception {
		
		List<IFileSpec> submittedFiles = null;
		String currTestVer = null;
		
		try {
			debugPrintTestName();

			currTestVer = getTestFileVer();
			
			final String[] filePaths = {
				createClientPathSyntax(defaultTestClientName, testId + File.separator + "testfileNoExist" + currTestVer + "A"),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + "testfileNoExist" + currTestVer + "B"),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + "testfileNoExist" + currTestVer + "C")
			};
					
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesNotSubmitted(submittedFiles, -1, filePaths.length, true, "Submit aborted -- fix problems");
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	
	/** 
	 * Test uses all three path syntaxes to add the same file
	 * @throws Exception
	 */
	@Test
	public void testAddFilesSameReferenceE2E() throws Exception {

		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String clientFilePath =  clientRoot + File.separator + clientDir;
			String newFile = clientFilePath + File.separator + "testfileSameRef.txt";
			
			String newBaseFile = prepareTestFile(sourceFile, newFile, true);	
			final String[] filePaths = {
				new File(clientFilePath, newBaseFile).toString(),
				createDepotPathSyntax(clientDir + File.separator + newBaseFile),
				createClientPathSyntax(defaultTestClientName, clientDir + File.separator + newBaseFile),
			};
		
			debugPrint("Three Paths: \n" + filePaths[0], filePaths[1], filePaths[2]);
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, 1); 
			verifyTestFilesSubmitted(submittedFiles, 1);
			verifyTestFileRevision(submittedFiles, 1, 1);
			// FIXME: fails due to bug	verifySpecListFileType(submittedFiles, P4JTEST_FILETYPE_TEXT);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	/** 
	 * Test uses all three path syntaxes to add three files
	 * @throws Exception
	 */
	@Test
	public void testAddFilesDiffSyntaxE2E() throws Exception {

		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileDiffSyntax.txt";
			
			final String[] filePaths = {
				new File(clientRoot + File.separator + clientDir, prepareTestFile(sourceFile, newFile, true)).getAbsolutePath(),
				createDepotPathSyntax(clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
				createClientPathSyntax(defaultTestClientName, clientDir + File.separator + prepareTestFile(sourceFile, newFile, true)),
			};
		
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false);

			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
			// FIXME: fails due to bug	verifySpecListFileType(submittedFiles, P4JTEST_FILETYPE_TEXT);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	/** The test testAddFilesNoUpdate exercises the true 'condition' of noUpdate flag. Verifies that the 
	 * files are not added. 
	 */
	@Test
	public void testAddFilesNoUpdateE2E() throws Exception {

		List<IFileSpec> submittedFiles = null;
		int numValidFiles = 0;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNoUpdate.txt";
			
			final String[] filePaths = {
				new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
				createDepotPathSyntax(testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
				createClientPathSyntax(defaultTestClientName, testId + File.separator + prepareTestFile(sourceFile, newFile, true)),
			};
		
			numValidFiles = filePaths.length;
			
			//create a changelist
			IChangelist changelist  = createTestChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			
			//add the test files
			List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(filePaths);
			assertNotNull("FileSpecBuilder unexpectedly returned Null SpecList.", fList);
			assertFalse("File List should not be empty.", fList.isEmpty());
			assertEquals("Number of FileList entries should equal original number of files.", 
					fList.size(), numValidFiles);
			
			debugPrint("CHANGELIST ID: " + changelist.getId());
			List<IFileSpec> newAddedSpecList = client.addFiles(fList, true, changelist.getId(), P4JTEST_FILETYPE_TEXT, false);
			verifyFilesAdded(newAddedSpecList, numValidFiles);
	
			//submit files
			changelist.update();
			submittedFiles = changelist.submit(true);
			submittedFiles = changelist.submit(false);
			
			verifyFilesAdded(submittedFiles, 0);
			verifyTestFilesNotSubmitted(submittedFiles, 1, 0, true, "No files to submit.");
			//verifyTestFileRevision(submittedFiles, numValidFiles, 1);
			// FIXME: fails due to bug	verifySpecListFileType(submittedFiles, P4JTEST_FILETYPE_TEXT);
			
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}

	
	/**
	 * This test adds file, submits and then reopens files per the 'reopen' flag in submit
	 */
	@Test
	public void testAddSubmitReopenFileE2E() throws Exception {
		List<IFileSpec> submittedFiles = null;
		
		try {
			debugPrintTestName();

			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			String newFile = clientRoot + File.separator + testId + File.separator + "testfileNew.txt";
			
			final String[] filePaths = {
				new File(prepareTestFile(sourceFile, newFile, false)).getAbsolutePath(),
			};
		
			submittedFiles = taskAddSubmitTestFiles(server, filePaths, P4JTEST_FILETYPE_TEXT, false, true);
			verifyFilesAdded(submittedFiles, filePaths.length); 
			verifyTestFilesSubmitted(submittedFiles, filePaths.length);
			verifyTestFileRevision(submittedFiles, filePaths.length, 1);
			verifyFileAction(submittedFiles, filePaths.length, FileAction.ADD);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc.getLocalizedMessage());			
		}
	}
	
	
	//*******************//
	// Helper Functions //
	//******************//

	private void verifyFileAction(List<IFileSpec> newFSList, int expNumWithAction, FileAction expFileAction) {
		
		int validFSpecCount = 0;
		
		if(newFSList != null) {
			if(newFSList.size() > 0) {
				for (IFileSpec fileSpec : newFSList) {
					if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
						debugPrint("verifyFileAction on fileSpec: " + fileSpec, "" + expFileAction, "" + fileSpec.getAction());
						assertEquals("Expected FileSpec Action: " + expFileAction, expFileAction, fileSpec.getAction());
						validFSpecCount++;
					}
					if (fileSpec != null) {
						debugPrint("fileSpec: " + fileSpec, "fileSpec.getAction() " + fileSpec.getAction(),
								"fileSpec.getOpStatus(): " + fileSpec.getOpStatus());
					}
				}
			}
			debugPrint("VerifyFileAction - expNumAdded: " + expNumWithAction, "validFSpecCount: " + validFSpecCount);
			assertEquals("Expected number of files not added.", expNumWithAction, validFSpecCount);
		} else {
			debugPrint("FileSpec was null.");
		}
	}

	
	
	private void verifySpecListFileType(List<IFileSpec> newFSList, String expFType) {
		
		for (IFileSpec fSpec : newFSList) {
			if (fSpec != null && fSpec.getOpStatus() == FileSpecOpStatus.VALID) {
				String fType = fSpec.getFileType();
				//does the status string contain what we expect?
				debugPrint("VerifyFileType expFType: " + expFType, "ftype: " + fType);
				assertEquals("FileTypes don't match.", expFType, fType);
			} 
			
		}		
	}
	
	private void verifyFilesAdded(List<IFileSpec> newFSList, int expNumAdded) {
		
		int validFSpecCount = 0;
		
		if(newFSList.size() > 0) {
			for (IFileSpec fileSpec : newFSList) {
				if (fileSpec != null && fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
					assertEquals("Expected FileSpec Action ADD.", fileSpec.getAction(), FileAction.ADD);
					validFSpecCount++;
				}
				if (fileSpec != null) {
					debugPrint("VerifyFilesAdded fileSpec: " + fileSpec, "fileSpec.getAction() " + fileSpec.getAction(),
							"fileSpec.getOpStatus(): " + fileSpec.getOpStatus());
				}
			}
		}
		debugPrint("VerifyFilesAdded expNumAdded: " + expNumAdded, "validFSpecCount: " + validFSpecCount);
		assertEquals("Expected number of files not added.", expNumAdded, validFSpecCount);
		
	}


	public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType, boolean validOnly) {
		
		return (taskAddSubmitTestFiles(server, fNameList, fileType, validOnly, false));
	}

	
	public List<IFileSpec> taskAddSubmitTestFiles(IServer server, String[] fNameList, String fileType, 
				boolean validOnly, boolean reopenAfterSubmit) {
	
		IClient client = null;
		List<IFileSpec> submittedFiles = null;
		
		try {	
			
			client = server.getClient(getPlatformClientName(defaultTestClientName));
			server.setCurrentClient(client);
			assertNotNull("Null client returned.", client);
			
			//create a changelist
			IChangelist changelist  = createTestChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			
			//add the test files
			List<IFileSpec> testFiles = addTestFiles(client, fNameList, 
					changelist.getId(), validOnly, fileType);
			assertNotNull("testFiles should not be Null.", testFiles);
	
			submittedFiles = changelist.submit(reopenAfterSubmit);
			assertNotNull("submittedFiles should not be Null.", submittedFiles);

		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());			
		} 
		
		return submittedFiles;
	
	}

	
	public List<IFileSpec> taskAddSubmitTestFilesDefChangelist(IServer server, String[] fNameList, String fileType, boolean validOnly) {
		
		IClient client = null;
		List<IFileSpec> submittedFiles = null;
		
		try {	
			
			client = server.getClient(defaultTestClientName);
			server.setCurrentClient(client);
			assertNotNull("Null client returned.", client);
			
			//create a changelist
			IChangelist changelist  = createTestChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			
			//add the test files
			List<IFileSpec> testFiles = addTestFiles(client, fNameList, 
					0, validOnly, fileType);
			assertNotNull("testFiles should not be Null.", testFiles);
	
			//submit files
			changelist.update();
			submittedFiles = changelist.submit(false);
			assertNotNull("submittedFiles should not be Null.", submittedFiles);

		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());			
		} 
		
		return submittedFiles;
	
	}

	public List<IFileSpec> taskAddSubmitTestFilesTestChangelistGetId(IServer server, String[] fNameList, String fileType, boolean validOnly) {
		
		IClient client = null;
		List<IFileSpec> submittedFiles = null;
		
		try {	
			
			client = server.getClient(defaultTestClientName);
			server.setCurrentClient(client);
			assertNotNull("Null client returned.", client);
			
			//create a changelist that is the default
			IChangelist changelist  = createTestChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			
			//add the test files
			List<IFileSpec> testFiles = addTestFiles(client, fNameList, 
					changelist.getId(), validOnly, fileType);
			assertNotNull("testFiles should not be Null.", testFiles);
	
			//submit files
			changelist.update();
			submittedFiles = changelist.submit(false);
			assertNotNull("submittedFiles should not be Null.", submittedFiles);

		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());			
		} 
		
		return submittedFiles;
	
	}


	
	public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, boolean validOnly, String fileType) 
	throws ConnectionException, AccessException {

		int changelistId = 0;
		
		return (addTestFiles(client, testFiles, changelistId, validOnly, fileType));
	}


	public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, boolean validOnly) 
			throws ConnectionException, AccessException {

		int changelistId = 0;

		return (addTestFiles(client, testFiles, changelistId, validOnly, P4JTEST_FILETYPE_TEXT));
	}

	
	public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, 
			int changelistID, boolean validOnly, String fileType) 
			throws ConnectionException, AccessException {

		List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(testFiles);
		assertNotNull("FileSpecBuilder unexpectedly returned Null SpecList.", fList);
		assertFalse("File List should not be empty.", fList.isEmpty());
		assertEquals("Number of FileList entries should equal original number of files.", 
				fList.size(), testFiles.length);
		
		List<IFileSpec> newAddedSpecList = client.addFiles(fList, false, changelistID, fileType, true);

		if(validOnly) {
			return FileSpecBuilder.getValidFileSpecs(newAddedSpecList); 
		} else {
			return newAddedSpecList;
		}

	}

	public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr) {

		Changelist changeListImpl = null;
		try {
			changeListImpl = new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					userName,
					ChangelistStatus.NEW,
					new Date(),
					chgDescr,
					false,
					(Server) server
			);
		} catch (Exception exc) {
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());			
		} 

		return changeListImpl;
	}

	public IChangelist createTestChangelist(IServer server, IClient client, String chgDescr) throws
			 ConnectionException, RequestException, AccessException {
	
		Changelist changeListImpl = createNewChangelistImpl(server, client, chgDescr);
		IChangelist changelist = client.createChangelist(changeListImpl);
		
		debugPrint("Created Changelist ID: " + changelist.getId());

		return changelist;
	}

		
	private void verifyTestFilesSubmitted(List<IFileSpec> newFSList, int expNumSubmitted) {
		
		int validFSpecCount = 0;
		int invalidFSpecCount = 0;
		FileSpecOpStatus fSpecOpStatus = null;
		String fSpecMsg = "";
		String submitMsg = "Submitted as change";
		
		if(newFSList.size() > 0) {
			for (IFileSpec fileSpec : newFSList) {
				if (fileSpec != null) {	
					fSpecOpStatus = fileSpec.getOpStatus();
					debugPrint("Submitted FileStatus: " + fileSpec, "" + fSpecOpStatus);
					if(fSpecOpStatus == FileSpecOpStatus.VALID) {						
						validFSpecCount++;
					} else if(fSpecOpStatus == FileSpecOpStatus.INFO) {
						fSpecMsg = fileSpec.getStatusMessage();
						debugPrint("Submitted StatusMessage for INFO fileSpec: " + fSpecMsg);
						if (StringUtils.isNumeric(fSpecMsg)) {
							// The 2013.2 test P4d has a trigger which adds 
							// Triggers:
						       // example1 change-submit //depot/... "echo %changelist%"
						       // example2 change-submit //depot/... "echo %changelist%"
						       // example3 change-submit //depot/... "echo %changelist%"
							// So we get 3 info messages - lets ignore them not sure if triggers are needed
							debugPrint("Ignoring: " + fSpecMsg + " Think it was created with a trigger!");
						} else {
							assertTrue("Message should show file was submitted. ", fSpecMsg.toString().contains(submitMsg));
						}
					} else {
						fSpecMsg = fileSpec.getStatusMessage();
						invalidFSpecCount++;
						debugPrint("FileSpecOp: " + fSpecOpStatus, "StatusMsg: " + fSpecMsg);
					}
				}
				//debugPrint("Submitted File Operation Status: " + fileSpec, "" + fSpecOpStatus);
			}
		}
		debugPrint("Submitted expNumAdded: " + expNumSubmitted, "validFSpecCount: " + validFSpecCount);
		assertEquals("Expected number of files not submitted.", expNumSubmitted, validFSpecCount);
		assertEquals("Expected number of invalid FileSpecs should be zero", 0, invalidFSpecCount);
		
	}
	

	private void verifyTestFilesNotSubmitted(List<IFileSpec> newFSList, int expTotalFSpecs, int expValidFSpecs, boolean expectError, String expMsg) {
		
		int validFSpecCount = 0;
		int infoFSpecCount = 0;
		int errorFSpecCount = 0;
		FileSpecOpStatus fSpecOpStatus = null;
		String fSpecMsg = "";
		String submitMsg = "No files to submit.";
		
		debugPrint("** verifyTestFilesNotSubmitted **");
		//need to simplify this a bit
		if(expMsg != "") {
			submitMsg = expMsg; 
		}
		if(newFSList.size() > 0) {
			for (IFileSpec fileSpec : newFSList) {
				if (fileSpec != null) {	
					fSpecOpStatus = fileSpec.getOpStatus();
					fSpecMsg = fileSpec.getStatusMessage();
					debugPrint("Submitted FileStatus: " + fileSpec, " fSpecOpStatus: " + fSpecOpStatus, "Msg: " + fSpecMsg);
					if(fSpecOpStatus == FileSpecOpStatus.VALID) {						
						validFSpecCount++;
					} else if(fSpecOpStatus == FileSpecOpStatus.INFO) {						
						infoFSpecCount++;
						if(!expectError) {
							assertTrue("INFO-Message should show no files submitted. ", fSpecMsg.toString().contains(submitMsg));
						}
					} else if(fSpecOpStatus == FileSpecOpStatus.ERROR) {
						errorFSpecCount++;
						if(expectError) {
							assertTrue("ERROR-Message should show no files submitted. ", fSpecMsg.toString().contains(submitMsg));
						}
					} else {
						fail("Unexpected FSpecOpStatus: " + fSpecOpStatus + " FSpecMsg: " + fSpecMsg);
					}
				}
			}
		}
		debugPrint("newFSList.size(): " + newFSList.size(), "Total Exp FileSpecs: " + expTotalFSpecs, "validFSpecCount: " + validFSpecCount);
		if(expTotalFSpecs != -1) { //Then we don't care about that number
			assertEquals("Expected number of total FileSpecs is incorrect", expTotalFSpecs, infoFSpecCount + errorFSpecCount + validFSpecCount);
		} 
		if(expValidFSpecs != -1) { //Then we don't care about that number
			assertEquals("Expected number of Valid FileSpecs is incorrect", expValidFSpecs, validFSpecCount);
		} 
		
	}

	
	private void verifyTestFileRevision(List<IFileSpec> newFSList, int expNumWithRev, int expRev) {
		
		int validFSpecCount = 0;
		
		if(newFSList.size() > 0) {
			for (IFileSpec fileSpec : newFSList) {
				if (fileSpec != null) {	
					if(fileSpec.getOpStatus() == FileSpecOpStatus.VALID) {
						debugPrint("FileRev: " + fileSpec, "" + fileSpec.getEndRevision(), "" + expRev);
						assertEquals("Expected File Revision not found", expRev, fileSpec.getEndRevision() );
						validFSpecCount++;
					}
				}
			}
		}
		debugPrint("expNumWithRev: " + expNumWithRev, "validFSpecCount: " + validFSpecCount);
		assertEquals("Expected number of files with revision not found.", expNumWithRev, validFSpecCount);
		
	}
	@AfterClass
	public static void afterAll() throws Exception {
		afterEach(server);
	}

}

