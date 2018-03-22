/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features141;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "diff2 -Od" limits output to files that differ.
 */
@Jobs({ "job070486" })
@TestId("Dev141_GetFileDiffsLimitOutputTest")
public class GetFileDiffsLimitOutputTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;
	IChangelist changelist = null;
	List<IFileSpec> files = null;

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
			client = getDefaultClient(server);
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
	 * Test "diff2 -Od" limits output to files that differ.
	 */
	@Test
	public void testGetFileDiffs() {

		int randNum = getRandomInt();
		String depotFile = null;

		try {
			String path = "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/";
			String name = "P4JCommandCallbackImpl";
			String ext = ".java";
			String file = client.getRoot() + path + name + ext;
			String file2 = client.getRoot() + path + name + "-" + randNum + ext;
			depotFile = "//depot" + path + name + "-" + randNum + ext;

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(file),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Copy a file to be used for add
			copyFile(file, file2);

			changelist = getNewChangelist(server, client,
					"Dev112_EditFilesTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add the file with type "text"
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
					new AddFilesOptions().setChangelistId(changelist.getId()).setFileType("text"));

			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Limit the output to files that differ.
			List<IFileDiff> diffsList = server.getFileDiffs(new FileSpec(depotFile), new FileSpec(depotFile + "#1"),
					null, new GetFileDiffsOptions().setOutputDifferFilesOnly(true));
			assertNotNull(diffsList);

			// Should be empty
			assertTrue(diffsList.size() == 0);
			
			changelist = getNewChangelist(server, client,
					"Dev112_EditFilesTest edit files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Edit the file with type "ktext"
			files = client.editFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new EditFilesOptions().setChangelistId(changelist.getId()).setFileType("ktext"));

			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);
			
			diffsList = server.getFileDiffs(new FileSpec(depotFile), new FileSpec(depotFile + "#1"),
					null, new GetFileDiffsOptions());
			
			assertNotNull(diffsList);
			assertTrue(diffsList.size() > 0);
			assertNotNull(diffsList.get(0));
			assertNotNull(diffsList.get(0).getStatus());
			assertEquals(diffsList.get(0).getStatus(), IFileDiff.Status.TYPES);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (client != null) {
				if (changelist != null) {
					if (changelist.getStatus() == ChangelistStatus.PENDING) {
						try {
							// Revert files in pending changelist
							client.revertFiles(
									changelist.getFiles(true),
									new RevertFilesOptions()
											.setChangelistId(changelist.getId()));
						} catch (P4JavaException e) {
							// Can't do much here...
						}
					}
				}
			}
			if (client != null && server != null) {
				if (depotFile != null) {
					try {
						// Delete submitted test files
						IChangelist deleteChangelist = getNewChangelist(server,
								client,
								"Dev112_EditFilesTest delete submitted files");
						deleteChangelist = client
								.createChangelist(deleteChangelist);
						client.deleteFiles(FileSpecBuilder
								.makeFileSpecList(new String[] { depotFile }),
								new DeleteFilesOptions()
										.setChangelistId(deleteChangelist
												.getId()));
						deleteChangelist.refresh();
						deleteChangelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
