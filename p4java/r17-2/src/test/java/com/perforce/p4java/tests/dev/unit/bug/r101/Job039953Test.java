/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job039953Test")
public class Job039953Test extends P4JavaTestCase {

	public Job039953Test() {
	}

	@Test
	public void testJob039953DeleteOptions() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job039953Test";
		final String testFile01 = testRoot + "/" + "test01.txt";
		final String testFile02 = testRoot + "/" + "test02.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist01 = null;
		IChangelist changelist02 = null;
		DeleteFilesOptions opts = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			List<IFileSpec> syncFiles = this.forceSyncFiles(client, testRoot + "/...");
			assertNull(this.reportInvalidSpecs(syncFiles));
			changelist01 = client.createChangelist(new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					this.getUserName(),
					ChangelistStatus.NEW,
					null,
					"Bugs101_Job039953Test test submit changelist",
					false,
					(Server) server
				));
			assertNotNull(changelist01);
			opts = new DeleteFilesOptions().setChangelistId(changelist01.getId());
			opts.setImmutable(true);
			List<IFileSpec> deleteList = client.deleteFiles(
										FileSpecBuilder.makeFileSpecList(testFile01), opts);
			assertNotNull(deleteList);
			assertNull(this.reportInvalidSpecs(deleteList));
			
			changelist01.refresh();
			changelist02 = client.createChangelist(new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					this.getUserName(),
					ChangelistStatus.NEW,
					null,
					"Bugs101_Job039953Test test submit changelist",
					false,
					(Server) server
				));
			assertNotNull(changelist02);
			opts.setChangelistId(changelist02.getId());
			deleteList = client.deleteFiles(
						FileSpecBuilder.makeFileSpecList(testFile02), opts);
			assertNotNull(deleteList);
			changelist02.refresh();
			List<IFileSpec> cl01Files = changelist01.getFiles(true);
			List<IFileSpec> cl02Files = changelist02.getFiles(true);
			assertNotNull(cl01Files);
			assertNotNull(cl02Files);
			assertEquals(2, FileSpecBuilder.getValidFileSpecs(cl01Files).size());
			assertEquals(0, FileSpecBuilder.getValidFileSpecs(cl02Files).size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist01 != null) && (changelist01.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."), null);
							server.deletePendingChangelist(changelist01.getId());
						} catch (P4JavaException exc) {
						}
					}
					if ((changelist02 != null) && (changelist02.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."), null);
							server.deletePendingChangelist(changelist02.getId());
						} catch (P4JavaException exc) {
						}
					}
				}
				this.endServerSession(server);
			}
		}
	}
}
