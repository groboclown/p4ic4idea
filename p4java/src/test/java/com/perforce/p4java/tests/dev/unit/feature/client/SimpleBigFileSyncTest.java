/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Just tests syncing of a rather large binary file by first nuking it
 * on the client, then doing a standard forced sync. Can take
 * quite a long time...
 */
@TestId("Client_SimpleBigFileSyncTest")
public class SimpleBigFileSyncTest extends P4JavaTestCase {

	public SimpleBigFileSyncTest() {
	}

	@Test
	public void testBigFileSync() {
		final String testFilePath = "//depot/client/SimpleBigFileSyncTest/bigfile01.bin";

		IOptionsServer server = null;
		IClient client = null;
		File targetFile = null;
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			targetFile = new File(getSystemPath(client, testFilePath));
			if (targetFile.exists()) {
				// Nuke it with the old sync #0 trick...
				
				List<IFileSpec> syncList = client.sync(
									FileSpecBuilder.makeFileSpecList(testFilePath + "#0"),
									true, false, false, false);
				assertNotNull(syncList);
				assertEquals(1, syncList.size());
				assertNotNull(syncList.get(0));
				assertTrue(syncList.get(0).getOpStatus() != FileSpecOpStatus.ERROR);
			}
			
			// Now try to sync it:
			
			List<IFileSpec> syncList = client.sync(
					FileSpecBuilder.makeFileSpecList(testFilePath),
					true, false, false, false);
			assertNotNull(syncList);
			assertEquals(1, syncList.size());
			assertNotNull(syncList.get(0));
			assertTrue(syncList.get(0).getOpStatus() != FileSpecOpStatus.ERROR);
			
			// Check to see if it's all there:
			
			assertTrue("depot file diffs from local copy",
					this.diffTree(client, FileSpecBuilder.makeFileSpecList(testFilePath), true));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				endServerSession(server);
			}
			if (targetFile != null && targetFile.exists()) {
				targetFile.delete();
			}
		}
	}
}
