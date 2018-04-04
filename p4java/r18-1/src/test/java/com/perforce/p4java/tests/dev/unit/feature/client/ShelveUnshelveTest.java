/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

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
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Some simple-minded shelve/ unshelve tests. Not intended to
 * be a comprehensive test setup.
 */
@TestId("Client_ShelveUnshelveTest")
public class ShelveUnshelveTest extends P4JavaTestCase {
	
	public static final String TEST_ROOT = "//depot/client/ShelveUnshelveTest/...";

	public ShelveUnshelveTest() {
	}

	@Test
	public void testSimpleShelveUnshelve() {
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;
		IChangelist targetChangelist = null;
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			forceSyncFiles(client, TEST_ROOT);
			changelist = client.createChangelist(this.createChangelist(client));
			assertNotNull(changelist);
			List<IFileSpec> editList = client.editFiles(
										FileSpecBuilder.makeFileSpecList(TEST_ROOT),
											new EditFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(editList);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(editList).size());
			List<IFileSpec> shelveList = client.shelveFiles(
												FileSpecBuilder.makeFileSpecList(TEST_ROOT),
												changelist.getId(),
												new ShelveFilesOptions());
			assertNotNull(shelveList);
			List<IFileSpec> invList = FileSpecBuilder.getInvalidFileSpecs(shelveList);
			assertEquals(0, invList.size());
			
			// Try reshelving without force or replace; should fail.
			shelveList = client.shelveFiles(
									FileSpecBuilder.makeFileSpecList(TEST_ROOT),
									changelist.getId(),
									new ShelveFilesOptions());
			assertNotNull(shelveList);
			invList = FileSpecBuilder.getInvalidFileSpecs(shelveList);
			assertTrue(invList.size() > 0);
			
			// Try again with force; should succeed
			shelveList = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(TEST_ROOT),
					changelist.getId(),
					new ShelveFilesOptions().setForceShelve(true));
			assertNotNull(shelveList);
			invList = FileSpecBuilder.getInvalidFileSpecs(shelveList);
			assertEquals(0, invList.size());
			
			// Try again with replace; should succeed
			shelveList = client.shelveFiles(
					null,
					changelist.getId(),
					new ShelveFilesOptions().setReplaceFiles(true));
			assertNotNull(shelveList);
			invList = FileSpecBuilder.getInvalidFileSpecs(shelveList);
			assertEquals(0, invList.size());
			
			// Now try deleting them all:
			shelveList = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(TEST_ROOT),
					changelist.getId(),
					new ShelveFilesOptions().setDeleteFiles(true));
			assertNotNull(shelveList);
			assertEquals(1, shelveList.size());
			assertNotNull(shelveList.get(0));
			assertNotNull(shelveList.get(0).getStatusMessage());
			assertTrue(shelveList.get(0).getStatusMessage().contains("deleted"));
			
			// Re-shelve to setup the unshelve:
			shelveList = client.shelveFiles(
					FileSpecBuilder.makeFileSpecList(TEST_ROOT),
					changelist.getId(),
					new ShelveFilesOptions());
			assertNotNull(shelveList);
			invList = FileSpecBuilder.getInvalidFileSpecs(shelveList);
			assertEquals(0, invList.size());
			
			// Revert everything:
			
			List<IFileSpec> revertList = client.revertFiles(
						FileSpecBuilder.makeFileSpecList(TEST_ROOT),
						new RevertFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(revertList);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(shelveList).size());
			
			targetChangelist = client.createChangelist(this.createChangelist(client));
			assertNotNull(targetChangelist);
			
			// Try unshelving -- should work fine...
			List<IFileSpec> unshelveList = client.unshelveFiles(
						FileSpecBuilder.makeFileSpecList(TEST_ROOT),
						changelist.getId(), targetChangelist.getId(), null);
			assertNotNull(unshelveList);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(shelveList).size());
			
			// Try it again -- should work fine:
			unshelveList = client.unshelveFiles(
					FileSpecBuilder.makeFileSpecList(TEST_ROOT),
					changelist.getId(), targetChangelist.getId(), null);
			assertNotNull(unshelveList);
			assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(shelveList).size());
			
			// Leave cleanup for the finally clause below...
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (client != null) {
				try {
					// Expect one or all of these to fail even under normal
					// circumstances...
					
					@SuppressWarnings("unused")
					List<IFileSpec> fileList = client.revertFiles(
							FileSpecBuilder.makeFileSpecList(TEST_ROOT),
							new RevertFilesOptions().setChangelistId(changelist.getId()));
					fileList = client.revertFiles(
							FileSpecBuilder.makeFileSpecList(TEST_ROOT),
							new RevertFilesOptions().setChangelistId(targetChangelist.getId()));
					fileList = client.shelveFiles(
							FileSpecBuilder.makeFileSpecList(TEST_ROOT),
							changelist.getId(), new ShelveFilesOptions().setDeleteFiles(true));
					fileList = client.shelveFiles(
							FileSpecBuilder.makeFileSpecList(TEST_ROOT),
							targetChangelist.getId(), new ShelveFilesOptions().setDeleteFiles(true));
					try {
						String deleteResult = server.deletePendingChangelist(changelist.getId());
						assertNotNull(deleteResult);
						deleteResult = server.deletePendingChangelist(targetChangelist.getId());
						assertNotNull(deleteResult);
					} catch (Exception exc) {
						System.out.println(exc.getLocalizedMessage());
					}
				} catch (P4JavaException exc) {
					// Can't do much here...
				}
			}
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
