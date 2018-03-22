/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features112;

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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 dirs -S stream' command.
 */
@Jobs({ "job046696" })
@TestId("Dev112_GetStreamDirectoriesTest")
public class GetStreamDirectoriesTest extends P4JavaTestCase {

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
	 * Test 'p4 dirs -S stream' command.
	 */
	@Test
	public void testGetStreamDirs() {

		IClient mainStreamClient = null;

		IClient devStreamClient = null;
		String devStream = "//p4java_stream/dev";

		try {
			// Get the test main stream client
			mainStreamClient = server.getClient(getPlatformClientName("p4java_stream_main"));
			assertNotNull(mainStreamClient);

			// Get the test dev stream client
			devStreamClient = server.getClient(getPlatformClientName("p4java_stream_dev"));
			assertNotNull(devStreamClient);

			// Set the dev stream client to the server.
			server.setCurrentClient(devStreamClient);

			// Use a main stream directory
			String dir = mainStreamClient.getRoot()
					+ "/newtestbranch/readonly/*";

			List<IFileSpec> fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get an error message about the main stream directory not
			// in the current stream view (dev stream)
			assertEquals(1, fileSpecs.size());
			assertNotNull(fileSpecs.get(0));
			assertEquals(FileSpecOpStatus.ERROR, fileSpecs.get(0).getOpStatus());
			assertEquals("Path '" + dir + "/...' is not under client's root '"
					+ devStreamClient.getRoot() + "'.", fileSpecs.get(0)
					.getStatusMessage());

			// Use a regular depot directory
			dir = "//depot/*";

			fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get an error message about //depot/* is not there,
			// since is not in the dev stream view
			assertEquals(1, fileSpecs.size());
			assertNotNull(fileSpecs.get(0));
			assertEquals(FileSpecOpStatus.ERROR, fileSpecs.get(0).getOpStatus());
			assertEquals(dir + " - no such file(s).", fileSpecs.get(0)
					.getStatusMessage());
			
			// Use a dev stream directory
			dir = devStreamClient.getRoot() + "/newtestbranch/readonly/*";

			fileSpecs = server.getDirectories(
					FileSpecBuilder.makeFileSpecList(dir),
					new GetDirectoriesOptions().setStream(devStream));
			assertNotNull(fileSpecs);

			// Should get some directories
			assertTrue(fileSpecs.size() > 0);

			// The expected directories, but these might change in the future...
			String[] expectedDirs = new String[] {
					"newtestbranch/readonly/grep",
					"newtestbranch/readonly/labelsync",
					"newtestbranch/readonly/list",
					"newtestbranch/readonly/sync" };

			// All of the expected directories should be in the returned list.
			// Note, this may fail in the future if the dirs are removed.
			for (String exp : expectedDirs) {
				boolean found = false;
				for (IFileSpec fs : fileSpecs) {
					if (fs.getPreferredPathString().contains(exp)) {
						found = true;
						break;
					}
				}
				assertTrue("Directory '" + exp
						+ "' not found in the returned directories.", found);
			}

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
