/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test add files - auto detection of xbinary file type.
 */
@Jobs({ "job050676" })
@TestId("Dev112_FileTypeDetectionTest")
public class FileTypeDetectionTest extends P4JavaTestCase {

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
			// Requires super user
			server = ServerFactory.getOptionsServer("p4java://eng-p4java-vm.perforce.com:30111",
					null);
			assertNotNull(server);
			// Connect to the server.
			server.connect();

            // Set the Perforce charset.
            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                	server.setCharsetName("utf8");
                }
            }
			
			// Set the server user
			server.setUserName(this.userName);

			// Login with user password
			server.login(this.password);

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
	 * Test add files - auto detection of xbinary file type.
	 */
	@Test
	public void testAddFileTypeDetection() {
		int randNum = getRandomInt();
		String depotFile = null;

		try {
			String path = "/112Dev/xbin/eclipse.exe";
			String file = client.getRoot() + path;
			// Preserve the .exe or Windows will not see it as executable
			String file2 = (client.getRoot() + path).replace("eclipse", "eclipse" + randNum);
			depotFile = "//depot" + path.replace("eclipse", "eclipse" + randNum);

			List<IFileSpec> files = client.sync(
					FileSpecBuilder.makeFileSpecList(file),
					new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Copy a file to be used for add
			copyFile(file, file2);

			// Set the file to be executable
			File xbinFile = new File(file2);
			SysFileHelperBridge.getSysFileCommands().setExecutable(
			        xbinFile.getAbsolutePath(), true, false);
			
			changelist = getNewChangelist(server, client,
					"Dev112_FileTypeDetectionTest add files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Add a file (it should detect it as xbinary
			files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
					new AddFilesOptions().setChangelistId(changelist.getId()));

			assertNotNull(files);
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Verify the file in the depot has the specified "xbinary" type
			List<IExtendedFileSpec> extFiles = server.getExtendedFiles(
					FileSpecBuilder.makeFileSpecList(depotFile),
					new GetExtendedFilesOptions());
			assertNotNull(extFiles);
			assertTrue(extFiles.size() == 1);
			assertNotNull(extFiles.get(0));
			assertTrue(extFiles.get(0).getHeadAction() == FileAction.ADD);
			assertTrue(extFiles.get(0).getHeadType().contentEquals("xbinary"));

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
								"Dev112_FileTypeDetectionTest delete submitted files");
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
