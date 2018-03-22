/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.ShelveFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.client.UnshelveFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test shelve and unshelve utf16-le encoded files in the Windows environment.
 */
@Jobs({"job080389"})
@TestId("Dev152_ShelveUtf6leWindowsTest")
public class
ShelveUnshelveUtf6leWindowsTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ShelveUnshelveUtf6leWindowsTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, null);
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		if (SystemInfo.isWindows()) {

			try {
				if (server.isConnected()) {
					if (server.supportsUnicode()) {
						server.setCharsetName("utf8");
					}
				}
				server.setCurrentClient(server.getClient("p4TestUserWS20112Windows"));
			} catch (P4JavaException e) {
				fail("Unexpected exception: " + e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Test shelve unshelve utf16-le encoded files in the Windows environment
	 */
	@Test
	public void testShelveUnshelveUtf6leWindows() {
		if (SystemInfo.isWindows()) {
			IClient client = server.getCurrentClient();
			int randNum = getRandomInt();

			IChangelist changelist = null;
			IChangelist targetChangelist = null;

			String fileName = "utf16leExample";
			String fileExt = ".fm";

			String depotPath = "//depot/cases/180445/";
			String localPath = "/cases/180445/";

			String depotFile = depotPath + fileName + fileExt;
			String depotFile2 = depotPath + fileName + randNum + fileExt;

			String localFile = client.getRoot() + localPath + fileName + fileExt;
			String localFile2 = client.getRoot() + localPath + fileName + randNum + fileExt;

			try {
				List<IFileSpec> files = client.sync(FileSpecBuilder.makeFileSpecList(depotFile),
						new SyncOptions().setForceUpdate(true));
				assertNotNull(files);

				// Copy a file to be used for add and shelve
				copyFile(localFile, localFile2);

				changelist = getNewChangelist(server, client,
						"Dev152_ShelveUtf6leWindowsTest add files");
				assertNotNull(changelist);
				changelist = client.createChangelist(changelist);
				assertNotNull(changelist);

				// Add a file with set type to "utf16"
				files = client.addFiles(FileSpecBuilder.makeFileSpecList(localFile2),
						// new
						// AddFilesOptions().setChangelistId(changelist.getId()));
						new AddFilesOptions().setChangelistId(changelist.getId())
								.setFileType("utf16"));
				assertNotNull(files);
				assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(files).size());

				// Shelve the file
				files = client.shelveFiles(FileSpecBuilder.makeFileSpecList(depotFile2),
						changelist.getId(), new ShelveFilesOptions());
				assertNotNull(files);
				assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(files).size());

				// Revert the add file after shelving
				files = client.revertFiles(FileSpecBuilder.makeFileSpecList(depotFile2),
						new RevertFilesOptions().setChangelistId(changelist.getId()));
				assertNotNull(files);
				assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(files).size());

				// Delete the local file
				(new File(localFile2)).delete();

				// Create changelist for unshelving
				targetChangelist = client.createChangelist(this.createChangelist(client));
				assertNotNull(targetChangelist);

				// Unshelve the file
				files = client.unshelveFiles(FileSpecBuilder.makeFileSpecList(depotFile2),
						changelist.getId(), targetChangelist.getId(),
						new UnshelveFilesOptions().setForceUnshelve(true));
				assertNotNull(files);
				assertEquals(0, FileSpecBuilder.getInvalidFileSpecs(files).size());
			} catch (P4JavaException e) {
				fail("Unexpected exception: " + e.getLocalizedMessage());
			} finally {
				if (client != null) {
					try {
						@SuppressWarnings("unused")
						List<IFileSpec> fileList = client.revertFiles(
								FileSpecBuilder.makeFileSpecList(depotFile2),
								new RevertFilesOptions().setChangelistId(changelist.getId()));
						// fileList = client.revertFiles(
						// FileSpecBuilder.makeFileSpecList(depotFile2),
						// new
						// RevertFilesOptions().setChangelistId(targetChangelist.getId()));
						fileList = client.shelveFiles(FileSpecBuilder.makeFileSpecList(depotFile2),
								changelist.getId(), new ShelveFilesOptions().setDeleteFiles(true));
						// fileList = client.shelveFiles(
						// FileSpecBuilder.makeFileSpecList(depotFile2),
						// targetChangelist.getId(), new
						// ShelveFilesOptions().setDeleteFiles(true));
						try {
							String deleteResult = server
									.deletePendingChangelist(changelist.getId());
							assertNotNull(deleteResult);
							deleteResult = server.deletePendingChangelist(targetChangelist.getId());
							assertNotNull(deleteResult);

							// Delete the local file
							(new File(localFile2)).delete();
						} catch (Exception exc) {
							System.out.println(exc.getLocalizedMessage());
						}
					} catch (P4JavaException exc) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
