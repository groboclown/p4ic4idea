/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the server- (etc.) message handling underpinning job 043524. Test
 * MUST run against a specific version of the server, hence the special
 * server handling below. 
 */
@TestId("Bugs111_Job043524Test")
public class Job043524Test extends P4JavaTestCase {

	public Job043524Test() {
	}

	@Test
	@Ignore("p4java://10.0.101.240:1666 does not exist anymore")
	public void testJob043524Basics() {
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		final String serverUrl = "p4java://10.0.101.240:1666";
		final String clientName = "jteam-test-job043524";
		final String testRoot = "//depot/depot/bugs111/" + getTestId();
		final String srcFileName = "test01.txt";
		final String tgtFileName = "test01.txt";
		final String srcFileDepotPath = testRoot + "/src/" + srcFileName;
		final String tgtFileDepotPath = testRoot + "/tgt/" + tgtFileName;
		final String changelistDescription = "Changelist generated for p4java junit test " + getTestId();
		File tmpFile = null;
		InputStream inStream = null;
		PrintStream outStream = null;
		
		try {
			server = getServer(serverUrl, null, this.getUserName(), this.getPassword());
			assertNotNull("null server returned from server factory", server);
			client = server.getClient(clientName);
			assertNotNull("unable to retrieve test client", client);
			server.setCurrentClient(client);
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, testRoot + "/...");
			assertNotNull("null return from sync", syncFiles);
			assertEquals("wrong number of files force synced",
								2, syncFiles.size());	// May need adjustment over time...
			changelist = CoreFactory.createChangelist(client, changelistDescription, true);
			assertNotNull("unable to create edit changelist", changelist);
			List<IFileSpec> editFiles = client.editFiles(FileSpecBuilder.makeFileSpecList(srcFileDepotPath),
										new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull("null edit files list", editFiles);
			assertEquals(1, editFiles.size());
			changelist.refresh();
			inStream = new FileInputStream(new File(this.getSystemPath(client, srcFileDepotPath)));
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
			this.copyFile(tmpFile.getAbsolutePath(), this.getSystemPath(client, srcFileDepotPath), true);
			List<IFileSpec> submitFiles = changelist.submit(new SubmitOptions());
			assertNotNull(submitFiles);
			assertEquals(2, submitFiles.size());
			assertEquals("submit failure: " + submitFiles.get(0).getStatusMessage(),
						FileSpecOpStatus.VALID, submitFiles.get(0).getOpStatus());
			changelist = CoreFactory.createChangelist(client, changelistDescription, true);
			List<IFileSpec> integFiles = client.integrateFiles(
											new FileSpec(srcFileDepotPath),
											new FileSpec(tgtFileDepotPath),
											null,
											new IntegrateFilesOptions().setChangelistId(
																			changelist.getId()));
			assertNotNull("null return from integrate files", integFiles);
			assertEquals(1, integFiles.size());
			assertEquals("integ failure: " + integFiles.get(0).getStatusMessage(),
					FileSpecOpStatus.VALID, integFiles.get(0).getOpStatus());
			changelist.refresh();
			List<IFileSpec> resolveFiles = client.resolveFilesAuto(integFiles,
											new ResolveFilesAutoOptions().setSafeMerge(true));
			assertNotNull("null resolve files return", resolveFiles);
			changelist.refresh();
			submitFiles = changelist.submit(new SubmitOptions());
			assertNotNull(submitFiles);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			
			if ((client != null) && (changelist != null)
							&& (changelist.getStatus() != ChangelistStatus.SUBMITTED)) {
				try {
					client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."),
											new RevertFilesOptions());
				} catch (P4JavaException e) {
					// Ignore
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
