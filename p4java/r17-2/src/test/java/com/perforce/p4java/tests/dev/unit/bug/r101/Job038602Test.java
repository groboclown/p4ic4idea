/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job038602Test")
public class Job038602Test extends P4JavaTestCase {

	public Job038602Test() {
	}

	@Test
	public void testExtendedAscii() throws Exception {
		final String sourceDir = "//depot/101Bugs/Bugs101_Job038602Test";
		final String sourceFile = sourceDir + "/" + "test01.txt";
		final String targetFile = sourceDir + "/" + "test01Cpy.txt";
		IOptionsServer server = null;
		IClient client = null;
		File target = null;
		IChangelist changelist =  null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			client.revertFiles(FileSpecBuilder.makeFileSpecList(targetFile), null);
			this.forceSyncFiles(client, sourceDir + "/...");
			target = new File(this.getSystemPath(client, targetFile));
			if (!target.exists()) {
				this.copyFile(this.getSystemPath(client, sourceFile), this.getSystemPath(client, targetFile));
			}
			
			changelist = client.createChangelist(new Changelist(
															IChangelist.UNKNOWN,
															client.getName(),
															this.getUserName(),
															ChangelistStatus.NEW,
															null,
															"Bugs101_Job038602Test Changelist",
															false,
															(Server) server
														));
			assertNotNull(changelist);
			AddFilesOptions afo = new AddFilesOptions();
			afo.setChangelistId(changelist.getId());
			afo.setFileType("text");
			List<IFileSpec> addFiles = client.addFiles(
											FileSpecBuilder.makeFileSpecList(targetFile),
											afo);
			assertNotNull(addFiles);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(addFiles).size());
			List<IExtendedFileSpec> extSpecs = server.getExtendedFiles(
										FileSpecBuilder.makeFileSpecList(targetFile), null);
			assertNotNull(extSpecs);
			assertEquals(1, extSpecs.size());
			assertNotNull(extSpecs.get(0).getFileType());
			assertEquals("text", extSpecs.get(0).getFileType());
		} finally {
			if (server != null) {
				if (client != null) {
					if (changelist != null) {
						try {
							client.revertFiles(
										FileSpecBuilder.makeFileSpecList(targetFile),
										new RevertFilesOptions().setChangelistId(changelist.getId()));
							server.deletePendingChangelist(changelist.getId());
						} catch (Exception exc) {
							// ignore
						}
					}
				}
				this.endServerSession(server);
			}
			if (target != null) {
				target.delete();
			}
		}
	}
}
