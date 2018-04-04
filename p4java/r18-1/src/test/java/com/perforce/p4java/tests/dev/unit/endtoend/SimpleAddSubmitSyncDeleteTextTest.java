/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * A very simple end-to-end test that creates a new text file, creates
 * a new changelist on the default client, adds the new file, submits
 * the changelist, does a forced sync, then
 * compares the sync'd version with the original, then deletes the file.
 * 
 * A small amount of work is done to try to ensure a unique target file
 * name, but this may fail under some circumstances. Some amount of cleanup
 * ought to be done externally after this test has been run many times (e.g.
 * obliterating the target files)...
 */

@TestId("E2E_SimpleAddSubmitSyncDeleteTextTest")
public class SimpleAddSubmitSyncDeleteTextTest extends P4JavaTestCase {
	
	// Where to sync the reference files from:
	public static final String TEST_ROOT = "//depot/e2e/SimpleAddSubmitSyncTest";
	public static final String REF_ROOT = TEST_ROOT + "/ref";
	
	public static final String TGT_ROOT = TEST_ROOT + "/test";

	public SimpleAddSubmitSyncDeleteTextTest() {
		super();
	}

	@Test
	public void testSimpleAddSubmitSyncCycle() {
		IOptionsServer server = null;
		try {
			server = getServer();
			IClient client = null;
			assertNotNull(server);
			client = getDefaultClient(server);
			assertNotNull("null client returned: '" + getDefaultTestClientName() + "'",
										client);
			server.setCurrentClient(client);
			String clientName = client.getName();
			assertNotNull(clientName);
			String clientRoot = client.getRoot();
			assertNotNull(clientRoot);
			forceSyncFiles(client, TEST_ROOT + "/...");
			final String sourceFilePath = getSystemPath(client, REF_ROOT + "/simpletxt01.txt");
			final String targetFilePath = getSystemPath(client, TGT_ROOT + "/simpletxt01-"
												+ getRandomName(false, null) + ".txt");
			copyFile(sourceFilePath, targetFilePath);

			IChangelist changeList = client.createChangelist(createChangelist(clientName,
							server,
							"Changelist for SimpleAddSubmitSyncTest add test"));
			client.refresh();
			List<IFileSpec> addList = client.addFiles(
					FileSpecBuilder.makeFileSpecList(getSystemPath(client, targetFilePath)),
					false, changeList.getId(), P4JTEST_FILETYPE_TEXT, false);
			changeList.refresh();
			assertNotNull("null file list returned from client.addFiles", addList);
			assertTrue("added file path '" + targetFilePath + "' not found in added file list",
								checkFileList(addList, getDepotPath(client, targetFilePath),
										FileSpecOpStatus.VALID));
			
			List<IFileSpec> submitList = changeList.submit(false);
			assertNotNull("null file list returned from changelist submission", submitList);
			assertTrue("submitted file path '" + targetFilePath + "' not found in submitted file list",
									checkFileList(submitList, getDepotPath(client, targetFilePath),
											FileSpecOpStatus.VALID));
			
			assertEquals(P4JTEST_FILETYPE_TEXT, getFileType(server, getDepotPath(client, targetFilePath)));
			assertTrue("added file different from ref file in depot",
					diffDepotFiles(server, getDepotPath(client, targetFilePath),
										getDepotPath(client, sourceFilePath)));
			
			List<IFileSpec> syncList = client.sync(FileSpecBuilder.makeFileSpecList(targetFilePath),
							true, false, false, false);
			assertNotNull(syncList);
			assertTrue("Sync'd new file does not compare equal to reference file",
						cmpFiles(sourceFilePath, targetFilePath));
			
			changeList = client.createChangelist(createChangelist(clientName,
							server,
							"Changelist for SimpleAddSubmitSyncTest delete test"));
			assertNotNull(changeList);
			client.refresh();
			List<IFileSpec> deleteList = client.deleteFiles(
											FileSpecBuilder.makeFileSpecList(targetFilePath),
											changeList.getId(),
											false);
			assertNotNull(deleteList);
			changeList.refresh();
			submitList = changeList.submit(false);
			assertNotNull("null file list returned from changelist submission", submitList);
			assertTrue("submitted file path '" + targetFilePath + "' not found in submitted file list",
										checkFileList(submitList, getDepotPath(client, targetFilePath),
												FileSpecOpStatus.VALID));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				try {
					server.disconnect();
				} catch (Exception e) {
				} 
			}
		}
	}
}
