/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job040205Test")
public class Job040205Test extends P4JavaTestCase {

	public Job040205Test() {
	}

	@Test
	public void testDeletedActionSet() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040205Test";
		final String testFile = testRoot + "/" + "test.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			List<IFileSpec> syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFile + "#0"),
														new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles);
			for (IFileSpec syncFile : syncFiles) {
				assertNotNull(syncFile);
				if (syncFile.getOpStatus() == FileSpecOpStatus.VALID) {
					if (syncFile.getDepotPathString().equals(testFile)) {
						assertNotNull("file action is null", syncFile.getAction());
						assertEquals("", FileAction.DELETED, syncFile.getAction());
					}
				}
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null)
							&& (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client
									.revertFiles(
											FileSpecBuilder
													.makeFileSpecList(testRoot
															+ "/..."),
											new RevertFilesOptions()
													.setChangelistId(changelist
															.getId()));
							server.deletePendingChangelist(changelist.getId());
						} catch (P4JavaException exc) {
						}
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
