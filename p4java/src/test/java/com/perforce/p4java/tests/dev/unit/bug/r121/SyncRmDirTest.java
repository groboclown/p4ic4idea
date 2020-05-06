/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the sync testdir/...#0 with client option "rmdir". This should remove
 * the upstream empty directories (up to the client's root).
 */
@Jobs({ "job052977" })
@TestId("Dev112_SyncSafetyCheckTest")
public class SyncRmDirTest extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", SyncRmDirTest.class.getSimpleName());

	IClient client = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    setupServer(p4d.getRSHURL(), userName, password, true, props);
			client = getClient(server);
			createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/com/perforce/branch11136/file1.txt", "desc");
			createTextFileOnServer(client, "112Dev/GetOpenedFilesTest/src/com/perforce/branch11136/file2.txt", "desc");
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test the sync testdir/...#0 with client option "rmdir". This should
	 * remove the upstream empty directories (up to the client's root).
	 */
	@Test
	public void testSyncFilesRmDir() {

		// Relative path
		String relativePath = "112Dev/GetOpenedFilesTest/src/com/perforce/branch11136";

		// The parent directory
		File parentDir = new File(client.getRoot() + File.separator
				+ relativePath);

		List<IFileSpec> files = null;

		try {
			// Sync to head
			files = client.sync(
					FileSpecBuilder.makeFileSpecList("//depot/" + relativePath
							+ "/..."), new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Check for existing of parent directory
			assertTrue(parentDir.exists());

			// Sync to #0 with client option "rmdir"
			IClientSummary.IClientOptions clientOptions = new ClientOptions(false, false, false, false, false, true);
			client.setOptions(clientOptions);
			client.update();
			client = getClient(server);
			assertTrue(client.getOptions().isRmdir());
			files = client
					.sync(FileSpecBuilder.makeFileSpecList("//depot/"
							+ relativePath + "/...#0"),
							new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Check directories are deleted, not exist.
			assertFalse(parentDir.exists());

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (client != null) {
				try {
					files = client.sync(
							FileSpecBuilder.makeFileSpecList("//depot/"
									+ relativePath + "/..."),
							new SyncOptions().setForceUpdate(true));
					cleanupFiles(client);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
