/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests the 10.2 IClient.copyFiles method. Note that we're not really
 * testing the copy side effects but whether we're able to convince
 * the server to do a copy...
 */

@TestId("Features102_CopyFilesTest")
public class CopyFilesTest extends P4JavaTestCase {

	public CopyFilesTest() {
	}

	@Test
	public void testCopyFilesBasics() {
		final String testRoot = "//depot/102Dev/CopyFilesTest";
		final String fileNames = "test01.txt"; // same for both files...
		String srcFileName = null;
		String tgtFileName = null;
		IOptionsServer server = null;
		IClient client = null;
		InputStream inStream = null;
		PrintStream outStream = null;
		File tmpFile = null;
		
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			srcFileName = this.getSystemPath(client, testRoot + "/src/" + fileNames);
			client.revertFiles(FileSpecBuilder.makeFileSpecList(srcFileName), null);
			tgtFileName = this.getSystemPath(client, testRoot + "/tgt/" + fileNames);
			List<IFileSpec> files = this.forceSyncFiles(client, testRoot + "/...");
			assertNotNull(files);
			assertEquals("wrong number of files force synced", 3, files.size());
			
			
			IChangelist changelist = getNewChangelist(
								server, client, "Features102_CopyFilesTest changelist");
			assertNotNull("null changelist returned from getNewChangelist", changelist);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> editFiles = client.editFiles(
											FileSpecBuilder.makeFileSpecList(testRoot + "/src/" + fileNames),
											new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull("null files list from client.editFiles", editFiles);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(editFiles).size());
			changelist.refresh();
			
			inStream = new FileInputStream(new File(srcFileName));
			tmpFile = File.createTempFile(this.getTestId(), null);
			outStream = new PrintStream(tmpFile);
			int severity = 0;
			while (severity < 500) {
				severity = this.rand.nextInt(5000); // file must be bigger than 5K for this to be effective...
			}
			mangleTextFile(severity, inStream, outStream);
			outStream.flush();
			outStream.close();
			inStream.close();
			this.copyFile(tmpFile.getAbsolutePath(), srcFileName, true);
			List<IFileSpec> submitList = changelist.submit(null);
			assertNotNull(submitList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(submitList).size());
			IChangelist copyChangelist = getNewChangelist(
							server, client, "Features102_CopyFilesTest changelist");
			assertNotNull(copyChangelist);
			copyChangelist = client.createChangelist(copyChangelist);
			List<IFileSpec> copyFiles = client.copyFiles(
												new FileSpec(srcFileName),
												new FileSpec(tgtFileName),
												null,
												new CopyFilesOptions().setChangelistId(copyChangelist.getId()));
			assertNotNull(copyFiles);
			List<IFileSpec> nonValidFiles = FileSpecBuilder.getInvalidFileSpecs(copyFiles);
			if (nonValidFiles.size() != 0) {
				fail(nonValidFiles.get(0).getOpStatus() + ": " + nonValidFiles.get(0).getStatusMessage());
			}
			assertEquals("wrong number of valid filespecs after copy",
							1, FileSpecBuilder.getValidFileSpecs(copyFiles).size());
			copyChangelist.refresh();
			submitList = copyChangelist.submit(null);
			assertNotNull(submitList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(submitList).size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
				}
			}
			
			if (tmpFile != null) {
				tmpFile.delete();
			}
			
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
