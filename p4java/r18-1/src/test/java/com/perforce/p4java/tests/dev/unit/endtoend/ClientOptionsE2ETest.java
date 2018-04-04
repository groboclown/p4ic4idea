package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.apache.commons.io.FileUtils;

/**
 
 * The ClientOptionsTest class exercises the ClientOptions class. 
 * The test verifies the ClientOptions() as it affects files
 */

@TestId("ClientOptionsE2ETest01")
public class ClientOptionsE2ETest extends P4JavaTestCase {
	
	private static IClient client = null;
	private static String clientDir;
	private static String sourceFile;
	
	@BeforeClass
	public static void before() throws Exception {
		server = getServer();
		client = getDefaultClient(server);
		clientDir = defaultTestClientName + File.separator + testId;
		server.setCurrentClient(client);
		sourceFile = client.getRoot() + File.separator + textBaseFile;
		createTestSourceFile(sourceFile, false);
	}
	
	/**
	 * allwrite noallwrite   Leaves all files writable on the client;    
	 * else only checked out files are writable. If set, files may be clobbered 
	 * ignoring the clobber option below.
	 */
	@Test
	public void testSetAllWrite() {
		try {
			debugPrintTestName();
			
			//get the default options
			ClientOptions clientOpts = new ClientOptions();
			assertFalse("Default setting for isAllWrite should be false.", clientOpts.isAllWrite());
			
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			server.setCurrentClient(client);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "testfileCO.txt";
			String newFilePath = clientRoot + File.separator + clientDir;
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file2 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file3 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file4 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file5 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//now set the allwrite option to true
			clientOpts.setAllWrite(true);
			assertTrue("isAllWrite should be true.",clientOpts.isAllWrite());
			
			//create the filelist
			final String[] filePaths = {
					file1.getAbsolutePath(),
					file2.getAbsolutePath(),
					file3.getAbsolutePath(),
					file4.getAbsolutePath(),
					file5.getAbsolutePath()
			};	
			
			//add the files, submit them, reopen them
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
			assertNotNull("FileSpecs should not be Null.", fileSpecs);
			List<IFileSpec> addedFileSpecs = client.addFiles(fileSpecs, false, 0, P4JTEST_FILETYPE_TEXT, false);
			assertEquals("Number built & added fileSpecs should be equal.", fileSpecs.size(), addedFileSpecs.size());
			
			//submit files. Check if added files are in the correct changelist.
			List<IFileSpec> reopenedFileSpecs = client.reopenFiles(fileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);			
			List<IFileSpec> submittedFileSpecs = changelist.submit(true);
			int numSubmitted = FileSpecBuilder.getValidFileSpecs(submittedFileSpecs).size(); 
			
			assertEquals("numSubmitted should equal number of files created.", filePaths.length, numSubmitted);
			
			//verify the file permission
			assertTrue("File1 should be writeable.", file1.canWrite());
			assertTrue("File2 should be writeable.", file2.canWrite());
			assertTrue("File3 should be writeable.", file3.canWrite());
			assertTrue("File4 should be writeable.", file4.canWrite());
			assertTrue("File5 should be writeable.", file5.canWrite());
			
			//reset option
			clientOpts.setAllWrite(false);
			assertFalse("Setting for isAllWrite should be false.",clientOpts.isAllWrite());
			
			//Create a new changelist & open files
			changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			changelist = client.createChangelist(changelistImpl);
			List<IFileSpec> editedFileSpecs = client.editFiles(fileSpecs, false, false, 0, P4JTEST_FILETYPE_TEXT);
			assertEquals("Number built & edit fileSpecs should be equal.", fileSpecs.size(), editedFileSpecs.size());

			reopenedFileSpecs.clear();
			reopenedFileSpecs = client.reopenFiles(fileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);			
			
			//submit files and check if they are writable
			submittedFileSpecs.clear();
			submittedFileSpecs = changelist.submit(false);
			numSubmitted = FileSpecBuilder.getValidFileSpecs(submittedFileSpecs).size(); 
			assertEquals("numSubmitted should equal number of files created.", filePaths.length, numSubmitted);

			//verify the file permissions
			assertFalse("File1 should not be writeable.", file1.canWrite());
			assertFalse("File2 should not be writeable.", file2.canWrite());
			assertFalse("File3 should not be writeable.", file3.canWrite());
			assertFalse("File4 should not be writeable.", file4.canWrite());
			assertFalse("File5 should not be writeable.", file5.canWrite());

			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 *  clobber noclobber Allows 'p4 sync' to overwrite writable files on the client.
	 *  noclobber is ignored if allwrite is set.
	 * This functionality was tested at the setter and getter level, but \
	 * now we need to verify at the server level if this setting is heeded.
	 * Once that's done, this test can (hopefully) pass.
	 */	
	@Test
	@Ignore("Functionality not yet verified")
	public void testSetClobber() {
		try {
			
			debugPrintTestName();

			String verOptions = getVerificationString(false, true, false, false, false, false);
			ClientOptions clientOpts = new ClientOptions(verOptions);

			clientOpts.setClobber(true);
			fail("Functionality not yet verified");
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * compress nocompress Compresses data sent between the client
	 * and server to speed up slow connections.	
	 * This functionality was tested at the setter and getter level, but \
	 * now we need to verify at the server level if this setting is heeded.
	 * Once that's done, this test can (hopefully) pass.
	 */
	@Test
	@Ignore("Functionality not yet verified")
	public void testSetCompress() {
		try {
			
			debugPrintTestName();

			String verOptions = getVerificationString(false, false, true, false, false, false);
			ClientOptions clientOpts = new ClientOptions(verOptions);

			clientOpts.setCompress(true);
			fail("Functionality not yet verified");
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * locked unlocked Allows only the client owner to use the 
	 * client or change its specification. Prevents the client from being deleted.
	 * This functionality was tested at the setter and getter level, but
	 * now we need to verify at the server level if this setting is heeded.
	 * Once that's done, this test can (hopefully) pass.
	 */
	@Test
	@Ignore("Functionality not yet verified")
	public void testSetLocked() {
		try {
			
			debugPrintTestName();

			String verOptions = getVerificationString(false, false, false, true, false, false);
			ClientOptions clientOpts = new ClientOptions(verOptions);

			clientOpts.setLocked(true);
			fail("Functionality not yet verified");
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * modtime nomodtime Causes 'p4 sync' to preserve modification time from submitting client,
	 * as with files with +m type modifier. Otherwise modification time is left as 
	 * when the file was fetched.                    
	 * This functionality was tested at the setter and getter level, but \
	 * now we need to verify at the server level if this setting is heeded.
	 * Once that's done, this test can (hopefully) pass.
	 */
	@Test
	public void testSetModtime() {
		try {
			
			debugPrintTestName();

			ClientOptions clientOpts = new ClientOptions();

			clientOpts.setRmdir(true);
			
			
		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * rmdir normdir  Makes 'p4 sync' attempt to delete a client
	 * directory when all files are removed.
	 */
	@Test
	public void testRmdir() {
		try {
			debugPrintTestName();
			
			//get the default options
			ClientOptions tClient = new ClientOptions();
			assertFalse("Default setting for isRmdir() should be false.", tClient.isRmdir());
			tClient.setRmdir(true);
			assertTrue("Setting for isRmdir() should be true.", tClient.isRmdir());
			String clientRoot = client.getRoot();
			assertNotNull("clientRoot should not be Null.", clientRoot);
			
			//create the files
			String newFile = clientRoot + File.separator + clientDir + File.separator + "RMDIRTEST" + File.separator + "testfileCO.txt";
			String newFilePath = clientRoot + File.separator + clientDir + File.separator + "RMDIRTEST";
			File parentDir = new File(newFilePath);
			SysFileHelperBridge.getSysFileCommands().setWritable(parentDir.getCanonicalPath(), true);
            if (parentDir.exists() && parentDir.isDirectory()) {
                FileUtils.cleanDirectory(parentDir);
            }
			
			File file1 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file2 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			File file3 = new File(newFilePath + File.separator + prepareTestFile(sourceFile, newFile, true));	
			
			//create the filelist
			final String[] filePaths = {
					file1.getAbsolutePath(),
					file2.getAbsolutePath(),
					file3.getAbsolutePath(),
			};

			//add the files, submit them, reopen them
			IChangelist changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			IChangelist changelist = client.createChangelist(changelistImpl);
			
			List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(filePaths);
			assertNotNull("FileSpecs should not be Null.", fileSpecs);
			List<IFileSpec> addedFileSpecs = client.addFiles(fileSpecs, false, 0, P4JTEST_FILETYPE_TEXT, false);
			assertEquals("Number built & added fileSpecs should be equal.", fileSpecs.size(), addedFileSpecs.size());
			
			//submit files. Check if added files are in the correct changelist.
			List<IFileSpec> reopenedFileSpecs = client.reopenFiles(fileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);			
			List<IFileSpec> submittedFileSpecs = changelist.submit(false);
			int numSubmitted = FileSpecBuilder.getValidFileSpecs(submittedFileSpecs).size(); 			
			assertEquals("numSubmitted should equal number of files created.", filePaths.length, numSubmitted);
			
			List<IFileSpec> deletedFileSpecs = client.deleteFiles(fileSpecs, 0, false);
			int numDeleted = FileSpecBuilder.getValidFileSpecs(deletedFileSpecs).size(); 
			assertEquals("numSubmitted should equal number of files created.", filePaths.length, numDeleted);
			//submit the deleted files.
			changelistImpl  = getNewChangelist(server, client, 
					"Changelist to submit files for " + getName());		
			changelist = client.createChangelist(changelistImpl);
			reopenedFileSpecs.clear();
			reopenedFileSpecs = client.reopenFiles(fileSpecs, changelist.getId(), P4JTEST_FILETYPE_TEXT);
			assertEquals("Number built & reopened fileSpecs should be equal.", fileSpecs.size(), reopenedFileSpecs.size());
			
			submittedFileSpecs.clear();
			submittedFileSpecs = changelist.submit(false);
			
			List<IFileSpec> syncFileSpecs = client.sync(fileSpecs, true, false, false, false);
			dumpFileSpecInfo(syncFileSpecs, "Sync'ed files:");

			assertTrue(parentDir.listFiles() == null || parentDir.listFiles().length == 0);

			tClient.setRmdir(false);
			assertFalse("Setting for isRmdir() should be false.", tClient.isRmdir());

		} catch (Exception exc){
			fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
		}
	}

	/**
	 * This helper function takes the boolean values passed in and converts them to Perforce-standard 
	 * representation of these options for ClientOptions. The string that is returned is useful for 
	 * comparison against the return value of the toString() method of the ClientOptions class.
	 */
	private String getVerificationString(boolean allWriteVal, boolean clobberVal,
			boolean compressVal, boolean lockedVal, boolean modtimeVal, boolean rmdirVal) {
		
		String vString = null;
		
		vString = allWriteVal ? "allwrite" : "noallwrite";
		vString += clobberVal ? " clobber" : " noclobber";
		vString += compressVal ? " compress" : " nocompress";
		vString += lockedVal ? " locked" : " nolocked";
		vString += modtimeVal ? " modtime" : " nomodtime";
		vString += rmdirVal ? " rmdir" : " normdir";
		
		return vString;
	}

	@AfterClass
	public static void afterAll() throws Exception {
		afterEach(server);
	}
}
