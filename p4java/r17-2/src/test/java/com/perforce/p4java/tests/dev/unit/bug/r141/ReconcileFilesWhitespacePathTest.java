/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r141;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.PlatformType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test 'p4 reconcile' file path with whitespace.
 */
@Jobs({"job072688"})
@TestId("Dev141_ReconcileFilesWhitespacePathTest")
public class ReconcileFilesWhitespacePathTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ReconcileFilesWhitespacePathTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			client = server.getClient(getPlatformClientName("Job072688Client"));
			if (getHostPlatformType() == PlatformType.WINDOWS) {
				client = server.getClient("p4TestUserWS20112Windows");
			}
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Test 'p4 reconcile' file path with whitespace.
	 */
	@Test
	public void testReconcileFiles() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		int randNum = getRandomInt();
		String dir = client.getRoot() + "/112Dev/GetOpenedFilesTest/" + "branch" + randNum;

		String testZipFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd2/dir1.zip";

		String deleteFile = client.getRoot()
				+ "/112Dev/GetOpenedFilesTest/src/gnu/getopt2/LongOpt.java";

		try {
			files = client.sync(
					FileSpecBuilder.makeFileSpecList(new String[]{testZipFile, deleteFile}),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			unpack(new File(testZipFile), new File(dir));

			// Delete a local file
			File delFile = new File(deleteFile);
			assertTrue(delFile.delete());

			changelist = getNewChangelist(server, client, "Dev121_IgnoreFilesTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Reconcile files
			files = client.reconcileFiles(
					FileSpecBuilder.makeFileSpecList(new String[]{dir + "/...",
							new File(dir).getParent() + "/src/gnu/getopt2/..."}),
					new ReconcileFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(files);

			int ignoreCount = 0;

			System.out.println("*************************************");
			for (IFileSpec f : files) {
				if (f.getOpStatus() == FileSpecOpStatus.INFO) {
					assertNotNull(f.getStatusMessage());
					System.out.println("INFO: " + f.getStatusMessage());
					if (f.getStatusMessage().contains("ignored file can't be added")) {
						ignoreCount++;
					}
				} else if (f.getOpStatus() == FileSpecOpStatus.ERROR) {
					assertNotNull(f.getStatusMessage());
					System.out.println("ERROR: " + f.getStatusMessage());
				} else if (f.getOpStatus() == FileSpecOpStatus.VALID) {
					assertNotNull(f.getDepotPath());
					System.out.println("DEPOT: " + f.getDepotPath());
					assertNotNull(f.getClientPath());
					System.out.println("CLIENT: " + f.getClientPath());
					assertNotNull(f.getAction());
					System.out.println("ACTION: " + f.getAction());
				}
			}
			System.out.println("*************************************");

			// Should be greater than zero
			assertTrue(ignoreCount > 0);

			changelist.refresh();
			files = changelist.getFiles(true);
			assertNotNull(files);
			assertTrue(files.size() > 0);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (ZipException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(changelist.getFiles(true),
									new RevertFilesOptions().setChangelistId(changelist.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
			// Recursively delete the local test files
			if (dir != null) {
				deleteDir(new File(dir));
			}
		}
	}
}
