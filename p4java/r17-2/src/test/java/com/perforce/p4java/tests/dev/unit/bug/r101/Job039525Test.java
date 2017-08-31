/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test job039525 -- missing error messages for non-existent file checkouts.
 */
@TestId("Bugs101_Job039525Test")
public class Job039525Test extends P4JavaTestCase {

	public Job039525Test() {
	}

	@Test
	public void testJob039525CheckOut() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job039525Test";
		final String testFileName = testRoot + "/" + "test01.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			String localFileName = this.getSystemPath(client, testFileName);
			assertNotNull(localFileName);
			File testFile = new File(localFileName);
			assertTrue(testFile.exists());
			assertTrue("unable to set test file writeable",
					SysFileHelperBridge.getSysFileCommands().setWritable(testFile.getCanonicalPath(), true));
								//testFile.setWritable(true));
			assertTrue("unable to delete test file", testFile.delete());
			changelist = client.createChangelist(new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					this.getUserName(),
					ChangelistStatus.NEW,
					null,
					"Bugs101_Job039525Test test submit changelist",
					false,
					(Server) server
				));
			assertNotNull(changelist);
			List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(testFileName),
													new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editList);
			assertEquals(2, editList.size());
			assertEquals(FileSpecOpStatus.INFO, editList.get(1).getOpStatus());
			assertNotNull(editList.get(1).getStatusMessage());
			assertTrue(editList.get(1).getStatusMessage().startsWith("can't change mode of file"));
			changelist.refresh();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null) && (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testFileName),
									new RevertFilesOptions().setChangelistId(changelist.getId()));
							server.deletePendingChangelist(changelist.getId()); // not strictly necessary...
						} catch (P4JavaException exc) {
						}
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
