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
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job040649Test")
public class Job040649Test extends P4JavaTestCase {

	public Job040649Test() {
	}

	@Test
	public void testJob040649Add() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040649Test";
		final String sourceFile = testRoot + "/" + "test01.txt";
		final String targetFileName = "test02.txt";
		String targetPath = null;
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			assertNotNull(this.getSystemPath(client, testRoot));
			targetPath = this.getSystemPath(client, testRoot) + "/" + targetFileName;
			File targetFile = new File(targetPath);
			if (targetFile.exists()) {
				assertTrue("Unable to delete target file...", targetFile.delete());
			}
			this.copyFile(this.getSystemPath(client, sourceFile), targetPath);
			changelist = client.createChangelist(new Changelist(
					IChangelist.UNKNOWN,
					client.getName(),
					this.getUserName(),
					ChangelistStatus.NEW,
					null,
					"Bugs101_Job040601Test test submit changelist",
					false,
					(Server) server
				));
			assertNotNull(changelist);
			List<IFileSpec> addList = client.addFiles(
										FileSpecBuilder.makeFileSpecList(testRoot + "/" + targetFileName),
										new AddFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(addList);
			assertEquals(1, addList.size());
			assertEquals(FileSpecOpStatus.VALID, addList.get(0).getOpStatus());
			changelist.refresh();
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (client != null) {
					if ((changelist != null) && (changelist.getStatus() == ChangelistStatus.PENDING)) {
						try {
							client.revertFiles(FileSpecBuilder.makeFileSpecList(testRoot + "/..."), null);
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
