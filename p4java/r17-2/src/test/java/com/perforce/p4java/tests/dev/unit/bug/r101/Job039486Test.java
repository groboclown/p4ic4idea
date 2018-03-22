/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import com.perforce.p4java.tests.dev.unit.PlatformType;

/**
 * Test for job039486 -- false submission success with client-side
 * file issues (see job text for full description). Test will fail on
 * Windows boxes due to inability to set a file unreadable, so it's disabled
 * on those boxes.
 * 
 * Note that if this test fails, some obliterating will need to be done
 * in the long term to clean up afterwards; if it succeeds, it should clean
 * up after itself fairly well.
 */
@Jobs({"job039486"})
@TestId("Bugs101_Job039486Test")
public class Job039486Test extends P4JavaTestCase {

	public Job039486Test() {
	}

	@Test
	public void testSubmit() {
		final String testRoot = "//depot/101Bugs/tmp/" + this.testId;
		
		IOptionsServer server = null;
		IClient client = null;

		if (this.getHostPlatformType() == PlatformType.WINDOWS) {
			return; // success by fiat only, I'm afraid... (HR).
		}
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull("null test client returned", client);
			server.setCurrentClient(client);
			String systemTestRoot = this.getSystemPath(client, testRoot);
			assertNotNull(systemTestRoot);
			File dirFile = new File(systemTestRoot);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			File file1 = getUnusedFile(5, systemTestRoot, "txt");
			assertNotNull(file1);
			File file2 = getUnusedFile(5, systemTestRoot, "txt");
			assertNotNull(file2);
			File file3 = getUnusedFile(5, systemTestRoot, "txt");
			assertNotNull(file3);
			writeLine(file1, file1.getName());
			writeLine(file2, file2.getName());
			writeLine(file3, file3.getName());

			IChangelist changelist = new Changelist(
											IChangelist.UNKNOWN,
											client.getName(),
											this.userName,
											ChangelistStatus.NEW,
											null,
											"Changelist for test " + this.getTestId(),
											false,
											(Server) server
							);
			changelist = client.createChangelist(changelist);
			List<IFileSpec> addFiles = client.addFiles(
								FileSpecBuilder.makeFileSpecList(
										testRoot + "/" + file1.getName(),
										testRoot + "/" + file2.getName(),
										testRoot + "/" + file3.getName()),
								false, changelist.getId(), "text", false);
			assertNotNull(addFiles);
			for (IFileSpec file : addFiles) {
				assertNotNull(file);
				assertEquals("invalid filespec returned: " + file.getStatusMessage(),
						file.getOpStatus(), FileSpecOpStatus.VALID);
			}
			changelist.refresh();
			assertTrue("unable to set target file non-writable",
					SysFileHelperBridge.getSysFileCommands().setWritable(file2.getCanonicalPath(), false));
								//file2.setWritable(false, false));
			assertTrue("unable to set target file non-readable",
					SysFileHelperBridge.getSysFileCommands().setReadable(file2.getCanonicalPath(), false, false));
								//file2.setReadable(false, false));
			List<IFileSpec> submitFiles = changelist.submit(false);
			assertNotNull(submitFiles);
			
			for (IFileSpec file : submitFiles) {
				assertNotNull(file);
				if (file.getOpStatus() != FileSpecOpStatus.VALID 
					&& file.getOpStatus() != FileSpecOpStatus.INFO) {
					String opMsg = file.getStatusMessage();
					assertNotNull(opMsg);
					assertFalse("Changelist wrongly submitted after error condition",
							opMsg.contains("Submitted as change"));
					assertTrue("unexpected error / status message: " + opMsg,
							(opMsg.contains("(Permission denied)") && (opMsg.contains("missing on client")))
							|| opMsg.contains("Submit aborted")
							|| opMsg.contains("Command aborted"));
				}
			}
			
			// Now revert the files, if possible, then delete them locally
			
			List<IFileSpec> revertFiles = client.revertFiles(
									FileSpecBuilder.makeFileSpecList(
											testRoot + "/" + file1.getName(),
											testRoot + "/" + file2.getName(),
											testRoot + "/" + file3.getName()),
									false, changelist.getId(), false, false);
			assertNotNull(revertFiles);
			for (IFileSpec file : revertFiles) {
				assertNotNull(file);
				assertNotNull(file.getOpStatus());
				assertEquals(FileSpecOpStatus.VALID, file.getOpStatus());
			}
			file1.delete();
			file2.delete();
			file3.delete();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	protected void writeLine(File file, String line) {
		assertNotNull(file);
		assertNotNull(line);
		
		FileOutputStream outStream = null;

		try {
			outStream = new FileOutputStream(file);
			outStream.write((line + "\n").getBytes("UTF-8"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
}
