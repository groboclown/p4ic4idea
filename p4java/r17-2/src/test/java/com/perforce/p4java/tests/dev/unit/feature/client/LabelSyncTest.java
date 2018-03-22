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
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple non-extensive labelsync tests. Note that the server
 * sends info <i>or</i> error messages back for the label [already]
 * in sync state, and the resulting status messages are subtly different,
 * none of which helps us reliably test this method....
 */
@TestId("Client_LabelSyncTest")
public class LabelSyncTest extends P4JavaTestCase {

	public LabelSyncTest() {
	}

	@Test
	public void testLabelSync() {
		
		final String SYNC_ROOT = "//depot/basic/readonly/labelsync/...";
		final String SYNC_LABEL_NAME = "LabelSyncTestLabel";
		final String LABEL_IN_SYNC_MSG = "label in sync";
		
		IOptionsServer server = null;
		IClient client = null;
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			
			List<IFileSpec> syncList = client.sync(
					FileSpecBuilder.makeFileSpecList(SYNC_ROOT), new SyncOptions("-f"));
			assertNotNull(syncList);
			
			ILabel syncLabel = server.getLabel(SYNC_LABEL_NAME);
			assertNotNull("sync label '" + SYNC_LABEL_NAME + "' not available",
					syncLabel);
			
			// Get an idea of how many files are a part of the fundamental syncs:
			syncList = server.getDepotFiles(
								FileSpecBuilder.makeFileSpecList(SYNC_ROOT), null);
			assertNotNull(syncList);
			assertTrue(syncList.size() > 10);	// fairly arbitrary number intended to ensure
												// we don't just get a message back...
			int listSize = syncList.size();
			
			// Initial label sync with an add operation:
			syncList = client.labelSync(
							FileSpecBuilder.makeFileSpecList(SYNC_ROOT),
							SYNC_LABEL_NAME, false, true, false);
			assertNotNull(syncList);
			// List will either be the full list if we're running after a problem,
			// or it will be one element that's just a message, hopefully the 
			// "label in sync" message...
			assertTrue((listSize == syncList.size())
					|| ((syncList.size() == 1)
							&& (syncList.get(0) != null)
							&& ((syncList.get(0).getOpStatus() == FileSpecOpStatus.INFO)
									|| (syncList.get(0).getOpStatus() == FileSpecOpStatus.ERROR))
							&& syncList.get(0).getStatusMessage().contains(LABEL_IN_SYNC_MSG)));
			
			// try to update it to an earlier version:
			
			syncList = client.labelSync(
							FileSpecBuilder.makeFileSpecList(SYNC_ROOT + "#1"),
							SYNC_LABEL_NAME, false, false, false);
			assertNotNull(syncList);
			assertEquals(listSize, syncList.size());
			
			// Try it again with the -n flag:
			syncList = client.labelSync(
							FileSpecBuilder.makeFileSpecList(SYNC_ROOT + "#1"),
							SYNC_LABEL_NAME, true, false, false);
			assertNotNull(syncList);
			assertTrue((syncList.size() == 1)
							&& (syncList.get(0) != null)
							&& ((syncList.get(0).getOpStatus() == FileSpecOpStatus.INFO)
									|| (syncList.get(0).getOpStatus() == FileSpecOpStatus.ERROR))
							&& syncList.get(0).getStatusMessage().contains(LABEL_IN_SYNC_MSG));
			
			// Reset:
			 
			syncList = client.labelSync(
					FileSpecBuilder.makeFileSpecList(SYNC_ROOT),
					SYNC_LABEL_NAME, false, false, false);
			assertNotNull(syncList);
			assertTrue((syncList.size() == listSize));
			
			// Try to delete:
			
			syncList = client.labelSync(
					FileSpecBuilder.makeFileSpecList(SYNC_ROOT),
					SYNC_LABEL_NAME, false, false, true);
			assertNotNull(syncList);
			assertTrue((syncList.size() == listSize));
			
			// Bring it all back again with an add:
			
			syncList = client.labelSync(
					FileSpecBuilder.makeFileSpecList(SYNC_ROOT),
					SYNC_LABEL_NAME, false, true, false);
			assertNotNull(syncList);
			assertTrue((syncList.size() == listSize));
			
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
