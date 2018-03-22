/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

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
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 edit -t auto" functionality which causes file type to be determined
 * as if the file were being added.
 */
@Jobs({ "job042443" })
@TestId("Dev112_ChangelistSubmitTest")
public class ChangelistSubmitTest extends P4JavaTestCase {

	/** The server. */
	private IOptionsServer server = null;
	
	/** The client. */
	private IClient client = null;
	
	/** The changelist. */
	private IChangelist changelist = null;
	
	/**
	 * One time set up.
	 *
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	}

	/**
	 * One time tear down.
	 *
	 * @AfterClass annotation to a method to be run after all the tests in a
	 *             class.
	 */
	@AfterClass
	public static void oneTimeTearDown() {
		// one-time cleanup code (after all the tests).
	}

	/**
	 * Sets the up.
	 *
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			server = getServer();
			assertNotNull(server);
			client = server.getClient(getPlatformClientName("p4TestUserWS20112"));
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Tear down.
	 *
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
	 * Test FileSpec with changelist.
	 */
	@Test
	public void testChangelistSubmit() {

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

			// Add a file specified as "binary" even though it is "text"
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
					new AddFilesOptions().setChangelistId(changelist.getId())
							.setFileType("binary"));

			assertNotNull(files);
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Verify the file in the depot has the specified "binary" type
			List<IExtendedFileSpec> extFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new GetExtendedFilesOptions());
			assertNotNull(extFiles);
			assertTrue(extFiles.size() == 1);
			assertNotNull(extFiles.get(0));
			assertTrue(extFiles.get(0).getHeadAction() == FileAction.ADD);
			assertTrue(extFiles.get(0).getHeadType().contentEquals("binary"));

			changelist = getNewChangelist(server, client,
					"Dev112_EditFilesTest edit files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Edit a file using the "-t auto" should detect it's a "text" file
			files = client.editFiles(FileSpecBuilder
					.makeFileSpecList(depotFile), new EditFilesOptions()
					.setChangelistId(changelist.getId()).setFileType("auto"));

			assertNotNull(files);
			files = changelist.submit(new SubmitOptions());

			// Verify the file in the depot has the specified "text" type
			extFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new GetExtendedFilesOptions());
			assertNotNull(extFiles);
			assertTrue(extFiles.size() == 1);
			assertNotNull(extFiles.get(0));
			assertTrue(extFiles.get(0).getHeadAction() == FileAction.EDIT);
			assertTrue(extFiles.get(0).getHeadType().contentEquals("text"));
			
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
						deleteChangelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
