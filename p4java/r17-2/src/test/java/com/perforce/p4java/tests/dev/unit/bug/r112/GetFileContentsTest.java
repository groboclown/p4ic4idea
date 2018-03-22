/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r112;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
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
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 print -a' command. Retrieve the contents of a depot file to the
 * client's standard output. The file is not synced. If file is specified using
 * client syntax, Perforce uses the client view to determine the corresponding
 * depot file.
 * 
 * The "-a" flag prints all revisions within the specified range, rather than
 * just the highest revision in the range.
 * 
 * Since "-q" is not used, the output will have the initial line that displays
 * the file name and revision.
 */
@Jobs({ "job042748" })
@TestId("Dev112_GetFileContentsTest")
public class GetFileContentsTest extends P4JavaTestCase {

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
			client = server.getClient("p4TestUserWS");
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
	 * When requesting all revisions of a given file, and setting the third
	 * argument "setNoHeaders" to false, which should cause this to work almost
	 * like "p4 print -a" no headers are returned between the files content.
	 * 
	 * When specifying a specific file revision in the first argument, such as
	 * "myfile.java#9", but setting the second argument "setAllRevs" to true,
	 * all revisions are still returned. I would suggest that the scope of the
	 * filespec should limit what could be returned. I think most devs would
	 * take "setAllRevs" to mean "all revs within the bounds of the specified
	 * filespec".
	 */
	@Test
	public void testGetFileContent() {

		// This file has 6 revs
		String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java";

		// Limit the file to a rev (up-to that rev)
		String depotFileRev = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java#3";

		try {
			// Test "p4 -a" with file header (file name, rev, etc)
			BufferedReader br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFile), true,
							false)));

			// Capture file headers
			List<String> headers = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			assertNotNull(headers);
			// Should contain at least 6 headers
			assertTrue(headers.size() >= 6);
			
			// Test "p4 -a" with revision number appended to the depot file 
			br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFileRev), true,
							false)));

			// Capture file headers
			headers = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			// Should contain 3 headers
			assertTrue(headers.size() == 3);
			
			// Test "p4 -a -q" suppressing file header
			br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFile), true,
							true)));

			// Capture file headers
			headers = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			// Should contain no headers
			assertTrue(headers.size() == 0);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
