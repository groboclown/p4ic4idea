/**
 *
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

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
import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test file types with executable bit set.
 */
@Jobs({ "job051617" })
@TestId("Dev112_FiletypesTest")
public class FiletypesTest extends P4JavaTestCase {

	/** The Constant highSecurityLevelServerURL. */
	private static final String HIGH_SECURITY_SERVER_URL =
	        "p4java://eng-p4java-vm.perforce.com:30111";

	/** The server. */
	private IOptionsServer server = null;
	
	/** The client. */
	private IClient client = null;
	
	/** The files helper. */
	private static SymbolicLinkHelper filesHelper;

	/**
	 * One time set up.
	 *
	 * @BeforeClass annotation to a method to be run before all the tests in a
	 *              class.
	 */
	@BeforeClass
	public static void oneTimeSetUp() {
		// one-time initialization code (before all the tests).
	    filesHelper = OSUtils.isWindows()
	            ? new WindowsRpcSystemFileCommandsHelper() : new RpcSystemFileCommandsHelper();
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
			server = ServerFactory.getOptionsServer(HIGH_SECURITY_SERVER_URL,
					null);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(final int key, final int genericCode,
						final int severityCode, final String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(final int key, final String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(final int key, final String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(final int key, final String command) {
					serverMessage = command;
				}

				public void completedServerCommand(final int key, final long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});
			server.connect();
            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                	server.setCharsetName("utf8");
                }
            }
			server.setUserName(getUserName());
			server.login(getPassword());
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
	 * Test resolve filetype changes (-At).
	 */
	@Test
	public void testResolveFiletypeChanges() {
		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		// Source and target files and directories for copy
		String copySourceFile =
		        "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties";
		String copyTargetFile = "//depot/112Dev/GetOpenedFilesTest/bin/gnu/getopt/"
				+ dir + "/MessagesBundle_es.properties";

		IChangelist changelist = null;
		List<IFileSpec> fileSpecs = null;

		try {
			// Create a changelist for copy
			changelist = getNewChangelist(server, client,
					"Dev112_FiletypesTest copy changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Make a new copy of a file
			fileSpecs = client.copyFiles(new FileSpec(copySourceFile),
					new FileSpec(copyTargetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(fileSpecs);

			// Submit the file in the copy changelist
			changelist.refresh();
			fileSpecs = changelist.submit(null);
			assertNotNull(fileSpecs);

			// Check the permission of the file
			fileSpecs = client.where(FileSpecBuilder
					.makeFileSpecList(copyTargetFile));
			assertNotNull(fileSpecs);
			assertNotNull(fileSpecs.get(0));
			assertNotNull(fileSpecs.get(0).getLocalPathString());

			String localTargetFilePath = fileSpecs.get(0).getLocalPathString();

			File file = new File(localTargetFilePath);
			assertNotNull(file);
			assertTrue(file.exists());

			// The executable bit of this file should be off
			assertFalse(filesHelper.canExecute(file.getAbsolutePath()));

			// Create a changelist for edit
			changelist = getNewChangelist(server, client,
					"Dev112_FiletypesTest edit changelist");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);

			// Edit the file and change the file type to xunicode (unicode+x)
			fileSpecs = client.editFiles(FileSpecBuilder
					.makeFileSpecList(copyTargetFile), new EditFilesOptions()
					.setChangelistId(changelist.getId())
					.setFileType("xunicode"));
			assertNotNull(fileSpecs);

			// Submit the file in the edit changelist
			changelist.refresh();
			fileSpecs = changelist.submit(null);
			assertNotNull(fileSpecs);

			fileSpecs = client.sync(
					FileSpecBuilder.makeFileSpecList(copyTargetFile),
					new SyncOptions().setForceUpdate(true));

			// Check if the file
			file = new File(localTargetFilePath);
			assertNotNull(file);
			assertTrue(file.exists());

			// The executable bit of this file should be off
			assertTrue(file.canExecute());

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
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
				try {
					// Delete submitted test files
					IChangelist deleteChangelist = getNewChangelist(server,
							client,
							"Dev112_FiletypesTest delete submitted test files changelist");
					deleteChangelist = client
							.createChangelist(deleteChangelist);
					client.deleteFiles(FileSpecBuilder
							.makeFileSpecList(new String[] { copyTargetFile }),
							new DeleteFilesOptions()
									.setChangelistId(deleteChangelist.getId()));
					deleteChangelist.refresh();
					deleteChangelist.submit(null);
				} catch (P4JavaException e) {
					// Can't do much here...
				}
			}
		}
	}
}
