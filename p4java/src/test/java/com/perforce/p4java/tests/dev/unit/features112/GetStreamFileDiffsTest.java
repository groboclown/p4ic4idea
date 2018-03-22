/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test "p4 diff2 -S -P".
 */
@Jobs({ "job046695" })
@TestId("Dev112_GetStreamFileDiffsTest")
public class GetStreamFileDiffsTest extends P4JavaTestCase {

	IOptionsServer server = null;
	IClient client = null;

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
	 * Test "p4 diff2" with non-existing file paths
	 */
	@Test
	public void testNonExistingPaths() {
		IClient devStreamClient = null;

		String devStream = "//p4java_stream/dev";
	
		try {
			// Get the test dev stream client
			devStreamClient = server.getClient("p4java_stream_dev");
			assertNotNull(devStreamClient);

			// Set the dev stream client to the server.
			//server.setCurrentClient(devStreamClient);

			// Get the file diffs between 2 non-exiting paths
			List<IFileDiff> fileDiffs = server.getFileDiffs(new FileSpec("//p4java_stream/dev/abc "),
					new FileSpec("//p4java_stream/dev/xyz"),
					null, new GetFileDiffsOptions().setStream(devStream));
			
			assertNotNull(fileDiffs);
			assertTrue(fileDiffs.size() == 0);
			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Test "p4 diff2 -S -P".
	 */
	@Test
	public void testGetFileDiffs() {

		IClient devStreamClient = null;

		String devStream = "//p4java_stream/dev";
		String mainStream = "//p4java_stream/main";

		String mainSourceFile = mainStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_ja.properties";

		String devTargetFile = devStream
				+ "/core/GetOpenedFilesTest/src/gnu/getopt/MessagesBundle_ja.properties";

		try {
			// Get the test dev stream client
			devStreamClient = server.getClient("p4java_stream_dev");
			assertNotNull(devStreamClient);

			// Set the dev stream client to the server.
			server.setCurrentClient(devStreamClient);

			// Get the file diffs of a stream file in the dev and main streams
			List<IFileDiff> fileDiffs = server.getFileDiffs(new FileSpec(
					devTargetFile + "#3"), new FileSpec(mainSourceFile + "#1"),
					null, new GetFileDiffsOptions().setStream(devStream));
			assertNotNull(fileDiffs);
			assertEquals(1, fileDiffs.size());
			assertNotNull(fileDiffs.get(0));
			assertEquals(IFileDiff.Status.CONTENT, fileDiffs.get(0).getStatus());
			assertEquals(devTargetFile, fileDiffs.get(0).getDepotFile1());
			assertEquals(mainSourceFile, fileDiffs.get(0).getDepotFile2());
			assertEquals(3, fileDiffs.get(0).getRevision1());
			assertEquals(1, fileDiffs.get(0).getRevision2());
			assertEquals("text", fileDiffs.get(0).getFileType1());
			assertEquals("text", fileDiffs.get(0).getFileType2());

			InputStream is = server.getFileDiffsStream(new FileSpec(
					devTargetFile + "#3"), new FileSpec(mainSourceFile + "#1"),
					null, new GetFileDiffsOptions().setStream(devStream));
			assertNotNull(is);

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			// Capture diff lines
			List<String> lines = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();

			// Should contain at least one line of diff
			assertTrue(lines.size() > 0);

			// Verify the first line is a content diff header
			assertEquals("==== " + devTargetFile + "#3 (text) - "
					+ mainSourceFile + "#1 (text) ==== content", lines.get(0));

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
