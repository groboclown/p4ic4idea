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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 *
 */
@TestId("Bugs101_Job040346Test")
public class Job040346Test extends P4JavaTestCase {

	public Job040346Test() {
	}

	@Test
	public void testBaseInfoSettings() {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040346Test";
		final String testFile01 = testRoot + "/" + "test01.txt";
		final String testFile02 = testRoot + "/" + "test02.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			this.forceSyncFiles(client, testRoot + "/...");
			
			changelist = client.createChangelist(new Changelist(
										IChangelist.UNKNOWN,
										client.getName(),
										this.getUserName(),
										ChangelistStatus.NEW,
										null,
										"Bugs101_Job040346Test test changelist",
										false,
										(Server) server
								));
			assertNotNull(changelist);
			List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(testFile01),
														new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
			changelist.refresh();
			List<IFileSpec> submitList = changelist.submit(null);
			assertNotNull(submitList);
			assertEquals(1, FileSpecBuilder.getValidFileSpecs(submitList).size());
			changelist = client.createChangelist(new Changelist(
														IChangelist.UNKNOWN,
														client.getName(),
														this.getUserName(),
														ChangelistStatus.NEW,
														null,
														"Bugs101_Job040346Test test changelist",
														false,
														(Server) server
												));
			List<IFileSpec> integList = client.integrateFiles(
													new FileSpec(testFile01),
													new FileSpec(testFile02),
													null,
													new IntegrateFilesOptions()
															.setChangelistId(changelist.getId())
															.setDisplayBaseDetails(true));
			assertNotNull(integList);
			assertEquals(1, integList.size());
			assertNotNull(integList.get(0).getBaseName());
			assertEquals(testFile01, integList.get(0).getBaseName());
			assertTrue(integList.get(0).getBaseRev() > 0);
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
