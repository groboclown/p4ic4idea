/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 copy -Sstream -PparentStream -F".
 */
@Jobs({ "job046694" })
@TestId("Dev112_CopyStreamFilesTest")
public class CopyStreamFilesTest extends P4JavaTestCase {

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
	 * Test "p4 copy -Sstream -PparentStream -F".
	 */
	@Test
	public void testCopyStreamFiles() {

		IClient devStreamClient = null;
		IClient mainStreamClient = null;

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		String devStream = "//p4java_stream/dev";
		String mainStream = "//p4java_stream/main";

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String devSourceFile = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/Getopt.java";
		String devTargetFile = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt.java";

		String devTargetFile2 = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt2.java";

		String devTargetFile3 = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt3.java";

		String mainTargetFile = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt.java";

		String mainTargetFile2 = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt2.java";

		String mainTargetFile3 = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt3.java";

		try {
			// Get the test main stream client
			mainStreamClient = server.getClient("p4java_stream_main");
			assertNotNull(mainStreamClient);

			// Get the test dev stream client
			devStreamClient = server.getClient("p4java_stream_dev");
			assertNotNull(devStreamClient);

			// Set the dev stream client to the server.
			server.setCurrentClient(devStreamClient);

			// Copy the dev source file to dev target
			changelist = getNewChangelist(server, devStreamClient,
					"Dev112_CopyStreamFilesTest copy files");
			assertNotNull(changelist);
			changelist = devStreamClient.createChangelist(changelist);
			files = devStreamClient.copyFiles(new FileSpec(devSourceFile),
					new FileSpec(devTargetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			files = devStreamClient.copyFiles(new FileSpec(devSourceFile),
					new FileSpec(devTargetFile2), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			files = devStreamClient.copyFiles(new FileSpec(devSourceFile),
					new FileSpec(devTargetFile3), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Copy-up the new file from the dev stream to the main stream
			// Change the client workspace to target client workspace
			server.setCurrentClient(mainStreamClient);
			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_CopyStreamFilesTest copy files");
			assertNotNull(changelist);

			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient
					.copyFiles(null, FileSpecBuilder
							.makeFileSpecList(new String[] { mainTargetFile,
									mainTargetFile2, mainTargetFile3 }),
							new CopyFilesOptions().setChangelistId(changelist
									.getId()).setStream(devStream));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Check the stream integration status
			IStreamIntegrationStatus integrationStatus = server
					.getStreamIntegrationStatus(devStream,
							new StreamIntegrationStatusOptions()
									.setBidirectional(true));
			assertNotNull(integrationStatus);
			assertTrue(integrationStatus.isChangeFlowsFromParent());
			assertTrue(integrationStatus.isChangeFlowsToParent());
			assertFalse(integrationStatus.isFirmerThanParent());
			assertTrue(integrationStatus.isIntegFromParent());
			assertEquals(integrationStatus.getIntegFromParentHow(), "merge");
			assertTrue(integrationStatus.isIntegToParent());
			assertEquals(integrationStatus.getIntegToParentHow(), "copy");

			// Retrieve the revision history ('filelog') of the copied file
			Map<IFileSpec, List<IFileRevisionData>> fileRevisionHisotryMap = server
					.getRevisionHistory(
							FileSpecBuilder.makeFileSpecList(new String[] {
									mainTargetFile, mainTargetFile2,
									mainTargetFile3 }),
							new GetRevisionHistoryOptions());
			assertNotNull(fileRevisionHisotryMap);
			assertEquals(3, fileRevisionHisotryMap.size());
			for (Map.Entry<IFileSpec, List<IFileRevisionData>> entry : fileRevisionHisotryMap
					.entrySet()) {
				assertNotNull(entry);
				List<IFileRevisionData> fileRevisionDataList = entry.getValue();
				assertNotNull(fileRevisionDataList);
				assertEquals(1, fileRevisionDataList.size());
				IFileRevisionData fileRevisionData = fileRevisionDataList
						.get(0);
				assertNotNull(fileRevisionData);
				List<IRevisionIntegrationData> revisionIntegrationDataList = fileRevisionData
						.getRevisionIntegrationDataList();
				assertEquals(1, revisionIntegrationDataList.size());
				IRevisionIntegrationData revisionIntegrationData = revisionIntegrationDataList
						.get(0);
				assertNotNull(revisionIntegrationData);

				// Verify the copied stream file
				assertEquals(-1, revisionIntegrationData.getStartFromRev());
				assertEquals(1, revisionIntegrationData.getEndFromRev());
				assertEquals("branch from",
						revisionIntegrationData.getHowFrom());
				assertTrue(revisionIntegrationData.getFromFile().contains(
						devStream));
			}

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
			if (server != null) {
				if (devStreamClient != null) {
					try {
						// Delete submitted test files in the dev stream
						server.setCurrentClient(devStreamClient);
						changelist = getNewChangelist(server, devStreamClient,
								"Dev112_CopyStreamFilesTest delete submitted files");
						changelist = devStreamClient
								.createChangelist(changelist);
						devStreamClient.deleteFiles(FileSpecBuilder
								.makeFileSpecList(new String[] { devTargetFile,
										devTargetFile2, devTargetFile3 }),
								new DeleteFilesOptions()
										.setChangelistId(changelist.getId()));
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
								"Dev112_CopyStreamFilesTest delete submitted files");
						changelist = mainStreamClient
								.createChangelist(changelist);
						mainStreamClient.deleteFiles(FileSpecBuilder
								.makeFileSpecList(new String[] {
										mainTargetFile, mainTargetFile2,
										mainTargetFile3 }),
								new DeleteFilesOptions()
										.setChangelistId(changelist.getId()));
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
