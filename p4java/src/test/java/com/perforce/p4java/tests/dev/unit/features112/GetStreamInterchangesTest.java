/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.server.GetInterchangesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 interchanges -S stream -P parent".
 */
@Jobs({ "job046697" })
@TestId("Dev112_GetStreamInterchangesTest")
public class GetStreamInterchangesTest extends P4JavaTestCase {

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
	 * Test "p4 interchanges -S stream -P parent".
	 */
	@Test
	public void testStreamInterchanges() {

		IClient devStreamClient = null;
		IClient mainStreamClient = null;

		IChangelist changelist = null;
		List<IFileSpec> files = null;

		String devStream = "//p4java_stream/dev";
		String mainStream = "//p4java_stream/main";

		int randNum = getRandomInt();
		String dir = "interchanges" + randNum;

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

			List<IChangelist> expectedChangelists = new ArrayList<IChangelist>();

			// Copy the main source file to main target
			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_GetStreamInterchangesTest copy files " + randNum);
			assertNotNull(changelist);
			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);
			expectedChangelists.add(changelist);

			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_GetStreamInterchangesTest copy files " + randNum);
			assertNotNull(changelist);
			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile2), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);
			expectedChangelists.add(changelist);

			changelist = getNewChangelist(server, mainStreamClient,
					"Dev112_GetStreamInterchangesTest copy files " + randNum);
			assertNotNull(changelist);
			changelist = mainStreamClient.createChangelist(changelist);
			files = mainStreamClient.copyFiles(new FileSpec(mainSourceFile),
					new FileSpec(mainTargetFile3), null,
					new CopyFilesOptions().setChangelistId(changelist.getId()));
			assertNotNull(files);
			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);
			expectedChangelists.add(changelist);

			// Get a list interchanges
			List<IChangelist> returnedChangelists = server.getInterchanges(
					null, null,
					new GetInterchangesOptions().setReverseMapping(true).setStream(devStream));
			assertNotNull(returnedChangelists);

			// Verify the expected changelists are in the returned list of
			// interchanges, since the streams files are not yet integrated from
			// the source stream to the target stream.
			for (IChangelist exp : expectedChangelists) {
				boolean found = false;
				for (IChangelist change : returnedChangelists) {
					if (exp.getId() == change.getId()) {
						found = true;
					}
				}
				// Should be found
				assertTrue(found);
			}

			// Merge-down the new file from the main stream to the dev stream.
			server.setCurrentClient(devStreamClient);
			changelist = getNewChangelist(server, devStreamClient,
					"Dev112_GetStreamInterchangesTest integ files");
			assertNotNull(changelist);
			changelist = devStreamClient.createChangelist(changelist);

			// Since the specification of a mainline stream is not allowed, we
			// will need to add the "-r" flag along with the development stream
			// to reverse the direction of the merge source.
			files = devStreamClient.integrateFiles(
					null,
					FileSpecBuilder.makeFileSpecList(new String[] {
							devTargetFile, devTargetFile2, devTargetFile3 }),
					new IntegrateFilesOptions().setChangelistId(
							changelist.getId()).setReverseMapping(true).setStream(devStream));
			assertNotNull(files);

			changelist.refresh();
			files = changelist.submit(new SubmitOptions());
			assertNotNull(files);

			// Get a list interchanges
			returnedChangelists = server.getInterchanges(
					null, null, null,
					new GetInterchangesOptions().setReverseMapping(true).setStream(devStream));
			assertNotNull(returnedChangelists);

			// Verify the expected changelists are NOT in the returned list of
			// interchanges, since the stream files in the expected changeslists
			// had been integrated from the source stream to the target stream
			for (IChangelist exp : expectedChangelists) {
				boolean found = false;
				for (IChangelist change : returnedChangelists) {
					if (exp.getId() == change.getId()) {
						found = true;
					}
				}
				// Should NOT be found
				assertFalse(found);
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
								"Dev112_GetStreamInterchangesTest delete submitted files");
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
								"Dev112_GetStreamInterchangesTest delete submitted files");
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
