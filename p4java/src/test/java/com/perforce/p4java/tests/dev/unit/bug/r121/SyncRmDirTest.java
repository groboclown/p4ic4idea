/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r121;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the sync testdir/...#0 with client option "rmdir". This should remove
 * the upstream empty directories (up to the client's root).
 */
@Jobs({ "job052977" })
@TestId("Dev112_SyncSafetyCheckTest")
public class SyncRmDirTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

	/**
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getServer();
			assertNotNull(server);
			client = server.getClient("p4TestUserWS20112");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
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
				} catch (P4JavaException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
