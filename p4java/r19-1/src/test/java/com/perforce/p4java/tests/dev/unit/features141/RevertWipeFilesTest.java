/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test "revert -w" wipe add files command option.
 */
@Jobs({ "job070485" })
@TestId("Dev141_RevertWipeFilesTest")
public class RevertWipeFilesTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", RevertWipeFilesTest.class.getSimpleName());

	IChangelist changelist = null;
	List<IFileSpec> files = null;

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			setupServer(p4d.getRSHURL(), userName, password, true, null);
			client = getClient(server);
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
	 * Test "revert -w" wipe add files command option.
	 */
	@Test
	public void testRevertWipeAddFiles() {
		int randNum = getRandomInt();

		try {
			String path = "/112Dev/CopyFilesTest/src/test01";
			String ext = ".txt";
			String test = "-job070485-";
			String file = client.getRoot() + path + ext;
			String file2 = client.getRoot() + path + test + randNum + ext;

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(file),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Copy a file to be used for add
			copyFile(file, file2);

			changelist = getNewChangelist(server, client,
					"Bug131_AddFilesCheckSymlinkTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add a file
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
					new AddFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(files);

			// Revert files with "revert -w" option
			client.revertFiles(changelist.getFiles(true),
					new RevertFilesOptions()
							.setWipeAddFiles(true)
							.setChangelistId(changelist.getId()));
			// Delete changelist
			server.deletePendingChangelist(changelist.getId());

			// Local add file should not exits
			assertFalse((new File(file2)).exists());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
