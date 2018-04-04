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
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.MergeFilesOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.StreamIntegrationStatusOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 merge -S stream -r -P parent".
 */
@Jobs({ "job046667" })
@TestId("Dev112_MergetStreamFilesTest")
public class MergetStreamFilesTest extends P4JavaTestCase {

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
	 * Test "p4 integrate -S stream -r -P parent".
	 */
	@Test
	public void testIntegStreamFiles() {

		IClient devStreamClient = null;
		IClient mainStreamClient = null;

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		String devStream = "//p4java_stream/dev";
		String mainStream = "//p4java_stream/main";

		int randNum = getRandomInt();
		String dir = "branch" + randNum;

		String mainSourceFile = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/Getopt.java";
		String mainTargetFile = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt.java";
		String mainTargetFile2 = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt2.java";
		String mainTargetFile3 = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt3.java";
		String devTargetFile = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt.java";
		String devTargetFile2 = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt2.java";
		String devTargetFile3 = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/" + dir
				+ "/Getopt3.java";

		try {
			// Get the test main stream client
			mainStreamClient = server.getClient("p4java_stream_main");
			assertNotNull(mainStreamClient);

			// Get the test dev stream client
			devStreamClient = server.getClient("p4java_stream_dev");
			assertNotNull(devStreamClient);

			// Set the main stream client to the server.
			server.setCurrentClient(mainStreamClient);

			// Copy the main source file to main target
			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_MergetStreamFilesTest copy files");
			assertNotNull(changelist);
			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile2), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile3), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Merge-down the new file from the main stream to the dev stream.
			server.setCurrentClient(devStreamClient);
			changelist = getNewChangelist(server, devStreamClient,
					"Dev112_MergetStreamFilesTest integ files");
			assertNotNull(changelist);
			changelist = devStreamClient.createChangelist(changelist);

			// Set the target stream to the main stream
			// We should get an error, since main is of type 'mainline'
			files = devStreamClient
					.mergeFiles(null, FileSpecBuilder
							.makeFileSpecList(new String[] { devTargetFile,
									devTargetFile2, devTargetFile3 }),
							new MergeFilesOptions().setChangelistId(changelist
									.getId()).setStream(mainStream));
			assertNotNull(files);
			assertEquals(1, files.size());
			assertNotNull(files.get(0));
			assertEquals(FileSpecOpStatus.ERROR, files.get(0).getOpStatus());
			assertEquals("Stream '" + mainStream
					+ "' has no parent, therefore (command not allowed).", files
					.get(0).getStatusMessage());

			// Since the specification of a mainline stream is not allowed, we
			// will need to add the "-r" flag along with the development stream
			// to reverse the direction of the merge source.
			files = devStreamClient.mergeFiles(null, FileSpecBuilder
					.makeFileSpecList(new String[] { devTargetFile,
							devTargetFile2, devTargetFile3 }),
					new MergeFilesOptions().setChangelistId(changelist.getId())
							.setReverseMapping(true).setStream(devStream));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Check the stream integration status.
			IStreamIntegrationStatus integrationStatus = server
					.getStreamIntegrationStatus(devStream,
							new StreamIntegrationStatusOptions()
									.setBidirectional(true)
									.setForceUpdate(true));
			assertNotNull(integrationStatus);
			assertTrue(integrationStatus.isChangeFlowsFromParent());
			assertTrue(integrationStatus.isChangeFlowsToParent());
			assertFalse(integrationStatus.isFirmerThanParent());
			assertTrue(integrationStatus.isIntegFromParent());
			assertEquals(integrationStatus.getIntegFromParentHow(), "merge");
			assertTrue(integrationStatus.isIntegToParent());
			assertEquals(integrationStatus.getIntegToParentHow(), "copy");

			// Retrieve the revision history ('filelog') of the copied files
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
				assertTrue(revisionIntegrationDataList.size() > 0);

				// Verify the integ stream file revision data
				IRevisionIntegrationData revisionIntegrationData = revisionIntegrationDataList
						.get(0);
				assertNotNull(revisionIntegrationData);
				assertEquals(-1, revisionIntegrationData.getStartFromRev());
				assertEquals(1, revisionIntegrationData.getEndFromRev());
				assertEquals("branch from",
						revisionIntegrationData.getHowFrom());
				assertEquals(mainSourceFile,
						revisionIntegrationData.getFromFile());
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
								"Dev112_MergetStreamFilesTest delete submitted files");
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
								"Dev112_MergetStreamFilesTest delete submitted files");
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
