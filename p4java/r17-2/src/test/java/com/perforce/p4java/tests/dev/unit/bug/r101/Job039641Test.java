/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple test for job039641.
 * 
 */
@Jobs({"job039641"})
@TestId("Bugs101_Job039641Test")
public class Job039641Test extends P4JavaTestCase {
	
	public static final String TEST_ROOT = "//depot/101Bugs/Bugs101_Job039641Test/...";

	public Job039641Test() {
	}

	@Test
	public void testJob039641ReopenIssue() {
		IOptionsServer server = null;
		IClient client = null;

		try {
			List<IFileSpec> testFiles = FileSpecBuilder.makeFileSpecList(TEST_ROOT);
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			forceSyncFiles(client, TEST_ROOT);
			
			IChangelist changelist1 = client.createChangelist(this.createChangelist(client));
			assertNotNull(changelist1);
			List<IFileSpec> editFiles = client.editFiles(testFiles, null);
			assertNotNull(editFiles);
			List<IFileSpec> validFiles = FileSpecBuilder.getValidFileSpecs(editFiles);
			assertNotNull(validFiles);
			assertEquals(2, validFiles.size());
			
			IChangelist changelist2 = client.createChangelist(this.createChangelist(client));
			assertNotNull(changelist2);
			List<IFileSpec> reopenFiles = client.reopenFiles(editFiles, changelist2.getId(), null);
			validFiles = FileSpecBuilder.getValidFileSpecs(reopenFiles);
			assertNotNull(validFiles);
			assertEquals(2, validFiles.size());
			List<IFileSpec> changelistFiles = changelist2.getFiles(true);
			assertNotNull(changelistFiles);
			assertEquals(2, changelistFiles.size());
			
			List<IFileSpec> revertFiles = client.revertFiles(testFiles, null);
			assertNotNull(revertFiles);
			String deleteResult = server.deletePendingChangelist(changelist1.getId());
			assertNotNull(deleteResult);
			assertTrue(deleteResult.contains("deleted"));
			deleteResult = server.deletePendingChangelist(changelist2.getId());
			assertNotNull(deleteResult);
			assertTrue(deleteResult.contains("deleted"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
