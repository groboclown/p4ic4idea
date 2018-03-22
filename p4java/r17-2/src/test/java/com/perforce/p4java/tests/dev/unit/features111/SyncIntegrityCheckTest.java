/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Scaffold for the 10.2 sync (etc.) integrity checking on transfer.
 * Not currently a particularly meaningful test (it's very difficult to
 * test negatives here), but kept around anyway as it's still somewhat
 * useful for positive testing.
 * 
 * Note the sequence below - sync to #0 to delete the local copy,
 * force sync, then force sync again. The first forced sync uses
 * a different path under the covers in the RPC layer than the
 * second, and we need to test both (this is the difference
 * between using a temp file first with a later copy vs a
 * direct sync).
 */

@TestId("Features102_SyncIntegrityCheckTest")
public class SyncIntegrityCheckTest extends P4JavaTestCase {

	public SyncIntegrityCheckTest() {
	}

	@Test
	public void testSyncIntegrity() {
		IOptionsServer server = null;
		IClient client = null;
		final String testRoot = "//depot/102Dev/SyncIntegrityCheckTest";
		final String testFileText = testRoot + "/" + "test01.txt";
		final String testFileUBinary = testRoot + "/" + "test02.jpg";
		final String testFileBinary = testRoot + "/" + "test03.bin";
		List<IFileSpec> syncFiles = null;
		
		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull("null client", client);
			server.setCurrentClient(client);
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileText),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileText + "#0"),
												new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileText),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileText),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileUBinary),
										new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileUBinary + "#0"),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileUBinary),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileUBinary),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileBinary),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileBinary + "#0"),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileBinary),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFileBinary),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	/**
	 * Simple transfer integrity test for UTF-16 files from QA server.
	 * This test may fail if the QA server moves, etc., and / or the
	 * default test client ('p4jtest-utf16') isn't suitable for the test
	 * system. We should do better than this, but this is just test
	 * scaffolding, after all... and note that the client here uses
	 * windows line ending translation, which exercises things a bit
	 * more as well.
	 */
	@Test
	public void testUnicodeTransfer() {
		IOptionsServer server = null;
		IClient client = null;
		final String testServer = "p4java://win-qa7.perforce.com:8838";
		final String clientName = "p4jtest-utf16";
		final String userName = "p4jtestuser";
		final String password = "p4jtestuser";
		final String charsetName = "utf16";
		final String utf16FilePath = "//depot/charset_files/Unicode/UTF-16/utf16file1.txt";
		final String textFilePath = "//depot/README";
		List<IFileSpec> syncFiles = null;
		
		try {
			server = getServer(testServer, null);
			assertNotNull("null server returned", server);
			server.setCharsetName(charsetName);
			server.setUserName(userName);
			server.login(password);
			client = server.getClient(clientName);
			assertNotNull("null client returned", client);
			server.setCurrentClient(client);
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(utf16FilePath),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());

			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(utf16FilePath + "#0"),
										new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(utf16FilePath),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(utf16FilePath),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
			
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(textFilePath),
					new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
	
	@Test
	public void testLineEndTransfer() {
		IOptionsServer server = null;
		IClient client = null;
		final String clientName = "p4TestUserWSLineEndWin";
		final String testRoot = "//depot/102Dev/SyncIntegrityCheckTest";
		final String testFile = testRoot + "/" + "test04.txt";
		List<IFileSpec> syncFiles = null;
		
		try {
			server = getServer();
			client = server.getClient(clientName);
			assertNotNull("null client returned", client);
			server.setCurrentClient(client);
			syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(testFile),
										new SyncOptions().setForceUpdate(true));
			assertNotNull("null file list returned from initial sync", syncFiles);
			assertEquals("wrong number of sync file results returned", 1, syncFiles.size());
			assertNotNull("null file list element", syncFiles.get(0));
			assertEquals(FileSpecOpStatus.VALID, syncFiles.get(0).getOpStatus());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
