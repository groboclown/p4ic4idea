/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.core;

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
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple, limited, opened files tests. Need to be fleshed out
 * later on down the line.
 */

@TestId("Core_GetOpenedFilesTest")
public class GetOpenedFilesTest extends P4JavaTestCase {

	public GetOpenedFilesTest() {
	}

	@Test
	public void testGetOpenedFiles() {
		
		final String testPath = "//depot/basic/core/GetOpenedFilesTest/...";
		final String testClientName = "p4jtest-GetOpenedFilesTest";
		IOptionsServer server = null;
		IClient client = null;

		try {
			server = getServer();
			client = server.getClient(testClientName);
			assertNotNull(client);
			server.setCurrentClient(client);
			
			// First tests don't really have any set expected results; just want to
			// make sure nothing bad happens...
			List<IFileSpec> openedFiles = server.getOpenedFiles(
							null, true, null, 0, IChangelist.UNKNOWN);
			assertNotNull(openedFiles);
						
			// Do some trivial comparisons of convenience (client) vs. main (server) methods:
			
			List<IFileSpec> fileList = FileSpecBuilder.makeFileSpecList(testPath);
			assertNotNull(fileList);
			List<IFileSpec> syncList = this.forceSyncFiles(client, testPath);
			assertNotNull(syncList);
			
			openedFiles = server.getOpenedFiles(null, false, client.getName(), 0, IChangelist.UNKNOWN);
			assertNotNull(openedFiles);
			int filesRetrieved = openedFiles.size();
			openedFiles = client.openedFiles(null, 0, IChangelist.UNKNOWN);
			assertNotNull(openedFiles);
			assertEquals(filesRetrieved, openedFiles.size());

			// Now open some target files for edit:

			IChangelist changelist = client.createChangelist(new Changelist(
											IChangelist.UNKNOWN,
											client.getName(),
											this.getUserName(),
											ChangelistStatus.NEW,
											null,
											"Temporary changelist for " + this.getTestId(),
											false,
											(Server) server
									));
			assertNotNull(changelist);
			
			List<IFileSpec> editFiles = client.editFiles(fileList, false, false, changelist.getId(), null);
			assertNotNull(editFiles);
			assertTrue(editFiles.size() > 1);
			openedFiles = client.openedFiles(fileList, 0, changelist.getId());
			assertNotNull(openedFiles);
			assertEquals(FileSpecBuilder.getValidFileSpecs(editFiles).size(), openedFiles.size());
			filesRetrieved = openedFiles.size();
			openedFiles = server.getOpenedFiles(fileList, false, client.getName(), 0, changelist.getId());
			assertNotNull(openedFiles);
			assertEquals(filesRetrieved, openedFiles.size());
			
			openedFiles = client.openedFiles(fileList, new OpenedFilesOptions().setMaxFiles(5));
			assertNotNull(openedFiles);
			assertEquals(5, openedFiles.size());
			
			List<IFileSpec> revertedFiles = client.revertFiles(fileList, null);
			assertNotNull(revertedFiles);
			String deleteResult = server.deletePendingChangelist(changelist.getId());
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
