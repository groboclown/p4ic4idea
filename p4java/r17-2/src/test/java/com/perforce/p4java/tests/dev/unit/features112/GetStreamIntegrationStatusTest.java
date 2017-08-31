/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test the IOptionsServer.getStreamIntegrationStatus method.
 */
@Jobs({ "job046687" })
@TestId("Dev112_GetStreamIntegrationStatusTest")
public class GetStreamIntegrationStatusTest extends P4JavaTestCase {

	/** Line separator for this system. */
	protected static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	IOptionsServer server = null;

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
	 * Test IOptionsServer.getStreamIntegrationStatus method
	 */
	@Test
	public void testStreams() {
		IClient devStreamClient = null;
		IClient mainStreamClient = null;

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		String devStream = "//p4java_stream/dev";
		String mainStream = "//p4java_stream/main";

		int randNum = getRandomInt();
		String dir = "testfiles" + randNum;

		String relativeFilePath = dir + "/testnewfile.txt";
		String devTargetFile = devStream + "/" + relativeFilePath;
		String mainTargetFile = mainStream + "/" + relativeFilePath;

		try {
			// Get the test main stream client
			mainStreamClient = server.getClient("p4java_stream_main");
			assertNotNull(mainStreamClient);

			// Get the test dev stream client
			devStreamClient = server.getClient("p4java_stream_dev");
			assertNotNull(devStreamClient);

			// Set the main stream client to the server.
			server.setCurrentClient(devStreamClient);

			// Add a new file to the dev stream
			File newFile = new File(devStreamClient.getRoot() + "/"
					+ relativeFilePath);
			if (newFile.getParent() != null) {
				File newDir = new File(newFile.getParent());
				newDir.mkdirs();
			}
			newFile.createNewFile();
			writeFileBytes(newFile.getCanonicalPath(), LINE_SEPARATOR
					+ "// test text"
					+ Calendar.getInstance().getTime().toString(), true);

			changelist = getNewChangelist(server, devStreamClient,
					"Dev112_GetStreamIntegrationStatusTest dev stream changelist");
			changelist = devStreamClient.createChangelist(changelist);
			assertNotNull(changelist);
			files = devStreamClient.addFiles(FileSpecBuilder
					.makeFileSpecList(newFile.getCanonicalPath()),
					new AddFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Copy-up the new file from the dev stream to the main stream
			// Change the client workspace to target client workspace
			server.setCurrentClient(mainStreamClient);
			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_StreamCopyFilesTest copy files");
			assertNotNull(changelist);

			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient.copyFiles(null, FileSpecBuilder
					.makeFileSpecList(new String[] { mainTargetFile }),
					new CopyFilesOptions().setChangelistId(changelist.getId()).setStream(devStream));
			assertNotNull(files);

			// Get the cached stream integration status before the submit
			IStreamIntegrationStatus integrationStatus = server
					.getStreamIntegrationStatus(devStream,
							new StreamIntegrationStatusOptions()
									.setBidirectional(true).setNoRefresh(true));
			assertNotNull(integrationStatus);
			assertTrue(integrationStatus.isChangeFlowsFromParent());
			assertTrue(integrationStatus.isChangeFlowsToParent());
			assertFalse(integrationStatus.isFirmerThanParent());
			assertFalse(integrationStatus.isIntegFromParent());
			assertFalse(integrationStatus.isIntegToParent());

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Get the stream integration status with cache update after the
			// submit
			integrationStatus = server.getStreamIntegrationStatus(devStream,
					new StreamIntegrationStatusOptions().setBidirectional(true)
							.setForceUpdate(true));
			assertNotNull(integrationStatus);
			assertTrue(integrationStatus.isChangeFlowsFromParent());
			assertTrue(integrationStatus.isChangeFlowsToParent());
			assertFalse(integrationStatus.isFirmerThanParent());
			assertTrue(integrationStatus.isIntegFromParent());
			assertEquals(integrationStatus.getIntegFromParentHow(), "merge");
			assertTrue(integrationStatus.isIntegToParent());
			assertEquals(integrationStatus.getIntegToParentHow(), "copy");

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (devStreamClient != null) {
					try {
						// Delete submitted test files in the dev stream
						server.setCurrentClient(devStreamClient);
						changelist = getNewChangelist(server, devStreamClient,
								"Dev112_StreamCopyFilesTest delete submitted files");
						changelist = devStreamClient
								.createChangelist(changelist);
						devStreamClient
								.deleteFiles(
										FileSpecBuilder
												.makeFileSpecList(new String[] { devTargetFile }),
										new DeleteFilesOptions()
												.setChangelistId(changelist
														.getId()));
						changelist.refresh();
						changelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
				if (mainStreamClient != null) {
					try {
						// Delete submitted test files in the main stream
						server.setCurrentClient(mainStreamClient);
						changelist = getNewChangelist(server, mainStreamClient,
								"Dev112_StreamCopyFilesTest delete submitted files");
						changelist = mainStreamClient
								.createChangelist(changelist);
						mainStreamClient
								.deleteFiles(
										FileSpecBuilder
												.makeFileSpecList(new String[] { mainTargetFile }),
										new DeleteFilesOptions()
												.setChangelistId(changelist
														.getId()));
						changelist.refresh();
						changelist.submit(null);
					} catch (P4JavaException e) {
						// Can't do much here...
					}
				}
			}
		}
	}
}
