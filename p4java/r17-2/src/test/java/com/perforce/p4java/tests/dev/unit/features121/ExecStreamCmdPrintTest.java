/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features121;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test 'p4 print' command, should return null if file not found.
 */
@Jobs({ "job053606" })
@TestId("Dev121_ExecStreamCmdPrintTest")
public class ExecStreamCmdPrintTest extends P4JavaTestCase {

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
	 * Test 'p4 print' stream command, should return 'no file(s) found' if file not found.
	 */
	@Test
	public void testPrintStreamingCommand() {

		int randNum = getRandomInt();
		String nonExistingDepotFile = "//depot/non-existing-file-" + randNum + ".txt";

		String [] depotFiles = new String[] {"//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdServerCommands.java",
			nonExistingDepotFile, "//depot/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdMiscCommands.java" };

		InputStream is = null;
		
		try {
			is = server.execStreamCmd("print", depotFiles);
			assertNotNull(is);
			/* TODO: modify behaviour to return null and change the assert */
			is = server.execStreamCmd("print", new String[] { nonExistingDepotFile });
			assertEquals(nonExistingDepotFile + " - no such file(s).",
					IOUtils.toString(is, "UTF-8").trim());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Can't do anyting else here...
				}
			}
		}
	}
}
