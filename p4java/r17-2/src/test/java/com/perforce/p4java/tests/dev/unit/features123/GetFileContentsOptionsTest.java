/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

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
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test GetFileContentsOptions with the 'dontAnnotateFiles' option set to true.
 * If true, don't append revision specifiers (# and @) to the filespecs. By
 * default the filespecs passed to IOptionsServer.getFileContents() would get
 * revisions appended to them during parameter processing.
 */
@Jobs({ "job050047" })
@TestId("Dev112_GetFileContentsOptionsTest")
public class GetFileContentsOptionsTest extends P4JavaTestCase {

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
	 * Test GetFileContentsOptions with the 'dontAnnotateFiles' option set to true.
	 */
	@Test
	public void testGetFileContent() {

		// This file has at least 6 revs
		String depotFile = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java";

		// Limit the file to a rev (up-to that rev#)
		String depotFileRev = "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java#3";

		try {
			// Test "p4 -a" with file header (file name, rev, etc)
			BufferedReader br = new BufferedReader(new InputStreamReader(
					server.getFileContents(
							FileSpecBuilder.makeFileSpecList(depotFile),
							new GetFileContentsOptions().setAllrevs(true))));

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
							FileSpecBuilder.makeFileSpecList(depotFileRev),
							new GetFileContentsOptions().setAllrevs(true))));

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
			
			// Don't annotate files (filespecs input parameter)
			br = new BufferedReader(new InputStreamReader(
					server.getFileContents(FileSpecBuilder
							.makeFileSpecList(depotFileRev),
							new GetFileContentsOptions().setAllrevs(true)
									.setDontAnnotateFiles(true))));

			// Capture file headers
			headers = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.contains(depotFile)) {
					headers.add(line);
				}
			}
			br.close();
			// Should contain at least 6 headers, since we tell it not to append
			// any revision specifiers to the filespecs. 
			assertTrue(headers.size() >= 6);

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
