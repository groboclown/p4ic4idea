/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features151;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.zip.ZipException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.client.ReconcileFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 reconcile -m', checking file modtime instead of digests.
 */
@Jobs({ "job077547" })
@TestId("Dev151_ReconcileUseModTimeTest")
public class ReconcileUseModTimeTest extends P4JavaTestCase {

	final static String serverURL = "p4java://eng-p4java-vm.perforce.com:20141";

	IOptionsServer server = null;
	IClient client = null;
	String serverMessage = null;

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
			server = ServerFactory.getOptionsServer(serverURL, null);
			assertNotNull(server);

			// Register callback
			server.registerCallback(new ICommandCallback() {
				public void receivedServerMessage(int key, int genericCode,
						int severityCode, String message) {
					serverMessage = message;
				}

				public void receivedServerInfoLine(int key, String infoLine) {
					serverMessage = infoLine;
				}

				public void receivedServerErrorLine(int key, String errorLine) {
					serverMessage = errorLine;
				}

				public void issuingServerCommand(int key, String command) {
					serverMessage = command;
				}

				public void completedServerCommand(int key, long millisecsTaken) {
					serverMessage = String.valueOf(millisecsTaken);
				}
			});
			server.connect();
			if (server.isConnected()) {
				if (server.supportsUnicode()) {
					server.setCharsetName("utf8");
				}
			}
			server.setUserName("p4jtestuser");
			server.login("p4jtestuser");
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
	 * Test 'p4 reconcile -m', checking file modtime instead of digests.
	 */
	@Test
	public void testReconcileUseModTime() {

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		String depotFile = "//depot/Dev/rteam/ESPN723/src/lProj.java";
		String clientFile = client.getRoot() + "/Dev/rteam/ESPN723/src/lProj.java";

		try {
			files = client.sync(FileSpecBuilder.makeFileSpecList(new String[] {depotFile}), new SyncOptions().setForceUpdate(true));
			assertNotNull(files);

			// Write something to the file outside of Perforce.
			File testFile = new File(clientFile);
			if(testFile.exists()) {
				testFile.setWritable(true);
				writeFileBytes(clientFile, "// Make change #1 to test file." + LINE_SEPARATOR, true);
			}

			changelist = getNewChangelist(server, client, "Dev151_ReconcileUseModTimeTest files");
			assertNotNull(changelist);
			changelist = client.createChangelist(changelist);
			assertNotNull(changelist);

			// Reconcile files
			files = client.reconcileFiles(FileSpecBuilder.makeFileSpecList(new String[]{clientFile}),
					new ReconcileFilesOptions().setChangelistId(changelist.getId()).setOutsideEdit(true)
					.setCheckModTime(true));
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
		}
	}
}
